package com.fiorenmas.cleansociallink.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.fiorenmas.cleansociallink.R
import com.fiorenmas.cleansociallink.utils.preferences.Prefs

object CleanUrlAction {
    const val EXTRA_INTERNAL_SHARE = "com.fiorenmas.cleansociallink.EXTRA_INTERNAL_SHARE"

    fun handle(context: Context, cleanUrl: String) {
        if (Prefs.getShareAction(context) == Prefs.ACTION_SHARE) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, cleanUrl)
                putExtra(EXTRA_INTERNAL_SHARE, true)
            }
            context.startActivity(Intent.createChooser(shareIntent, null))
        } else {
            copyToClipboard(context, cleanUrl)
            Toast.makeText(context, R.string.copied_dialog_message, Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(context: Context, text: String) {
        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clip.setPrimaryClip(ClipData.newPlainText("Clean URL", text))
    }
}

