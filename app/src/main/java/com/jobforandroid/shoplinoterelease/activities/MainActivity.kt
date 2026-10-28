package com.jobforandroid.shoplinoterelease.activities

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.animation.ValueAnimator
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.jobforandroid.shoplinoterelease.utils.ThemeManager


class MainActivity : AppCompatActivity(), NewListDialog.Listener {
    lateinit var binding: ActivityMainBinding
    private var currentMenuItemId = R.id.notes
    private lateinit var defPref: SharedPreferences
    private var currentTheme = ""
    private var iAd: InterstitialAd? = null
    private lateinit var navPanelBackground: FloatingNavBackgroundDrawable
    private var activeCircleAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        defPref = PreferenceManager.getDefaultSharedPreferences(this)
        currentTheme = ThemeManager.getThemeKey(this)
        setTheme(getSelectedTheme())
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val outlineValue = TypedValue()
        theme.resolveAttribute(R.attr.theme_card_outline, outlineValue, true)
        val outlineColor = if (outlineValue.resourceId != 0) {
            ContextCompat.getColor(this, outlineValue.resourceId)
        } else {
            outlineValue.data
        }
        navPanelBackground = FloatingNavBackgroundDrawable(
            outlineColor,
            resources.displayMetrics.density
        )
        binding.bNav.background = navPanelBackground
        FragmentManager.setFragment(ShopListNamesFragment.newInstance(), this)

        setBottomNavListener()
        selectNavigationItem(currentMenuItemId, animate = false)
        applyNavigationBarInsets()
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
        } else {
            adListener.onFinish()
        }
    }

    private fun setBottomNavListener() {
        binding.navNew.setOnClickListener {
            selectNavigationItem(R.id.new_item)
            supportActionBar?.title = getString(R.string.new_item)
            FragmentManager.currentFrag?.onClickNew()
        }
        binding.navNotes.setOnClickListener {
            selectNavigationItem(R.id.notes)
            supportActionBar?.title = getString(R.string.notes)
            currentMenuItemId = R.id.notes
            FragmentManager.setFragment(NoteFragment.newInstance(), this)
        }
        binding.navPurchases.setOnClickListener {
            selectNavigationItem(R.id.shop_list)
            supportActionBar?.title = getString(R.string.shop_list)
            currentMenuItemId = R.id.shop_list
            FragmentManager.setFragment(ShopListNamesFragment.newInstance(), this)
        }
        binding.navSettings.setOnClickListener {
            selectNavigationItem(R.id.settings)
            showInterAd(object : AdListener {
                override fun onFinish() {
                    startActivity(Intent(this@MainActivity, SettingActivity::class.java))
                }
            })
        }
    }

    private fun selectNavigationItem(itemId: Int, animate: Boolean = true) {
        val inactiveColor = ContextCompat.getColor(this, R.color.nav_inactive_gray)
        val themePrimary = when (ThemeManager.getThemeKey(this)) {
            ThemeManager.AMBER_GLOW -> ContextCompat.getColor(this, R.color.amber_glow_primary)
            ThemeManager.FOREST_ESSENCE -> ContextCompat.getColor(this, R.color.forest_essence_primary)
            else -> ContextCompat.getColor(this, R.color.lavender_dream_primary)
        }
        val entries = listOf(
            NavEntry(R.id.new_item, binding.navNew, binding.navNewIcon, binding.navNewLabel,
                R.drawable.ic_new_gradient, themePrimary),
            NavEntry(R.id.notes, binding.navNotes, binding.navNotesIcon, binding.navNotesLabel,
                R.drawable.ic_note, ContextCompat.getColor(this, R.color.amber_glow_primary)),
            NavEntry(R.id.shop_list, binding.navPurchases, binding.navPurchasesIcon,
                binding.navPurchasesLabel, R.drawable.ic_shopp,
                ContextCompat.getColor(this, R.color.forest_essence_primary)),
            NavEntry(R.id.settings, binding.navSettings, binding.navSettingsIcon,
                binding.navSettingsLabel, R.drawable.ic_settings, themePrimary)
        )
        val selected = entries.firstOrNull { it.menuId == itemId } ?: return
        entries.forEach { entry ->
            val color = if (entry.menuId == itemId) selected.accentColor else inactiveColor
            entry.icon.imageTintList = ColorStateList.valueOf(color)
            entry.icon.alpha = if (entry.menuId == itemId) 0f else 1f
            entry.label.setTextColor(color)
        }
        binding.navActiveIcon.setImageResource(selected.iconRes)
        binding.navActiveIcon.imageTintList = ColorStateList.valueOf(selected.accentColor)
        binding.navActiveCircle.post {
            val activeCircle = binding.navActiveCircle
            val panelWidth = binding.bNav.width.toFloat()
            val rawCenter = selected.cell.x + selected.cell.width / 2f
            val safeCenter = if (panelWidth < navPanelBackground.minimumNotchCenter * 2f) {
                panelWidth / 2f
            } else {
                rawCenter.coerceIn(
                    navPanelBackground.minimumNotchCenter,
                    panelWidth - navPanelBackground.minimumNotchCenter
                )
            }
            val targetX = safeCenter - activeCircle.width / 2f
            activeCircleAnimator?.cancel()
            if (animate) {
                activeCircleAnimator = ValueAnimator.ofFloat(activeCircle.translationX, targetX).apply {
                    duration = 240
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { animator ->
                        val circleX = animator.animatedValue as Float
                        activeCircle.translationX = circleX
                        navPanelBackground.setNotchCenter(circleX + activeCircle.width / 2f)
                    }
                    start()
                }
            } else {
                activeCircle.translationX = targetX
                navPanelBackground.setNotchCenter(safeCenter)
            }
        }
    }

    private fun applyNavigationBarInsets() {
        val baseBottomMargin = (16 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot) { _, insets ->
            val navigationInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val params = binding.bNav.layoutParams as ConstraintLayout.LayoutParams
            params.bottomMargin = baseBottomMargin + navigationInset
            binding.bNav.layoutParams = params
            insets
        }
        ViewCompat.requestApplyInsets(binding.mainRoot)
    }

    private data class NavEntry(
        val menuId: Int,
        val cell: View,
        val icon: android.widget.ImageView,
        val label: android.widget.TextView,
        val iconRes: Int,
        val accentColor: Int
    )

    override fun onResume() {
        super.onResume()
        selectNavigationItem(currentMenuItemId, animate = false)
        if (ThemeManager.getThemeKey(this) != currentTheme) recreate()
    }

    private fun getSelectedTheme(): Int {
        return ThemeManager.getThemeStyle(this)
    }

    override fun onClick(name: String) {
        Log.d("MyLog", "Name: $name")
    }

    interface AdListener {
        fun onFinish()
    }
}
