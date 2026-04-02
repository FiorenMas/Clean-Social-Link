package com.fiorenmas.cleansociallink.socials

import android.net.Uri
import com.fiorenmas.cleansociallink.utils.SocialHandler
import java.util.Locale

object X : SocialHandler {
    override val hosts = setOf(
        "x.com",
        "www.x.com",
        "twitter.com",
        "www.twitter.com",
        "mobile.twitter.com",
        "m.twitter.com",
        "t.co"
    )

    private val shortHosts = setOf("t.co")

    override fun needsRedirectResolve(url: String): Boolean {
        val host = Uri.parse(url).host?.lowercase(Locale.ROOT).orEmpty()
        return shortHosts.contains(host)
    }

    override fun cleanUrl(url: String): String {
        val uri = Uri.parse(url)
        val host = uri.host?.lowercase(Locale.ROOT).orEmpty()
        val canonicalHost = when (host) {
            "twitter.com", "www.twitter.com", "mobile.twitter.com", "m.twitter.com" -> "x.com"
            else -> host
        }

        return uri.buildUpon()
            .scheme("https")
            .encodedAuthority(canonicalHost)
            .clearQuery()
            .fragment(null)
            .build()
            .toString()
            .trimEnd('/')
    }

    override fun isValidRedirect(url: String, originalUrl: String): Boolean {
        if (url == originalUrl) return false

        val host = Uri.parse(url).host?.lowercase(Locale.ROOT).orEmpty()
        if (!hosts.contains(host) || shortHosts.contains(host)) return false

        val path = Uri.parse(url).path?.lowercase(Locale.ROOT).orEmpty()
        if (path.isBlank() || path == "/") return false
        if (path.contains("/login")) return false
        return true
    }

    fun isXUrl(url: String): Boolean {
        val host = Uri.parse(url).host?.lowercase(Locale.ROOT).orEmpty()
        return host == "x.com" ||
            host == "www.x.com" ||
            host == "twitter.com" ||
            host == "www.twitter.com" ||
            host == "mobile.twitter.com" ||
            host == "m.twitter.com"
    }

    fun buildPreviewFetchCandidates(pageUrl: String): List<String> {
        val candidates = linkedSetOf(pageUrl)
        if (!isXUrl(pageUrl)) return candidates.toList()

        val parsed = try {
            Uri.parse(pageUrl)
        } catch (_: Throwable) {
            null
        } ?: return candidates.toList()

        val path = parsed.encodedPath.orEmpty()
        if (path.isBlank() || path == "/") return candidates.toList()

        candidates.add("https://x.com$path")
        candidates.add("https://twitter.com$path")
        return candidates.toList()
    }

    fun extractStatusId(url: String): String? {
        val path = try {
            Uri.parse(url).path.orEmpty()
        } catch (_: Throwable) {
            return null
        }
        val match = Regex("/status/(\\d+)", RegexOption.IGNORE_CASE).find(path) ?: return null
        return match.groupValues.getOrNull(1)
    }

    fun isLikelyPlaceholderPreviewImage(imageUrl: String): Boolean {
        val lower = imageUrl.lowercase(Locale.ROOT)
        return lower.contains("abs.twimg.com") ||
            lower.contains("twimg.com/favicon") ||
            lower.contains("default_profile") ||
            lower.contains("/profile_images/") ||
            lower.contains("/profile_banners/") ||
            lower.contains("/emoji/")
    }

    fun isUsablePreviewImage(imageUrl: String): Boolean {
        if (imageUrl.isBlank()) return false
        val lower = imageUrl.lowercase(Locale.ROOT)
        if (isLikelyPlaceholderPreviewImage(lower)) return false

        val hasKnownImageExt =
            lower.contains(".jpg") ||
                lower.contains(".jpeg") ||
                lower.contains(".png") ||
                lower.contains(".webp") ||
                lower.contains(".avif")
        val isTwimgImage =
            lower.contains("pbs.twimg.com") ||
                lower.contains("twimg.com") ||
                lower.contains("format=jpg") ||
                lower.contains("format=jpeg") ||
                lower.contains("format=png") ||
                lower.contains("format=webp")
        return (hasKnownImageExt || isTwimgImage) &&
            (lower.startsWith("http://") || lower.startsWith("https://"))
    }
}
