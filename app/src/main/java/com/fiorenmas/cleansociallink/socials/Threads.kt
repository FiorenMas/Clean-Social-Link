package com.fiorenmas.cleansociallink.socials

import android.net.Uri
import com.fiorenmas.cleansociallink.utils.SocialHandler
import java.util.Locale

object Threads : SocialHandler {
    override val hosts = setOf("threads.com", "www.threads.com")

    override fun needsRedirectResolve(url: String): Boolean = false

    override fun cleanUrl(url: String): String {
        val uri = Uri.parse(url)
        return uri.buildUpon().clearQuery().fragment(null).build().toString().trimEnd('/')
    }

    override fun isValidRedirect(url: String, originalUrl: String): Boolean = false

    fun isThreadsUrl(url: String): Boolean {
        val host = Uri.parse(url).host?.lowercase(Locale.ROOT).orEmpty()
        return host == "threads.com" || host == "www.threads.com"
    }

    fun buildPreviewFetchCandidates(pageUrl: String): List<String> {
        val candidates = linkedSetOf(pageUrl)
        if (!isThreadsUrl(pageUrl)) return candidates.toList()

        val parsed = try {
            Uri.parse(pageUrl)
        } catch (_: Throwable) {
            null
        } ?: return candidates.toList()

        val path = parsed.encodedPath.orEmpty()
        if (path.isNotBlank()) {
            candidates.add("https://www.threads.com$path")
            candidates.add("https://www.threads.com$path?hl=en")
        }
        return candidates.toList()
    }

    fun isLikelyPlaceholderPreviewImage(imageUrl: String): Boolean {
        val lower = imageUrl.lowercase(Locale.ROOT)
        return lower.contains("static.cdninstagram.com/rsrc.php") ||
            lower.contains("/favicon") ||
            lower.contains("threads.com/login")
    }

    fun isUsablePreviewImage(imageUrl: String): Boolean {
        if (imageUrl.isBlank()) return false
        val lower = imageUrl.lowercase(Locale.ROOT)
        if (isLikelyPlaceholderPreviewImage(lower)) return false

        val isLikelyMediaHost =
            lower.contains("scontent.") ||
                lower.contains("cdninstagram.com") ||
                lower.contains("fbcdn.net") ||
                lower.contains("instagram.")
        return isLikelyMediaHost || (lower.startsWith("http://") || lower.startsWith("https://"))
    }
}

