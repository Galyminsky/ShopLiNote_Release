package com.jobforandroid.shoplinoterelease.utils

import android.content.Intent
import com.jobforandroid.shoplinoterelease.entities.ShopListItem

object ShareHelper {
    fun shareShopList(shopList: List<ShopListItem>, listName: String): Intent {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.apply {
            putExtra(Intent.EXTRA_TEXT, makeShareText(shopList, listName))
        }
        return intent

    }

    private fun makeShareText(shopList: List<ShopListItem>, listName: String): String {
        val sBuilder = StringBuilder()
        sBuilder.append("<< $listName >>")
        sBuilder.append("\n")
        var counter = 0
        shopList.forEach {
            sBuilder.append("${++counter}. ${it.name} (${it.itemInfo})")
            sBuilder.append("\n")
        }
        return sBuilder.toString()
    }
}