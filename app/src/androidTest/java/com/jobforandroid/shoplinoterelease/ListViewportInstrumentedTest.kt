package com.jobforandroid.shoplinoterelease

import android.graphics.Rect
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Detached fixtures exercise actual Android layout/scrolling without reading or writing user records. */
@RunWith(AndroidJUnit4::class)
class ListViewportInstrumentedTest {
    private class Cards(private val layout: Int) : RecyclerView.Adapter<Cards.Holder>() {
        class Holder(view: View) : RecyclerView.ViewHolder(view)
        override fun getItemCount() = 40
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.itemView.findViewById<TextView>(if (layout == R.layout.note_list_item_ds) R.id.tvTitle else R.id.tvListName).text = "Card " + position
            holder.itemView.findViewById<TextView>(if (layout == R.layout.note_list_item_ds) R.id.tvDescription else R.id.tvListLeft).text = "Content"
        }
    }

    @Test fun notesAndPurchasesKeepHeadersFixedAndLastCardVisible() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_App_DS)
            val inflater = LayoutInflater.from(context)
            val density = context.resources.displayMetrics.density
            for (layout in listOf(R.layout.fragment_note_ds, R.layout.fragment_shop_list_names_ds)) {
                for (grid in if (layout == R.layout.fragment_note_ds) listOf(false, true) else listOf(false)) {
                    val root = inflater.inflate(layout, null) as ViewGroup
                    val notes = layout == R.layout.fragment_note_ds
                    val list = root.findViewById<RecyclerView>(if (notes) R.id.rcViewNote else R.id.rvShopLists)
                    val viewport = root.findViewById<FrameLayout>(R.id.listViewport)
                    val header = root.findViewById<View>(if (notes) R.id.notesHeader else R.id.tvShopTitle)
                    val section = root.findViewById<View>(if (notes) R.id.sectionHeader else R.id.shopSectionHeader)
                    section.visibility = View.VISIBLE
                    if (!notes) (root.findViewById<View>(R.id.btnNewList).parent as View).visibility = View.GONE
                    list.layoutManager = if (grid) StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL) else LinearLayoutManager(context)
                    list.itemAnimator = null
                    list.adapter = Cards(if (notes) R.layout.note_list_item_ds else R.layout.shop_list_name_item_ds)
                    val main = inflater.inflate(R.layout.activity_main_ds, null) as ViewGroup
                    val placeholder = main.findViewById<FrameLayout>(R.id.placeHolder)
                    placeholder.addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    main.measure(View.MeasureSpec.makeMeasureSpec((360 * density).toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec((640 * density).toInt(), View.MeasureSpec.EXACTLY))
                    main.layout(0, 0, main.measuredWidth, main.measuredHeight)
                    fun mainBounds(view: View) = Rect(0, 0, view.width, view.height).also { main.offsetDescendantRectToMyCoords(view, it) }
                    val create = main.findViewById<View>(R.id.newNoteInclude)
                    val navigation = main.findViewById<View>(R.id.bottomNavInclude)
                    val createBounds = mainBounds(create)
                    val navigationBounds = mainBounds(navigation)
                    assertTrue("The list container must end above the fixed create button", mainBounds(placeholder).bottom <= createBounds.top)
                    fun bounds(view: View) = Rect(0, 0, view.width, view.height).also { root.offsetDescendantRectToMyCoords(view, it) }
                    val headerBounds = bounds(header)
                    val sectionBounds = bounds(section)
                    assertTrue(viewport.clipChildren)
                    assertTrue(viewport.clipToPadding)
                    assertSame(viewport, list.parent)
                    assertEquals(viewport.height, list.height)
                    assertEquals(0, list.top)
                    assertTrue(sectionBounds.bottom <= bounds(viewport).top)
                    fun drawHeader(): Bitmap = Bitmap.createBitmap(root.width, viewport.top, Bitmap.Config.ARGB_8888).also { root.draw(Canvas(it)) }
                    val beforeScroll = drawHeader()
                    list.scrollBy(0, (20000 * density).toInt())
                    val afterScroll = drawHeader()
                    assertTrue("Scrolled cards and their shadows must not change any header pixels", beforeScroll.sameAs(afterScroll))
                    beforeScroll.recycle()
                    afterScroll.recycle()
                    assertEquals(createBounds, mainBounds(create))
                    assertEquals(navigationBounds, mainBounds(navigation))
                    assertEquals(headerBounds, bounds(header))
                    assertEquals(sectionBounds, bounds(section))
                    assertFalse("Last card must reach the end of the list", list.canScrollVertically(1))
                    val last = list.layoutManager!!.findViewByPosition(39)
                    assertNotNull(last)
                    assertTrue("Last card must fit above the list bottom padding", last!!.bottom <= list.height - list.paddingBottom)
                    assertSame(viewport, root.findViewById<View>(if (notes) R.id.emptyState else R.id.emptyShopLists).parent)
                    list.adapter = null
                }
            }
        }
    }

    @Test fun settingsTitleUsesTheSameTypographyAndHeaderStaysOutsideList() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_App_DS)
            val inflater = LayoutInflater.from(context)
            val settings = inflater.inflate(R.layout.fragment_settings_ds, null) as ViewGroup
            val notes = inflater.inflate(R.layout.notes_header_ds, null)
            val title = settings.findViewById<TextView>(R.id.settingsTitle)
            val noteTitle = notes.findViewById<TextView>(R.id.tvScreenTitle)
            assertEquals(noteTitle.textSize, title.textSize, 0f)
            assertEquals(noteTitle.typeface, title.typeface)
            assertEquals(context.getString(R.string.ds_settings_subtitle), settings.findViewById<TextView>(R.id.settingsSubtitle).text.toString())
            val header = settings.findViewById<View>(R.id.settingsHeader)
            val viewport = settings.findViewById<FrameLayout>(R.id.settingsListViewport)
            assertSame(settings, header.parent)
            assertSame(settings, viewport.parent)
            assertTrue(viewport.clipChildren)
            assertTrue(viewport.clipToPadding)
        }
    }
}
