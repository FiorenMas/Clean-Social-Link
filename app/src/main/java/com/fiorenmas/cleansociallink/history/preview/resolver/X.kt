package com.fiorenmas.cleansociallink.history.preview.resolver

import com.fiorenmas.cleansociallink.socials.X
import com.fiorenmas.cleansociallink.utils.UrlCleaner
import org.json.JSONObject

object XResolver {
    private const val X_CRAWLER_UA = "Twitterbot/1.0"

    fun resolveForCandidate(pageUrl: String): String? {
        resolveFromFxTwitter(pageUrl)?.let { return it }

        val userAgents = listOf(UrlCleaner.DESKTOP_UA, X_CRAWLER_UA)
        for (ua in userAgents) {
            val image = ResolverCore.resolveByHtmlFetch(pageUrl, ua) ?: continue
            if (X.isUsablePreviewImage(image)) return image
        }

        resolveFromFxTwitter(pageUrl)?.let { return it }
        return null
    }

    private fun resolveFromFxTwitter(pageUrl: String): String? {
        val statusId = X.extractStatusId(pageUrl) ?: return null
        val endpoint = "https://api.fxtwitter.com/i/status/$statusId"
        val json = ResolverCore.fetchText(
            pageUrl = endpoint,
            accept = "application/json,*/*",
            userAgent = UrlCleaner.DESKTOP_UA
        ) ?: return null

        val root = try {
            JSONObject(json)
        } catch (_: Throwable) {
            return null
        }

        val mediaItems = root.optJSONObject("tweet")
            ?.optJSONObject("media")
            ?.optJSONArray("all")
            ?: return null

        for (i in 0 until mediaItems.length()) {
            val item = mediaItems.optJSONObject(i) ?: continue
            val thumbnailUrl = ResolverCore.decodeHtmlEntities(item.optString("thumbnail_url").orEmpty().trim())
            if (X.isUsablePreviewImage(thumbnailUrl)) return thumbnailUrl

            val mediaUrl = ResolverCore.decodeHtmlEntities(item.optString("url").orEmpty().trim())
            if (X.isUsablePreviewImage(mediaUrl)) return mediaUrl
        }

        return null
    }
}

