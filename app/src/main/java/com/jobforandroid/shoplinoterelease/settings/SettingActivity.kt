package com.jobforandroid.shoplinoterelease.settings


import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.jobforandroid.shoplinoterelease.R

class SettingActivity : AppCompatActivity() {

    private lateinit var defPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        defPref = PreferenceManager.getDefaultSharedPreferences(this)
        setTheme(getSelectedTheme())
        setContentView(R.layout.activity_setting)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.placeHolder, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getSelectedTheme(): Int {
        return if (defPref.getString("theme_key", "main") == "main") {
            R.style.Theme_ShopLiNoteMain
        } else if (defPref.getString("theme_key", "main") == "green") {
            R.style.Theme_ShopLiNoteGreen
        } else {
            (defPref.getString("theme_key", "main") == "red")
            R.style.Theme_ShopLiNoteRed
        }
    }
}