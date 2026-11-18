package com.jobforandroid.shoplinoterelease.utils

import android.content.SharedPreferences
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

object TimeManager {
    // Kept for decoding previously installed versions; those values have no AM/PM information.
    const val DEF_TIME_FORMAT = "hh:mm:ss - dd.MM.yy"
    const val DEFAULT_DISPLAY_FORMAT = "HH:mm:ss - dd.MM.yyyy"

    // UTC ISO timestamps preserve afternoon hours, milliseconds and time zones without a schema change.
    fun getCurrentTime(): String = Instant.now().toString()

    fun getTimeFormat(time: String, preferences: SharedPreferences): String {
        val date = parseStoredTime(time) ?: return time
        return format(date, preferredFormat(preferences)) ?: time
    }

    fun getTimeFormat(timestamp: Long, preferences: SharedPreferences): String =
        format(Date(timestamp), preferredFormat(preferences)) ?: timestamp.toString()

    fun getTimeAndDate(time: String, preferences: SharedPreferences): Pair<String, String?> {
        val date = parseStoredTime(time) ?: return time to null
        return timeAndDate(date, preferences)
    }

    fun getTimeAndDate(timestamp: Long, preferences: SharedPreferences): Pair<String, String?> =
        timeAndDate(Date(timestamp), preferences)

    private fun timeAndDate(date: Date, preferences: SharedPreferences): Pair<String, String?> {
        val pattern = preferredFormat(preferences)
        val formatted = format(date, pattern) ?: format(date, DEFAULT_DISPLAY_FORMAT).orEmpty()
        val parts = formatted.split(" - ", limit = 2)
        if (parts.size == 2) {
            val timePosition = pattern.indexOfAny(charArrayOf('h', 'H', 'k', 'K'))
            val datePosition = pattern.indexOf('d')
            return if (timePosition < datePosition) parts[0] to parts[1] else parts[1] to parts[0]
        }
        return formatted to format(date, "dd.MM.yy")
    }

    fun parseStoredTime(time: String): Date? {
        if (time.isBlank()) return null
        runCatching { Date.from(Instant.parse(time)) }.getOrNull()?.let { return it }
        return runCatching {
            val formatter = SimpleDateFormat(DEF_TIME_FORMAT, Locale.getDefault()).apply { isLenient = false }
            val position = ParsePosition(0)
            formatter.parse(time, position)?.takeIf { position.index == time.length && position.errorIndex < 0 }
        }.getOrNull()
    }

    private fun preferredFormat(preferences: SharedPreferences): String {
        val saved = runCatching { preferences.getString("time_format_key", DEFAULT_DISPLAY_FORMAT) }.getOrNull()
        return normalizedDisplayFormat(saved)
    }

    fun normalizedDisplayFormat(saved: String?): String = when (saved) {
        "dd.MM.yyyy - hh:mm:ss", "dd.MM.yyyy - HH:mm:ss" -> "dd.MM.yyyy - HH:mm:ss"
        "h:mm a" -> "h:mm a"
        else -> DEFAULT_DISPLAY_FORMAT
    }

    private fun format(date: Date, pattern: String): String? = runCatching {
        SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }.getOrNull()
}
