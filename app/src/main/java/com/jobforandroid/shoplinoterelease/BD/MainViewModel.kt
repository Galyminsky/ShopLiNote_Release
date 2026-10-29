package com.jobforandroid.shoplinoterelease.BD


import androidx.lifecycle.*
import androidx.room.withTransaction
import com.jobforandroid.shoplinoterelease.entities.LibraryItem
import com.jobforandroid.shoplinoterelease.entities.NoteItem
import com.jobforandroid.shoplinoterelease.entities.ShopListItem
import com.jobforandroid.shoplinoterelease.entities.ShopListNameItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainViewModel(private val database: MainDataBase) : ViewModel() {

    val dao = database.getDao()
    val libraryItems = MutableLiveData<List<LibraryItem>>()
    private var librarySearchRequestId = 0L
    val allNotes: LiveData<List<NoteItem>> = dao.getAllNotes().asLiveData()
    val allShopListNameItem: LiveData<List<ShopListNameItem>> =
        dao.getAllShopListNames().asLiveData()

    fun getAllItemsFromList(listId: Int): LiveData<List<ShopListItem>> {
        return dao.getAllShopListItems(listId).asLiveData()
    }

    fun getAllLibraryItems(name: String) {
        val requestId = ++librarySearchRequestId
        viewModelScope.launch {
            val result = dao.getAllLibraryItems(name)
            if (requestId == librarySearchRequestId) {
                libraryItems.value = result
            }
        }
    }

    fun insertNote(note: NoteItem) = viewModelScope.launch {
        dao.insertNote(note)
    }

    fun insertShopListName(listName: ShopListNameItem) = viewModelScope.launch {
        dao.insertShopListName(listName)
    }

    fun insertShopItem(shopListItem: ShopListItem): Deferred<Result<Unit>> =
        viewModelScope.async {
            try {
                database.withTransaction {
                    dao.insertItem(shopListItem)
                    if (!isLibraryItemExists(shopListItem.name))
                        dao.insertLibraryItem(LibraryItem(null, shopListItem.name))
                    dao.updateShopListCounters(shopListItem.listId)
                }
                Result.success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    fun updateNote(note: NoteItem) = viewModelScope.launch {
        dao.updateNote(note)
    }

    fun updateLibraryItem(item: LibraryItem) = viewModelScope.launch {
        dao.updateLibraryItem(item)
    }

    fun updateListItem(item: ShopListItem) = viewModelScope.launch {
        database.withTransaction {
            dao.updateListItem(item)
            dao.updateShopListCounters(item.listId)
        }
    }

    fun updateListName(shopListName: ShopListNameItem) = viewModelScope.launch {
        dao.updateListName(shopListName)
    }

    fun refreshShopListCounters(listId: Int): Deferred<Result<Unit>> =
        viewModelScope.async {
            try {
                dao.updateShopListCounters(listId)
                Result.success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    fun deleteNote(id: Int) = viewModelScope.launch {
        dao.deleteNote(id)
    }

    fun deleteLibraryItem(id: Int) = viewModelScope.launch {
        dao.deleteLibraryItem(id)
    }

    fun deleteShopList(id: Int, deleteList: Boolean) = viewModelScope.launch {
        database.withTransaction {
            if (deleteList) dao.deleteShopListName(id)
            dao.deleteShopItemsByListId(id)
            if (!deleteList) dao.updateShopListCounters(id)
        }
    }

    private suspend fun isLibraryItemExists(name: String): Boolean {
        return dao.getAllLibraryItems(name).isNotEmpty()
    }

    class MainViewModelFactory(val database: MainDataBase) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(database) as T
            }
            throw IllegalAccessException("Unknown ViewModelClass")
        }
    }
}
