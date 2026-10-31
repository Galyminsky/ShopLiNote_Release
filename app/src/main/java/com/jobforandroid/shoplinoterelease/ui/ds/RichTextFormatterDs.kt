package com.jobforandroid.shoplinoterelease.ui.ds

import android.graphics.Typeface
import android.text.Spannable
import android.text.Editable
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan

object RichTextFormatterDs {
    fun toggleBold(editable: Editable, start: Int, end: Int) = toggleStyle(editable, start, end, Typeface.BOLD)

    fun toggleItalic(editable: Editable, start: Int, end: Int) = toggleStyle(editable, start, end, Typeface.ITALIC)

    private fun toggleStyle(editable: Spannable, start: Int, end: Int, style: Int) {
        if (start < 0 || end <= start || end > editable.length) return
        val covering = editable.getSpans(start, end, StyleSpan::class.java)
            .firstOrNull { editable.getSpanStart(it) <= start && editable.getSpanEnd(it) >= end }
        val currentStyle = covering?.style ?: Typeface.NORMAL
        if (covering != null) {
            val spanStart = editable.getSpanStart(covering)
            val spanEnd = editable.getSpanEnd(covering)
            editable.removeSpan(covering)
            val updated = if (currentStyle and style != 0) currentStyle and style.inv() else currentStyle or style
            if (updated != Typeface.NORMAL) editable.setSpan(StyleSpan(updated), spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {
            editable.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun applyColor(editable: Editable, start: Int, end: Int, color: Int?) {
        if (start < 0 || end <= start || end > editable.length) return
        editable.getSpans(start, end, ForegroundColorSpan::class.java)
            .filter { editable.getSpanStart(it) < end && editable.getSpanEnd(it) > start }
            .forEach { span ->
            val spanStart = editable.getSpanStart(span)
            val spanEnd = editable.getSpanEnd(span)
            val flags = editable.getSpanFlags(span)
            val oldColor = span.foregroundColor
            editable.removeSpan(span)
            if (spanStart < start) editable.setSpan(ForegroundColorSpan(oldColor), spanStart, start, flags)
            if (spanEnd > end) editable.setSpan(ForegroundColorSpan(oldColor), end, spanEnd, flags)
        }
        if (color != null) editable.setSpan(ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
