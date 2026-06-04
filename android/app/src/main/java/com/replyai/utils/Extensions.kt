package com.replyai.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.replyai.data.models.ToneChoices
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
    showToast("Скопировано")
}

fun String?.formatApiDate(): String {
    if (this.isNullOrBlank()) return ""
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val output = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        val date = input.parse(this.substringBefore('+').substringBefore('Z')) ?: return this
        output.format(date)
    } catch (_: Exception) {
        this
    }
}

/** @see ToneChoices.toApiValue */
fun String.toApiTone(): String = ToneChoices.toApiValue(this)

fun String.toDisplayTone(): String = when (ToneChoices.toApiValue(this)) {
    ToneChoices.FORMAL -> "Формальный"
    ToneChoices.FRIENDLY -> "Дружеский"
    ToneChoices.NEUTRAL -> "Нейтральный"
    ToneChoices.ASSERTIVE -> "Уверенный"
    ToneChoices.EMPATHETIC -> "Эмпатичный"
    ToneChoices.EMPTY -> "—"
    else -> this
}

fun String.messengerIconRes(): Int = when (this.lowercase()) {
    "telegram" -> com.replyai.R.drawable.ic_telegram
    "instagram" -> com.replyai.R.drawable.ic_instagram
    "whatsapp" -> com.replyai.R.drawable.ic_whatsapp
    else -> com.replyai.R.drawable.ic_reply_ai
}

fun String.messengerFromPackage(packageName: String): String = when {
    packageName.contains("telegram") -> "telegram"
    packageName.contains("instagram") -> "instagram"
    packageName.contains("whatsapp") -> "whatsapp"
    else -> "telegram"
}

fun parseApiError(body: String?): String {
    if (body.isNullOrBlank()) return "Ошибка запроса"
    return try {
        val fieldErrors = Regex(""""(\w+)"\s*:\s*\["([^"]+)"/""").findAll(body)
            .map { "${it.groupValues[1]}: ${it.groupValues[2]}" }
            .joinToString("; ")
        if (fieldErrors.isNotBlank()) return fieldErrors
        Regex(""""detail"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1) ?: body.take(200)
    } catch (_: Exception) {
        body.take(200)
    }
}
