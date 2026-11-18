package com.jobforandroid.shoplinoterelease.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.jobforandroid.shoplinoterelease.R
import com.jobforandroid.shoplinoterelease.databinding.ActivityNoteViewDsBinding
import com.jobforandroid.shoplinoterelease.entities.NoteItem
import com.jobforandroid.shoplinoterelease.fragments.NoteFragmentDs
import com.jobforandroid.shoplinoterelease.ui.ds.DeleteNoteDialogDs
import com.jobforandroid.shoplinoterelease.utils.HtmlManager
import com.jobforandroid.shoplinoterelease.utils.TimeManager

class NoteViewActivityDs : AppCompatActivity() {
    private lateinit var binding: ActivityNoteViewDsBinding
    private lateinit var editorLauncher: ActivityResultLauncher<Intent>
    private lateinit var note: NoteItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteViewDsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setResult(Activity.RESULT_CANCELED)

        note = try {
            @Suppress("DEPRECATION")
            (savedInstanceState?.getSerializable(NOTE_EXTRA) ?: intent.getSerializableExtra(NOTE_EXTRA)) as? NoteItem
        } catch (_: RuntimeException) {
            null
        } ?: run {
            finish()
            return
        }

        editorLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val editedNote = try {
                @Suppress("DEPRECATION")
                result.data?.getSerializableExtra(NoteFragmentDs.NEW_NOTE_KEY) as? NoteItem
            } catch (_: RuntimeException) {
                null
            } ?: return@registerForActivityResult
            val state = try {
                result.data?.getStringExtra(NoteFragmentDs.EDIT_STATE_KEY)
            } catch (_: RuntimeException) {
                null
            }
            if (state == "update" && editedNote.id != null && editedNote.id > 0) {
                note = note.copy(
                    title = editedNote.title,
                    content = editedNote.content,
                    modifiedAt = editedNote.modifiedAt, hasBeenEdited = editedNote.hasBeenEdited
                )
                renderNote()
                setResult(Activity.RESULT_OK, buildResultIntent("update", note))
            }
        }

        if (savedInstanceState?.getBoolean("updated") == true) setResult(Activity.RESULT_OK, buildResultIntent("update", note))
        renderNote()
        binding.btnBack.setOnClickListener { finish() }
        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, NewNoteActivityDs::class.java).apply {
                putExtra(NoteFragmentDs.NEW_NOTE_KEY, note)
            }
            editorLauncher.launch(intent)
        }
        binding.btnDelete.setOnClickListener { showDeleteConfirmation() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(NOTE_EXTRA, note)
        outState.putBoolean("updated", note != intent.getSerializableExtra(NOTE_EXTRA))
        super.onSaveInstanceState(outState)
    }

    private fun renderNote() = with(binding) {
        tvNoteTitle.text = note.title
        com.jobforandroid.shoplinoterelease.utils.NoteTextPreferences.apply(tvNoteTitle, tvNoteText)
        tvNoteTitle.visibility = if (note.title.isEmpty()) View.GONE else View.VISIBLE

        val content = HtmlManager.getFromHtml(note.content).trim()
        if (content.isBlank()) {
            tvNoteText.visibility = View.GONE
            tvNoteEmpty.visibility = View.VISIBLE
        } else {
            tvNoteText.text = content
            tvNoteText.visibility = View.VISIBLE
            tvNoteEmpty.visibility = View.GONE
        }

        val preferences = PreferenceManager.getDefaultSharedPreferences(this@NoteViewActivityDs)
        val (time, date) = TimeManager.getTimeAndDate(note.time, preferences)
        val dateLabel = listOfNotNull(date, time).joinToString(" · ")
        tvViewDate.text = dateLabel
        val editedTime = java.text.SimpleDateFormat(TimeManager.DEF_TIME_FORMAT, java.util.Locale.getDefault()).format(java.util.Date(note.modifiedAt))
        tvEditedLabel.text = getString(R.string.note_view_edited_prefix).trimEnd() + " " + TimeManager.getTimeFormat(editedTime, preferences)
        tvEditedLabel.visibility = if (note.hasBeenEdited) View.VISIBLE else View.GONE
    }

    private fun showDeleteConfirmation() {
        DeleteNoteDialogDs.show(this) {
            setResult(Activity.RESULT_OK, buildResultIntent("delete", note))
            finish()
        }
    }

    private fun buildResultIntent(state: String, resultNote: NoteItem) =
        Intent().apply {
            putExtra(NoteFragmentDs.NEW_NOTE_KEY, resultNote)
            putExtra(NoteFragmentDs.EDIT_STATE_KEY, state)
        }

    companion object {
        const val NOTE_EXTRA = "note_view_item"
    }
}
