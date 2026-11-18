package com.jobforandroid.shoplinoterelease.ui.ds
import android.content.Context
import com.jobforandroid.shoplinoterelease.R
object DiscardChangesDialogDs {
    fun show(context: Context, onDiscard: () -> Unit) = DsDialogs.confirm(context, context.getString(R.string.note_edit_discard_title), context.getString(R.string.note_edit_discard_message), R.string.note_edit_discard_cancel, R.string.note_edit_discard_continue, onDiscard)
}
