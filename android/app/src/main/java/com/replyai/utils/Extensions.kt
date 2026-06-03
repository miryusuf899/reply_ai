package com.replyai.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun View.showSnackbar(message: String, length: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, message, length).show()
}

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    showToast("Copied to clipboard")
}

fun String?.formatApiDate(): String {
    if (this.isNullOrBlank()) return ""
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val output = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        val date = input.parse(this.substringBefore('+').substringBefore('Z')) ?: return this
        output.format(date)
    } catch (_: Exception) {
        this
    }
}

/**
 * Maps UI tone chips to API values.
 * UI: formal | friendly | empathic → API: formal | friendly | empathetic
 * (Django accepts "empathetic", not "empathic")
 */
fun String.toApiTone(): String = when (this.lowercase()) {
    "formal" -> "formal"
    "friendly" -> "friendly"
    "empathic", "empathetic" -> "empathetic"
    else -> "friendly"
}

fun String.toDisplayTone(): String = when (this.lowercase()) {
    "formal" -> "Formal"
    "friendly" -> "Friendly"
    "empathetic", "empathic" -> "Empathic"
    "neutral" -> "Neutral"
    "assertive" -> "Assertive"
    else -> this.replaceFirstChar { it.uppercase() }
}

fun String.messengerIconRes(): Int = when (this.lowercase()) {
    "telegram" -> com.replyai.R.drawable.ic_telegram
    "instagram" -> com.replyai.R.drawable.ic_instagram
    "whatsapp" -> com.replyai.R.drawable.ic_whatsapp
    else -> com.replyai.R.drawable.ic_chat
}
