package com.jobforandroid.shoplinoterelease.dialogs
import android.content.Context
import com.jobforandroid.shoplinoterelease.R
import com.jobforandroid.shoplinoterelease.ui.ds.DsDialogs
object DeleteDialog {
    fun showDialog(context: Context, listener: Listener) = DsDialogs.confirm(context, context.getString(R.string.delete_alert), context.getString(R.string.delete_item), R.string.delete_button, R.string.cancel_button) { listener.onClick() }
    interface Listener { fun onClick() }
}
