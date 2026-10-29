package com.jobforandroid.shoplinoterelease.utils

import android.content.SharedPreferences
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*

object TimeManager {
    const val DEF_TIME_FORMAT = "hh:mm:ss - dd.MM.yy"

    fun getCurrentTime(): String {
        val formatter = SimpleDateFormat(DEF_TIME_FORMAT, Locale.getDefault())
        return formatter.format(Calendar.getInstance().time)
    }

    fun getTimeFormat(time: String, defPreferences: SharedPreferences): String {
        val date = parseStoredTime(time) ?: return time
        val newFormat = getPreferredTimeFormat(defPreferences)
        return format(date, newFormat) ?: time
    }

    fun getTimeAndDate(time: String, preferences: SharedPreferences): Pair<String, String?> {
        val storedDate = parseStoredTime(time) ?: return time to null
        val format = getPreferredTimeFormat(preferences)
        val formatted = format(storedDate, format) ?: return time to null
        val parts = formatted.split(" - ", limit = 2)
        if (parts.size == 2) {
            val timePosition = format.indexOfAny(charArrayOf('h', 'H', 'k', 'K'))
            val datePosition = format.indexOf('d')
            return if (timePosition < datePosition) {
                parts[0] to parts[1]
            } else {
                parts[1] to parts[0]
            }
        }

        val date = runCatching {
            SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(storedDate)
        }.getOrNull()
        return formatted to date
    }

    private fun parseStoredTime(time: String): Date? {
        if (time.isBlank()) return null

        return runCatching {
            val formatter = SimpleDateFormat(DEF_TIME_FORMAT, Locale.getDefault()).apply {
                isLenient = false
            }
            val position = ParsePosition(0)
            formatter.parse(time, position)?.takeIf {
                position.index == time.length && position.errorIndex < 0
            }
        }.getOrNull()
    }

    private fun getPreferredTimeFormat(preferences: SharedPreferences): String {
        return runCatching {
            preferences.getString("time_format_key", DEF_TIME_FORMAT)
        }.getOrNull() ?: DEF_TIME_FORMAT
    }

    private fun format(date: Date, pattern: String): String? {
        return runCatching {
            SimpleDateFormat(pattern, Locale.getDefault()).format(date)
        }.getOrNull()
    }
}
