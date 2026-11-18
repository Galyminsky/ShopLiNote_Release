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
        val spans = editable.getSpans(0, editable.length, StyleSpan::class.java)
        data class Run(val start: Int, val end: Int, val style: Int)
        val runs = spans.map { Run(editable.getSpanStart(it), editable.getSpanEnd(it), it.style) }
        val points = (runs.flatMap { listOf(it.start, it.end) } + listOf(0, start, end, editable.length)).distinct().sorted()
        fun styleAt(a: Int, b: Int) = runs.filter { it.start < b && it.end > a }.fold(0) { value, run -> value or run.style }
        val remove = points.zipWithNext().filter { it.first >= start && it.second <= end }
            .all { styleAt(it.first, it.second) and style != 0 }
        spans.forEach { editable.removeSpan(it) }
        points.zipWithNext().forEach { (a, b) ->
            var value = styleAt(a, b)
            if (a >= start && b <= end) value = if (remove) value and style.inv() else value or style
            if (value != Typeface.NORMAL && b > a) editable.setSpan(StyleSpan(value), a, b, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
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
