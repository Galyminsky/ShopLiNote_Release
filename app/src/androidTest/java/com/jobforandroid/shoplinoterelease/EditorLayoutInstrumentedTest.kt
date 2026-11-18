package com.jobforandroid.shoplinoterelease

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jobforandroid.shoplinoterelease.databinding.ActivityNoteEditDsBinding
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorLayoutInstrumentedTest {
    @Test fun editorKeepsDateLabelAndTitleOrderedAtBothViewportSizes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_App_DS)
            val binding = ActivityNoteEditDsBinding.inflate(LayoutInflater.from(context))
            val density = context.resources.displayMetrics.density
            fun bounds(view: View) = Rect(0, 0, view.width, view.height).also { binding.svEditor.offsetDescendantRectToMyCoords(view, it) }
            binding.etContent.setText("Long note line\n".repeat(2000))
            for (title in listOf("", "Short title", "First line\nSecond line\nThird line")) {
                binding.etTitle.setText(title)
                for (height in listOf(640, 320, 640)) {
                    for (focus in listOf(binding.etTitle, binding.etContent)) {
                        focus.requestFocus()
                        binding.root.measure(View.MeasureSpec.makeMeasureSpec((360 * density).toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec((height * density).toInt(), View.MeasureSpec.EXACTLY))
                        binding.root.layout(0, 0, binding.root.measuredWidth, binding.root.measuredHeight)
                        val date = bounds(binding.tvEditDate)
                        val label = bounds(binding.tvFieldLabel)
                        val titleBounds = bounds(binding.etTitle)
                        assertTrue("Date must precede the field label", date.bottom <= label.top)
                        assertTrue("Label must precede title", label.bottom <= titleBounds.top)
                        assertTrue("The editor card must retain its measured content", binding.cardEditor.height >= binding.etContent.height)
                        assertTrue(binding.cardEditor.clipChildren)
                    }
                }
            }
        }
    }

    @Test fun obsoleteDarkModeIsNormalizedAndThemeChoiceRemains() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val titleSize = preferences.getString("title_size_key", null)
            val contentSize = preferences.getString("content_size_key", null)
            preferences.edit().putString("ds_mode", "dark").commit()
            assertEquals("light", preferences.getString("ds_mode", null))
            assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())
            assertEquals(listOf("light"), context.resources.getStringArray(R.array.ds_mode_values).toList())
            assertEquals(1, context.resources.getStringArray(R.array.ds_mode_labels).size)
            assertEquals(titleSize, preferences.getString("title_size_key", null))
            assertEquals(contentSize, preferences.getString("content_size_key", null))
        }
    }
}
