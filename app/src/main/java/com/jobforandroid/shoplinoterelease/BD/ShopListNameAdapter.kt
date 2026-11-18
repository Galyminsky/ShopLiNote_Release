package com.jobforandroid.shoplinoterelease.BD

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jobforandroid.shoplinoterelease.R
import com.jobforandroid.shoplinoterelease.databinding.ShopListNameItemDsBinding
import com.jobforandroid.shoplinoterelease.entities.ShopListNameItem
import com.jobforandroid.shoplinoterelease.utils.TimeManager

class ShopListNameAdapter(private val listener: Listener) : ListAdapter<ShopListNameItem, ShopListNameAdapter.ItemHolder>(ItemComparator()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ItemHolder(ShopListNameItemDsBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: ItemHolder, position: Int) = holder.bind(getItem(position), listener)
    class ItemHolder(private val binding: ShopListNameItemDsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ShopListNameItem, listener: Listener) = with(binding) {
            val resources = root.resources
            val total = item.allItemCounter.coerceAtLeast(0)
            val checked = item.checkedItemsCounter.coerceIn(0, total)
            val left = total - checked
            tvListName.text = item.name
            tvListLeft.text = resources.getQuantityString(R.plurals.shop_list_left, left, left)
            tvListChecked.text = resources.getString(R.string.ds_shop_checked, checked, total)
            tvListDate.text = TimeManager.getTimeFormat(item.time, PreferenceManager.getDefaultSharedPreferences(root.context))
            progressFill.max = total.coerceAtLeast(1)
            progressFill.progress = checked
            root.variant = if (total > 0 && checked == total) 2 else 0
            root.setOnClickListener { listener.onClickItem(item) }
            btnRename.setOnClickListener { listener.editItem(item) }
            btnDelete.setOnClickListener { item.id?.let(listener::deleteItem) }
        }
    }
    class ItemComparator : DiffUtil.ItemCallback<ShopListNameItem>() {
        override fun areItemsTheSame(oldItem: ShopListNameItem, newItem: ShopListNameItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ShopListNameItem, newItem: ShopListNameItem) = oldItem == newItem
    }
    interface Listener {
        fun deleteItem(id: Int)
        fun editItem(shopListNameItem: ShopListNameItem)
        fun onClickItem(shopListNameItem: ShopListNameItem)
    }
}
