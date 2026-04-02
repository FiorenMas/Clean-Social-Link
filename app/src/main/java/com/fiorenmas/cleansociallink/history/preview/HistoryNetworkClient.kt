package com.fiorenmas.cleansociallink.history.preview

import android.net.Uri
import com.fiorenmas.cleansociallink.utils.UrlCleaner
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object HistoryNetworkClient {
    fun <T> get(
        url: String,
        accept: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
        userAgent: String = UrlCleaner.DESKTOP_UA,
        referer: String? = inferReferer(url),
        reader: (HttpURLConnection) -> T?
    ): T? {
        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (_: Throwable) {
            return null
        }

        return try {
            connection.instanceFollowRedirects = true
            connection.useCaches = false
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.requestMethod = "GET"
            connection.doInput = true
            connection.setRequestProperty("User-Agent", userAgent)
            connection.setRequestProperty("Accept", accept)
            connection.setRequestProperty("Accept-Language", Locale.getDefault().toLanguageTag())
            connection.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0")
            connection.setRequestProperty("Pragma", "no-cache")
            if (!referer.isNullOrBlank()) {
                connection.setRequestProperty("Referer", referer)
            }
            connection.connect()
            if (connection.responseCode !in 200..299) return null
            reader(connection)
        } catch (_: Throwable) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun inferReferer(url: String): String? {
        val host = Uri.parse(url).host?.lowercase(Locale.ROOT).orEmpty()
        return when {
            host.contains("threads.com") || host.contains("threads.net") ->
                "https://www.threads.com/"
            host.contains("instagram.com") || host.contains("cdninstagram.com") || host.contains("instagram.") ->
                "https://www.instagram.com/"
            host.contains("facebook.com") || host.contains("fbcdn.net") || host.contains("scontent.") ->
                "https://www.facebook.com/"
            host.contains("reddit.com") || host.contains("redd.it") || host.contains("redditmedia.com") ->
                "https://www.reddit.com/"
            host.contains("tiktok.com") || host.contains("tiktokcdn.com") || host.contains("muscdn.com") ->
                "https://www.tiktok.com/"
            host.contains("x.com") || host.contains("twitter.com") || host.contains("twimg.com") ->
                "https://x.com/"
            host.contains("fxtwitter.com") || host.contains("vxtwitter.com") || host.contains("fixupx.com") ->
                "https://x.com/"
            else -> null
        }
    }
}
