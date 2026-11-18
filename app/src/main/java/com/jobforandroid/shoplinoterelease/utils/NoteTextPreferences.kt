package com.jobforandroid.shoplinoterelease.utils

import android.util.TypedValue
import android.widget.TextView
import androidx.preference.PreferenceManager

object NoteTextPreferences {
    fun apply(title: TextView, content: TextView) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(title.context)
        fun size(key: String, fallback: Int): Float = runCatching { preferences.getString(key, null)?.toIntOrNull() }.getOrNull()
            ?.takeIf { it in 12..34 }?.toFloat() ?: fallback.toFloat()
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, size("title_size_key", 20))
        content.setTextSize(TypedValue.COMPLEX_UNIT_SP, size("content_size_key", 16))
        title.setLineSpacing(0f, 1.2f)
        content.setLineSpacing(0f, 1.4f)
    }
}
