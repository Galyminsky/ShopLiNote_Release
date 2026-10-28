package com.jobforandroid.shoplinoterelease

import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jobforandroid.shoplinoterelease.utils.ThemeManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeManagerInstrumentedTest {
    @Test
    fun migratesLegacyKeysAndDefaultsAndPersistsThemeSelection() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val hadOriginalValue = preferences.contains(ThemeManager.THEME_KEY)
        val originalValue = preferences.getString(ThemeManager.THEME_KEY, null)

        try {
            val cases = listOf(
                "main" to ThemeManager.LAVENDER_DREAM,
                "green" to ThemeManager.AMBER_GLOW,
                "red" to ThemeManager.FOREST_ESSENCE,
                ThemeManager.LAVENDER_DREAM to ThemeManager.LAVENDER_DREAM,
                ThemeManager.AMBER_GLOW to ThemeManager.AMBER_GLOW,
                ThemeManager.FOREST_ESSENCE to ThemeManager.FOREST_ESSENCE,
                "unknown_theme" to ThemeManager.LAVENDER_DREAM
            )

            for ((storedKey, expectedKey) in cases) {
                preferences.edit().putString(ThemeManager.THEME_KEY, storedKey).commit()
                assertEquals(expectedKey, ThemeManager.getThemeKey(context))
                assertEquals(expectedKey, preferences.getString(ThemeManager.THEME_KEY, null))
            }

            preferences.edit().remove(ThemeManager.THEME_KEY).commit()
            assertEquals(ThemeManager.LAVENDER_DREAM, ThemeManager.getThemeKey(context))
            assertEquals(
                ThemeManager.LAVENDER_DREAM,
                preferences.getString(ThemeManager.THEME_KEY, null)
            )

            repeat(3) {
                for (key in listOf(
                    ThemeManager.LAVENDER_DREAM,
                    ThemeManager.AMBER_GLOW,
                    ThemeManager.FOREST_ESSENCE
                )) {
                    preferences.edit().putString(ThemeManager.THEME_KEY, key).commit()
                    assertEquals(key, ThemeManager.getThemeKey(context))
                }
            }
        } finally {
            val editor = preferences.edit()
            if (hadOriginalValue) {
                editor.putString(ThemeManager.THEME_KEY, originalValue)
            } else {
                editor.remove(ThemeManager.THEME_KEY)
            }
            editor.commit()
        }
    }
}
