package com.fiorenmas.cleansociallink.history.data

import android.content.Context
import android.net.Uri
import com.fiorenmas.cleansociallink.history.preview.HistoryPreviewPolicy
import com.fiorenmas.cleansociallink.history.preview.HistoryPreviewResolver
import com.fiorenmas.cleansociallink.utils.preferences.Prefs
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.Date
import java.util.concurrent.Executors
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet

object HistoryStorage {
    private const val PREFS_NAME = "history_storage"
    private const val KEY_ITEMS = "items"

    private const val KEY_ID = "id"
    private const val KEY_CLEAN_URL = "clean_url"
    private const val KEY_STORED_AT_EPOCH = "stored_at_epoch"
    private const val KEY_STORED_AT_LOCAL = "stored_at_local"
    private const val KEY_METADATA = "metadata"
    private const val KEY_META_IMAGE = "image"
    private const val KEY_META_TEXT = "text"
    private const val KEY_META_DOMAIN = "domain"
    private const val PREVIEW_RETRY_COOLDOWN_MS = 20_000L
    private val changeListeners = CopyOnWriteArraySet<() -> Unit>()
    private val storageLock = Any()
    private val previewLock = Any()
    private val previewExecutor = Executors.newFixedThreadPool(2)
    private val previewInFlight = ConcurrentHashMap.newKeySet<String>()
    private val previewLastAttemptAt = ConcurrentHashMap<String, Long>()

    fun addOnHistoryChangedListener(listener: () -> Unit) {
        changeListeners.add(listener)
    }

    fun removeOnHistoryChangedListener(listener: () -> Unit) {
        changeListeners.remove(listener)
    }

    fun saveCleanedUrl(context: Context, cleanUrl: String, rawText: String? = null) {
        if (!Prefs.isHistoryEnabled(context)) return

        val now = System.currentTimeMillis()
        val uri = Uri.parse(cleanUrl)
        val domain = uri.host?.lowercase(Locale.ROOT).orEmpty()

        val entry = HistoryEntry(
            id = UUID.randomUUID().toString(),
            cleanUrl = cleanUrl,
            storedAtEpochMs = now,
            storedAtLocalText = formatLocalTime(now),
            metadata = HistoryMetadata(
                image = extractImageUrl(uri),
                text = (rawText?.trim().takeUnless { it.isNullOrBlank() } ?: buildMetaText(uri, cleanUrl)),
                domain = domain
            )
        )

        synchronized(storageLock) {
            val items = readAll(context).toMutableList()
            items.add(0, entry)
            val cleaned = applyRetention(items, Prefs.getHistoryRetention(context))
            writeAll(context, cleaned)
        }

        if (entry.metadata.image == null) {
            fetchPreviewImageAsync(context.applicationContext, entry.id, cleanUrl)
        }
    }

    fun getAll(context: Context): List<HistoryEntry> {
        val sorted = synchronized(storageLock) {
            val raw = readAll(context)
            val cleaned = applyRetention(raw, Prefs.getHistoryRetention(context))
            if (cleaned.size != raw.size) {
                writeAll(context, cleaned)
            }
            cleaned.sortedByDescending { it.storedAtEpochMs }
        }
        sorted.filter { shouldFetchPreviewImage(it) }
            .take(8)
            .forEach { entry ->
                fetchPreviewImageAsync(
                    context = context.applicationContext,
                    entryId = entry.id,
                    pageUrl = entry.cleanUrl,
                    forceReplace = HistoryPreviewPolicy.shouldForceRefreshPreview(entry)
                )
            }
        return sorted
    }

    fun deleteByIds(context: Context, ids: Set<String>) {
        if (ids.isEmpty()) return
        synchronized(storageLock) {
            val filtered = readAll(context).filterNot { ids.contains(it.id) }
            writeAll(context, filtered)
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun readAll(context: Context): List<HistoryEntry> {
        val raw = prefs(context).getString(KEY_ITEMS, null).orEmpty()
        if (raw.isBlank()) return emptyList()

        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    fromJson(obj)?.let(::add)
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun writeAll(context: Context, items: List<HistoryEntry>) {
        val array = JSONArray()
        items.forEach { array.put(toJson(it)) }
        val saved = prefs(context).edit().putString(KEY_ITEMS, array.toString()).commit()
        if (saved) {
            notifyHistoryChanged()
        }
    }

    private fun applyRetention(items: List<HistoryEntry>, retention: Int): List<HistoryEntry> {
        val days = when (retention) {
            Prefs.HISTORY_RETENTION_7_DAYS -> 7L
            Prefs.HISTORY_RETENTION_30_DAYS -> 30L
            Prefs.HISTORY_RETENTION_90_DAYS -> 90L
            Prefs.HISTORY_RETENTION_1_YEAR -> 365L
            else -> null
        } ?: return items

        val cutoff = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L)
        return items.filter { it.storedAtEpochMs >= cutoff }
    }

    private fun formatLocalTime(timeMs: Long): String {
        val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
        return formatter.format(Date(timeMs))
    }

    private fun buildMetaText(uri: Uri, fallback: String): String {
        val path = uri.path.orEmpty().trim('/')
        return if (path.isNotBlank()) path else fallback
    }

    private fun extractImageUrl(uri: Uri): String? {
        val imageParam = uri.getQueryParameter("image")
            ?: uri.getQueryParameter("img")
            ?: uri.getQueryParameter("thumbnail")
            ?: uri.getQueryParameter("thumb")
        return imageParam?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }

    private fun fetchPreviewImageAsync(
        context: Context,
        entryId: String,
        pageUrl: String,
        forceReplace: Boolean = false
    ) {
        val shouldSchedule = synchronized(previewLock) {
            val now = System.currentTimeMillis()
            val lastAttempt = previewLastAttemptAt[entryId] ?: 0L
            if (now - lastAttempt < PREVIEW_RETRY_COOLDOWN_MS) {
                false
            } else if (!previewInFlight.add(entryId)) {
                false
            } else {
                previewLastAttemptAt[entryId] = now
                true
            }
        }
        if (!shouldSchedule) return

        previewExecutor.execute {
            try {
                val imageUrl = fetchPreviewImageFromPage(pageUrl) ?: return@execute

                synchronized(storageLock) {
                    val items = readAll(context).toMutableList()
                    val index = items.indexOfFirst { it.id == entryId }
                    if (index < 0) return@synchronized

                    val target = items[index]
                    if (!forceReplace && !target.metadata.image.isNullOrBlank()) return@synchronized
                    if (target.metadata.image == imageUrl) return@synchronized

                    items[index] = target.copy(
                        metadata = target.metadata.copy(image = imageUrl)
                    )
                    writeAll(context, items)
                }
            } finally {
                previewInFlight.remove(entryId)
            }
        }
    }

    private fun shouldFetchPreviewImage(entry: HistoryEntry): Boolean =
        HistoryPreviewPolicy.shouldFetchPreviewImage(entry)

    private fun fetchPreviewImageFromPage(pageUrl: String): String? {
        return HistoryPreviewResolver.resolvePreviewImage(pageUrl)
    }

    private fun notifyHistoryChanged() {
        changeListeners.forEach { listener ->
            try {
                listener()
            } catch (_: Throwable) {
            }
        }
    }

    private fun toJson(entry: HistoryEntry): JSONObject {
        return JSONObject().apply {
            put(KEY_ID, entry.id)
            put(KEY_CLEAN_URL, entry.cleanUrl)
            put(KEY_STORED_AT_EPOCH, entry.storedAtEpochMs)
            put(KEY_STORED_AT_LOCAL, entry.storedAtLocalText)
            put(KEY_METADATA, JSONObject().apply {
                put(KEY_META_IMAGE, entry.metadata.image)
                put(KEY_META_TEXT, entry.metadata.text)
                put(KEY_META_DOMAIN, entry.metadata.domain)
            })
        }
    }

    private fun fromJson(obj: JSONObject): HistoryEntry? {
        val id = obj.optString(KEY_ID).trim()
        val cleanUrl = obj.optString(KEY_CLEAN_URL).trim()
        val epoch = obj.optLong(KEY_STORED_AT_EPOCH, -1L)
        val local = obj.optString(KEY_STORED_AT_LOCAL).trim()
        val metaObj = obj.optJSONObject(KEY_METADATA)

        if (id.isBlank() || cleanUrl.isBlank() || epoch <= 0L || local.isBlank() || metaObj == null) return null

        val text = metaObj.optString(KEY_META_TEXT).trim().ifBlank { cleanUrl }
        val domain = metaObj.optString(KEY_META_DOMAIN).trim()
        val image = metaObj.opt(KEY_META_IMAGE)?.toString()?.trim()
            ?.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }

        return HistoryEntry(
            id = id,
            cleanUrl = cleanUrl,
            storedAtEpochMs = epoch,
            storedAtLocalText = local,
            metadata = HistoryMetadata(
                image = image,
                text = text,
                domain = domain
            )
        )
    }
}

