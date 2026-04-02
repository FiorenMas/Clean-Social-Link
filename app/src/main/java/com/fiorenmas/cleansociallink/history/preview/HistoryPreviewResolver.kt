package com.fiorenmas.cleansociallink.history.preview

import com.fiorenmas.cleansociallink.history.preview.resolver.PreviewResolverCoordinator

object HistoryPreviewResolver {
    fun resolvePreviewImage(pageUrl: String): String? {
        return PreviewResolverCoordinator.resolvePreviewImage(pageUrl)
    }
}
