package com.jobforandroid.shoplinoterelease.activities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import kotlin.math.min

/** A crisp accent ring surrounded by a static, softly fading neon halo. */
internal class NeonCircleOutlineDrawable(
    private val accentColor: Int,
    density: Float
) : Drawable() {
    private val densityScale = density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val glowExtent = 7f * density
    private val ringSpacing = 0.55f * density
    private val ringWidth = 1.05f * density
    private val coreWidth = 1.25f * density

    override fun draw(canvas: Canvas) {
        val b = bounds
        val centerX = b.exactCenterX()
        val centerY = b.exactCenterY()
        val coreRadius = min(b.width(), b.height()) / 2f - coreWidth / 2f - 0.5f * densityScale
        if (coreRadius <= 0f) return

        val ringCount = (glowExtent / ringSpacing).toInt()
        for (ring in ringCount downTo 0) {
            val distance = ring * ringSpacing
            val fade = 1f - distance / glowExtent
            val alpha = (Color.alpha(accentColor) * 0.28f * fade * fade).toInt()
            if (alpha == 0) continue
            paint.color = Color.argb(
                alpha,
                Color.red(accentColor),
                Color.green(accentColor),
                Color.blue(accentColor)
            )
            paint.strokeWidth = ringWidth
            canvas.drawCircle(centerX, centerY, coreRadius + coreWidth / 2f + distance, paint)
        }

        paint.color = accentColor
        paint.strokeWidth = coreWidth
        canvas.drawCircle(centerX, centerY, coreRadius, paint)
    }

    override fun setAlpha(alpha: Int) {
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
