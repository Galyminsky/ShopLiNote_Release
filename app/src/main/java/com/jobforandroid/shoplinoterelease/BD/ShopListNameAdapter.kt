package com.jobforandroid.shoplinoterelease.BD


import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jobforandroid.shoplinoterelease.R
import com.jobforandroid.shoplinoterelease.databinding.ListNameItemBinding
import com.jobforandroid.shoplinoterelease.entities.ShopListNameItem
import com.jobforandroid.shoplinoterelease.utils.CardGradient
import com.jobforandroid.shoplinoterelease.utils.TimeManager

class ShopListNameAdapter(private val listener: Listener) :
    ListAdapter<ShopListNameItem, ShopListNameAdapter.ItemHolder>(ItemComparator()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        holder.setData(getItem(position), listener, position)
    }


    class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ListNameItemBinding.bind(view)

        fun setData(shopListNameItem: ShopListNameItem, listener: Listener, position: Int) = with(binding) {
            CardGradient.apply(cardContent, position)
            val isComplete = shopListNameItem.checkedItemsCounter == shopListNameItem.allItemCounter
            val colorState = ColorStateList.valueOf(
                getThemeColor(
                    binding.root.context,
                    if (isComplete) R.attr.theme_count_complete else R.attr.theme_count_incomplete
                )
            )
            tvListName.text = shopListNameItem.name
            val preferences = PreferenceManager.getDefaultSharedPreferences(binding.root.context)
            val (time, date) = TimeManager.getTimeAndDate(shopListNameItem.time, preferences)
            tvTime.text = time
            tvDate.text = date.orEmpty()
            dateRow.visibility = if (date == null) View.GONE else View.VISIBLE
            pBar.max = shopListNameItem.allItemCounter
            pBar.progress = shopListNameItem.checkedItemsCounter
            pBar.progressTintList = ColorStateList.valueOf(
                getThemeColor(binding.root.context, R.attr.theme_progress_color)
            )
            counterCard.backgroundTintList = colorState
            val counterText =
                "${shopListNameItem.checkedItemsCounter} / ${shopListNameItem.allItemCounter}"
            tvCounter.text = counterText
            tvCounter.setTextColor(getThemeColor(binding.root.context, R.attr.theme_count_text))
            itemView.setOnClickListener {
                listener.onClickItem(shopListNameItem)
            }
            imDelete.setOnClickListener {
                listener.deleteItem(shopListNameItem.id!!)
            }

            imEdit.setOnClickListener {
                listener.editItem(shopListNameItem)
            }
        }

        private fun getThemeColor(context: Context, attribute: Int): Int {
            val value = TypedValue()
            context.theme.resolveAttribute(attribute, value, true)
            return if (value.resourceId != 0) {
                ContextCompat.getColor(context, value.resourceId)
            } else {
                value.data
            }
        }

        companion object {
            fun create(parent: ViewGroup): ItemHolder {
                return ItemHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.list_name_item, parent, false)
                )
            }
        }
    }

    class ItemComparator : DiffUtil.ItemCallback<ShopListNameItem>() {
        override fun areItemsTheSame(
            oldItem: ShopListNameItem,
            newItem: ShopListNameItem
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ShopListNameItem,
            newItem: ShopListNameItem
        ): Boolean {
            return oldItem == newItem
        }
    }

    interface Listener {
        fun deleteItem(id: Int)
        fun editItem(shopListNameItem: ShopListNameItem)
        fun onClickItem(shopListNameItem: ShopListNameItem)
    }
}
