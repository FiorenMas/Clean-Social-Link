package com.fiorenmas.cleansociallink.utils.theme

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.fiorenmas.cleansociallink.R
import com.fiorenmas.cleansociallink.utils.preferences.Prefs
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object ThemeUi {

    fun showThemeSettings(activity: AppCompatActivity) {
        val modeOptions = arrayOf(
            activity.getString(R.string.theme_system),
            activity.getString(R.string.theme_light),
            activity.getString(R.string.theme_dark)
        )

        val colors = arrayOf(
            Pair(R.string.color_default, "#6750A4"),
            Pair(R.string.color_blue, "#1976D2"),
            Pair(R.string.color_green, "#388E3C"),
            Pair(R.string.color_purple, "#7B1FA2"),
            Pair(R.string.color_red, "#D32F2F"),
            Pair(R.string.color_orange, "#E64A19"),
            Pair(R.string.color_teal, "#00796B")
        )
        val backgroundColors = arrayOf(
            Pair(R.string.color_default, "#F1EDF7"),
            Pair(R.string.color_blue, "#E7EEFF"),
            Pair(R.string.color_green, "#E8F3E8"),
            Pair(R.string.color_purple, "#F1E8FF"),
            Pair(R.string.color_red, "#F9E8E8"),
            Pair(R.string.color_orange, "#FFF0E5"),
            Pair(R.string.color_teal, "#E4F4F2")
        )

        val originalMode = Prefs.getThemeMode(activity)
        val originalColor = Prefs.getColor(activity)
        val originalBackgroundColor = Prefs.getBackgroundColor(activity)
        var selectedColor = originalColor
        var selectedBackgroundColor = originalBackgroundColor

        val density = activity.resources.displayMetrics.density
        val sectionSpacing = (16 * density).toInt()
        val swatchSize = (48 * density).toInt()
        val colorSize = (40 * density).toInt()
        val circleMargin = (10 * density).toInt()
        val selectedStroke = (3 * density).toInt()

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (12 * density).toInt(), (24 * density).toInt(), (8 * density).toInt())
        }

        val modeTitle = TextView(activity).apply {
            setText(R.string.settings_theme)
        }

        val modeGroup = RadioGroup(activity).apply {
            orientation = RadioGroup.VERTICAL
        }

        modeOptions.forEachIndexed { index, text ->
            val button = RadioButton(activity).apply {
                id = android.view.View.generateViewId()
                tag = index
                this.text = text
            }
            modeGroup.addView(button)
        }

        (modeGroup.getChildAt(originalMode) as? RadioButton)?.let { modeGroup.check(it.id) }

        val colorTitle = TextView(activity).apply {
            setText(R.string.settings_color)
            setPadding(0, sectionSpacing, 0, 0)
        }

        val colorScroll = HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
        }

        val colorRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (12 * density).toInt(), 0, 0)
        }

        val swatchViews = mutableListOf<FrameLayout>()
        fun refreshColorSelection() {
            swatchViews.forEachIndexed { index, swatch ->
                swatch.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.TRANSPARENT)
                    val strokeColor = if (index == selectedColor) Color.WHITE else Color.TRANSPARENT
                    setStroke(selectedStroke, strokeColor)
                }
            }
        }

        colors.forEachIndexed { index, (nameRes, hex) ->
            val innerCircle = android.view.View(activity).apply {
                layoutParams = FrameLayout.LayoutParams(colorSize, colorSize, Gravity.CENTER)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(hex))
                }
            }

            val swatch = FrameLayout(activity).apply {
                contentDescription = activity.getString(nameRes)
                layoutParams = LinearLayout.LayoutParams(swatchSize, swatchSize).apply {
                    marginEnd = if (index == colors.lastIndex) 0 else circleMargin
                }
                setOnClickListener {
                    selectedColor = index
                    refreshColorSelection()
                }
            }
            swatch.addView(innerCircle)

            swatchViews.add(swatch)
            colorRow.addView(swatch)
        }

        refreshColorSelection()
        colorScroll.addView(colorRow)

        val backgroundTitle = TextView(activity).apply {
            setText(R.string.settings_background_color)
            setPadding(0, sectionSpacing, 0, 0)
        }

        val backgroundScroll = HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
        }

        val backgroundRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (12 * density).toInt(), 0, 0)
        }

        val backgroundSwatchViews = mutableListOf<FrameLayout>()
        fun refreshBackgroundSelection() {
            backgroundSwatchViews.forEachIndexed { index, swatch ->
                swatch.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.TRANSPARENT)
                    val strokeColor = if (index == selectedBackgroundColor) Color.WHITE else Color.TRANSPARENT
                    setStroke(selectedStroke, strokeColor)
                }
            }
        }

        backgroundColors.forEachIndexed { index, (nameRes, hex) ->
            val innerCircle = android.view.View(activity).apply {
                layoutParams = FrameLayout.LayoutParams(colorSize, colorSize, Gravity.CENTER)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(hex))
                }
            }

            val swatch = FrameLayout(activity).apply {
                contentDescription = activity.getString(nameRes)
                layoutParams = LinearLayout.LayoutParams(swatchSize, swatchSize).apply {
                    marginEnd = if (index == backgroundColors.lastIndex) 0 else circleMargin
                }
                setOnClickListener {
                    selectedBackgroundColor = index
                    refreshBackgroundSelection()
                }
            }
            swatch.addView(innerCircle)

            backgroundSwatchViews.add(swatch)
            backgroundRow.addView(swatch)
        }

        refreshBackgroundSelection()
        backgroundScroll.addView(backgroundRow)

        root.addView(modeTitle)
        root.addView(modeGroup)
        root.addView(colorTitle)
        root.addView(colorScroll)
        root.addView(backgroundTitle)
        root.addView(backgroundScroll)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.settings_theme)
            .setView(root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ ->
                val checked = modeGroup.findViewById<RadioButton>(modeGroup.checkedRadioButtonId)
                val selectedMode = checked?.tag as? Int ?: originalMode

                val modeChanged = selectedMode != originalMode
                val colorChanged = selectedColor != originalColor
                val backgroundChanged = selectedBackgroundColor != originalBackgroundColor
                if (!modeChanged && !colorChanged && !backgroundChanged) return@setPositiveButton

                Prefs.setThemeMode(activity, selectedMode)
                Prefs.setColor(activity, selectedColor)
                Prefs.setBackgroundColor(activity, selectedBackgroundColor)
                ThemeManager.applyThemeMode(selectedMode)
                activity.recreate()
            }
            .show()
        ThemeManager.applyDialogBackground(activity, dialog)
    }

    fun showThemeMode(activity: AppCompatActivity) = showThemeSettings(activity)
    fun showColorPicker(activity: AppCompatActivity) = showThemeSettings(activity)
}

