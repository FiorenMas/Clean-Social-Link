package com.fiorenmas.cleansociallink.socials

import android.net.Uri
import com.fiorenmas.cleansociallink.utils.SocialHandler
import java.util.Locale

object Reddit : SocialHandler {
    override val hosts = setOf("reddit.com", "www.reddit.com", "old.reddit.com", "m.reddit.com")

    override fun needsRedirectResolve(url: String): Boolean {
        val path = Uri.parse(url).path ?: return false
        return path.contains("/s/")
    }

    override fun cleanUrl(url: String): String {
        val uri = Uri.parse(url)
        return uri.buildUpon().clearQuery().fragment(null).build().toString().trimEnd('/')
    }

    override fun isValidRedirect(url: String, originalUrl: String): Boolean {
        if (url == originalUrl) return false
        val host = Uri.parse(url).host?.lowercase() ?: return false
        if (!hosts.any { host == it }) return false
        val path = Uri.parse(url).path ?: return false
        return !path.contains("/s/") && !path.contains("/login") && path.length > 1
    }

    fun isRedditUrl(url: String): Boolean {
        val host = Uri.parse(url).host?.lowercase(Locale.ROOT).orEmpty()
        return host == "reddit.com" ||
            host == "www.reddit.com" ||
            host == "old.reddit.com" ||
            host == "m.reddit.com"
    }

    fun buildPreviewFetchCandidates(pageUrl: String): List<String> {
        val candidates = linkedSetOf<String>()
        if (!isRedditUrl(pageUrl)) return listOf(pageUrl)

        val parsed = try {
            Uri.parse(pageUrl)
        } catch (_: Throwable) {
            null
        } ?: return listOf(pageUrl)

        val path = parsed.encodedPath.orEmpty().trimEnd('/')
        if (path.isBlank()) return listOf(pageUrl)

        if (path.endsWith(".json", ignoreCase = true)) {
            candidates.add("https://www.reddit.com$path?raw_json=1")
        } else {
            candidates.add("https://www.reddit.com$path/.json?raw_json=1")
            candidates.add("https://www.reddit.com$path.json?raw_json=1")
        }

        candidates.add("https://www.reddit.com$path")
        candidates.add("https://old.reddit.com$path")
        candidates.add(pageUrl)
        return candidates.toList()
    }

    fun isLikelyPlaceholderPreviewImage(imageUrl: String): Boolean {
        val lower = imageUrl.lowercase(Locale.ROOT)
        return lower.contains("redditstatic.com") ||
            lower.contains("styles.redditmedia.com") ||
            lower.contains("/icon.") ||
            lower.contains("/avatar")
    }

    fun isUsablePreviewImage(imageUrl: String): Boolean {
        if (imageUrl.isBlank()) return false
        val lower = imageUrl.lowercase(Locale.ROOT)
        if (isLikelyPlaceholderPreviewImage(lower)) return false
        val isLikelyMediaHost =
            lower.contains("preview.redd.it") ||
                lower.contains("i.redd.it") ||
                lower.contains("external-preview.redd.it") ||
                lower.contains("redditmedia.com")
        return isLikelyMediaHost || (lower.startsWith("http://") || lower.startsWith("https://"))
    }
}

