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
        ShopListItem::class, ShopListNameItem::class], version = 2
)

abstract class MainDataBase : RoomDatabase() {

    abstract fun getDao(): Dao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE note_list ADD COLUMN modifiedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE note_list ADD COLUMN hasBeenEdited INTEGER NOT NULL DEFAULT 0")
                db.query("SELECT id, time FROM note_list").use { cursor ->
                    while (cursor.moveToNext()) {
                        val timestamp = com.jobforandroid.shoplinoterelease.utils.TimeManager.parseStoredTime(cursor.getString(1))?.time ?: 0L
                        db.execSQL("UPDATE note_list SET modifiedAt = ? WHERE id = ?", arrayOf<Any>(timestamp, cursor.getInt(0)))
                    }
                }
            }
        }

        @Volatile
        private var INSTANCE: MainDataBase? = null

        fun getDataBase(context: Context): MainDataBase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let { return@synchronized it }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MainDataBase::class.java,
                    "shopping_list.db"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }

}