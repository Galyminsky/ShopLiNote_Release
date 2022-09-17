package com.jobforandroid.shoplinoterelease.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.jobforandroid.shoplinoterelease.R
import com.jobforandroid.shoplinoterelease.databinding.ActivityMainBinding
import com.jobforandroid.shoplinoterelease.dialogs.NewListDialog
import com.jobforandroid.shoplinoterelease.fragments.FragmentManager
import com.jobforandroid.shoplinoterelease.fragments.NoteFragment
import com.jobforandroid.shoplinoterelease.fragments.ShopListNamesFragment
import com.jobforandroid.shoplinoterelease.settings.SettingActivity


class MainActivity : AppCompatActivity(), NewListDialog.Listener {
    lateinit var binding: ActivityMainBinding
    private var currentMenuItemId = R.id.notes
    private lateinit var defPref: SharedPreferences
    private var currentTheme = ""

    override fun onCreate(savedInstanceState: Bundle?) {

        defPref = PreferenceManager.getDefaultSharedPreferences(this)
        currentTheme = defPref.getString("theme_key", "main").toString()
        setTheme(getSelectedTheme())

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        FragmentManager.setFragment(ShopListNamesFragment.newInstance(), this)

        setBottomNavListener()

    }

    private fun setBottomNavListener() {
        binding.bNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.settings -> {
                    startActivity(Intent(this, SettingActivity::class.java))
                }
                R.id.notes -> {
                    currentMenuItemId = R.id.notes
                    FragmentManager.setFragment(NoteFragment.newInstance(), this)
                }
                R.id.shop_list -> {
                    currentMenuItemId = R.id.shop_list
                    FragmentManager.setFragment(ShopListNamesFragment.newInstance(), this)
                }
                R.id.new_item -> {
                    FragmentManager.currentFrag?.onClickNew()
                }
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bNav.selectedItemId = currentMenuItemId
        if (defPref.getString("theme_key", "main") != currentTheme) recreate()
    }

    private fun getSelectedTheme(): Int {
        return if (defPref.getString("theme_key", "main") == "main") {
            R.style.Theme_ShopLiNoteMain
        } else if (defPref.getString("theme_key", "green") == "green") {
            R.style.Theme_ShopLiNoteGreen
        } else {
            (defPref.getString("theme_key", "red") == "red")
            R.style.Theme_ShopLiNoteRed
        }
    }

    override fun onClick(name: String) {
        Log.d("MyLog", "Name: $name")
    }
}