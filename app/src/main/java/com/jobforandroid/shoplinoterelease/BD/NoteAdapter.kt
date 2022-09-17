package com.jobforandroid.shoplinoterelease.BD


import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jobforandroid.shoplinoterelease.R
import com.jobforandroid.shoplinoterelease.databinding.NoteListItemBinding
import com.jobforandroid.shoplinoterelease.entities.NoteItem
import com.jobforandroid.shoplinoterelease.utils.HtmlManager
import com.jobforandroid.shoplinoterelease.utils.TimeManager

class NoteAdapter(private val listener: Listener, private val defRref: SharedPreferences) :
    ListAdapter<NoteItem, NoteAdapter.ItemHolder>(ItemComparator()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        holder.setData(getItem(position), listener, defRref)
    }


    class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val binding = NoteListItemBinding.bind(view)

        fun setData(note: NoteItem, listener: Listener, defRref: SharedPreferences) =
            with(binding) {
                tvTitle.text = note.title
                tvDescription.text = HtmlManager.getFromHtml(note.content).trim()
                tvTime.text = TimeManager.getTimeFormat(note.time, defRref)
                itemView.setOnClickListener {
                    listener.onClickItem(note)
                }
                imDelete.setOnClickListener {
                    listener.deleteItem(note.id!!)
                }
            }

        companion object {
            fun create(parent: ViewGroup): ItemHolder {
                return ItemHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.note_list_item, parent, false)
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
        fun deleteItem(id: Int)
        fun onClickItem(note: NoteItem)
    }


}