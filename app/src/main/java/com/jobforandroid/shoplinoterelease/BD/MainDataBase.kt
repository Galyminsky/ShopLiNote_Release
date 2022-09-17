package com.jobforandroid.shoplinoterelease.BD

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jobforandroid.shoplinoterelease.entities.LibraryItem
import com.jobforandroid.shoplinoterelease.entities.NoteItem
import com.jobforandroid.shoplinoterelease.entities.ShopListItem
import com.jobforandroid.shoplinoterelease.entities.ShopListNameItem


@Database(
    entities = [LibraryItem::class, NoteItem::class,
        ShopListItem::class, ShopListNameItem::class], version = 1
)

abstract class MainDataBase : RoomDatabase() {

    abstract fun getDao(): Dao

    companion object {
        @Volatile
        private var INSTANCE: MainDataBase? = null

        fun getDataBase(context: Context): MainDataBase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MainDataBase::class.java,
                    "shopping_list.db"
                ).build()
                instance
            }
        }
    }

}