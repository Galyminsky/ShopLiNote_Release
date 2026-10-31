package com.jobforandroid.shoplinoterelease.ui.ds

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jobforandroid.shoplinoterelease.R

object DiscardChangesDialogDs {
    fun show(context: Context, onDiscard: () -> Unit) {
        MaterialAlertDialogBuilder(ContextThemeWrapper(context, R.style.Theme_App_DS))
            .setTitle(R.string.note_edit_discard_title)
            .setMessage(R.string.note_edit_discard_message)
            .setPositiveButton(R.string.note_edit_discard_continue, null)
            .setNegativeButton(R.string.note_edit_discard_cancel) { _, _ -> onDiscard() }
            .show()
    }
}
