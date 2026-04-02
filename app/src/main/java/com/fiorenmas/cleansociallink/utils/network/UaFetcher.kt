package com.fiorenmas.cleansociallink.utils.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.fiorenmas.cleansociallink.utils.UrlCleaner

object UaFetcher {
    private const val URL = "https://www.whatismybrowser.com/guides/the-latest-user-agent/chrome"
    private const val TIMEOUT_MS = 10_000L
    private const val JS = """
        (function() {
            var headers = document.querySelectorAll('h2');
            for (var i = 0; i < headers.length; i++) {
                if (headers[i].textContent.indexOf('Windows 10') !== -1) {
                    var table = headers[i].nextElementSibling;
                    while (table && table.tagName !== 'TABLE') table = table.nextElementSibling;
                    if (table) {
                        var span = table.querySelector('span.code');
                        if (span) return span.textContent.trim();
                    }
                }
            }
            return '';
        })()
    """

    fun fetch(context: Context, onResult: (String?) -> Unit) {
        val webView = WebViewRequest.create(context, UrlCleaner.DESKTOP_UA)
        val mainHandler = Handler(Looper.getMainLooper())
        var completed = false

        fun complete(result: String?) {
            if (completed) return
            completed = true
            mainHandler.removeCallbacksAndMessages(null)
            WebViewRequest.destroy(webView)
            onResult(result)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript(JS) { result ->
                    val ua = result?.trim('"')?.trim()?.takeUnless { it.isBlank() || it == "null" }
                    complete(ua)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    complete(null)
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 200) >= 400) {
                    complete(null)
                }
            }
        }

        mainHandler.postDelayed({ complete(null) }, TIMEOUT_MS)
        webView.loadUrl(URL)
    }
}

