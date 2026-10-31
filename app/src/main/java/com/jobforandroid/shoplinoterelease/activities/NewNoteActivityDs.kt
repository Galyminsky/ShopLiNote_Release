package com.jobforandroid.shoplinoterelease.activities

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ActionMode
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupWindow
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import com.jobforandroid.shoplinoterelease.R
import com.jobforandroid.shoplinoterelease.databinding.ActivityNoteEditDsBinding
import com.jobforandroid.shoplinoterelease.databinding.PopupNoteColorsDsBinding
import com.jobforandroid.shoplinoterelease.entities.NoteItem
import com.jobforandroid.shoplinoterelease.fragments.NoteFragmentDs
import com.jobforandroid.shoplinoterelease.utils.HtmlManager
import com.jobforandroid.shoplinoterelease.utils.TimeManager
import com.jobforandroid.shoplinoterelease.ui.ds.RichTextFormatterDs
import com.jobforandroid.shoplinoterelease.ui.ds.DiscardChangesDialogDs

class NewNoteActivityDs : AppCompatActivity() {
    private lateinit var binding: ActivityNoteEditDsBinding
    private var note: NoteItem? = null
    private var savedSelectionStart = -1
    private var savedSelectionEnd = -1
    private var selectedPaletteColor: Int = R.color.ds_note_color_accent
    private var colorPopup: PopupWindow? = null
    private var initialTitle = ""
    private var initialHtml = ""
    private var dirty = false

    private data class PaletteColor(val resource: Int, val value: Int?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditDsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setResult(RESULT_CANCELED)
        selectedPaletteColor = savedInstanceState?.getInt(STATE_SELECTED_COLOR, R.color.ds_note_color_accent)
            ?: R.color.ds_note_color_accent

        note = readNote() ?: if (intent.hasExtra(NoteFragmentDs.NEW_NOTE_KEY)) {
            finish()
            return
        } else null

        applyInsets()
        render()
        initialTitle = savedInstanceState?.getString(STATE_INITIAL_TITLE) ?: note?.title.orEmpty()
        initialHtml = savedInstanceState?.getString(STATE_INITIAL_HTML) ?: HtmlManager.toHtml(binding.etContent.text)
        binding.btnCancel.setOnClickListener { requestCancel() }
        binding.btnSave.setOnClickListener { if (binding.btnSave.isEnabled) setMainResult() }
        binding.etTitle.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSaveState()
                updateDirtyState()
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        binding.etContent.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = updateDirtyState()
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        updateSaveState()
        updateDirtyState()
        setUpFormatting()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = requestCancel()
        })

        val focusTarget = if (note == null) binding.etTitle else binding.etContent
        focusTarget.requestFocus()
        focusTarget.post {
            focusTarget.setSelection(focusTarget.text.length)
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(focusTarget, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_SELECTED_COLOR, selectedPaletteColor)
        outState.putString(STATE_INITIAL_TITLE, initialTitle)
        outState.putString(STATE_INITIAL_HTML, initialHtml)
        super.onSaveInstanceState(outState)
    }

    private fun setUpFormatting() = with(binding) {
        listOf(btnBold, btnItalic, btnColor).forEach { button ->
            button.isFocusable = false
            button.isFocusableInTouchMode = false
        }
        btnBold.setOnClickListener { formatSelection(bold = true) }
        btnItalic.setOnClickListener { formatSelection(bold = false) }
        btnColor.setOnClickListener {
            rememberSelection()
            showColorPopup()
        }
        etContent.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean { menu?.clear(); return true }
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean { menu?.clear(); return true }
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = true
            override fun onDestroyActionMode(mode: ActionMode?) = Unit
        }
        val current = ContextCompat.getColor(this@NewNoteActivityDs, selectedPaletteColor)
        tvColorGlyph.setTextColor(current)
        vColorBar.backgroundTintList = ColorStateList.valueOf(current)
    }

    private fun rememberSelection() {
        val editor = binding.etContent
        val start = editor.selectionStart
        val end = editor.selectionEnd
        if (start >= 0 && end > start) {
            savedSelectionStart = start
            savedSelectionEnd = end
        } else {
            savedSelectionStart = -1
            savedSelectionEnd = -1
        }
    }

    private fun restoreSelection(): Pair<Int, Int>? {
        val editor = binding.etContent
        val start = savedSelectionStart.takeIf { it >= 0 } ?: editor.selectionStart
        val end = savedSelectionEnd.takeIf { it >= 0 } ?: editor.selectionEnd
        if (start < 0 || end <= start || end > editor.text.length) return null
        editor.requestFocus()
        editor.setSelection(start, end)
        return start to end
    }

    private fun formatSelection(bold: Boolean) {
        rememberSelection()
        val (start, end) = restoreSelection() ?: return
        if (bold) RichTextFormatterDs.toggleBold(binding.etContent.text, start, end)
        else RichTextFormatterDs.toggleItalic(binding.etContent.text, start, end)
        binding.etContent.setSelection(start, end)
        savedSelectionStart = -1
        savedSelectionEnd = -1
        updateDirtyState()
    }

    private fun showColorPopup() {
        colorPopup?.dismiss()
        val popupBinding = PopupNoteColorsDsBinding.inflate(layoutInflater)
        val popup = PopupWindow(
            popupBinding.root,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            isOutsideTouchable = true
            isTouchable = true
            elevation = 8 * resources.displayMetrics.density
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setOnDismissListener { colorPopup = null }
        }
        val palette = listOf(
            PaletteColor(R.color.ds_note_color_ink, null),
            PaletteColor(R.color.ds_note_color_accent, R.color.ds_note_color_accent),
            PaletteColor(R.color.ds_note_color_red, R.color.ds_note_color_red),
            PaletteColor(R.color.ds_note_color_orange, R.color.ds_note_color_orange),
            PaletteColor(R.color.ds_note_color_green, R.color.ds_note_color_green),
            PaletteColor(R.color.ds_note_color_blue, R.color.ds_note_color_blue)
        )
        val swatches = listOf(popupBinding.swatch1, popupBinding.swatch2, popupBinding.swatch3,
            popupBinding.swatch4, popupBinding.swatch5, popupBinding.swatch6)
        swatches.forEachIndexed { index, swatch ->
            val item = palette[index]
            val resolved = ContextCompat.getColor(this, item.resource)
            (swatch.getChildAt(0) as? View)?.backgroundTintList = ColorStateList.valueOf(resolved)
            if (swatch.childCount < 2) {
                swatch.addView(View(this).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        (32 * resources.displayMetrics.density).toInt(),
                        (32 * resources.displayMetrics.density).toInt(),
                        Gravity.CENTER
                    )
                    setBackgroundResource(R.drawable.ds_bg_swatch_selected)
                })
            }
            val ring = swatch.getChildAt(1)
            ring.visibility = if (item.resource == selectedPaletteColor) View.VISIBLE else View.GONE
            ring.background = ring.background?.mutate()?.also { it.setTint(resolved) }
            swatch.setOnClickListener {
                val range = restoreSelection()
                if (range != null) {
                    val color = item.value?.let { ContextCompat.getColor(this, it) }
                    RichTextFormatterDs.applyColor(binding.etContent.text, range.first, range.second, color)
                    binding.etContent.setSelection(range.first, range.second)
                    savedSelectionStart = -1
                    savedSelectionEnd = -1
                    updateDirtyState()
                }
                selectedPaletteColor = item.resource
                binding.tvColorGlyph.setTextColor(resolved)
                binding.vColorBar.backgroundTintList = ColorStateList.valueOf(resolved)
                popup.dismiss()
            }
        }
        colorPopup = popup
        popupBinding.root.measure(
            View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.heightPixels, View.MeasureSpec.AT_MOST)
        )
        val anchor = binding.btnColor
        val loc = IntArray(2)
        anchor.getLocationOnScreen(loc)
        val rootLoc = IntArray(2)
        binding.root.getLocationOnScreen(rootLoc)
        val popupWidth = popupBinding.root.measuredWidth
        val popupHeight = popupBinding.root.measuredHeight
        val x = loc[0] - rootLoc[0] + anchor.width / 2 - popupWidth / 2
        val above = loc[1] - rootLoc[1] - popupHeight
        val y = if (above >= 0) above else loc[1] - rootLoc[1] + anchor.height
        popup.showAtLocation(binding.root, Gravity.TOP or Gravity.LEFT, x, y)
    }

    private fun readNote(): NoteItem? {
        if (!intent.hasExtra(NoteFragmentDs.NEW_NOTE_KEY)) return null
        val incoming = try {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(NoteFragmentDs.NEW_NOTE_KEY) as? NoteItem
        } catch (_: RuntimeException) { null } ?: return null
        if (incoming.id == null || incoming.id <= 0) return null
        return incoming
    }

    private fun render() = with(binding) {
        tvEditScreenTitle.setText(if (note == null) R.string.note_edit_new_title else R.string.note_edit_edit_title)
        val displayTime = note?.time ?: TimeManager.getCurrentTime()
        val (time, date) = TimeManager.getTimeAndDate(
            displayTime,
            PreferenceManager.getDefaultSharedPreferences(this@NewNoteActivityDs)
        )
        tvEditDate.text = listOfNotNull(date, time).joinToString(" \u00B7 ")
        note?.let {
            etTitle.setText(it.title)
            etContent.setText(HtmlManager.getFromHtml(it.content).trim())
        }
    }

    private fun updateSaveState() {
        val enabled = binding.etTitle.text.toString().isNotBlank()
        binding.btnSave.isEnabled = enabled
        binding.btnSave.alpha = if (enabled) 1f else 0.4f
    }

    private fun updateDirtyState() {
        dirty = binding.etTitle.text.toString().trim() != initialTitle.trim() ||
            HtmlManager.toHtml(binding.etContent.text) != initialHtml
        binding.tvEditStatus.visibility = if (dirty) View.VISIBLE else View.INVISIBLE
        if (dirty) binding.tvEditStatus.setText(
            if (note == null) R.string.note_edit_status_draft else R.string.note_edit_status_unsaved
        )
    }

    private fun requestCancel() {
        if (dirty) DiscardChangesDialogDs.show(this) { finish() } else finish()
    }

    private fun setMainResult() {
        val resultNote = note?.copy(
            title = binding.etTitle.text.toString(),
            content = HtmlManager.toHtml(binding.etContent.text)
        ) ?: NoteItem(
            null,
            binding.etTitle.text.toString(),
            HtmlManager.toHtml(binding.etContent.text),
            TimeManager.getCurrentTime(),
            ""
        )
        val state = if (note == null) "new" else "update"
        setResult(RESULT_OK, Intent().apply {
            putExtra(NoteFragmentDs.NEW_NOTE_KEY, resultNote)
            putExtra(NoteFragmentDs.EDIT_STATE_KEY, state)
        })
        finish()
    }

    private fun applyInsets() {
        val root = binding.root
        val initialLeft = root.paddingLeft
        val initialTop = root.paddingTop
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navigation = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.setPadding(initialLeft, initialTop + status, initialRight, initialBottom + maxOf(navigation, ime))
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    companion object {
        private const val STATE_SELECTED_COLOR = "selected_palette_color"
        private const val STATE_INITIAL_TITLE = "initial_title"
        private const val STATE_INITIAL_HTML = "initial_html"
    }
}
