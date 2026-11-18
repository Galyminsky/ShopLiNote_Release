package com.jobforandroid.shoplinoterelease.ui.ds
import android.content.Context
import com.jobforandroid.shoplinoterelease.R
object DeleteNoteDialogDs {
    fun show(context: Context, onConfirm: () -> Unit) = DsDialogs.confirm(context, context.getString(R.string.note_view_delete_confirm_title), context.getString(R.string.note_view_delete_confirm_message), R.string.note_view_delete, R.string.note_view_cancel, onConfirm)
}
