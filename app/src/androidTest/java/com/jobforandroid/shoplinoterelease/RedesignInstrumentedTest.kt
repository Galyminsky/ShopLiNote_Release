package com.jobforandroid.shoplinoterelease

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spannable
import android.text.style.StyleSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Room
import com.jobforandroid.shoplinoterelease.BD.MainDataBase
import com.jobforandroid.shoplinoterelease.ui.ds.RichTextFormatterDs
import com.jobforandroid.shoplinoterelease.utils.HtmlManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RedesignInstrumentedTest {
    private fun styleAt(text: android.text.Spanned, index: Int): Int = text.getSpans(index, index + 1, StyleSpan::class.java)
        .filter { text.getSpanStart(it) <= index && text.getSpanEnd(it) > index }.fold(0) { value, span -> value or span.style }

    @Test fun partialBoldDoesNotChangeOutsideSelection() {
        val text = SpannableStringBuilder("abcdef")
        text.setSpan(StyleSpan(Typeface.BOLD_ITALIC), 0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        RichTextFormatterDs.toggleBold(text, 2, 4)
        assertEquals(listOf(3, 3, 2, 2, 3, 3), (0..5).map { styleAt(text, it) })
        RichTextFormatterDs.toggleBold(text, 2, 4)
        assertTrue((0..5).all { styleAt(text, it) == Typeface.BOLD_ITALIC })
    }

    @Test fun overlappingStylesAndHtmlRoundTrip() {
        val text = SpannableStringBuilder("abcdef")
        text.setSpan(StyleSpan(Typeface.BOLD), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(StyleSpan(Typeface.ITALIC), 2, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        RichTextFormatterDs.toggleItalic(text, 1, 5)
        assertEquals(listOf(1, 3, 3, 3, 2, 2), (0..5).map { styleAt(text, it) })
        RichTextFormatterDs.toggleBold(text, 2, 4)
        assertEquals(listOf(1, 3, 2, 2, 2, 2), (0..5).map { styleAt(text, it) })
        val restored = HtmlManager.getFromHtml(HtmlManager.toHtml(text))
        assertEquals(text.toString(), restored.toString().trim())
        assertEquals((0..5).map { styleAt(text, it) }, (0..5).map { styleAt(restored, it) })
    }

    @Test fun migrationPreservesLegacyRecordsAndRoomValidatesSchema() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "redesign-migration-test-" + java.util.UUID.randomUUID() + ".db"
        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE note_list (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, content TEXT NOT NULL, time TEXT NOT NULL, category TEXT NOT NULL)")
                    db.execSQL("CREATE TABLE shopping_list_names (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, time TEXT NOT NULL, allItemCounter INTEGER NOT NULL, checkedItemsCounter INTEGER NOT NULL, itemsIds TEXT NOT NULL)")
                    db.execSQL("CREATE TABLE shop_list_item (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, itemInfo TEXT NOT NULL, itemChecked INTEGER NOT NULL, listId INTEGER NOT NULL, itemType INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE library (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)")
                    db.execSQL("INSERT INTO note_list VALUES (7, 'Legacy', '<b>Body</b>', '09:30:00 - 17.11.26', 'category')")
                    db.execSQL("INSERT INTO note_list VALUES (8, 'Invalid date', '', 'unknown', '')")
                    db.execSQL("INSERT INTO shopping_list_names VALUES (2, 'Groceries', 'time', 1, 1, '')")
                    db.execSQL("INSERT INTO shop_list_item VALUES (3, 'Milk', 'info', 1, 2, 0)")
                    db.execSQL("INSERT INTO library VALUES (4, 'Milk')")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build())
        try {
            helper.writableDatabase
            helper.close()
            val room = Room.databaseBuilder(context, MainDataBase::class.java, name)
                .addMigrations(MainDataBase.MIGRATION_1_2).allowMainThreadQueries().build()
            try {
                val db = room.openHelper.writableDatabase
                db.query("SELECT title, content, time, category, modifiedAt, hasBeenEdited FROM note_list WHERE id=7").use {
                    assertTrue(it.moveToFirst()); assertEquals("Legacy", it.getString(0)); assertEquals("<b>Body</b>", it.getString(1))
                    assertEquals("09:30:00 - 17.11.26", it.getString(2)); assertEquals("category", it.getString(3))
                    assertTrue(it.getLong(4) > 0); assertEquals(0, it.getInt(5))
                }
                db.query("SELECT modifiedAt, hasBeenEdited FROM note_list WHERE id=8").use { assertTrue(it.moveToFirst()); assertEquals(0L, it.getLong(0)); assertEquals(0, it.getInt(1)) }
                for (table in listOf("shopping_list_names", "shop_list_item", "library")) {
                    db.query("SELECT COUNT(*) FROM " + table).use { assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0)) }
                }
                db.query("SELECT itemChecked, listId FROM shop_list_item WHERE id=3").use { assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0)); assertEquals(2, it.getInt(1)) }
            } finally { room.close() }
        } finally { helper.close(); context.deleteDatabase(name) }
    }
}
