package com.jobforandroid.shoplinoterelease

import android.content.Intent
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jobforandroid.shoplinoterelease.activities.NewNoteActivityDs
import com.jobforandroid.shoplinoterelease.entities.NoteItem
import com.jobforandroid.shoplinoterelease.fragments.NoteFragmentDs
import com.jobforandroid.shoplinoterelease.ui.ds.RichTextFormatterDs
import com.jobforandroid.shoplinoterelease.utils.TimeManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorStateInstrumentedTest {
    @Test fun whitespaceEditIsDirtyAndStyledDraftSurvivesRecreationWithoutSaving() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // MIUI can silently reject startActivitySync from a background test process.
        // Enter through the exported launcher first, without changing device permissions or app data.
        val launchOutput = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "am start -W -n "+context.packageName+"/.activities.MainActivityDs"
        ).let { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
        }
        assertTrue(launchOutput, launchOutput.contains("Status: ok"))
        val intent = Intent(context, NewNoteActivityDs::class.java).putExtra(NoteFragmentDs.NEW_NOTE_KEY, NoteItem(7, "Title", "<p>Body</p>", TimeManager.getCurrentTime(), ""))
        ActivityScenario.launch<NewNoteActivityDs>(intent).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.etTitle).setText(" Title ")
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.tvEditStatus).visibility)
                val content = activity.findViewById<EditText>(R.id.etContent)
                content.setText(" body ")
                RichTextFormatterDs.toggleBold(content.text, 1, 5)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals(" Title ", activity.findViewById<EditText>(R.id.etTitle).text.toString())
                val content = activity.findViewById<EditText>(R.id.etContent).text
                assertEquals(" body ", content.toString())
                assertTrue((content as Spanned).getSpans(1, 5, StyleSpan::class.java).any { it.style and 1 != 0 })
            }
        }
    }
}
