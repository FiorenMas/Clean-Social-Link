package com.fiorenmas.cleansociallink.history.preview.resolver

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object InstagramResolver {
    fun resolveFromOEmbed(pageUrl: String): String? {
        val encodedUrl = URLEncoder.encode(pageUrl, StandardCharsets.UTF_8.name())
        val endpoint = "https://www.instagram.com/api/v1/oembed/?url=$encodedUrl"
        val json = ResolverCore.fetchText(endpoint, "application/json,*/*") ?: return null
        val match = Regex("\"thumbnail_url\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(json)
        val rawUrl = match?.groupValues?.getOrNull(1).orEmpty()
        if (rawUrl.isBlank()) return null
        val decoded = ResolverCore.decodeHtmlEntities(rawUrl)
        return if (decoded.startsWith("http://") || decoded.startsWith("https://")) decoded else null
    }
}

