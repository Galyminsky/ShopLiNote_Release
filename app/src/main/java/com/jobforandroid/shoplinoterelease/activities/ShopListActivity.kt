package com.jobforandroid.shoplinoterelease.activities


import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jobforandroid.shoplinoterelease.BD.MainViewModel
import com.jobforandroid.shoplinoterelease.BD.ShopListItemAdapter
import com.jobforandroid.shoplinoterelease.R
import com.jobforandroid.shoplinoterelease.utils.ThemeManager
import com.jobforandroid.shoplinoterelease.databinding.ActivityShopListBinding
import com.jobforandroid.shoplinoterelease.dialogs.EditListItemDialog
import com.jobforandroid.shoplinoterelease.entities.LibraryItem
import com.jobforandroid.shoplinoterelease.entities.ShopListItem
import com.jobforandroid.shoplinoterelease.entities.ShopListNameItem
import com.jobforandroid.shoplinoterelease.utils.ShareHelper
import kotlin.math.max

class ShopListActivity : AppCompatActivity(), ShopListItemAdapter.Listener {

    private lateinit var binding: ActivityShopListBinding
    private var shopListNameItem: ShopListNameItem? = null
    private lateinit var saveItem: MenuItem
    private var edItem: EditText? = null
    private var adapter: ShopListItemAdapter? = null
    private var newItemMenu: MenuItem? = null
    private var currentShopItems: List<ShopListItem> = emptyList()
    private var isAddingItemMode = false
    private var additionalActionsOpen = false
    private var isDeletingList = false
    private var isAddingLibrarySuggestion = false
    private var pendingAddedItemName: String? = null
    private lateinit var textWatcher: TextWatcher
    private lateinit var defPref: SharedPreferences
    private var currentTheme = ""

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory((applicationContext as MainApp).database)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        defPref = PreferenceManager.getDefaultSharedPreferences(this)
        currentTheme = ThemeManager.getThemeKey(this)
        setTheme(getSelectedTheme())
        super.onCreate(savedInstanceState)
        binding = ActivityShopListBinding.inflate(layoutInflater)

        setContentView(binding.root)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        init()
        supportActionBar?.title = shopListNameItem?.name
        initRcView()
        listItemObserver()
        setupFloatingActions()
        applyFloatingActionInsets()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.shop_list_menu, menu)
        saveItem = menu.findItem(R.id.save_item)!!
        val newItem = menu.findItem(R.id.new_item)
        newItemMenu = newItem
        edItem = newItem.actionView?.findViewById(R.id.edNewShopItem) as EditText
        newItem.setOnActionExpandListener(expandActionView())
        newItem.isVisible = false
        saveItem.isVisible = false
        textWatcher = textWatcher()
        return true
    }

    private fun textWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d("MyLog", "On Text Changed: $s")
                mainViewModel.getAllLibraryItems("%$s%")
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.save_item) {
            addNewShopItem(edItem?.text.toString())
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addNewShopItem(name: String, onComplete: ((Throwable?) -> Unit)? = null) {
        if (name.isEmpty()) return
        val item = ShopListItem(
            null,
            name,
            "",
            false,
            shopListNameItem?.id!!,
            0
        )
        val insertJob = mainViewModel.insertShopItem(item)
        if (onComplete == null) {
            edItem?.setText("")
        } else {
            insertJob.invokeOnCompletion { error ->
                runOnUiThread { onComplete(error) }
            }
        }
    }

    private fun listItemObserver() {
        mainViewModel.getAllItemsFromList(shopListNameItem?.id!!).observe(this) {
            currentShopItems = it
            if (!isAddingItemMode) {
                adapter?.submitList(it) { showPendingAddedItemFeedback() }
                updateEmptyState(it.isEmpty())
            }
        }
    }

    private fun libraryItemObserver() {
        mainViewModel.libraryItems.observe(this) {
            val tempShopList = ArrayList<ShopListItem>()
            it.forEach { item ->
                val shopItem = ShopListItem(
                    item.id,
                    item.name,
                    "",
                    false,
                    0,
                    1
                )
                tempShopList.add(shopItem)
            }
            adapter?.submitList(tempShopList)
            updateEmptyState(it.isEmpty())
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showPendingAddedItemFeedback() {
        val itemName = pendingAddedItemName ?: return
        if (adapter?.highlightAddedItem(itemName, binding.rcView) == true) {
            pendingAddedItemName = null
        }
    }

    private fun initRcView() = with(binding) {
        adapter = ShopListItemAdapter(this@ShopListActivity)
        rcView.layoutManager = LinearLayoutManager(this@ShopListActivity)
        rcView.adapter = adapter
    }

    private fun expandActionView(): MenuItem.OnActionExpandListener {
        return object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                isAddingItemMode = true
                saveItem.isVisible = false
                binding.actionAdd.setImageResource(R.drawable.ic_speed_dial_save)
                binding.actionAdd.contentDescription = getString(R.string.save_menu)
                edItem?.addTextChangedListener(textWatcher)
                libraryItemObserver()
                mainViewModel.getAllLibraryItems("%%")
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                isAddingItemMode = false
                saveItem.isVisible = false
                binding.actionAdd.setImageResource(R.drawable.ic_speed_dial_plus)
                binding.actionAdd.contentDescription = getString(R.string.add_shop_item_action)
                edItem?.removeTextChangedListener(textWatcher)
                binding.root.post {
                    if (!item.isActionViewExpanded) item.isVisible = false
                }
                mainViewModel.libraryItems.removeObservers(this@ShopListActivity)
                edItem?.setText("")
                adapter?.submitList(currentShopItems) { showPendingAddedItemFeedback() }
                updateEmptyState(currentShopItems.isEmpty())
                supportActionBar?.title = shopListNameItem?.name
                return true
            }
        }
    }

    private fun init() {
        shopListNameItem = intent.getSerializableExtra(SHOP_LIST_NAME) as ShopListNameItem
    }

    companion object {
        const val SHOP_LIST_NAME = "shop_list_name"
    }

    override fun onClickItem(shopListItem: ShopListItem, state: Int) {
        when (state) {
            ShopListItemAdapter.CHECK_BOX -> mainViewModel.updateListItem(shopListItem)
            ShopListItemAdapter.EDIT -> editListItem(shopListItem)
            ShopListItemAdapter.EDIT_LIBRARY_ITEM -> editLibraryItem(shopListItem)
            ShopListItemAdapter.ADD -> {
                if (isAddingLibrarySuggestion || shopListItem.name.isEmpty()) return
                isAddingLibrarySuggestion = true
                addNewShopItem(shopListItem.name) { error ->
                    isAddingLibrarySuggestion = false
                    if (error == null) {
                        pendingAddedItemName = shopListItem.name
                        newItemMenu?.collapseActionView()
                    } else {
                        Toast.makeText(this, R.string.action_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            ShopListItemAdapter.DELETE_LIBRARY_ITEM -> {
                mainViewModel.deleteLibraryItem(shopListItem.id!!)
                mainViewModel.getAllLibraryItems("%${edItem?.text.toString()}%")
            }
        }
    }

    private fun editListItem(item: ShopListItem) {
        EditListItemDialog.showDialog(this, item, object : EditListItemDialog.Listener {
            override fun onClick(item: ShopListItem) {
                mainViewModel.updateListItem(item)
            }
        })
    }

    private fun editLibraryItem(item: ShopListItem) {
        EditListItemDialog.showDialog(this, item, object : EditListItemDialog.Listener {
            override fun onClick(item: ShopListItem) {
                mainViewModel.updateLibraryItem(LibraryItem(item.id, item.name))
                mainViewModel.getAllLibraryItems("%${edItem?.text.toString()}%")
            }
        })
    }

    private fun saveItemCount() {
        val items = currentShopItems
        val tempShopListNameItem = shopListNameItem?.copy(
            allItemCounter = items.size,
            checkedItemsCounter = items.count { it.itemChecked }
        )
        if (tempShopListNameItem != null) mainViewModel.updateListName(tempShopListNameItem)
    }

    private fun getSelectedTheme(): Int {
        return ThemeManager.getThemeStyle(this)

    }

    override fun onBackPressed() {
        if (isDeletingList) return
        if (additionalActionsOpen) {
            setAdditionalActionsOpen(false)
            return
        }
        if (newItemMenu?.isActionViewExpanded == true) {
            newItemMenu?.collapseActionView()
            return
        }
        saveItemCount()
        super.onBackPressed()
    }

    private fun setupFloatingActions() = with(binding) {
        actionAdd.setOnClickListener {
            if (isDeletingList) return@setOnClickListener
            if (isAddingItemMode || newItemMenu?.isActionViewExpanded == true) {
                saveCurrentInput()
                return@setOnClickListener
            }
            setAdditionalActionsOpen(false)
            val addAction = newItemMenu ?: return@setOnClickListener
            if (!addAction.expandActionView()) {
                addAction.isVisible = true
                addAction.expandActionView()
            }
        }
        actionShare.setOnClickListener {
            if (isDeletingList) return@setOnClickListener
            setAdditionalActionsOpen(false)
            shareCurrentList()
        }
        actionMore.setOnClickListener {
            if (isDeletingList) return@setOnClickListener
            setAdditionalActionsOpen(!additionalActionsOpen)
        }
        actionDeleteList.setOnClickListener {
            setAdditionalActionsOpen(false)
            confirmDeleteCurrentList()
        }
        actionClearList.setOnClickListener {
            setAdditionalActionsOpen(false)
            confirmClearCurrentList()
        }
    }

    private fun saveCurrentInput() {
        val hasInput = !edItem?.text.isNullOrEmpty()
        onOptionsItemSelected(saveItem)
        if (hasInput) newItemMenu?.collapseActionView()
    }

    private fun shareCurrentList() {
        val listName = shopListNameItem?.name ?: return
        startActivity(
            Intent.createChooser(
                ShareHelper.shareShopList(currentShopItems, listName),
                getString(R.string.share_chooser_title)
            )
        )
    }

    private fun confirmDeleteCurrentList() {
        val list = shopListNameItem ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_alert)
            .setMessage(getString(R.string.delete_current_list_confirm, list.name))
            .setNegativeButton(R.string.cancel_button, null)
            .setPositiveButton(R.string.delete_button) { _, _ ->
                deleteCurrentList(list.id ?: return@setPositiveButton)
            }
            .show()
    }

    private fun deleteCurrentList(listId: Int) {
        if (isDeletingList) return
        isDeletingList = true
        mainViewModel.deleteShopList(listId, true).invokeOnCompletion { error ->
            runOnUiThread {
                isDeletingList = false
                if (error == null) {
                    finish()
                } else {
                    Toast.makeText(this, R.string.action_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun confirmClearCurrentList() {
        val list = shopListNameItem ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_list)
            .setMessage(getString(R.string.clear_current_list_confirm, list.name))
            .setNegativeButton(R.string.cancel_button, null)
            .setPositiveButton(R.string.clear_all_action) { _, _ ->
                val listId = list.id ?: return@setPositiveButton
                mainViewModel.deleteShopList(listId, false).invokeOnCompletion { error ->
                    if (error != null) runOnUiThread {
                        Toast.makeText(this, R.string.action_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun setAdditionalActionsOpen(open: Boolean) {
        additionalActionsOpen = open
        animateAdditionalAction(binding.actionDeleteList, open, -54f, -6f)
        animateAdditionalAction(binding.actionClearList, open, -8f, -64f)
        binding.actionMore.animate()
            .rotation(if (open) 90f else 0f)
            .setDuration(180)
            .start()
    }

    private fun animateAdditionalAction(view: View, open: Boolean, xDp: Float, yDp: Float) {
        view.animate().cancel()
        if (open) {
            view.visibility = View.VISIBLE
            view.alpha = 0f
            view.scaleX = 0.65f
            view.scaleY = 0.65f
            view.translationX = 0f
            view.translationY = 0f
        }
        view.doOnLayout {
            if (open && !additionalActionsOpen) {
                view.visibility = View.GONE
                return@doOnLayout
            }
            val x = xDp * resources.displayMetrics.density
            val y = yDp * resources.displayMetrics.density
            val halfWidth = view.width / 2f
            val halfHeight = view.height / 2f
            val targetCenterX = (binding.actionMore.x + binding.actionMore.width / 2f + x)
                .coerceIn(halfWidth, binding.floatingActions.width - halfWidth)
            val targetCenterY = (binding.actionMore.y + binding.actionMore.height / 2f + y)
                .coerceIn(halfHeight, binding.floatingActions.height - halfHeight)
            val targetX = if (open) targetCenterX - view.left - halfWidth else 0f
            val targetY = if (open) targetCenterY - view.top - halfHeight else 0f
            view.animate()
                .translationX(targetX)
                .translationY(targetY)
                .alpha(if (open) 1f else 0f)
                .scaleX(if (open) 1f else 0.65f)
                .scaleY(if (open) 1f else 0.65f)
                .setDuration(200)
                .withEndAction { if (!open) view.visibility = View.GONE }
                .start()
        }
    }

    private fun applyFloatingActionInsets() {
        val baseBottomMargin = dp(20)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val safeBottom = max(navBottom, imeBottom)
            val params = binding.floatingActions.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.bottomMargin = baseBottomMargin + safeBottom
            binding.floatingActions.layoutParams = params
            binding.rcView.setPadding(
                binding.rcView.paddingLeft,
                binding.rcView.paddingTop,
                binding.rcView.paddingRight,
                dp(112) + safeBottom
            )
            if (additionalActionsOpen) setAdditionalActionsOpen(true)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

}

