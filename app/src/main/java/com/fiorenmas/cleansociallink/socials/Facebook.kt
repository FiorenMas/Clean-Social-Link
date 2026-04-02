package com.fiorenmas.cleansociallink.socials

import android.net.Uri
import com.fiorenmas.cleansociallink.utils.SocialHandler
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

object Facebook : SocialHandler {
    override val hosts = setOf("facebook.com", "www.facebook.com", "m.facebook.com", "mbasic.facebook.com")

    override fun needsRedirectResolve(url: String): Boolean {
        val path = Uri.parse(url).path ?: return false
        return path.contains("/share/") || path.contains("/reel/s/")
    }

    override fun cleanUrl(url: String): String {
        val uri = Uri.parse(url)
        val path = uri.path.orEmpty()
        if (path.equals("/story.php", ignoreCase = true)) {
            val storyFbid = uri.getQueryParameter("story_fbid")?.trim().orEmpty()
            val id = uri.getQueryParameter("id")?.trim().orEmpty()

            val builder = uri.buildUpon()
                .clearQuery()
                .fragment(null)

            if (storyFbid.isNotBlank()) {
                builder.appendQueryParameter("story_fbid", storyFbid)
            }
            if (id.isNotBlank()) {
                builder.appendQueryParameter("id", id)
            }
            return builder.build().toString()
        }
        return uri.buildUpon().clearQuery().fragment(null).build().toString().trimEnd('/') + "/"
    }

    override fun isValidRedirect(url: String, originalUrl: String): Boolean {
        if (url == originalUrl) return false
        if (!matchesHost(url)) return false
        val path = Uri.parse(url).path ?: return false
        return !path.contains("/share/") && !path.contains("/login") && path.length > 1
    }

    fun isFacebookUrl(url: String): Boolean {
        val host = Uri.parse(url).host?.lowercase(Locale.ROOT).orEmpty()
        return host == "facebook.com" ||
            host == "www.facebook.com" ||
            host == "m.facebook.com" ||
            host == "mbasic.facebook.com"
    }

    fun buildPreviewFetchCandidates(pageUrl: String): List<String> {
        val candidates = linkedSetOf(pageUrl)
        if (!isFacebookUrl(pageUrl)) return candidates.toList()

        val parsed = try {
            Uri.parse(pageUrl)
        } catch (_: Throwable) {
            null
        } ?: return candidates.toList()

        val path = parsed.encodedPath.orEmpty()
        val query = parsed.encodedQuery?.let { "?$it" }.orEmpty()
        candidates.add("https://m.facebook.com$path$query")
        candidates.add("https://mbasic.facebook.com$path$query")

        val encodedOriginal = URLEncoder.encode(pageUrl, StandardCharsets.UTF_8.name())
        val postPluginUrls = listOf(
            "https://www.facebook.com/plugins/post.php?href=$encodedOriginal&show_text=true&width=500",
            "https://www.facebook.com/plugins/post.php?href=$encodedOriginal&show_text=false&width=500",
            "https://www.facebook.com/plugins/post.php?href=$encodedOriginal"
        )
        val videoPluginUrls = listOf(
            "https://www.facebook.com/plugins/video.php?href=$encodedOriginal&show_text=false&width=560",
            "https://www.facebook.com/plugins/video.php?href=$encodedOriginal"
        )

        val pathLower = parsed.path.orEmpty().lowercase(Locale.ROOT)
        val isPostPath = pathLower.contains("/posts/") ||
            pathLower.contains("/permalink/") ||
            pathLower.contains("/story.php")
        val ordered = if (isPostPath) postPluginUrls + videoPluginUrls else videoPluginUrls + postPluginUrls
        ordered.forEach(candidates::add)
        return candidates.toList()
    }

    fun extractPreviewImageFromHtml(html: String, pageUrl: String): String? {
        val imgTagRegex = Regex("<img\\s+[^>]*>", RegexOption.IGNORE_CASE)
        val attrRegex = Regex("([a-zA-Z:_-]+)\\s*=\\s*(['\"])(.*?)\\2", RegexOption.IGNORE_CASE)

        var bestUrl: String? = null
        var bestScore = Int.MIN_VALUE
        for (match in imgTagRegex.findAll(html)) {
            val attrs = mutableMapOf<String, String>()
            for (attr in attrRegex.findAll(match.value)) {
                attrs[attr.groupValues[1].lowercase(Locale.ROOT)] = attr.groupValues[3]
            }

            val raw = attrs["src"]
                ?: attrs["data-src"]
                ?: attrs["data-original"]
                ?: attrs["data-image"]
                ?: continue
            val src = decodeHtmlEntities(raw.trim())
            val resolved = resolveCandidateUrl(src, pageUrl) ?: continue
            if (!looksLikePreviewImage(resolved)) continue

            val score = scoreFacebookPreview(resolved)
            if (score > bestScore) {
                bestScore = score
                bestUrl = resolved
            }

            val srcSetRaw = attrs["srcset"].orEmpty()
            if (srcSetRaw.isNotBlank()) {
                val srcSetUrl = srcSetRaw.split(",")
                    .map { it.trim() }
                    .mapNotNull { candidate ->
                        val part = candidate.substringBefore(" ").trim()
                        if (part.isBlank()) null else decodeHtmlEntities(part)
                    }
                    .mapNotNull { resolveCandidateUrl(it, pageUrl) }
                    .firstOrNull { looksLikePreviewImage(it) }
                if (!srcSetUrl.isNullOrBlank()) {
                    val srcSetScore = scoreFacebookPreview(srcSetUrl)
                    if (srcSetScore > bestScore) {
                        bestScore = srcSetScore
                        bestUrl = srcSetUrl
                    }
                }
            }
        }
        return bestUrl
    }

    private fun resolveCandidateUrl(raw: String, pageUrl: String): String? {
        if (raw.isBlank()) return null
        return when {
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            raw.startsWith("//") -> "https:$raw"
            else -> try {
                URL(URL(pageUrl), raw).toString()
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun decodeHtmlEntities(value: String): String {
        return value
            .replace("\\/", "/")
            .replace("\\u0026", "&")
            .replace("&#x2F;", "/")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun looksLikePreviewImage(url: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        val hasKnownImageExt =
            lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".png") || lower.contains(".webp")
        val isFacebookCdnImage = lower.contains("scontent.") || lower.contains("fbcdn.net")
        if (!hasKnownImageExt && !isFacebookCdnImage) {
            return false
        }
        if (lower.contains("emoji") || lower.contains("sprite")) {
            return false
        }
        return true
    }

    private fun scoreFacebookPreview(url: String): Int {
        val lower = url.lowercase(Locale.ROOT)
        var score = 0
        if (lower.contains("scontent.")) score += 10
        if (lower.contains("fbcdn.net")) score += 4
        if (lower.contains(".jpg") || lower.contains(".jpeg")) score += 3
        if (lower.contains("static.xx.fbcdn.net/rsrc.php")) score -= 8
        if (lower.contains("s40x40") || lower.contains("s50x50") || lower.contains("s80x80")) score -= 6
        if (lower.contains("profile") || lower.contains("avatar")) score -= 4
        return score
    }
}

