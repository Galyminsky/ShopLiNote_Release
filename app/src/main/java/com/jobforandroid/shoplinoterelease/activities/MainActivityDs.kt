package com.jobforandroid.shoplinoterelease.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.jobforandroid.shoplinoterelease.R
import com.jobforandroid.shoplinoterelease.databinding.ActivityMainDsBinding
import com.jobforandroid.shoplinoterelease.fragments.BaseFragment
import com.jobforandroid.shoplinoterelease.fragments.NoteFragmentDs
import com.jobforandroid.shoplinoterelease.fragments.ShopListNamesFragment
import com.jobforandroid.shoplinoterelease.settings.SettingsFragment

class MainActivityDs : AppCompatActivity() {
    private lateinit var binding: ActivityMainDsBinding
    private var selectedTabIndex = 0
    private var previousTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainDsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom + resources.getDimensionPixelSize(R.dimen.ds_nav_bottom_gap))
            insets
        }
        androidx.core.view.ViewCompat.requestApplyInsets(binding.root)
        previousTab = savedInstanceState?.getInt("previous_tab") ?: 0
        binding.bottomNavInclude.navNotes.setOnClickListener { showTab(0) }
        binding.bottomNavInclude.navPurchases.setOnClickListener { showTab(1) }
        binding.bottomNavInclude.navSettings.setOnClickListener { showTab(2) }
        binding.newNoteInclude.navNew.setOnClickListener {
            (supportFragmentManager.findFragmentByTag("ds_tab_" + selectedTabIndex) as? BaseFragment)?.onClickNew()
        }
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (selectedTabIndex) {
                    2 -> showTab(previousTab)
                    1 -> showTab(0)
                    else -> { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
                }
            }
        })
        selectedTabIndex = savedInstanceState?.getInt("selected_tab") ?: 0
        showTab(selectedTabIndex)
    }

    private fun showTab(index: Int) {
        if (index == 2 && selectedTabIndex != 2) previousTab = selectedTabIndex
        val manager = supportFragmentManager
        val transaction = manager.beginTransaction().setReorderingAllowed(true)
        manager.fragments.filter { it.tag?.startsWith("ds_tab_") == true }.forEach {
            transaction.hide(it).setMaxLifecycle(it, androidx.lifecycle.Lifecycle.State.STARTED)
        }
        val tag = "ds_tab_" + index
        val fragment = manager.findFragmentByTag(tag) ?: when (index) {
            0 -> NoteFragmentDs.newInstance()
            1 -> ShopListNamesFragment.newInstance()
            else -> SettingsFragment()
        }
        if (!fragment.isAdded) transaction.add(R.id.placeHolder, fragment, tag) else transaction.show(fragment)
        transaction.setMaxLifecycle(fragment, androidx.lifecycle.Lifecycle.State.RESUMED).commitNow()
        selectTab(index)
        binding.newNoteInclude.root.visibility = if (index == 2) View.GONE else View.VISIBLE
        binding.newNoteInclude.navNew.setText(if (index == 1) R.string.shop_new_list else R.string.note_edit_new_title)
    }

    private fun selectTab(index: Int) {
        selectedTabIndex = index
        val tabs = listOf(
            Triple(binding.bottomNavInclude.navNotesBg, binding.bottomNavInclude.navNotesIcon, binding.bottomNavInclude.navNotesLabel),
            Triple(binding.bottomNavInclude.navPurchasesBg, binding.bottomNavInclude.navPurchasesIcon, binding.bottomNavInclude.navPurchasesLabel),
            Triple(binding.bottomNavInclude.navSettingsBg, binding.bottomNavInclude.navSettingsIcon, binding.bottomNavInclude.navSettingsLabel)
        )
        listOf(binding.bottomNavInclude.navNotes, binding.bottomNavInclude.navPurchases, binding.bottomNavInclude.navSettings).forEachIndexed { tab, view -> view.isSelected = tab == index }
        val accent = ContextCompat.getColor(this, R.color.ds_accent_muted)
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
        outState.putInt("selected_tab", selectedTabIndex)
        outState.putInt("previous_tab", previousTab)
        super.onSaveInstanceState(outState)
    }
}
