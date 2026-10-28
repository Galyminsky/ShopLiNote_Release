package com.jobforandroid.shoplinoterelease.activities

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
import com.jobforandroid.shoplinoterelease.R
import com.jobforandroid.shoplinoterelease.databinding.ActivityMainBinding
import com.jobforandroid.shoplinoterelease.dialogs.NewListDialog
import com.jobforandroid.shoplinoterelease.fragments.FragmentManager
import com.jobforandroid.shoplinoterelease.fragments.NoteFragment
import com.jobforandroid.shoplinoterelease.fragments.ShopListNamesFragment
import com.jobforandroid.shoplinoterelease.settings.SettingsFragment
import com.jobforandroid.shoplinoterelease.utils.ThemeManager


class MainActivity : AppCompatActivity(), NewListDialog.Listener {
    lateinit var binding: ActivityMainBinding
    private var currentMenuItemId = R.id.notes
    private lateinit var defPref: SharedPreferences
    private var currentTheme = ""
    private lateinit var navPanelBackground: FloatingNavBackgroundDrawable
    private var activeCircleAnimator: ValueAnimator? = null
    private var settingsNavigationOpen = false
    private var settingsReturnNavItemId = R.id.notes
    private var settingsReturnTitle: CharSequence? = null
    private var selectedNavigationItemId = R.id.notes
    private val themePreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == ThemeManager.THEME_KEY) {
                val selectedTheme = ThemeManager.getThemeKey(this)
                if (selectedTheme != currentTheme) {
                    currentTheme = selectedTheme
                    recreate()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        defPref = PreferenceManager.getDefaultSharedPreferences(this)
        currentTheme = ThemeManager.getThemeKey(this)
        setTheme(getSelectedTheme())
        super.onCreate(savedInstanceState)
        settingsNavigationOpen = savedInstanceState?.getBoolean(STATE_SETTINGS_OPEN) ?: false
        currentMenuItemId = savedInstanceState?.getInt(STATE_CURRENT_MENU_ITEM) ?: R.id.notes
        selectedNavigationItemId =
            savedInstanceState?.getInt(STATE_SELECTED_NAV_ITEM) ?: currentMenuItemId
        settingsReturnNavItemId =
            savedInstanceState?.getInt(STATE_SETTINGS_RETURN_NAV_ITEM) ?: R.id.notes
        settingsReturnTitle = savedInstanceState?.getCharSequence(STATE_SETTINGS_RETURN_TITLE)
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
        if (savedInstanceState == null) {
            FragmentManager.setFragment(ShopListNamesFragment.newInstance(), this)
        } else {
            (supportFragmentManager.findFragmentById(R.id.placeHolder) as? com.jobforandroid.shoplinoterelease.fragments.BaseFragment)
                ?.let { FragmentManager.currentFrag = it }
            supportActionBar?.title = if (settingsNavigationOpen) {
                getString(R.string.settings)
            } else {
                savedInstanceState.getCharSequence(STATE_ACTION_BAR_TITLE)
            }
        }
        supportFragmentManager.addOnBackStackChangedListener {
            if (settingsNavigationOpen &&
                supportFragmentManager.findFragmentByTag(SETTINGS_FRAGMENT_TAG)?.isVisible != true
            ) {
                settingsNavigationOpen = false
                (supportFragmentManager.findFragmentById(R.id.placeHolder) as? com.jobforandroid.shoplinoterelease.fragments.BaseFragment)
                    ?.let { FragmentManager.currentFrag = it }
                selectNavigationItem(settingsReturnNavItemId)
                supportActionBar?.title = settingsReturnTitle
            }
        }

        setBottomNavListener()
        selectNavigationItem(
            if (settingsNavigationOpen) R.id.settings else selectedNavigationItemId,
            animate = false
        )
        applyNavigationBarInsets()

    }

    override fun onStart() {
        super.onStart()
        defPref.registerOnSharedPreferenceChangeListener(themePreferenceChangeListener)
    }

    override fun onStop() {
        defPref.unregisterOnSharedPreferenceChangeListener(themePreferenceChangeListener)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_SETTINGS_OPEN, settingsNavigationOpen)
        outState.putInt(STATE_CURRENT_MENU_ITEM, currentMenuItemId)
        outState.putInt(STATE_SELECTED_NAV_ITEM, selectedNavigationItemId)
        outState.putInt(STATE_SETTINGS_RETURN_NAV_ITEM, settingsReturnNavItemId)
        outState.putCharSequence(STATE_SETTINGS_RETURN_TITLE, settingsReturnTitle)
        outState.putCharSequence(STATE_ACTION_BAR_TITLE, supportActionBar?.title)
        super.onSaveInstanceState(outState)
    }

    private fun setBottomNavListener() {
        binding.navNew.setOnClickListener {
            closeSettingsForNavigation()
            selectNavigationItem(R.id.new_item)
            FragmentManager.currentFrag?.onClickNew()
        }
        binding.navNotes.setOnClickListener {
            closeSettingsForNavigation()
            selectNavigationItem(R.id.notes)
            supportActionBar?.title = getString(R.string.notes)
            currentMenuItemId = R.id.notes
            FragmentManager.setFragment(NoteFragment.newInstance(), this)
        }
        binding.navPurchases.setOnClickListener {
            closeSettingsForNavigation()
            selectNavigationItem(R.id.shop_list)
            supportActionBar?.title = getString(R.string.shop_list)
            currentMenuItemId = R.id.shop_list
            FragmentManager.setFragment(ShopListNamesFragment.newInstance(), this)
        }
        binding.navSettings.setOnClickListener {
            showSettingsFragment()
        }
    }

    private fun showSettingsFragment() {
        val fragmentManager = supportFragmentManager
        if (settingsNavigationOpen ||
            fragmentManager.findFragmentByTag(SETTINGS_FRAGMENT_TAG)?.isAdded == true
        ) return

        settingsReturnNavItemId = selectedNavigationItemId
        settingsReturnTitle = supportActionBar?.title
        settingsNavigationOpen = true
        selectNavigationItem(R.id.settings)
        supportActionBar?.title = getString(R.string.settings)
        fragmentManager.beginTransaction()
            .replace(R.id.placeHolder, SettingsFragment(), SETTINGS_FRAGMENT_TAG)
            .addToBackStack(SETTINGS_BACK_STACK_TAG)
            .setReorderingAllowed(true)
            .commit()
    }

    private fun closeSettingsForNavigation() {
        if (!settingsNavigationOpen) return
        supportFragmentManager.popBackStackImmediate(
            SETTINGS_BACK_STACK_TAG,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        settingsNavigationOpen = false
    }

    private fun selectNavigationItem(itemId: Int, animate: Boolean = true) {
        selectedNavigationItemId = itemId
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
        if (settingsNavigationOpen) {
            selectNavigationItem(R.id.settings, animate = false)
        } else {
            selectNavigationItem(currentMenuItemId, animate = false)
        }
        if (ThemeManager.getThemeKey(this) != currentTheme) recreate()
    }

    private fun getSelectedTheme(): Int {
        return ThemeManager.getThemeStyle(this)
    }

    override fun onClick(name: String) {
        Log.d("MyLog", "Name: $name")
    }

    companion object {
        private const val SETTINGS_FRAGMENT_TAG = "main_settings_fragment"
        private const val SETTINGS_BACK_STACK_TAG = "main_settings_back_stack"
        private const val STATE_SETTINGS_OPEN = "main_settings_open"
        private const val STATE_CURRENT_MENU_ITEM = "main_current_menu_item"
        private const val STATE_SELECTED_NAV_ITEM = "main_selected_nav_item"
        private const val STATE_SETTINGS_RETURN_NAV_ITEM = "main_settings_return_nav_item"
        private const val STATE_SETTINGS_RETURN_TITLE = "main_settings_return_title"
        private const val STATE_ACTION_BAR_TITLE = "main_action_bar_title"
    }
}
