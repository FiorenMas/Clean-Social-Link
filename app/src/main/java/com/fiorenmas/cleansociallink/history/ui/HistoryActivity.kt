package com.fiorenmas.cleansociallink.history.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fiorenmas.cleansociallink.R
import com.fiorenmas.cleansociallink.history.data.HistoryStorage
import com.fiorenmas.cleansociallink.history.preview.HistoryImageLoader
import com.fiorenmas.cleansociallink.utils.CleanUrlAction
import com.fiorenmas.cleansociallink.utils.preferences.Prefs
import com.fiorenmas.cleansociallink.utils.theme.ThemeManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HistoryActivity : AppCompatActivity() {
    private lateinit var adapter: HistoryAdapter
    private lateinit var recyclerHistory: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var checkSelectAll: CheckBox
    private lateinit var btnDelete: MaterialButton
    private var ignoreSelectAllChange = false
    private val historyChangedListener: () -> Unit = {
        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                loadHistory()
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Prefs.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyThemeMode(Prefs.getThemeMode(this))
        setTheme(ThemeManager.getThemeResId(Prefs.getColor(this)))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        applyCustomBackground()
        applySystemBarsStyle()

        recyclerHistory = findViewById(R.id.recyclerHistory)
        textEmpty = findViewById(R.id.textEmpty)
        checkSelectAll = findViewById(R.id.checkSelectAll)
        btnDelete = findViewById(R.id.btnDeleteHistory)

        adapter = HistoryAdapter(
            onItemOpen = { item ->
                CleanUrlAction.handle(this, item.cleanUrl)
            },
            onPreviewOpen = { previewUrl ->
                showPreviewDialog(previewUrl)
            },
            onSelectionChanged = { selectedCount, totalCount ->
                btnDelete.isEnabled = selectedCount > 0
                ignoreSelectAllChange = true
                checkSelectAll.isChecked = totalCount > 0 && selectedCount == totalCount
                ignoreSelectAllChange = false
            }
        )

        recyclerHistory.layoutManager = LinearLayoutManager(this)
        recyclerHistory.adapter = adapter

        checkSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (!ignoreSelectAllChange) {
                adapter.setAllSelected(isChecked)
            }
        }

        btnDelete.setOnClickListener {
            val selectedIds = adapter.getSelectedIds()
            if (selectedIds.isEmpty()) {
                Toast.makeText(this, R.string.history_select_item_first, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.history_delete_confirm, selectedIds.size))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    HistoryStorage.deleteByIds(this, selectedIds)
                    loadHistory()
                }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    override fun onStart() {
        super.onStart()
        HistoryStorage.addOnHistoryChangedListener(historyChangedListener)
    }

    override fun onStop() {
        HistoryStorage.removeOnHistoryChangedListener(historyChangedListener)
        super.onStop()
    }

    private fun loadHistory() {
        val items = HistoryStorage.getAll(this)
        adapter.submit(items)

        val empty = items.isEmpty()
        textEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        recyclerHistory.visibility = if (empty) View.GONE else View.VISIBLE

        if (empty) {
            ignoreSelectAllChange = true
            checkSelectAll.isChecked = false
            ignoreSelectAllChange = false
        }
    }

    private fun showPreviewDialog(previewUrl: String) {
        if (previewUrl.isBlank()) return

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_history_preview, null, false)
        val imagePreview = dialogView.findViewById<ImageView>(R.id.imagePreviewLarge)

        HistoryImageLoader.loadInto(imagePreview, previewUrl)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun applyCustomBackground() {
        val root = findViewById<View>(R.id.historyRoot)
        val color = ThemeManager.getBackgroundColorInt(this, Prefs.getBackgroundColor(this))
        root.setBackgroundColor(color)
    }

    private fun applySystemBarsStyle() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val root = findViewById<View>(R.id.historyRoot)
        val initialLeft = root.paddingLeft
        val initialTop = root.paddingTop
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom
        val extraTopSpacing = (12 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                initialLeft,
                initialTop + systemBars.top + extraTopSpacing,
                initialRight,
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
}

