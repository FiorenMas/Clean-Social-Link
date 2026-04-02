package com.fiorenmas.cleansociallink.socials

import android.net.Uri
import com.fiorenmas.cleansociallink.utils.SocialHandler
import java.util.Locale

object TikTok : SocialHandler {
    override val hosts = setOf(
        "tiktok.com", "www.tiktok.com", "m.tiktok.com",
        "vt.tiktok.com", "vm.tiktok.com"
    )

    private val shortHosts = setOf("vt.tiktok.com", "vm.tiktok.com")

    override fun needsRedirectResolve(url: String): Boolean {
        val host = Uri.parse(url).host?.lowercase() ?: return false
        return shortHosts.any { host == it }
    }

    override fun cleanUrl(url: String): String {
        val uri = Uri.parse(url)
        return uri.buildUpon().clearQuery().fragment(null).build().toString().trimEnd('/')
    }

    override fun isValidRedirect(url: String, originalUrl: String): Boolean {
        if (url == originalUrl) return false
        val host = Uri.parse(url).host?.lowercase() ?: return false
        if (!hosts.any { host == it }) return false
        if (shortHosts.any { host == it }) return false
        val path = Uri.parse(url).path?.lowercase() ?: return false
        if (path.contains("/login")) return false
        return path.length > 1
    }

    fun isTikTokUrl(url: String): Boolean {
        val host = Uri.parse(url).host?.lowercase(Locale.ROOT).orEmpty()
        return host == "tiktok.com" ||
            host == "www.tiktok.com" ||
            host == "m.tiktok.com" ||
            host == "vt.tiktok.com" ||
            host == "vm.tiktok.com"
    }

    fun buildPreviewFetchCandidates(pageUrl: String): List<String> {
        val candidates = linkedSetOf(pageUrl)
        if (!isTikTokUrl(pageUrl)) return candidates.toList()

        val parsed = try {
            Uri.parse(pageUrl)
        } catch (_: Throwable) {
            null
        } ?: return candidates.toList()

        val path = parsed.encodedPath.orEmpty()
        if (path.isNotBlank() && !path.equals("/", ignoreCase = true)) {
            candidates.add("https://www.tiktok.com$path")
        }
        return candidates.toList()
    }

    fun isLikelyPlaceholderPreviewImage(imageUrl: String): Boolean {
        val lower = imageUrl.lowercase(Locale.ROOT)
        return lower.contains("default") ||
            lower.contains("/favicon") ||
            lower.contains("/icon") ||
            lower.contains("tiktok.com/static")
    }

    fun isUsablePreviewImage(imageUrl: String): Boolean {
        if (imageUrl.isBlank()) return false
        val lower = imageUrl.lowercase(Locale.ROOT)
        if (isLikelyPlaceholderPreviewImage(lower)) return false
        val isLikelyMediaHost =
            lower.contains("tiktokcdn.com") ||
                lower.contains("byteimg.com") ||
                lower.contains("muscdn.com") ||
                lower.contains("p16-sign")
        return isLikelyMediaHost || (lower.startsWith("http://") || lower.startsWith("https://"))
    }
}

