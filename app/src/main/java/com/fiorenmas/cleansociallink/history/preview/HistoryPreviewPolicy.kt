package com.fiorenmas.cleansociallink.history.preview

import com.fiorenmas.cleansociallink.history.data.HistoryEntry
import com.fiorenmas.cleansociallink.socials.Reddit
import com.fiorenmas.cleansociallink.socials.TikTok
import com.fiorenmas.cleansociallink.socials.Threads
import com.fiorenmas.cleansociallink.socials.X
import java.util.Locale

object HistoryPreviewPolicy {
    fun shouldFetchPreviewImage(entry: HistoryEntry): Boolean {
        if (entry.metadata.image.isNullOrBlank()) return true
        return shouldForceRefreshPreview(entry)
    }

    fun shouldForceRefreshPreview(entry: HistoryEntry): Boolean {
        val domain = entry.metadata.domain.lowercase(Locale.ROOT)
        val image = entry.metadata.image?.lowercase(Locale.ROOT).orEmpty()
        if (domain.contains("facebook.com")) {
            return image.isBlank() ||
                !image.contains("fbcdn") ||
                image.contains("static.xx.fbcdn.net/rsrc.php") ||
                image.contains("s40x40") ||
                image.contains("s50x50") ||
                image.contains("s80x80")
        }
        if (domain.contains("instagram.com")) {
            if (image.isBlank()) return true
            val isStaticOrIcon =
                image.contains("static.cdninstagram.com") ||
                    image.contains("/icons/") ||
                    image.contains("/favicon") ||
                    image.contains("instagram.com/static/") ||
                    image.endsWith(".svg")
            if (isStaticOrIcon) return true

            val isLikelyPostMedia =
                image.contains("scontent.cdninstagram.com") ||
                    image.contains("cdninstagram.com") ||
                    image.contains("fbcdn.net") ||
                    image.contains("instagram.")
            if (isLikelyPostMedia) return false

            return image.contains("/icons/") ||
                image.contains("/favicon")
        }
        if (domain.contains("threads.com")) {
            if (image.isBlank()) return true
            if (Threads.isLikelyPlaceholderPreviewImage(image)) return true
            val isLikelyMedia =
                image.contains("scontent.") ||
                    image.contains("cdninstagram.com") ||
                    image.contains("fbcdn.net") ||
                    image.contains("instagram.")
            return !isLikelyMedia
        }
        if (domain.contains("reddit.com")) {
            if (image.isBlank()) return true
            if (Reddit.isLikelyPlaceholderPreviewImage(image)) return true
            val isLikelyRedditMedia =
                image.contains("preview.redd.it") ||
                    image.contains("i.redd.it") ||
                    image.contains("external-preview.redd.it") ||
                    image.contains("redditmedia.com")
            return !isLikelyRedditMedia
        }
        if (domain.contains("tiktok.com")) {
            if (image.isBlank()) return true
            if (TikTok.isLikelyPlaceholderPreviewImage(image)) return true
            val isLikelyTikTokMedia =
                image.contains("tiktokcdn.com") ||
                    image.contains("muscdn.com") ||
                    image.contains("byteimg.com") ||
                    image.contains("p16-sign")
            return !isLikelyTikTokMedia
        }
        if (domain.contains("x.com") || domain.contains("twitter.com")) {
            if (image.isBlank()) return true
            if (X.isLikelyPlaceholderPreviewImage(image)) return true
            val isLikelyXMedia =
                image.contains("pbs.twimg.com") ||
                    image.contains("twimg.com")
            return !isLikelyXMedia
        }
        return false
    }
}
