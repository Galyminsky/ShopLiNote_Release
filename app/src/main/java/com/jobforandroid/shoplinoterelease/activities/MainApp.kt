package com.jobforandroid.shoplinoterelease.activities

import android.app.Application
import com.jobforandroid.shoplinoterelease.BD.MainDataBase


class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        fun applyMode() {
            // Retain the preference and every other setting, but normalize obsolete values.
            if (runCatching { preferences.getString("ds_mode", "light") }.getOrNull() != "light") {
                preferences.edit().putString("ds_mode", "light").apply()
            }
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        }
        themeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if (key == "ds_mode") applyMode() }
        preferences.registerOnSharedPreferenceChangeListener(themeListener)
        applyMode()
    }
    private lateinit var themeListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener

    val database by lazy { MainDataBase.getDataBase(this) }

}