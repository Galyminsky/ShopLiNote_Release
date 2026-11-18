package com.jobforandroid.shoplinoterelease.ui.ds

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.jobforandroid.shoplinoterelease.R
import com.jobforandroid.shoplinoterelease.databinding.DsDialogBinding

/** Shared DS window treatment; transparent only outside the raised surface, retaining dim and IME. */
object DsDialogs {
    fun show(dialog: AlertDialog): AlertDialog {
        var ownerContext = dialog.context
        while (ownerContext is android.content.ContextWrapper && ownerContext !is androidx.lifecycle.LifecycleOwner) {
            ownerContext = ownerContext.baseContext
        }
        val owner = ownerContext as? androidx.lifecycle.LifecycleOwner
        if (owner?.lifecycle?.currentState == androidx.lifecycle.Lifecycle.State.DESTROYED) return dialog
        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) { dialog.dismiss() }
        }
        owner?.lifecycle?.addObserver(observer)
        dialog.setOnDismissListener { owner?.lifecycle?.removeObserver(observer) }
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            decorView.setPadding(0, 0, 0, 0)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.32f)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        return dialog
    }

    fun confirm(context: Context, title: CharSequence, message: CharSequence, positive: Int, negative: Int, onConfirm: () -> Unit) {
        val binding = DsDialogBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context).setView(binding.root).create()
        binding.dialogTitle.text = title
        binding.dialogContent.addView(TextView(context).apply {
            text = message
            setTextAppearance(R.style.TextAppearance_DS_NoteBody)
        })
        binding.dialogPositive.setText(positive)
        binding.dialogNegative.setText(negative)
        binding.dialogPositive.setOnClickListener { dialog.dismiss(); onConfirm() }
        binding.dialogNegative.setOnClickListener { dialog.dismiss() }
        show(dialog)
    }

    fun choose(context: Context, title: CharSequence, entries: Array<CharSequence>, selected: Int, onSelect: (Int) -> Unit) {
        val binding = DsDialogBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context).setView(binding.root).create()
        binding.dialogTitle.text = title
        binding.dialogPositive.visibility = View.GONE
        (binding.dialogPositive.parent as View).visibility = View.GONE
        binding.dialogNegative.setText(R.string.cancel_button)
        binding.dialogNegative.setOnClickListener { dialog.dismiss() }
        entries.forEachIndexed { index, label ->
            val row = LayoutInflater.from(context).inflate(R.layout.ds_choice_row, binding.dialogContent, false)
            val radio = row.findViewById<RadioButton>(R.id.choiceLabel)
            radio.text = label
            radio.isChecked = index == selected
            row.setOnClickListener { onSelect(index); dialog.dismiss() }
            binding.dialogContent.addView(row)
        }
        show(dialog)
    }
}
