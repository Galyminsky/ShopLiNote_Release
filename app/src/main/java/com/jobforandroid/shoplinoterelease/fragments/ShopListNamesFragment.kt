package com.jobforandroid.shoplinoterelease.fragments


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.jobforandroid.shoplinoterelease.BD.MainViewModel
import com.jobforandroid.shoplinoterelease.BD.ShopListNameAdapter
import com.jobforandroid.shoplinoterelease.activities.MainApp
import com.jobforandroid.shoplinoterelease.activities.ShopListActivity
import com.jobforandroid.shoplinoterelease.databinding.FragmentShopListNamesDsBinding
import com.jobforandroid.shoplinoterelease.dialogs.DeleteDialog
import com.jobforandroid.shoplinoterelease.dialogs.NewListDialog
import com.jobforandroid.shoplinoterelease.entities.ShopListNameItem
import com.jobforandroid.shoplinoterelease.utils.TimeManager


class ShopListNamesFragment : BaseFragment(), ShopListNameAdapter.Listener {
    private var _binding: FragmentShopListNamesDsBinding? = null
    private val binding get() = _binding!!
    private var lists: List<ShopListNameItem> = emptyList()
    private var query = ""
    private var adapter: ShopListNameAdapter? = null
    private val mainViewModel: MainViewModel by activityViewModels {
        MainViewModel.MainViewModelFactory((requireActivity().application as MainApp).database)
    }

    override fun onClickNew() {
        NewListDialog.showDialog(requireContext(), object : NewListDialog.Listener {
            override fun onClick(name: String) {
                val shopListName = ShopListNameItem(
                    null,
                    name,
                    TimeManager.getCurrentTime(),
                    0,
                    0,
                    ""
                )
                mainViewModel.insertShopListName(shopListName)
            }
        }, "")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopListNamesDsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRcView()
        query = savedInstanceState?.getString("shop_query").orEmpty()
        binding.tvShopSearchHint.setText(query)
        (binding.btnNewList.parent as View).visibility = View.GONE
        binding.tvShopSearchHint.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { query = s.toString(); renderLists() }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        observer()
    }

    private fun initRcView() = with(binding) {
        rvShopLists.layoutManager = LinearLayoutManager(activity)
        adapter = ShopListNameAdapter(this@ShopListNamesFragment)
        rvShopLists.adapter = adapter
    }

    private fun observer() {
        mainViewModel.allShopListNameItem.observe(viewLifecycleOwner) {
            lists = it
            renderLists()
        }
    }

    private fun renderLists() {
        val filtered = lists.filter { it.name.contains(query, true) }
        adapter?.submitList(filtered)
        binding.rvShopLists.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyShopLists.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.shopSectionHeader.root.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        val active = lists.count { it.allItemCounter == 0 || it.checkedItemsCounter < it.allItemCounter }
        binding.tvShopSubtitle.text = resources.getQuantityString(com.jobforandroid.shoplinoterelease.R.plurals.shop_active_lists, active, active)
        binding.shopSectionHeader.tvSectionTitle.setText(com.jobforandroid.shoplinoterelease.R.string.ds_shop_all_lists)
        binding.shopSectionHeader.tvSectionCount.text = resources.getQuantityString(com.jobforandroid.shoplinoterelease.R.plurals.shop_list_count, filtered.size, filtered.size)
        (binding.emptyShopLists.getChildAt(0) as android.widget.TextView).setText(if (query.isBlank()) com.jobforandroid.shoplinoterelease.R.string.shop_empty_title else com.jobforandroid.shoplinoterelease.R.string.ds_no_results)
        binding.emptyShopLists.getChildAt(1).visibility = if (query.isBlank()) View.VISIBLE else View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("shop_query", query)
        super.onSaveInstanceState(outState)
    }

    companion object {
        @JvmStatic
        fun newInstance() = ShopListNamesFragment()
    }

    override fun deleteItem(id: Int) {
        DeleteDialog.showDialog(requireContext(), object : DeleteDialog.Listener {
            override fun onClick() {
                mainViewModel.deleteShopList(id, true)
            }
        })
    }

    override fun editItem(shopListNameItem: ShopListNameItem) {
        NewListDialog.showDialog(requireContext(), object : NewListDialog.Listener {
            override fun onClick(name: String) {
                mainViewModel.updateListName(shopListNameItem.copy(name = name))
            }
        }, shopListNameItem.name)
    }

    override fun onClickItem(shopListNameItem: ShopListNameItem) {
        val i = Intent(activity, ShopListActivity::class.java).apply {
            putExtra(ShopListActivity.SHOP_LIST_NAME, shopListNameItem)
        }
        startActivity(i)
    }

    override fun onDestroyView() {
        binding.rvShopLists.adapter = null
        adapter = null
        _binding = null
        super.onDestroyView()
    }
}
