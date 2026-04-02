package com.fiorenmas.cleansociallink.utils.preferences

import android.content.Context

object PrefsStorage {
    internal const val NAME = "settings"

    private const val KEY_SHARE_ACTION = "share_action"
    private const val KEY_CUSTOM_UA = "custom_ua"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_COLOR = "color"
    private const val KEY_BACKGROUND_COLOR = "background_color"
    private const val KEY_HISTORY_ENABLED = "history_enabled"
    private const val KEY_HISTORY_RETENTION = "history_retention"

    private fun prefs(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getShareAction(context: Context): Int =
        prefs(context).getInt(KEY_SHARE_ACTION, Prefs.ACTION_COPY)

    fun setShareAction(context: Context, action: Int) =
        prefs(context).edit().putInt(KEY_SHARE_ACTION, action).apply()

    fun getCustomUa(context: Context): String =
        prefs(context).getString(KEY_CUSTOM_UA, "") ?: ""

    fun setCustomUa(context: Context, ua: String) =
        prefs(context).edit().putString(KEY_CUSTOM_UA, ua).apply()

    fun getLanguage(context: Context): Int =
        prefs(context).getInt(KEY_LANGUAGE, Prefs.LANG_AUTO)

    fun setLanguage(context: Context, lang: Int) =
        prefs(context).edit().putInt(KEY_LANGUAGE, lang).apply()

    fun getThemeMode(context: Context): Int =
        prefs(context).getInt(KEY_THEME_MODE, Prefs.MODE_SYSTEM)

    fun setThemeMode(context: Context, mode: Int) =
        prefs(context).edit().putInt(KEY_THEME_MODE, mode).apply()

    fun getColor(context: Context): Int =
        prefs(context).getInt(KEY_COLOR, Prefs.COLOR_DEFAULT)

    fun setColor(context: Context, color: Int) =
        prefs(context).edit().putInt(KEY_COLOR, color).apply()

    fun getBackgroundColor(context: Context): Int =
        prefs(context).getInt(KEY_BACKGROUND_COLOR, Prefs.BACKGROUND_COLOR_DEFAULT)

    fun setBackgroundColor(context: Context, color: Int) =
        prefs(context).edit().putInt(KEY_BACKGROUND_COLOR, color).apply()

    fun isHistoryEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HISTORY_ENABLED, true)

    fun setHistoryEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_HISTORY_ENABLED, enabled).apply()

    fun getHistoryRetention(context: Context): Int =
        prefs(context).getInt(KEY_HISTORY_RETENTION, Prefs.HISTORY_RETENTION_NEVER)

    fun setHistoryRetention(context: Context, retention: Int) =
        prefs(context).edit().putInt(KEY_HISTORY_RETENTION, retention).apply()
}

