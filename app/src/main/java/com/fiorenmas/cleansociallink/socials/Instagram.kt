package com.fiorenmas.cleansociallink.socials

import android.net.Uri
import com.fiorenmas.cleansociallink.utils.SocialHandler
import java.util.Locale

object Instagram : SocialHandler {
    override val hosts = setOf("instagram.com", "www.instagram.com", "m.instagram.com")

    override fun needsRedirectResolve(url: String): Boolean = false

    override fun cleanUrl(url: String): String {
        val uri = Uri.parse(url)
        return uri.buildUpon().clearQuery().fragment(null).build().toString().trimEnd('/')
    }

    override fun isValidRedirect(url: String, originalUrl: String): Boolean = false

    fun isInstagramUrl(url: String): Boolean {
        val host = Uri.parse(url).host?.lowercase(Locale.ROOT).orEmpty()
        return host == "instagram.com" || host == "www.instagram.com" || host == "m.instagram.com"
    }

    fun buildPreviewFetchCandidates(pageUrl: String): List<String> {
        val candidates = linkedSetOf(pageUrl)
        if (!isInstagramUrl(pageUrl)) return candidates.toList()

        val parsed = try {
            Uri.parse(pageUrl)
        } catch (_: Throwable) {
            null
        } ?: return candidates.toList()

        val path = parsed.encodedPath.orEmpty()
        candidates.add("https://www.instagram.com$path")
        candidates.add("https://www.instagram.com$path?img_index=1")
        return candidates.toList()
    }
}

