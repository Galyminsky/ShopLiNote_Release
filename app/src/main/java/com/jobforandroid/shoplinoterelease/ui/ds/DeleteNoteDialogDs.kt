package com.jobforandroid.shoplinoterelease.ui.ds

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jobforandroid.shoplinoterelease.R

object DeleteNoteDialogDs {
    fun show(context: Context, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(ContextThemeWrapper(context, R.style.Theme_App_DS))
            .setTitle(R.string.note_view_delete_confirm_title)
            .setMessage(R.string.note_view_delete_confirm_message)
            .setNegativeButton(R.string.note_view_cancel, null)
            .setPositiveButton(R.string.note_view_delete) { _, _ -> onConfirm() }
            .show()
    }
}
