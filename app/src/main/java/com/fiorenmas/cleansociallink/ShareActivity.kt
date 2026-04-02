package com.fiorenmas.cleansociallink

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fiorenmas.cleansociallink.history.data.HistoryStorage
import com.fiorenmas.cleansociallink.utils.CleanUrlAction
import com.fiorenmas.cleansociallink.utils.network.UrlResolver
import com.fiorenmas.cleansociallink.utils.UrlCleaner
import com.fiorenmas.cleansociallink.utils.preferences.Prefs
import com.fiorenmas.cleansociallink.utils.theme.ThemeManager


class ShareActivity : AppCompatActivity() {
    private var resolver: UrlResolver? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Prefs.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyThemeMode(Prefs.getThemeMode(this))
        setTheme(ThemeManager.getTransparentThemeResId(Prefs.getColor(this)))
        super.onCreate(savedInstanceState)

        if (intent?.getBooleanExtra(CleanUrlAction.EXTRA_INTERNAL_SHARE, false) == true) {
            finish()
            return
        }

        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val url = text?.let { UrlCleaner.extractUrl(it) }
        if (url == null) {
            Toast.makeText(this, R.string.error_no_url, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (!UrlCleaner.isSupportedUrl(url)) {
            Toast.makeText(this, R.string.error_unsupported_domain, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (UrlCleaner.needsRedirectResolve(url)) {
            Toast.makeText(this, R.string.cleaning, Toast.LENGTH_SHORT).show()
            resolver = UrlResolver(this)
            resolver!!.resolve(
                url,
                onResult = { cleanUrl ->
                    HistoryStorage.saveCleanedUrl(this, cleanUrl, text)
                    CleanUrlAction.handle(this, cleanUrl)
                    finish()
                },
                onError = {
                    Toast.makeText(this, R.string.error_network_cant_clean, Toast.LENGTH_SHORT).show()
                    finish()
                }
            )
        } else {
            val cleanUrl = UrlCleaner.cleanUrl(url)
            HistoryStorage.saveCleanedUrl(this, cleanUrl, text)
            CleanUrlAction.handle(this, cleanUrl)
            finish()
        }
    }

    override fun onDestroy() {
        resolver?.destroy()
        super.onDestroy()
    }
}

