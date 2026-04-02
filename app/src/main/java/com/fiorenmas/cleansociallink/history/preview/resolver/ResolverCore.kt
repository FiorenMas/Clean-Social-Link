package com.fiorenmas.cleansociallink.history.preview.resolver

import com.fiorenmas.cleansociallink.history.preview.HistoryNetworkClient
import com.fiorenmas.cleansociallink.socials.Facebook
import com.fiorenmas.cleansociallink.utils.UrlCleaner
import java.net.URL
import java.util.Locale

object ResolverCore {
    private const val FETCH_MAX_CHARS = 300_000
    private const val CONNECT_TIMEOUT_MS = 5000
    private const val READ_TIMEOUT_MS = 6000

    private val imageMetaKeys = setOf(
        "og:image",
        "og:image:url",
        "og:image:secure_url",
        "twitter:image",
        "twitter:image:src",
        "image"
    )

    fun fetchHtml(pageUrl: String, userAgent: String = UrlCleaner.DESKTOP_UA): String? {
        return fetchText(
            pageUrl = pageUrl,
            accept = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            userAgent = userAgent
        )
    }

    fun fetchText(
        pageUrl: String,
        accept: String,
        userAgent: String = UrlCleaner.DESKTOP_UA
    ): String? {
        return HistoryNetworkClient.get(
            url = pageUrl,
            accept = accept,
            connectTimeoutMs = CONNECT_TIMEOUT_MS,
            readTimeoutMs = READ_TIMEOUT_MS,
            userAgent = userAgent
        ) { connection ->
            connection.inputStream.bufferedReader().use { reader ->
                val sb = StringBuilder()
                val buffer = CharArray(4096)
                while (sb.length < FETCH_MAX_CHARS) {
                    val read = reader.read(buffer)
                    if (read <= 0) break
                    sb.append(buffer, 0, read)
                }
                sb.toString()
            }
        }
    }

    fun resolveByHtmlFetch(pageUrl: String, userAgent: String = UrlCleaner.DESKTOP_UA): String? {
        val html = fetchHtml(pageUrl, userAgent) ?: return null
        return extractImageUrlFromHtml(html, pageUrl)
    }

    fun extractImageUrlFromHtml(html: String, pageUrl: String): String? {
        if (Facebook.isFacebookUrl(pageUrl)) {
            Facebook.extractPreviewImageFromHtml(html, pageUrl)?.let { return it }
        }

        val metaTagRegex = Regex("<meta\\s+[^>]*>", RegexOption.IGNORE_CASE)
        val imgTagRegex = Regex("<img\\s+[^>]*>", RegexOption.IGNORE_CASE)
        val attrRegex = Regex("([a-zA-Z:_-]+)\\s*=\\s*(['\"])(.*?)\\2", RegexOption.IGNORE_CASE)

        for (match in metaTagRegex.findAll(html)) {
            val attrs = mutableMapOf<String, String>()
            for (attr in attrRegex.findAll(match.value)) {
                attrs[attr.groupValues[1].lowercase(Locale.ROOT)] = attr.groupValues[3]
            }
            val key = (attrs["property"] ?: attrs["name"] ?: attrs["itemprop"]).orEmpty().lowercase(Locale.ROOT)
            if (!imageMetaKeys.contains(key)) continue
            val content = decodeHtmlEntities(attrs["content"].orEmpty().trim())
            val resolved = resolveCandidateUrl(content, pageUrl)
            if (!resolved.isNullOrBlank()) return resolved
        }

        for (match in imgTagRegex.findAll(html)) {
            val attrs = mutableMapOf<String, String>()
            for (attr in attrRegex.findAll(match.value)) {
                attrs[attr.groupValues[1].lowercase(Locale.ROOT)] = attr.groupValues[3]
            }
            val src = decodeHtmlEntities(attrs["src"].orEmpty().trim())
            val resolved = resolveCandidateUrl(src, pageUrl) ?: continue
            if (looksLikePreviewImage(resolved)) return resolved
        }

        val scriptImageRegex =
            Regex("""https?:\\/\\/[^\\"']+\.(?:jpg|jpeg|png|webp)(?:[^\\"']*)""", RegexOption.IGNORE_CASE)
        val scriptMatch = scriptImageRegex.find(html)?.value
        if (!scriptMatch.isNullOrBlank()) {
            val unescaped = scriptMatch.replace("\\/", "/")
            resolveCandidateUrl(unescaped, pageUrl)?.let { return it }
        }

        return null
    }

    fun decodeHtmlEntities(value: String): String {
        return value
            .replace("\\/", "/")
            .replace("\\u0026", "&")
            .replace("&#x2F;", "/")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun resolveCandidateUrl(raw: String, pageUrl: String): String? {
        if (raw.isBlank()) return null
        return when {
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            raw.startsWith("//") -> "https:$raw"
            else -> try {
                URL(URL(pageUrl), raw).toString()
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun looksLikePreviewImage(url: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        val hasKnownImageExt =
            lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".png") || lower.contains(".webp")
        val isKnownCdnImage =
            lower.contains("scontent.cdninstagram.com") || lower.contains("cdninstagram.com") ||
                lower.contains("scontent.") || lower.contains("fbcdn.net") ||
                lower.contains("preview.redd.it") || lower.contains("i.redd.it") ||
                lower.contains("external-preview.redd.it") ||
                lower.contains("tiktokcdn.com") || lower.contains("muscdn.com") ||
                lower.contains("byteimg.com")
        if (!hasKnownImageExt && !isKnownCdnImage) {
            return false
        }
        if (lower.contains("emoji") || lower.contains("sprite")) return false
        return true
    }
}

