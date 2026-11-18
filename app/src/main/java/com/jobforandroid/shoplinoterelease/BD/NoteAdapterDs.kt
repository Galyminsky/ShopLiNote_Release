package com.jobforandroid.shoplinoterelease.BD

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jobforandroid.shoplinoterelease.R
import com.jobforandroid.shoplinoterelease.databinding.NoteListItemDsBinding
import com.jobforandroid.shoplinoterelease.databinding.PopupNoteActionsDsBinding
import com.jobforandroid.shoplinoterelease.entities.NoteItem
import com.jobforandroid.shoplinoterelease.utils.HtmlManager
import com.jobforandroid.shoplinoterelease.utils.TimeManager

class NoteAdapterDs(private val listener: Listener, private val defRref: SharedPreferences) :
    ListAdapter<NoteItem, NoteAdapterDs.ItemHolder>(ItemComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        holder.setData(getItem(position), listener, defRref)
    }

    override fun onViewRecycled(holder: ItemHolder) {
        holder.dismissActions()
        super.onViewRecycled(holder)
    }

    override fun onViewDetachedFromWindow(holder: ItemHolder) {
        holder.dismissActions()
        super.onViewDetachedFromWindow(holder)
    }

    class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val binding = NoteListItemDsBinding.bind(view)
        private var actionsPopup: PopupWindow? = null

        fun dismissActions() { actionsPopup?.dismiss(); actionsPopup = null }

        fun setData(note: NoteItem, listener: Listener, defRref: SharedPreferences) =
            with(binding) {
                dismissActions()
                tvTitle.text = note.title
                com.jobforandroid.shoplinoterelease.utils.NoteTextPreferences.apply(tvTitle, tvDescription)
                tvDescription.text = HtmlManager.getFromHtml(note.content).trim()
                val (time, date) = if (note.modifiedAt > 0) TimeManager.getTimeAndDate(note.modifiedAt, defRref) else TimeManager.getTimeAndDate(note.time, defRref)
                tvTime.text = time
                tvDate.text = date.orEmpty()
                dateRow.visibility = if (date == null) View.GONE else View.VISIBLE
                itemView.setOnClickListener {
                    listener.onClickItem(note)
                }
                imDelete.setOnClickListener {
                    showActionsPopup(imDelete, note, listener)
                }
            }

        private fun showActionsPopup(anchor: View, note: NoteItem, listener: Listener) {
            dismissActions()
            if (!anchor.isAttachedToWindow) return
            val menuBinding = PopupNoteActionsDsBinding.inflate(LayoutInflater.from(anchor.context))
            val visibleFrame = Rect()
            anchor.getWindowVisibleDisplayFrame(visibleFrame)
            menuBinding.root.measure(
                View.MeasureSpec.makeMeasureSpec(visibleFrame.width(), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(visibleFrame.height(), View.MeasureSpec.AT_MOST)
            )
            val popupHeight = menuBinding.root.measuredHeight
            val anchorLocation = IntArray(2)
            anchor.getLocationOnScreen(anchorLocation)
            val fitsBelow = anchorLocation[1] + anchor.height + popupHeight <= visibleFrame.bottom

            val popup = PopupWindow(
                menuBinding.root,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            ).apply {
                isOutsideTouchable = true
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                animationStyle = 0
                setOnDismissListener { actionsPopup = null }
            }
            actionsPopup = popup
            menuBinding.rowEdit.setOnClickListener {
                popup.dismiss()
                listener.onEditClick(note)
            }
            menuBinding.rowDelete.setOnClickListener {
                popup.dismiss()
                listener.onDeleteClick(note)
            }
            val yOffset = if (fitsBelow) 0 else -anchor.height - popupHeight
            popup.showAsDropDown(anchor, 0, yOffset, Gravity.END)
        }

        companion object {
            fun create(parent: ViewGroup): ItemHolder {
                return ItemHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.note_list_item_ds, parent, false)
                )
            }
        }
    }

    class ItemComparator : DiffUtil.ItemCallback<NoteItem>() {
        override fun areItemsTheSame(oldItem: NoteItem, newItem: NoteItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NoteItem, newItem: NoteItem): Boolean {
            return oldItem == newItem
        }
    }

    interface Listener {
        fun onClickItem(note: NoteItem)
        fun onEditClick(note: NoteItem)
        fun onDeleteClick(note: NoteItem)
    }
}
