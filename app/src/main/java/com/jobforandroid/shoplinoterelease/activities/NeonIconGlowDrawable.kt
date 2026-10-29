package com.jobforandroid.shoplinoterelease.activities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.BlurMaskFilter

/** Pre-renders a small accent-colored alpha glow once, then draws the original icon on top. */
internal class NeonIconGlowDrawable(
    private val icon: Drawable,
    accentColor: Int,
    density: Float
) : Drawable() {
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(128, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
    }
    private val blurRadius = 5f * density
    private var blurredAlpha: Bitmap? = null
    private var glowOffsetX = 0
    private var glowOffsetY = 0

    init {
        icon.setTint(accentColor)
    }

    override fun onBoundsChange(bounds: Rect) {
        rebuildGlow(bounds)
    }

    override fun draw(canvas: Canvas) {
        blurredAlpha?.let {
            canvas.drawBitmap(
                it,
                (bounds.left + glowOffsetX).toFloat(),
                (bounds.top + glowOffsetY).toFloat(),
                glowPaint
            )
        }
        icon.bounds = bounds
        icon.draw(canvas)
    }

    private fun rebuildGlow(bounds: Rect) {
        blurredAlpha?.recycle()
        blurredAlpha = null
        if (bounds.width() <= 0 || bounds.height() <= 0) return

        val mask = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(mask)
        icon.bounds = Rect(0, 0, bounds.width(), bounds.height())
        icon.draw(maskCanvas)
        val offsets = IntArray(2)
        blurredAlpha = mask.extractAlpha(
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            },
            offsets
        )
        glowOffsetX = offsets[0]
        glowOffsetY = offsets[1]
        mask.recycle()
        icon.bounds = bounds
    }

    override fun setAlpha(alpha: Int) {
        glowPaint.alpha = alpha
        icon.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        icon.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = icon.intrinsicWidth
    override fun getIntrinsicHeight(): Int = icon.intrinsicHeight

    override fun getMinimumWidth(): Int = icon.minimumWidth
    override fun getMinimumHeight(): Int = icon.minimumHeight

    override fun isStateful(): Boolean = icon.isStateful
    override fun onStateChange(state: IntArray): Boolean = icon.setState(state)
    override fun onLevelChange(level: Int): Boolean = icon.setLevel(level)
}
