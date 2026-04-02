package com.fiorenmas.cleansociallink.utils.theme

import android.content.res.ColorStateList
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.fiorenmas.cleansociallink.R
import com.fiorenmas.cleansociallink.utils.preferences.Prefs
import com.google.android.material.shape.MaterialShapeDrawable

object ThemeManager {

    fun applyThemeMode(mode: Int) {
        val nightMode = when (mode) {
            Prefs.MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            Prefs.MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun getThemeResId(color: Int): Int {
        return when (color) {
            Prefs.COLOR_BLUE -> R.style.Theme_CleanSocialLink_Blue
            Prefs.COLOR_GREEN -> R.style.Theme_CleanSocialLink_Green
            Prefs.COLOR_PURPLE -> R.style.Theme_CleanSocialLink_Purple
            Prefs.COLOR_RED -> R.style.Theme_CleanSocialLink_Red
            Prefs.COLOR_ORANGE -> R.style.Theme_CleanSocialLink_Orange
            Prefs.COLOR_TEAL -> R.style.Theme_CleanSocialLink_Teal
            else -> R.style.Theme_CleanSocialLink
        }
    }

    fun getTransparentThemeResId(color: Int): Int {
        return when (color) {
            Prefs.COLOR_BLUE -> R.style.Theme_CleanSocialLink_Blue_Transparent
            Prefs.COLOR_GREEN -> R.style.Theme_CleanSocialLink_Green_Transparent
            Prefs.COLOR_PURPLE -> R.style.Theme_CleanSocialLink_Purple_Transparent
            Prefs.COLOR_RED -> R.style.Theme_CleanSocialLink_Red_Transparent
            Prefs.COLOR_ORANGE -> R.style.Theme_CleanSocialLink_Orange_Transparent
            Prefs.COLOR_TEAL -> R.style.Theme_CleanSocialLink_Teal_Transparent
            else -> R.style.Theme_CleanSocialLink_Transparent
        }
    }

    fun getBackgroundColorInt(context: Context, backgroundColor: Int): Int {
        val isDark = when (Prefs.getThemeMode(context)) {
            Prefs.MODE_DARK -> true
            Prefs.MODE_LIGHT -> false
            else -> {
                val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightMask == Configuration.UI_MODE_NIGHT_YES
            }
        }

        val hex = if (isDark) {
            when (backgroundColor) {
                Prefs.BACKGROUND_COLOR_BLUE -> "#172339"
                Prefs.BACKGROUND_COLOR_GREEN -> "#172B1E"
                Prefs.BACKGROUND_COLOR_PURPLE -> "#221A33"
                Prefs.BACKGROUND_COLOR_RED -> "#321A1C"
                Prefs.BACKGROUND_COLOR_ORANGE -> "#332315"
                Prefs.BACKGROUND_COLOR_TEAL -> "#162A28"
                else -> "#1F1B24"
            }
        } else {
            when (backgroundColor) {
                Prefs.BACKGROUND_COLOR_BLUE -> "#E7EEFF"
                Prefs.BACKGROUND_COLOR_GREEN -> "#E8F3E8"
                Prefs.BACKGROUND_COLOR_PURPLE -> "#F1E8FF"
                Prefs.BACKGROUND_COLOR_RED -> "#F9E8E8"
                Prefs.BACKGROUND_COLOR_ORANGE -> "#FFF0E5"
                Prefs.BACKGROUND_COLOR_TEAL -> "#E4F4F2"
                else -> "#F1EDF7"
            }
        }
        return Color.parseColor(hex)
    }

    fun applyDialogBackground(activity: AppCompatActivity, dialog: AlertDialog) {
        val color = getBackgroundColorInt(activity, Prefs.getBackgroundColor(activity))
        val corner = 28f * activity.resources.displayMetrics.density
        val background = MaterialShapeDrawable().apply {
            fillColor = ColorStateList.valueOf(color)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(corner)
                .build()
            initializeElevationOverlay(activity)
        }
        dialog.window?.setBackgroundDrawable(background)
    }
}

