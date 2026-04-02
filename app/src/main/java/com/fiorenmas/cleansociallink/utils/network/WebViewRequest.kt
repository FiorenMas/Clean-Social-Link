package com.fiorenmas.cleansociallink.utils.network

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.fiorenmas.cleansociallink.utils.preferences.Prefs

object WebViewRequest {

    @SuppressLint("SetJavaScriptEnabled")
    fun create(context: Context, ua: String? = null): WebView {
        return WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.userAgentString = ua ?: Prefs.getEffectiveUa(context)
            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        }
    }

    fun destroy(webView: WebView?) {
        webView?.stopLoading()
        webView?.destroy()
    }
}

