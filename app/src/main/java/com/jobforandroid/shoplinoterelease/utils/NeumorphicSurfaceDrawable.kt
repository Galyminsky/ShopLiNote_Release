package com.jobforandroid.shoplinoterelease.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

/** Draws a soft inset bevel over an existing themed surface without replacing its fill. */
internal class NeumorphicSurfaceDrawable(
    private val surface: Drawable,
    private val cornerRadius: Float,
    private val depth: Float,
    private val highlightColor: Int,
    private val shadowColor: Int
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val shapePath = Path()
    private var drawableAlpha = 255
    private var colorFilter: ColorFilter? = null

    override fun onBoundsChange(bounds: Rect) {
        surface.bounds = bounds
    }

    override fun draw(canvas: Canvas) {
        surface.bounds = bounds
        surface.alpha = drawableAlpha
        surface.colorFilter = colorFilter
        surface.draw(canvas)

        rect.set(bounds)
        shapePath.reset()
        shapePath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        drawInnerBevel(
            canvas = canvas,
            path = shapePath,
            bounds = rect,
            depth = depth,
            highlightColor = highlightColor,
            shadowColor = shadowColor,
            paint = paint
        )
    }

    override fun setAlpha(alpha: Int) {
        drawableAlpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        this.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getOutline(outline: Outline) {
        surface.getOutline(outline)
    }

    companion object {
        /**
         * Paints broad, low-contrast fades at the inside edge of a shape. Opposite edges
         * use theme highlight/shadow colors to suggest a raised surface without blur filters.
         */
        fun drawInnerBevel(
            canvas: Canvas,
            path: Path,
            bounds: RectF,
            depth: Float,
            highlightColor: Int,
            shadowColor: Int,
            paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        ) {
            if (bounds.isEmpty || depth <= 0f) return
            val fadeDepth = depth.coerceAtMost(minOf(bounds.width(), bounds.height()) / 3f)
            val save = canvas.save()
            canvas.clipPath(path)

            drawFade(
                canvas, bounds, fadeDepth, horizontal = false, fromEnd = false,
                color = withOpacity(highlightColor, 0.58f), paint = paint
            )
            drawFade(
                canvas, bounds, fadeDepth, horizontal = true, fromEnd = false,
                color = withOpacity(highlightColor, 0.46f), paint = paint
            )
            drawFade(
                canvas, bounds, fadeDepth, horizontal = false, fromEnd = true,
                color = withOpacity(shadowColor, 0.40f), paint = paint
            )
            drawFade(
                canvas, bounds, fadeDepth, horizontal = true, fromEnd = true,
                color = withOpacity(shadowColor, 0.34f), paint = paint
            )

            paint.shader = null
            canvas.restoreToCount(save)
        }

        private fun drawFade(
            canvas: Canvas,
            bounds: RectF,
            depth: Float,
            horizontal: Boolean,
            fromEnd: Boolean,
            color: Int,
            paint: Paint
        ) {
            val transparent = color and 0x00FFFFFF
            val startX: Float
            val startY: Float
            val endX: Float
            val endY: Float
            val rect: RectF
            if (horizontal) {
                startX = if (fromEnd) bounds.right - depth else bounds.left
                endX = if (fromEnd) bounds.right else bounds.left + depth
                startY = bounds.top
                endY = bounds.top
                rect = if (fromEnd) RectF(bounds.right - depth, bounds.top, bounds.right, bounds.bottom)
                else RectF(bounds.left, bounds.top, bounds.left + depth, bounds.bottom)
            } else {
                startY = if (fromEnd) bounds.bottom - depth else bounds.top
                endY = if (fromEnd) bounds.bottom else bounds.top + depth
                startX = bounds.left
                endX = bounds.left
                rect = if (fromEnd) RectF(bounds.left, bounds.bottom - depth, bounds.right, bounds.bottom)
                else RectF(bounds.left, bounds.top, bounds.right, bounds.top + depth)
            }

            val colors = if (fromEnd) intArrayOf(transparent, color) else intArrayOf(color, transparent)
            paint.shader = LinearGradient(startX, startY, endX, endY, colors, null, Shader.TileMode.CLAMP)
            canvas.drawRect(rect, paint)
        }

        private fun withOpacity(color: Int, opacity: Float): Int =
            Color.argb((Color.alpha(color) * opacity).toInt(), Color.red(color), Color.green(color), Color.blue(color))
    }
}
