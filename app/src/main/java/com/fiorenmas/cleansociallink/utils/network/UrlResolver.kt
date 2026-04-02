package com.fiorenmas.cleansociallink.utils.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.fiorenmas.cleansociallink.utils.UrlCleaner

class UrlResolver(private val context: Context) {
    private var webView: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    fun resolve(url: String, onResult: (String) -> Unit, onError: (() -> Unit)? = null) {
        var resolved = false

        fun completeWithResult(cleanUrl: String) {
            if (resolved) return
            resolved = true
            clearTimeout()
            onResult(cleanUrl)
            destroy()
        }

        fun completeWithError() {
            if (resolved) return
            resolved = true
            clearTimeout()
            onError?.invoke()
            destroy()
        }

        webView = WebViewRequest.create(context).apply {
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    if (request?.isForMainFrame != true) return false
                    val redirectUrl = request.url?.toString() ?: return false
                    if (UrlCleaner.isValidRedirect(redirectUrl, url)) {
                        completeWithResult(UrlCleaner.cleanUrl(redirectUrl))
                        return true
                    }
                    return false
                }

                override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                    super.onPageFinished(view, finishedUrl)
                    val redirectUrl = finishedUrl ?: return
                    if (UrlCleaner.isValidRedirect(redirectUrl, url)) {
                        completeWithResult(UrlCleaner.cleanUrl(redirectUrl))
                    }
                }
            }
            loadUrl(url)
        }

        timeoutRunnable = Runnable { completeWithError() }
        mainHandler.postDelayed(timeoutRunnable!!, TIMEOUT_MS)
    }

    private fun clearTimeout() {
        timeoutRunnable?.let(mainHandler::removeCallbacks)
        timeoutRunnable = null
    }

    fun destroy() {
        clearTimeout()
        WebViewRequest.destroy(webView)
        webView = null
    }

    companion object {
        private const val TIMEOUT_MS = 5000L
    }
}

