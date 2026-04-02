package com.fiorenmas.cleansociallink.history.preview.resolver

import com.fiorenmas.cleansociallink.socials.TikTok
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object TikTokResolver {
    fun resolveFromOEmbed(pageUrl: String): String? {
        val encodedUrl = URLEncoder.encode(pageUrl, StandardCharsets.UTF_8.name())
        val endpoint = "https://www.tiktok.com/oembed?url=$encodedUrl"
        val json = ResolverCore.fetchText(endpoint, "application/json,*/*") ?: return null
        val rawUrl = try {
            JSONObject(json).optString("thumbnail_url").orEmpty()
        } catch (_: Throwable) {
            Regex("\"thumbnail_url\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
                .find(json)
                ?.groupValues
                ?.getOrNull(1)
                .orEmpty()
        }
        if (rawUrl.isBlank()) return null
        val decoded = ResolverCore.decodeHtmlEntities(rawUrl)
        return decoded.takeIf { TikTok.isUsablePreviewImage(it) }
    }
}

