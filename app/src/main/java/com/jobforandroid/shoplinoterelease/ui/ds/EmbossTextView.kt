package com.jobforandroid.shoplinoterelease.ui.ds

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.jobforandroid.shoplinoterelease.R

/** Two crisp, low-opacity edge passes; unlike a shadow layer, neither pass blurs the glyphs. */
class EmbossTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {
    private val edgeOffset: Float
    private val highlightColor: Int
    private val edgeShadowColor: Int

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.DsEmbossHeading, defStyleAttr, 0)
        try {
            edgeOffset = attributes.getDimension(R.styleable.DsEmbossHeading_dsEmbossOffset, resources.getDimension(R.dimen.ds_emboss_offset))
            highlightColor = attributes.getColor(R.styleable.DsEmbossHeading_dsEmbossHighlight, ContextCompat.getColor(context, R.color.ds_emboss_highlight))
            edgeShadowColor = attributes.getColor(R.styleable.DsEmbossHeading_dsEmbossShadow, ContextCompat.getColor(context, R.color.ds_emboss_shadow))
        } finally {
            attributes.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val textLayout = layout
        if (textLayout != null && text.isNotEmpty() && isEnabled) {
            val extraHeight = (height - extendedPaddingTop - extendedPaddingBottom - textLayout.height).coerceAtLeast(0)
            val verticalOffset = when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
                Gravity.BOTTOM -> extraHeight
                Gravity.CENTER_VERTICAL -> extraHeight / 2
                else -> 0
            }
            val originalColor = paint.color
            fun drawEdge(color: Int, offset: Float) {
                val save = canvas.save()
                canvas.translate(compoundPaddingLeft - scrollX + offset, extendedPaddingTop - scrollY + verticalOffset + offset)
                paint.color = color
                textLayout.draw(canvas)
                canvas.restoreToCount(save)
            }
            try {
                drawEdge(edgeShadowColor, edgeOffset)
                drawEdge(highlightColor, -edgeOffset)
            } finally {
                paint.color = originalColor
            }
        }
        // The unchanged foreground pass keeps the original typeface, size, color and contrast.
        super.onDraw(canvas)
    }
}
