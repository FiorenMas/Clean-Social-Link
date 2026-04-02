package com.fiorenmas.cleansociallink.history.preview.resolver

import com.fiorenmas.cleansociallink.socials.Threads
import com.fiorenmas.cleansociallink.utils.UrlCleaner

object ThreadsResolver {
    private const val THREADS_CRAWLER_UA =
        "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)"

    fun resolveForCandidate(pageUrl: String): String? {
        val userAgents = listOf(UrlCleaner.DESKTOP_UA, THREADS_CRAWLER_UA)
        for (ua in userAgents) {
            val image = ResolverCore.resolveByHtmlFetch(pageUrl, ua) ?: continue
            if (Threads.isUsablePreviewImage(image)) return image
        }
        return null
    }
}

