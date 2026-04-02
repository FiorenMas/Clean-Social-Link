package com.fiorenmas.cleansociallink

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.fiorenmas.cleansociallink.history.data.HistoryStorage
import com.fiorenmas.cleansociallink.history.ui.HistoryActivity
import com.fiorenmas.cleansociallink.utils.CleanUrlAction
import com.fiorenmas.cleansociallink.utils.network.UrlResolver
import com.fiorenmas.cleansociallink.utils.UrlCleaner
import com.fiorenmas.cleansociallink.utils.preferences.Prefs
import com.fiorenmas.cleansociallink.utils.preferences.SettingsUi
import com.fiorenmas.cleansociallink.utils.theme.ThemeManager

import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {
    private lateinit var urlInput: TextInputEditText
    private lateinit var resultText: TextInputEditText
    private lateinit var resultLayout: TextInputLayout
    private lateinit var progress: LinearProgressIndicator
    private lateinit var btnClean: MaterialButton
    private lateinit var btnHistory: MaterialButton
    private lateinit var mainScroll: NestedScrollView
    private var resolver: UrlResolver? = null
    private var isResolving = false
    private var resolveToken = 0

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Prefs.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyThemeMode(Prefs.getThemeMode(this))
        setTheme(ThemeManager.getThemeResId(Prefs.getColor(this)))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyCustomBackground()
        applySystemBarsStyle()

        urlInput = findViewById(R.id.urlInput)
        resultText = findViewById(R.id.resultText)
        resultLayout = findViewById(R.id.resultLayout)
        progress = findViewById(R.id.progress)
        btnClean = findViewById(R.id.btnClean)
        btnHistory = findViewById(R.id.btnHistory)
        mainScroll = findViewById(R.id.mainScroll)

        findViewById<MaterialButton>(R.id.btnPaste).setOnClickListener { pasteFromClipboard() }
        btnClean.setOnClickListener { cleanInput() }
        findViewById<MaterialButton>(R.id.btnSettings).setOnClickListener {
            SettingsUi.showSettings(this) { updateHistoryButtonVisibility() }
        }
        btnHistory.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        updateHistoryButtonVisibility()
    }

    private fun pasteFromClipboard() {
        val clip = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val text = clip.primaryClip?.getItemAt(0)?.text?.toString()
        if (text != null) {
            val url = UrlCleaner.extractUrl(text)
            urlInput.setText(url ?: text)
        }
    }

    private fun cleanInput() {
        val text = urlInput.text?.toString()?.trim() ?: return
        val url = UrlCleaner.extractUrl(text)
        if (url == null) {
            Toast.makeText(this, R.string.error_no_url, Toast.LENGTH_SHORT).show()
            return
        }
        if (!UrlCleaner.isSupportedUrl(url)) {
            Toast.makeText(this, R.string.error_unsupported_domain, Toast.LENGTH_SHORT).show()
            return
        }
        if (UrlCleaner.needsRedirectResolve(url)) {
            if (isResolving) return
            setResolvingState(true)
            resolver?.destroy()
            val token = ++resolveToken
            resolver = UrlResolver(this)
            resolver!!.resolve(
                url,
                onResult = { cleanUrl ->
                    if (token == resolveToken) {
                        setResolvingState(false)
                        showResult(cleanUrl)
                        HistoryStorage.saveCleanedUrl(this, cleanUrl, text)
                        CleanUrlAction.handle(this, cleanUrl)
                    }
                },
                onError = {
                    if (token == resolveToken) {
                        setResolvingState(false)
                        Toast.makeText(this, R.string.error_network_cant_clean, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } else {
            val cleanUrl = UrlCleaner.cleanUrl(url)
            showResult(cleanUrl)
            HistoryStorage.saveCleanedUrl(this, cleanUrl, text)
            CleanUrlAction.handle(this, cleanUrl)
        }
    }

    private fun showResult(url: String) {
        resultLayout.visibility = View.VISIBLE
        resultText.setText(url)
        mainScroll.post {
            mainScroll.smoothScrollTo(0, resultLayout.bottom)
        }
    }

    private fun updateHistoryButtonVisibility() {
        btnHistory.visibility = if (Prefs.isHistoryEnabled(this)) View.VISIBLE else View.GONE
    }

    private fun applyCustomBackground() {
        val root = findViewById<View>(R.id.mainRoot)
        val color = ThemeManager.getBackgroundColorInt(this, Prefs.getBackgroundColor(this))
        root.setBackgroundColor(color)
    }

    private fun setResolvingState(resolving: Boolean) {
        isResolving = resolving
        progress.visibility = if (resolving) View.VISIBLE else View.GONE
        btnClean.isEnabled = !resolving
    }

    private fun applySystemBarsStyle() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val root = findViewById<View>(R.id.mainRoot)
        val initialLeft = root.paddingLeft
        val initialTop = root.paddingTop
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                initialLeft + systemBars.left,
                initialTop + systemBars.top,
                initialRight + systemBars.right,
                initialBottom + systemBars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)

        val isDarkTheme = when (Prefs.getThemeMode(this)) {
            Prefs.MODE_DARK -> true
            Prefs.MODE_LIGHT -> false
            else -> {
                val nightMask = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightMask == Configuration.UI_MODE_NIGHT_YES
            }
        }

        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }
    }

    override fun onDestroy() {
        resolveToken++
        resolver?.destroy()
        super.onDestroy()
    }
}

