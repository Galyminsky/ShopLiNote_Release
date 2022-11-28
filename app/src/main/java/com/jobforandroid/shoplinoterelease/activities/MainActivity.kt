package com.jobforandroid.shoplinoterelease.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
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
    private var iAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        defPref = PreferenceManager.getDefaultSharedPreferences(this)
        currentTheme = defPref.getString("theme_key", "main").toString()
        setTheme(getSelectedTheme())
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FragmentManager.setFragment(ShopListNamesFragment.newInstance(), this)

        setBottomNavListener()
        loadInterAd()

    }

    private fun loadInterAd() {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(this, getString(R.string.inter_ad_id), request,
        object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(p0: InterstitialAd) {
                iAd = p0
                Log.d("MyLog", "Load Ok")
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                iAd = null
                Log.d("MyLog", "Load Error")
            }
        })
    }

    private fun showInterAd(adListener: AdListener) {
        if (iAd != null) {
            iAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    iAd = null
                    loadInterAd()
                    adListener.onFinish()
                }

                override fun onAdFailedToShowFullScreenContent(ad: AdError) {
                    iAd = null
                    loadInterAd()
                }

                override fun onAdShowedFullScreenContent() {
                    iAd = null
                    loadInterAd()
                    adListener.onFinish()
                }
            }
            iAd?.show(this@MainActivity)
        } else{
            adListener.onFinish()
        }
    }

    private fun setBottomNavListener() {
        binding.bNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.settings -> {
                    showInterAd(object : AdListener {
                        override fun onFinish() {
                            startActivity(Intent(this@MainActivity, SettingActivity::class.java))
                        }
                    })

                }
                R.id.notes -> {
                    supportActionBar?.title = getString(R.string.notes)
                    currentMenuItemId = R.id.notes
                    FragmentManager.setFragment(NoteFragment.newInstance(), this)
                }
                R.id.shop_list -> {
                    supportActionBar?.title = getString(R.string.shop_list)
                    currentMenuItemId = R.id.shop_list
                    FragmentManager.setFragment(ShopListNamesFragment.newInstance(), this)
                }
                R.id.new_item -> {
                    supportActionBar?.title = getString(R.string.new_item)
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

    interface AdListener {
        fun onFinish()
    }
}