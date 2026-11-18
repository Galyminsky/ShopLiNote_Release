package com.jobforandroid.shoplinoterelease.dialogs


import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.jobforandroid.shoplinoterelease.databinding.EditListItemDialogBinding
import com.jobforandroid.shoplinoterelease.entities.ShopListItem

object EditListItemDialog {
    fun showDialog(context: Context, item: ShopListItem, listener: Listener) {
        var dialog: AlertDialog? = null
        val builder = AlertDialog.Builder(context)
        val binding = EditListItemDialogBinding.inflate(LayoutInflater.from(context))
        builder.setView(binding.root)
        binding.apply {
            edName.setText(item.name)
            edInfo.setText(item.itemInfo)
            if (item.itemType == 1) edInfo.visibility = View.GONE
            bDeleteItem.visibility = if (item.itemType == 0) View.VISIBLE else View.GONE
            bDeleteItem.setOnClickListener {
                DeleteDialog.showDialog(context, object : DeleteDialog.Listener {
                    override fun onClick() { listener.onDelete(item); dialog?.dismiss() }
                })
            }
            bUpdate.setOnClickListener {
                if (edName.text.toString().isNotEmpty()) {
                    listener.onClick(
                        item.copy(
                            name = edName.text.toString(),
                            itemInfo = edInfo.text.toString()
                        )
                    )
                }
                dialog?.dismiss()
            }

        }
        dialog = builder.create()
        com.jobforandroid.shoplinoterelease.ui.ds.DsDialogs.show(dialog)
    }

    interface Listener {
        fun onClick(item: ShopListItem)
        fun onDelete(item: ShopListItem) = Unit
    }
}
