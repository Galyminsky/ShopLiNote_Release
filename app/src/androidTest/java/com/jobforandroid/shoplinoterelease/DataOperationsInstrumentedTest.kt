package com.jobforandroid.shoplinoterelease

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.viewModelScope
import com.jobforandroid.shoplinoterelease.BD.MainDataBase
import com.jobforandroid.shoplinoterelease.BD.MainViewModel
import com.jobforandroid.shoplinoterelease.entities.NoteItem
import com.jobforandroid.shoplinoterelease.entities.ShopListNameItem
import com.jobforandroid.shoplinoterelease.entities.ShopListItem
import com.jobforandroid.shoplinoterelease.utils.TimeManager
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataOperationsInstrumentedTest {
    @Test fun noteCrudAndMetadataKeepCreationTimeAndSortDeterministically() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, MainDataBase::class.java).build()
        val model = MainViewModel(db)
        try {
            val dao = db.getDao()
            val creation = TimeManager.getCurrentTime()
            model.insertNote(NoteItem(null, "Title", "<b>Body</b>", creation, "category")).join()
            val inserted = dao.getAllNotes().first().single()
            assertEquals(creation, inserted.time); assertTrue(inserted.modifiedAt > 0); assertFalse(inserted.hasBeenEdited)
            model.updateNote(inserted.copy(title = "Edited")).join()
            val edited = dao.getAllNotes().first().single()
            assertEquals(creation, edited.time); assertEquals("Edited", edited.title); assertTrue(edited.hasBeenEdited)
            assertTrue(edited.modifiedAt >= inserted.modifiedAt)
            dao.insertNote(NoteItem(100, "Tie one", "", creation, "", edited.modifiedAt, false))
            dao.insertNote(NoteItem(101, "Tie two", "", creation, "", edited.modifiedAt, false))
            assertEquals(listOf(101, 100, edited.id), dao.getAllNotes().first().map { it.id })
            model.deleteNote(edited.id!!).join()
            assertEquals(2, dao.getAllNotes().first().size)
        } finally { model.viewModelScope.cancel(); db.close() }
    }

    @Test fun shoppingTransactionsPreserveCountersLibraryAndClearVersusDelete() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, MainDataBase::class.java).build()
        val model = MainViewModel(db)
        try {
            val dao = db.getDao()
            model.insertShopListName(ShopListNameItem(7, "List", TimeManager.getCurrentTime(), 0, 0, "")).join()
            model.insertShopItem(ShopListItem(null, "Milk", "", false, 7, 0)).await().getOrThrow()
            model.insertShopItem(ShopListItem(null, "Milk%", "", false, 7, 0)).await().getOrThrow()
            assertTrue(dao.libraryItemExists("Milk%")); assertFalse(dao.libraryItemExists("Milk_"))
            assertEquals(setOf("Milk", "Milk%"), dao.getAllLibraryItems("%%").map { it.name }.toSet())
            val milk = dao.getAllShopListItems(7).first().first { it.name == "Milk" }
            model.updateListItem(milk.copy(itemChecked = true)).join()
            var list = dao.getAllShopListNames().first().single()
            assertEquals(2, list.allItemCounter); assertEquals(1, list.checkedItemsCounter)
            model.updateListName(list.copy(name = "Renamed")).join()
            assertEquals("Renamed", dao.getAllShopListNames().first().single().name)
            model.deleteShopItem(milk).join()
            list = dao.getAllShopListNames().first().single()
            assertEquals(1, list.allItemCounter); assertEquals(0, list.checkedItemsCounter)
            model.deleteShopList(7, false).join()
            assertTrue(dao.getAllShopListItems(7).first().isEmpty())
            assertEquals(0, dao.getAllShopListNames().first().single().allItemCounter)
            model.deleteShopList(7, true).join()
            assertTrue(dao.getAllShopListNames().first().isEmpty())
        } finally { model.viewModelScope.cancel(); db.close() }
    }
}
