package com.jobforandroid.shoplinoterelease.utils

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.jobforandroid.shoplinoterelease.R

object ThemeManager {
    const val THEME_KEY = "theme_key"
    const val LAVENDER_DREAM = "lavender_dream"
    const val AMBER_GLOW = "amber_glow"
    const val FOREST_ESSENCE = "forest_essence"

    fun getThemeKey(context: Context): String {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val savedTheme = preferences.getString(THEME_KEY, null)
        val theme = when (savedTheme) {
            "main", LAVENDER_DREAM -> LAVENDER_DREAM
            "green", AMBER_GLOW -> AMBER_GLOW
            "red", FOREST_ESSENCE -> FOREST_ESSENCE
            else -> LAVENDER_DREAM
        }
        if (savedTheme != theme) {
            preferences.edit { putString(THEME_KEY, theme) }
        }
        return theme
    }

    fun getThemeStyle(context: Context, newNote: Boolean = false): Int {
        return when (getThemeKey(context)) {
            AMBER_GLOW -> if (newNote) R.style.Theme_NewNoteAmberGlow else R.style.Theme_ShopLiNoteAmberGlow
            FOREST_ESSENCE -> if (newNote) R.style.Theme_NewNoteForestEssence else R.style.Theme_ShopLiNoteForestEssence
            else -> if (newNote) R.style.Theme_NewNoteLavenderDream else R.style.Theme_ShopLiNoteLavenderDream
        }
    }
}
