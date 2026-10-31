package com.jobforandroid.shoplinoterelease.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.jobforandroid.shoplinoterelease.R
import com.jobforandroid.shoplinoterelease.databinding.ActivityMainDsBinding
import com.jobforandroid.shoplinoterelease.fragments.BaseFragment
import com.jobforandroid.shoplinoterelease.fragments.FragmentManager
import com.jobforandroid.shoplinoterelease.fragments.NoteFragmentDs
import com.jobforandroid.shoplinoterelease.fragments.ShopListNamesFragment
import com.jobforandroid.shoplinoterelease.settings.SettingsFragment

class MainActivityDs : AppCompatActivity() {
    private lateinit var binding: ActivityMainDsBinding
    private var selectedTabIndex = 0
    private var settingsOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainDsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            FragmentManager.setFragment(NoteFragmentDs.newInstance(), this)
        } else {
            settingsOpen = supportFragmentManager.findFragmentByTag(SETTINGS_FRAGMENT_TAG)?.isAdded == true
            (supportFragmentManager.findFragmentById(R.id.placeHolder) as? BaseFragment)
                ?.let { FragmentManager.currentFrag = it }
            selectedTabIndex = savedInstanceState.getInt(STATE_SELECTED_TAB, 0)
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val current = supportFragmentManager.findFragmentById(R.id.placeHolder)
            settingsOpen = current is SettingsFragment
            (current as? BaseFragment)?.let { FragmentManager.currentFrag = it }
            if (!settingsOpen && current is NoteFragmentDs) selectTab(0)
            if (!settingsOpen && current is ShopListNamesFragment) selectTab(1)
        }

        binding.bottomNavInclude.navNotes.setOnClickListener {
            leaveSettingsIfNeeded()
            selectTab(0)
            FragmentManager.setFragment(NoteFragmentDs.newInstance(), this)
        }
        binding.bottomNavInclude.navPurchases.setOnClickListener {
            leaveSettingsIfNeeded()
            selectTab(1)
            FragmentManager.setFragment(ShopListNamesFragment.newInstance(), this)
        }
        binding.bottomNavInclude.navSettings.setOnClickListener {
            if (!settingsOpen) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.placeHolder, SettingsFragment(), SETTINGS_FRAGMENT_TAG)
                    .addToBackStack(SETTINGS_BACK_STACK_TAG)
                    .setReorderingAllowed(true)
                    .commit()
                settingsOpen = true
                selectTab(2)
            }
        }
        binding.newNoteInclude.navNew.setOnClickListener {
            if (!settingsOpen) FragmentManager.currentFrag?.onClickNew()
        }

        selectTab(if (settingsOpen) 2 else selectedTabIndex)
    }

    private fun leaveSettingsIfNeeded() {
        if (settingsOpen) {
            supportFragmentManager.popBackStackImmediate(
                SETTINGS_BACK_STACK_TAG,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            settingsOpen = false
        }
    }

    private fun selectTab(index: Int) {
        selectedTabIndex = index
        val tabs = listOf(
            Triple(binding.bottomNavInclude.navNotesBg, binding.bottomNavInclude.navNotesIcon, binding.bottomNavInclude.navNotesLabel),
            Triple(binding.bottomNavInclude.navPurchasesBg, binding.bottomNavInclude.navPurchasesIcon, binding.bottomNavInclude.navPurchasesLabel),
            Triple(binding.bottomNavInclude.navSettingsBg, binding.bottomNavInclude.navSettingsIcon, binding.bottomNavInclude.navSettingsLabel)
        )
        val accent = ContextCompat.getColor(this, R.color.ds_accent)
        val inactive = ContextCompat.getColor(this, R.color.ds_on_surface_variant)
        tabs.forEachIndexed { tabIndex, (background, icon, label) ->
            val active = tabIndex == index
            background.visibility = if (active) View.VISIBLE else View.GONE
            ImageViewCompat.setImageTintList(
                icon,
                android.content.res.ColorStateList.valueOf(if (active) accent else inactive)
            )
            label.setTextAppearance(
                if (active) R.style.TextAppearance_DS_NavLabel_Active
                else R.style.TextAppearance_DS_NavLabel
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_SELECTED_TAB, selectedTabIndex)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val SETTINGS_FRAGMENT_TAG = "main_settings_fragment_ds"
        private const val SETTINGS_BACK_STACK_TAG = "main_settings_back_stack_ds"
        private const val STATE_SELECTED_TAB = "main_selected_tab_ds"
    }
}
