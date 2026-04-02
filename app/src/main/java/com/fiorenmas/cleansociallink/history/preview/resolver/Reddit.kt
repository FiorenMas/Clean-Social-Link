package com.fiorenmas.cleansociallink.history.preview.resolver

import android.net.Uri
import com.fiorenmas.cleansociallink.socials.Reddit
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object RedditResolver {
    fun resolveForCandidate(pageUrl: String): String? {
        if (isRedditJsonCandidate(pageUrl)) {
            val json = ResolverCore.fetchText(pageUrl, "application/json,*/*") ?: return null
            return parseRedditPreviewImageFromJson(json)
        }

        val image = ResolverCore.resolveByHtmlFetch(pageUrl) ?: return null
        return image.takeIf { Reddit.isUsablePreviewImage(it) }
    }

    private fun isRedditJsonCandidate(url: String): Boolean {
        val path = Uri.parse(url).path.orEmpty().lowercase(Locale.ROOT)
        return path.endsWith(".json") || path.contains("/.json")
    }

    private fun parseRedditPreviewImageFromJson(rawJson: String): String? {
        val root = try {
            JSONArray(rawJson)
        } catch (_: Throwable) {
            return null
        }

        val postData = root.optJSONObject(0)
            ?.optJSONObject("data")
            ?.optJSONArray("children")
            ?.optJSONObject(0)
            ?.optJSONObject("data")
            ?: return null

        val previewSource = postData.optJSONObject("preview")
            ?.optJSONArray("images")
            ?.optJSONObject(0)
            ?.optJSONObject("source")
            ?.optString("url")
            .orEmpty()
        normalizePreviewUrl(previewSource)?.let { return it }

        val mediaMetadata = postData.optJSONObject("media_metadata")
        if (mediaMetadata != null) {
            val keys = mediaMetadata.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val mediaItem = mediaMetadata.optJSONObject(key) ?: continue
                val sourceUrl = mediaItem.optJSONObject("s")?.optString("u").orEmpty()
                normalizePreviewUrl(sourceUrl)?.let { return it }
            }
        }

        val crosspostData = postData.optJSONArray("crosspost_parent_list")?.optJSONObject(0)
        if (crosspostData != null) {
            extractRedditImageFromPostData(crosspostData)?.let { return it }
        }

        val thumbnail = postData.optString("thumbnail").orEmpty()
        normalizePreviewUrl(thumbnail)?.let { return it }

        val overridden = postData.optString("url_overridden_by_dest").orEmpty()
        normalizePreviewUrl(overridden)?.let { return it }

        return null
    }

    private fun extractRedditImageFromPostData(postData: JSONObject): String? {
        val previewSource = postData.optJSONObject("preview")
            ?.optJSONArray("images")
            ?.optJSONObject(0)
            ?.optJSONObject("source")
            ?.optString("url")
            .orEmpty()
        normalizePreviewUrl(previewSource)?.let { return it }

        val mediaMetadata = postData.optJSONObject("media_metadata")
        if (mediaMetadata != null) {
            val keys = mediaMetadata.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val mediaItem = mediaMetadata.optJSONObject(key) ?: continue
                val sourceUrl = mediaItem.optJSONObject("s")?.optString("u").orEmpty()
                normalizePreviewUrl(sourceUrl)?.let { return it }
            }
        }

        val thumbnail = postData.optString("thumbnail").orEmpty()
        return normalizePreviewUrl(thumbnail)
    }

    private fun normalizePreviewUrl(raw: String): String? {
        if (raw.isBlank()) return null
        val decoded = ResolverCore.decodeHtmlEntities(raw.trim())
        if (!decoded.startsWith("http://") && !decoded.startsWith("https://")) return null
        if (!Reddit.isUsablePreviewImage(decoded)) return null
        return decoded
    }
}

