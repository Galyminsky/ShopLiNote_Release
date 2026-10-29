package com.jobforandroid.shoplinoterelease.activities


import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.preference.PreferenceManager
import com.jobforandroid.shoplinoterelease.R
import com.jobforandroid.shoplinoterelease.utils.ThemeManager
import com.jobforandroid.shoplinoterelease.databinding.ActivityNewNoteBinding
import com.jobforandroid.shoplinoterelease.entities.NoteItem
import com.jobforandroid.shoplinoterelease.fragments.NoteFragment
import com.jobforandroid.shoplinoterelease.utils.HtmlManager
import com.jobforandroid.shoplinoterelease.utils.TimeManager
import com.jobforandroid.shoplinoterelease.utils.CardGradient

class NewNoteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewNoteBinding
    private lateinit var defPref: SharedPreferences
    private var note: NoteItem? = null
    private var pref: SharedPreferences? = null
    private var openSubmenu: Submenu? = null
    private var colorSelectionStart = -1
    private var colorSelectionEnd = -1

    private enum class Submenu { COLOR, FORMATTING }

    override fun onCreate(savedInstanceState: Bundle?) {
        defPref = PreferenceManager.getDefaultSharedPreferences(this)
        setTheme(getSelectedTheme())
        super.onCreate(savedInstanceState)
        binding = ActivityNewNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        CardGradient.apply(binding.cardContent, 0)

        actionBarSetting()
        init()
        setTextSize()
        if (!getNote()) return
        onClickColorPicker()
        actionMenuCallback()
        setupSpeedDial()
        supportActionBar?.title = getString(
            if (note == null) R.string.new_note_screen_title else R.string.edit_note_screen_title
        )

    }

    private fun onClickColorPicker() = with(binding) {
        imRed.setOnClickListener {
            applyColorFromPicker(R.color.picker_red)
        }
        imGreen.setOnClickListener {
            applyColorFromPicker(R.color.picker_green)
        }
        imBlue.setOnClickListener {
            applyColorFromPicker(R.color.picker_blue)
        }
        imBlack.setOnClickListener {
            applyColorFromPicker(R.color.picker_black)
        }
        imYellow.setOnClickListener {
            applyColorFromPicker(R.color.picker_yellow)
        }
        imOrange.setOnClickListener {
            applyColorFromPicker(R.color.picker_orange)
        }
    }

    private fun init() {
        pref = PreferenceManager.getDefaultSharedPreferences(this)
    }

    private fun getNote(): Boolean {
        val hasNoteExtra = try {
            intent.hasExtra(NoteFragment.NEW_NOTE_KEY)
        } catch (_: RuntimeException) {
            finish()
            return false
        }
        if (!hasNoteExtra) return true

        val extra = try {
            intent.getSerializableExtra(NoteFragment.NEW_NOTE_KEY)
        } catch (_: RuntimeException) {
            finish()
            return false
        }
        val incomingNote = extra as? NoteItem ?: run {
            finish()
            return false
        }
        if (incomingNote.id == null || incomingNote.id <= 0) {
            finish()
            return false
        }

        note = incomingNote
        fillNote()
        return true
    }

    private fun fillNote() = with(binding) {
        edTitle.setText(note?.title.orEmpty())
        edDescription.setText(HtmlManager.getFromHtml(note?.content.orEmpty()).trim())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nw_note_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setBoldForSelectedText() = toggleStyleForSelectedText(Typeface.BOLD)

    private fun setItalicForSelectedText() = toggleStyleForSelectedText(Typeface.ITALIC)

    private fun toggleStyleForSelectedText(style: Int) = with(binding.edDescription) {
        restoreTextSelection() ?: return
        val startPos = selectionStart
        val endPos = selectionEnd
        if (startPos < 0 || endPos <= startPos) return

        val spans = text.getSpans(startPos, endPos, StyleSpan::class.java)
            .filter { text.getSpanStart(it) <= startPos && text.getSpanEnd(it) >= endPos }
        val current = spans.firstOrNull()
        val currentStyle = current?.style ?: Typeface.NORMAL
        val hasStyle = currentStyle and style != 0
        current?.let {
            val spanStart = text.getSpanStart(it)
            val spanEnd = text.getSpanEnd(it)
            text.removeSpan(it)
            val updatedStyle = if (hasStyle) currentStyle and style.inv() else currentStyle or style
            if (updatedStyle != Typeface.NORMAL) {
                text.setSpan(StyleSpan(updatedStyle), spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        if (current == null) {
            text.setSpan(StyleSpan(style), startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        setSelection(startPos, endPos)
        clearSavedTextSelection()
    }

    private fun setColorForSelectedText(colorId: Int) = with(binding) {
        restoreTextSelection() ?: return
        val startPos = edDescription.selectionStart
        val endPos = edDescription.selectionEnd
        if (startPos < 0 || endPos <= startPos) return

        val styles = edDescription.text.getSpans(startPos, endPos, ForegroundColorSpan::class.java)
        if (styles.isNotEmpty()) edDescription.text.removeSpan(styles[0])

        edDescription.text.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this@NewNoteActivity, colorId)),
            startPos,
            endPos,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        edDescription.text.trim()
        edDescription.setSelection(startPos, endPos)
        clearSavedTextSelection()
    }

    private fun setMainResult() {
        var editState = "new"
        val tempNote: NoteItem? = if (note == null) {
            createNewNote()
        } else {
            editState = "update"
            updateNote()
        }
        val i = Intent().apply {
            putExtra(NoteFragment.NEW_NOTE_KEY, tempNote)
            putExtra(NoteFragment.EDIT_STATE_KEY, editState)

        }
        setResult(RESULT_OK, i)
        finish()
    }

    private fun updateNote(): NoteItem? = with(binding) {
        return note?.copy(
            title = edTitle.text.toString(),
            content = HtmlManager.toHtml(edDescription.text)
        )
    }

    private fun createNewNote(): NoteItem {
        return NoteItem(
            null,
            binding.edTitle.text.toString(),
            HtmlManager.toHtml(binding.edDescription.text),
            TimeManager.getCurrentTime(),
            ""
        )
    }

    private fun actionBarSetting() {
        val ab = supportActionBar
        ab?.setDisplayHomeAsUpEnabled(true)
    }

    private fun applyColorFromPicker(colorId: Int) = with(binding.edDescription) {
        setColorForSelectedText(colorId)
        colorSelectionStart = -1
        colorSelectionEnd = -1
        setSubmenu(null)
    }

    private fun setupSpeedDial() {
        binding.speedDialSave.setOnClickListener {
            setMainResult()
        }
        binding.speedDialColor.setOnClickListener {
            rememberTextSelection()
            setSubmenu(if (openSubmenu == Submenu.COLOR) null else Submenu.COLOR)
        }
        binding.speedDialFormat.setOnClickListener {
            rememberTextSelection()
            setSubmenu(if (openSubmenu == Submenu.FORMATTING) null else Submenu.FORMATTING)
        }
        binding.speedDialBold.setOnClickListener {
            setBoldForSelectedText()
            setSubmenu(null)
        }
        binding.speedDialItalic.setOnClickListener {
            setItalicForSelectedText()
            setSubmenu(null)
        }
        applySpeedDialInsets()
    }

    private fun setSubmenu(submenu: Submenu?) {
        openSubmenu = submenu
        if (submenu == Submenu.COLOR) {
            binding.colorPicker.visibility = View.VISIBLE
            animateFan(colorButtons(), true, binding.speedDialColor, colorFanOffsets())
        } else {
            animateFan(colorButtons(), false, binding.speedDialColor, colorFanOffsets())
            binding.colorPicker.postDelayed({
                if (openSubmenu != Submenu.COLOR) binding.colorPicker.visibility = View.GONE
            }, 220)
        }
        val formatButtons = listOf(binding.speedDialBold, binding.speedDialItalic)
        animateFan(formatButtons, submenu == Submenu.FORMATTING, binding.speedDialFormat, formattingFanOffsets())
    }

    private fun colorButtons() = listOf(
        binding.imRed, binding.imGreen, binding.imBlue,
        binding.imBlack, binding.imYellow, binding.imOrange
    )

    private fun colorFanOffsets(): List<Pair<Float, Float>> {
        val radius = 70f * resources.displayMetrics.density
        return (0..5).map { index ->
            val angle = Math.toRadians(90.0 + index * 36.0)
            (kotlin.math.cos(angle).toFloat() * radius) to
                (kotlin.math.sin(angle).toFloat() * radius)
        }
    }

    private fun formattingFanOffsets(): List<Pair<Float, Float>> {
        val radius = 62f * resources.displayMetrics.density
        return listOf(135.0, 45.0).map { degrees ->
            (kotlin.math.cos(Math.toRadians(degrees)).toFloat() * radius) to
                (-kotlin.math.sin(Math.toRadians(degrees)).toFloat() * radius)
        }
    }

    private fun animateFan(
        buttons: List<View>,
        open: Boolean,
        anchor: View,
        offsets: List<Pair<Float, Float>>
    ) {
        buttons.forEachIndexed { index, button ->
            button.animate().cancel()
            if (open) {
                button.visibility = View.VISIBLE
                button.alpha = 0f
                button.scaleX = 0.65f
                button.scaleY = 0.65f
                button.translationX = 0f
                button.translationY = 0f
            }
            button.doOnLayout {
                if (open && openSubmenu != submenuFor(anchor)) {
                    button.visibility = View.GONE
                    return@doOnLayout
                }
                val targetX: Float
                val targetY: Float
                if (open) {
                    val anchorCenterX = anchor.x + anchor.width / 2f
                    val anchorCenterY = anchor.y + anchor.height / 2f
                    val (dx, dy) = offsets[index]
                    val halfW = button.width / 2f
                    val halfH = button.height / 2f
                    val targetCenterX = (anchorCenterX + dx).coerceIn(halfW, binding.root.width - halfW)
                    val targetCenterY = (anchorCenterY + dy).coerceIn(halfH, binding.root.height - halfH)
                    targetX = targetCenterX - button.left - halfW
                    targetY = targetCenterY - button.top - halfH
                } else {
                    targetX = 0f
                    targetY = 0f
                }
                button.animate()
                    .translationX(targetX)
                    .translationY(targetY)
                    .alpha(if (open) 1f else 0f)
                    .scaleX(if (open) 1f else 0.65f)
                    .scaleY(if (open) 1f else 0.65f)
                    .setDuration(220)
                    .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                    .withEndAction { if (!open) button.visibility = View.GONE }
                    .start()
            }
        }
    }

    private fun submenuFor(anchor: View) = if (anchor.id == R.id.speedDialColor) {
        Submenu.COLOR
    } else {
        Submenu.FORMATTING
    }

    private fun rememberTextSelection() {
        colorSelectionStart = binding.edDescription.selectionStart
        colorSelectionEnd = binding.edDescription.selectionEnd
    }

    private fun restoreTextSelection(): Unit? {
        val text = binding.edDescription
        val start = colorSelectionStart.takeIf { it >= 0 } ?: text.selectionStart
        val end = colorSelectionEnd.takeIf { it >= 0 } ?: text.selectionEnd
        if (start < 0 || end < 0 || end <= start || end > text.text.length) return null
        text.requestFocus()
        text.setSelection(start, end)
        return Unit
    }

    private fun clearSavedTextSelection() {
        colorSelectionStart = -1
        colorSelectionEnd = -1
    }

    private fun applySpeedDialInsets() {
        val baseBottomMargin = (24 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navigationInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val params = binding.speedDialSave.layoutParams as ConstraintLayout.LayoutParams
            params.bottomMargin = baseBottomMargin + navigationInset
            binding.speedDialSave.layoutParams = params
            binding.root.post { openSubmenu?.let(::setSubmenu) }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun actionMenuCallback() {
        val actionCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menu?.clear()
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menu?.clear()
                return true
            }

            override fun onActionItemClicked(mode: ActionMode?, menu: MenuItem?): Boolean {
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
            }
        }
        binding.edDescription.customSelectionActionModeCallback = actionCallback
    }

    private fun setTextSize() = with(binding) {
        edTitle.setTextSize(pref?.getString("title_size_key", "12"))
        edDescription.setTextSize(pref?.getString("content_size_key", "12"))
    }

    private fun EditText.setTextSize(size: String?) {
        if (size != null) this.textSize = size.toFloat()
    }

    private fun getSelectedTheme(): Int {
        return ThemeManager.getThemeStyle(this, newNote = true)
    }
}
