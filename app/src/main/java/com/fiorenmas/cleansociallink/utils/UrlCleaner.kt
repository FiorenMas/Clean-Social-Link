package com.fiorenmas.cleansociallink.utils

import android.net.Uri
import com.fiorenmas.cleansociallink.socials.Facebook
import com.fiorenmas.cleansociallink.socials.Instagram
import com.fiorenmas.cleansociallink.socials.Reddit
import com.fiorenmas.cleansociallink.socials.Threads
import com.fiorenmas.cleansociallink.socials.TikTok
import com.fiorenmas.cleansociallink.socials.X
import java.util.regex.Pattern

object UrlCleaner {
    private val URL_PATTERN: Pattern = Pattern.compile("https?://\\S+")
    private val TRAILING_PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '>', '"', '\'')
    internal val handlers: List<SocialHandler> = listOf(Facebook, TikTok, Threads, Instagram, Reddit, X)
    const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36"

    fun extractUrl(text: String): String? {
        val matcher = URL_PATTERN.matcher(text)
        return if (matcher.find()) matcher.group().trimEnd(*TRAILING_PUNCTUATION) else null
    }

    fun isSupportedUrl(url: String): Boolean {
        val host = Uri.parse(url).host?.lowercase() ?: return false
        return SupportedDomains.isSupported(host)
    }

    fun findHandler(url: String): SocialHandler? = handlers.find { it.matchesHost(url) }

    fun cleanUrl(url: String): String {
        return findHandler(url)?.cleanUrl(url) ?: defaultClean(url)
    }

    fun needsRedirectResolve(url: String): Boolean {
        return findHandler(url)?.needsRedirectResolve(url) ?: false
    }

    fun isValidRedirect(redirectUrl: String, originalUrl: String): Boolean {
        val originalHandler = findHandler(originalUrl) ?: return false
        return originalHandler.isValidRedirect(redirectUrl, originalUrl)
    }

    private fun defaultClean(url: String): String {
        val uri = Uri.parse(url)
        return uri.buildUpon().clearQuery().fragment(null).build().toString().trimEnd('/') + "/"
    }
}

