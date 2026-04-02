package com.fiorenmas.cleansociallink.utils.preferences

import android.content.Context
import android.content.res.Configuration
import com.fiorenmas.cleansociallink.utils.UrlCleaner
import java.util.Locale

object Prefs {
    const val ACTION_COPY = 0
    const val ACTION_SHARE = 1

    const val LANG_AUTO = 0
    const val LANG_EN = 1
    const val LANG_VI = 2
    const val LANG_ZH = 3
    const val LANG_HI = 4
    const val LANG_DE = 5
    const val LANG_FR = 6
    const val LANG_ES = 7
    const val LANG_AR = 8
    const val LANG_PT = 9
    const val LANG_RU = 10
    const val LANG_JA = 11

    const val MODE_SYSTEM = 0
    const val MODE_LIGHT = 1
    const val MODE_DARK = 2

    const val COLOR_DEFAULT = 0
    const val COLOR_BLUE = 1
    const val COLOR_GREEN = 2
    const val COLOR_PURPLE = 3
    const val COLOR_RED = 4
    const val COLOR_ORANGE = 5
    const val COLOR_TEAL = 6

    const val BACKGROUND_COLOR_DEFAULT = 0
    const val BACKGROUND_COLOR_BLUE = 1
    const val BACKGROUND_COLOR_GREEN = 2
    const val BACKGROUND_COLOR_PURPLE = 3
    const val BACKGROUND_COLOR_RED = 4
    const val BACKGROUND_COLOR_ORANGE = 5
    const val BACKGROUND_COLOR_TEAL = 6

    const val HISTORY_RETENTION_7_DAYS = 0
    const val HISTORY_RETENTION_30_DAYS = 1
    const val HISTORY_RETENTION_90_DAYS = 2
    const val HISTORY_RETENTION_1_YEAR = 3
    const val HISTORY_RETENTION_NEVER = 4

    private val supportedLanguages = setOf(
        "en",
        "vi",
        "zh",
        "hi",
        "de",
        "fr",
        "es",
        "ar",
        "pt",
        "ru",
        "ja"
    )

    fun getShareAction(context: Context) = PrefsStorage.getShareAction(context)
    fun setShareAction(context: Context, action: Int) = PrefsStorage.setShareAction(context, action)

    fun getCustomUa(context: Context) = PrefsStorage.getCustomUa(context)
    fun setCustomUa(context: Context, ua: String) = PrefsStorage.setCustomUa(context, ua)

    fun getEffectiveUa(context: Context): String {
        val custom = getCustomUa(context)
        return custom.ifBlank { UrlCleaner.DESKTOP_UA }
    }

    fun getLanguage(context: Context) = PrefsStorage.getLanguage(context)
    fun setLanguage(context: Context, lang: Int) = PrefsStorage.setLanguage(context, lang)

    fun getThemeMode(context: Context) = PrefsStorage.getThemeMode(context)
    fun setThemeMode(context: Context, mode: Int) = PrefsStorage.setThemeMode(context, mode)

    fun getColor(context: Context) = PrefsStorage.getColor(context)
    fun setColor(context: Context, color: Int) = PrefsStorage.setColor(context, color)

    fun getBackgroundColor(context: Context) = PrefsStorage.getBackgroundColor(context)
    fun setBackgroundColor(context: Context, color: Int) = PrefsStorage.setBackgroundColor(context, color)

    fun isHistoryEnabled(context: Context) = PrefsStorage.isHistoryEnabled(context)
    fun setHistoryEnabled(context: Context, enabled: Boolean) = PrefsStorage.setHistoryEnabled(context, enabled)

    fun getHistoryRetention(context: Context) = PrefsStorage.getHistoryRetention(context)
    fun setHistoryRetention(context: Context, retention: Int) = PrefsStorage.setHistoryRetention(context, retention)

    fun applyLocale(context: Context): Context {
        val lang = getLanguage(context)
        val systemLocale = context.resources.configuration.locales[0]
        val locale = when (lang) {
            LANG_EN -> Locale.ENGLISH
            LANG_VI -> Locale.forLanguageTag("vi")
            LANG_ZH -> Locale.forLanguageTag("zh")
            LANG_HI -> Locale.forLanguageTag("hi-IN")
            LANG_DE -> Locale.GERMAN
            LANG_FR -> Locale.FRENCH
            LANG_ES -> Locale.forLanguageTag("es")
            LANG_AR -> Locale.forLanguageTag("ar")
            LANG_PT -> Locale.forLanguageTag("pt")
            LANG_RU -> Locale.forLanguageTag("ru")
            LANG_JA -> Locale.JAPANESE
            else -> {
                val systemLanguage = systemLocale.language.lowercase(Locale.ROOT)
                if (supportedLanguages.contains(systemLanguage)) {
                    systemLocale
                } else {
                    Locale.ENGLISH
                }
            }
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}

