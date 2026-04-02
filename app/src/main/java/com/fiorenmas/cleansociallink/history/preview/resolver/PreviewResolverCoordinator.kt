package com.fiorenmas.cleansociallink.history.preview.resolver

import com.fiorenmas.cleansociallink.socials.Facebook
import com.fiorenmas.cleansociallink.socials.Instagram
import com.fiorenmas.cleansociallink.socials.Reddit
import com.fiorenmas.cleansociallink.socials.TikTok
import com.fiorenmas.cleansociallink.socials.Threads
import com.fiorenmas.cleansociallink.socials.X

object PreviewResolverCoordinator {
    fun resolvePreviewImage(pageUrl: String): String? {
        val isFacebook = Facebook.isFacebookUrl(pageUrl)
        val isInstagram = Instagram.isInstagramUrl(pageUrl)
        val isThreads = Threads.isThreadsUrl(pageUrl)
        val isReddit = Reddit.isRedditUrl(pageUrl)
        val isTikTok = TikTok.isTikTokUrl(pageUrl)
        val isX = X.isXUrl(pageUrl)

        if (isInstagram) {
            InstagramResolver.resolveFromOEmbed(pageUrl)?.let { return it }
        }

        val candidates = when {
            isFacebook -> Facebook.buildPreviewFetchCandidates(pageUrl)
            isInstagram -> Instagram.buildPreviewFetchCandidates(pageUrl)
            isThreads -> Threads.buildPreviewFetchCandidates(pageUrl)
            isReddit -> Reddit.buildPreviewFetchCandidates(pageUrl)
            isTikTok -> TikTok.buildPreviewFetchCandidates(pageUrl)
            isX -> X.buildPreviewFetchCandidates(pageUrl)
            else -> listOf(pageUrl)
        }

        for (candidate in candidates) {
            if (isFacebook) {
                FacebookResolver.resolveForCandidate(candidate)?.let { return it }
                continue
            }
            if (isThreads) {
                ThreadsResolver.resolveForCandidate(candidate)?.let { return it }
                continue
            }
            if (isReddit) {
                RedditResolver.resolveForCandidate(candidate)?.let { return it }
                continue
            }
            if (isTikTok) {
                TikTokResolver.resolveFromOEmbed(candidate)?.let { return it }
            }
            if (isX) {
                XResolver.resolveForCandidate(candidate)?.let { return it }
                continue
            }

            ResolverCore.resolveByHtmlFetch(candidate)?.let { return it }
        }

        return null
    }
}

