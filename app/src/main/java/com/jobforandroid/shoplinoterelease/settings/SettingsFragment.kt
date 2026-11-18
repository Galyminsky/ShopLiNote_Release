package com.jobforandroid.shoplinoterelease.settings

import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroupAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jobforandroid.shoplinoterelease.R
import com.jobforandroid.shoplinoterelease.ui.ds.DsDialogs
import com.jobforandroid.shoplinoterelease.ui.ds.NeumorphicSurface

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): View {
        val preferences = super.onCreateView(inflater, container, savedInstanceState)
        val root = inflater.inflate(R.layout.fragment_settings_ds, container, false)
        root.findViewById<android.widget.FrameLayout>(R.id.settingsListViewport).addView(
            preferences,
            android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
        )
        return root
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.setting_preference, rootKey)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference !is ListPreference) { super.onDisplayPreferenceDialog(preference); return }
        DsDialogs.choose(requireContext(), preference.dialogTitle ?: preference.title ?: getString(R.string.settings), preference.entries,
            preference.findIndexOfValue(preference.value)) { index ->
            val value = preference.entryValues[index].toString()
            if (preference.callChangeListener(value)) preference.value = value
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ds_surface))
        setDivider(null)
        listView.clipChildren = false
        listView.clipToPadding = false
        val padding = resources.getDimensionPixelSize(R.dimen.ds_screen_padding_h)
        val gap = resources.getDimensionPixelSize(R.dimen.ds_settings_section_gap)
        listView.setPadding(padding, resources.getDimensionPixelSize(R.dimen.ds_space_block), padding, gap)
        listView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            private val panels = mutableMapOf<Preference, NeumorphicSurface>()
            override fun getItemOffsets(outRect: Rect, child: View, parent: RecyclerView, state: RecyclerView.State) {
                val adapter = parent.adapter as? PreferenceGroupAdapter ?: return
                val position = parent.getChildAdapterPosition(child)
                if (position == RecyclerView.NO_POSITION) return
                val item = adapter.getItem(position)
                if (item is PreferenceCategory && position > 0) outRect.top = gap
            }
            override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                val adapter = parent.adapter as? PreferenceGroupAdapter ?: return
                val groups = linkedMapOf<Preference, Rect>()
                for (i in 0 until parent.childCount) {
                    val child = parent.getChildAt(i)
                    val position = parent.getChildAdapterPosition(child)
                    if (position == RecyclerView.NO_POSITION) continue
                    val item = adapter.getItem(position) ?: continue
                    val category = if (item is PreferenceCategory) item else item.parent ?: continue
                    val bounds = groups.getOrPut(category) { Rect(padding, child.top, parent.width - padding, child.bottom) }
                    bounds.bottom = child.bottom
                    if (item !is PreferenceCategory && groups[category]?.top == child.top) bounds.top = -gap * 2
                }
                groups.forEach { (category, bounds) ->
                    if (bounds.width() <= 0 || bounds.height() <= 0) return@forEach
                    val panel = panels.getOrPut(category) {
                        (layoutInflater.inflate(R.layout.ds_settings_surface, parent, false) as NeumorphicSurface)
                    }
                    panel.measure(View.MeasureSpec.makeMeasureSpec(bounds.width(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(bounds.height(), View.MeasureSpec.EXACTLY))
                    panel.layout(0, 0, bounds.width(), bounds.height())
                    val save = canvas.save()
                    canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
                    panel.draw(canvas)
                    canvas.restoreToCount(save)
                }
            }
        })
    }
}
