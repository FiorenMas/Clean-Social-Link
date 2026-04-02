package com.fiorenmas.cleansociallink.utils.preferences

import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.app.AppCompatActivity
import com.fiorenmas.cleansociallink.R
import com.fiorenmas.cleansociallink.utils.UrlCleaner
import com.fiorenmas.cleansociallink.utils.network.UaFetcher
import com.fiorenmas.cleansociallink.utils.theme.ThemeManager
import com.fiorenmas.cleansociallink.utils.theme.ThemeUi
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

object SettingsUi {

    fun showSettings(activity: AppCompatActivity, onPrefsChanged: (() -> Unit)? = null) {
        val items = arrayOf(
            activity.getString(R.string.settings_share_action),
            activity.getString(R.string.settings_theme),
            activity.getString(R.string.settings_history),
            activity.getString(R.string.settings_custom_ua),
            activity.getString(R.string.settings_language)
        )
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.settings)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showShareAction(activity)
                    1 -> ThemeUi.showThemeSettings(activity)
                    2 -> showHistorySettings(activity, onPrefsChanged)
                    3 -> showCustomUa(activity)
                    4 -> showLanguage(activity)
                }
            }
            .show()
        ThemeManager.applyDialogBackground(activity, dialog)
    }

    fun showShareAction(activity: AppCompatActivity) {
        val options = arrayOf(
            activity.getString(R.string.action_copy),
            activity.getString(R.string.action_share)
        )
        val current = Prefs.getShareAction(activity)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.settings_share_action)
            .setSingleChoiceItems(options, current) { dialog, which ->
                Prefs.setShareAction(activity, which)
                dialog.dismiss()
            }
            .show()
        ThemeManager.applyDialogBackground(activity, dialog)
    }

    fun showCustomUa(activity: AppCompatActivity) {
        val container = android.widget.LinearLayout(activity).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 0)
        }
        val input = com.google.android.material.textfield.TextInputEditText(activity).apply {
            setText(Prefs.getCustomUa(activity))
            hint = "Default: ${UrlCleaner.DESKTOP_UA}"
        }
        val fetchBtn = MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = activity.getString(R.string.settings_fetch_ua)
        }
        container.addView(input)
        container.addView(fetchBtn)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.settings_custom_ua)
            .setView(container)
            .setPositiveButton(R.string.ok) { _, _ ->
                Prefs.setCustomUa(activity, input.text?.toString()?.trim() ?: "")
            }
            .setNegativeButton(R.string.settings_reset) { _, _ ->
                Prefs.setCustomUa(activity, "")
            }
            .show()
        ThemeManager.applyDialogBackground(activity, dialog)

        fetchBtn.setOnClickListener {
            fetchBtn.isEnabled = false
            fetchBtn.text = activity.getString(R.string.settings_fetching_ua)
            UaFetcher.fetch(activity) { ua ->
                activity.runOnUiThread {
                    if (ua != null) {
                        input.setText(ua)
                    } else {
                        Toast.makeText(activity, R.string.error_failed, Toast.LENGTH_SHORT).show()
                    }
                    fetchBtn.isEnabled = true
                    fetchBtn.text = activity.getString(R.string.settings_fetch_ua)
                }
            }
        }
    }

    fun showLanguage(activity: AppCompatActivity) {
        data class LanguageOption(val code: Int, val label: String)

        val autoOption = LanguageOption(
            code = Prefs.LANG_AUTO,
            label = activity.getString(R.string.lang_auto)
        )
        val sortedOptions = listOf(
            LanguageOption(Prefs.LANG_EN, activity.getString(R.string.lang_en)),
            LanguageOption(Prefs.LANG_VI, activity.getString(R.string.lang_vi)),
            LanguageOption(Prefs.LANG_ZH, activity.getString(R.string.lang_zh)),
            LanguageOption(Prefs.LANG_HI, activity.getString(R.string.lang_hi)),
            LanguageOption(Prefs.LANG_DE, activity.getString(R.string.lang_de)),
            LanguageOption(Prefs.LANG_FR, activity.getString(R.string.lang_fr)),
            LanguageOption(Prefs.LANG_ES, activity.getString(R.string.lang_es)),
            LanguageOption(Prefs.LANG_AR, activity.getString(R.string.lang_ar)),
            LanguageOption(Prefs.LANG_PT, activity.getString(R.string.lang_pt)),
            LanguageOption(Prefs.LANG_RU, activity.getString(R.string.lang_ru)),
            LanguageOption(Prefs.LANG_JA, activity.getString(R.string.lang_ja))
        ).sortedBy { it.label.lowercase(Locale.getDefault()) }

        val allOptions = listOf(autoOption) + sortedOptions
        val labels = allOptions.map { it.label }.toTypedArray()
        val currentCode = Prefs.getLanguage(activity)
        val currentIndex = allOptions.indexOfFirst { it.code == currentCode }.takeIf { it >= 0 } ?: 0

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.settings_language)
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                Prefs.setLanguage(activity, allOptions[which].code)
                dialog.dismiss()
                activity.recreate()
            }
            .show()
        ThemeManager.applyDialogBackground(activity, dialog)
    }

    fun showHistorySettings(activity: AppCompatActivity, onPrefsChanged: (() -> Unit)? = null) {
        val retentionOptions = arrayOf(
            activity.getString(R.string.history_keep_7_days),
            activity.getString(R.string.history_keep_30_days),
            activity.getString(R.string.history_keep_90_days),
            activity.getString(R.string.history_keep_1_year),
            activity.getString(R.string.history_keep_never)
        )

        val currentEnabled = Prefs.isHistoryEnabled(activity)
        val currentRetention = Prefs.getHistoryRetention(activity)

        val density = activity.resources.displayMetrics.density
        val sidePadding = (24 * density).toInt()
        val topPadding = (12 * density).toInt()
        val sectionTop = (16 * density).toInt()

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(sidePadding, topPadding, sidePadding, 0)
        }

        val saveSwitch = SwitchCompat(activity).apply {
            text = activity.getString(R.string.settings_history_save_cleaned_url)
            isChecked = currentEnabled
        }

        val autoRemoveTitle = TextView(activity).apply {
            setText(R.string.settings_history_auto_remove)
            setPadding(0, sectionTop, 0, 0)
        }

        val retentionGroup = RadioGroup(activity).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(0, (8 * density).toInt(), 0, 0)
        }

        retentionOptions.forEachIndexed { index, option ->
            val button = RadioButton(activity).apply {
                id = android.view.View.generateViewId()
                tag = index
                text = option
            }
            retentionGroup.addView(button)
        }

        val currentButton = retentionGroup.getChildAt(currentRetention) as? RadioButton
        if (currentButton != null) {
            retentionGroup.check(currentButton.id)
        }

        fun setRetentionEnabled(enabled: Boolean) {
            autoRemoveTitle.alpha = if (enabled) 1f else 0.5f
            retentionGroup.isEnabled = enabled
            for (i in 0 until retentionGroup.childCount) {
                retentionGroup.getChildAt(i).isEnabled = enabled
            }
        }

        setRetentionEnabled(currentEnabled)
        saveSwitch.setOnCheckedChangeListener { _, isChecked ->
            setRetentionEnabled(isChecked)
        }

        container.addView(saveSwitch)
        container.addView(autoRemoveTitle)
        container.addView(retentionGroup)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.settings_history)
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                val selectedButton = retentionGroup.findViewById<RadioButton>(retentionGroup.checkedRadioButtonId)
                val selectedRetention = selectedButton?.tag as? Int ?: Prefs.HISTORY_RETENTION_NEVER

                Prefs.setHistoryEnabled(activity, saveSwitch.isChecked)
                Prefs.setHistoryRetention(activity, selectedRetention)
                onPrefsChanged?.invoke()
            }
            .show()
        ThemeManager.applyDialogBackground(activity, dialog)
    }
}

