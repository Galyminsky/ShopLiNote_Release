package com.jobforandroid.shoplinoterelease.utils

import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

object TimeManager {
    const val DEF_TIME_FORMAT = "hh:mm:ss - dd.MM.yy"

    fun getCurrentTime(): String {
        val formatter = SimpleDateFormat(DEF_TIME_FORMAT, Locale.getDefault())
        return formatter.format(Calendar.getInstance().time)
    }

    fun getTimeFormat(time: String, defPreferences: SharedPreferences): String {
        val defFormatter = SimpleDateFormat(DEF_TIME_FORMAT, Locale.getDefault())
        val defDate = defFormatter.parse(time)
        val newFormat = defPreferences.getString("time_format_key", DEF_TIME_FORMAT)
        val newFormatter = SimpleDateFormat(newFormat, Locale.getDefault())
        return if (defDate != null) {
            newFormatter.format(defDate)
        } else {
            time
        }
    }

    fun getTimeAndDate(time: String, preferences: SharedPreferences): Pair<String, String?> {
        val formatted = getTimeFormat(time, preferences)
        val parts = formatted.split(" - ", limit = 2)
        if (parts.size == 2) {
            val format = preferences.getString("time_format_key", DEF_TIME_FORMAT).orEmpty()
            val timePosition = format.indexOfAny(charArrayOf('h', 'H', 'k', 'K'))
            val datePosition = format.indexOf('d')
            return if (timePosition < datePosition) {
                parts[0] to parts[1]
            } else {
                parts[1] to parts[0]
            }
        }

        val storedDate = runCatching {
            SimpleDateFormat(DEF_TIME_FORMAT, Locale.getDefault()).parse(time)
        }.getOrNull()
        val date = storedDate?.let {
            SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(it)
        } ?: time.substringAfterLast(" - ").takeIf { " - " in time }

        return formatted to date
    }
}
