package com.fiorenmas.cleansociallink.utils

import android.net.Uri

interface SocialHandler {
    val hosts: Set<String>
    fun matchesHost(url: String): Boolean {
        val host = Uri.parse(url).host?.lowercase() ?: return false
        return hosts.any { host == it }
    }
    fun needsRedirectResolve(url: String): Boolean
    fun cleanUrl(url: String): String
    fun isValidRedirect(url: String, originalUrl: String): Boolean
}

