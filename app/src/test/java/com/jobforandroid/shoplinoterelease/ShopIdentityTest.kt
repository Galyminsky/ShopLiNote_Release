package com.jobforandroid.shoplinoterelease

import com.jobforandroid.shoplinoterelease.BD.ShopListItemAdapter
import com.jobforandroid.shoplinoterelease.entities.ShopListItem
import org.junit.Assert.*
import org.junit.Test

class ShopIdentityTest {
    @Test fun librarySuggestionAndSavedProductWithSameIdHaveDifferentIdentity() {
        val comparator = ShopListItemAdapter.ItemComparator()
        val product = ShopListItem(1, "Milk", "", false, 7, 0)
        val suggestion = product.copy(listId = 0, itemType = 1)
        assertFalse(comparator.areItemsTheSame(product, suggestion))
        assertTrue(comparator.areItemsTheSame(product, product.copy(itemChecked = true)))
        assertFalse(comparator.areContentsTheSame(product, product.copy(itemChecked = true)))
    }
}
