package com.fiorenmas.cleansociallink.history.preview.resolver

object FacebookResolver {
    fun resolveForCandidate(pageUrl: String): String? {
        return ResolverCore.resolveByHtmlFetch(pageUrl)
    }
}

