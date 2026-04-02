package com.fiorenmas.cleansociallink.utils

object SupportedDomains {
    val all: Set<String>
        get() = UrlCleaner.handlers.flatMap { it.hosts }.toSet()

    fun isSupported(host: String): Boolean {
        return all.any { host.equals(it, ignoreCase = true) }
    }
}

