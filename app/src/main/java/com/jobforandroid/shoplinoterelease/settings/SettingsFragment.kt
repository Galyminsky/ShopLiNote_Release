package com.jobforandroid.shoplinoterelease.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.jobforandroid.shoplinoterelease.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.setting_preference, rootKey)
    }
}