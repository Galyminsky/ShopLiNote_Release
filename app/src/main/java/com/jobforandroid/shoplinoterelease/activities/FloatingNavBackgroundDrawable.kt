package com.jobforandroid.shoplinoterelease.activities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import com.jobforandroid.shoplinoterelease.utils.NeumorphicSurfaceDrawable
import kotlin.math.cos
import kotlin.math.sin

internal class FloatingNavBackgroundDrawable(
    private val outlineColor: Int,
    private val highlightColor: Int,
    private val shadowColor: Int,
    density: Float
) : Drawable() {
    private val densityScale = density
    private val cornerRadius = 16f * density
    private val notchRadius = 39f * density
    private val transitionWidth = 5f * density
    private val transitionAngle = Math.toRadians(18.0)
    private val strokeWidth = density
    private var notchCenterX = 0f
    private var drawableAlpha = 255
    private var colorFilter: ColorFilter? = null

    private val path = Path()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bevelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@FloatingNavBackgroundDrawable.strokeWidth
        color = outlineColor
    }

    val minimumNotchCenter: Float
        get() = cornerRadius + notchRadius + transitionWidth

    fun setNotchCenter(centerX: Float) {
        if (notchCenterX != centerX) {
            notchCenterX = centerX
            invalidateSelf()
        }
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return
        buildPath(bounds)
        fillPaint.shader = LinearGradient(
            0f,
            bounds.top.toFloat(),
            0f,
            bounds.bottom.toFloat(),
            Color.WHITE,
            Color.rgb(252, 253, 255),
            Shader.TileMode.CLAMP
        )
        fillPaint.alpha = drawableAlpha
        fillPaint.colorFilter = colorFilter
        strokePaint.alpha = drawableAlpha
        strokePaint.colorFilter = colorFilter
        canvas.drawPath(path, fillPaint)
        NeumorphicSurfaceDrawable.drawInnerBevel(
            canvas = canvas,
            path = path,
            bounds = RectF(bounds),
            depth = 18f * densityScale,
            highlightColor = highlightColor,
            shadowColor = shadowColor,
            paint = bevelPaint
        )
        canvas.drawPath(path, strokePaint)
    }

    private fun buildPath(bounds: android.graphics.Rect) {
        val left = bounds.left.toFloat()
        val top = bounds.top.toFloat()
        val right = bounds.right.toFloat()
        val bottom = bounds.bottom.toFloat()
        val radius = cornerRadius
        val safeCenter = if (right - left < minimumNotchCenter * 2f) {
            (right - left) / 2f
        } else {
            notchCenterX.coerceIn(minimumNotchCenter, right - left - minimumNotchCenter)
        }
        val centerX = left + safeCenter
        val r = notchRadius
        val angle = transitionAngle
        val cosAngle = cos(angle).toFloat()
        val sinAngle = sin(angle).toFloat()
        val arcStartX = centerX - r * cosAngle
        val arcStartY = top + r * sinAngle
        val arcEndX = centerX + r * cosAngle
        val arcEndY = arcStartY
        val tangentLength = transitionWidth * 0.9f
        val startX = centerX - r - transitionWidth
        val endX = centerX + r + transitionWidth

        path.reset()
        path.moveTo(left + radius, top)
        path.lineTo(startX, top)
        path.cubicTo(
            startX + transitionWidth * 0.65f, top,
            arcStartX - sinAngle * tangentLength, arcStartY - cosAngle * tangentLength,
            arcStartX, arcStartY
        )
        path.arcTo(
            RectF(centerX - r, top - r, centerX + r, top + r),
            180f - Math.toDegrees(angle).toFloat(),
            -(180f - 2f * Math.toDegrees(angle).toFloat()),
            false
        )
        path.cubicTo(
            arcEndX + sinAngle * tangentLength, arcEndY - cosAngle * tangentLength,
            endX - transitionWidth * 0.65f, top,
            endX, top
        )
        path.lineTo(right - radius, top)
        path.quadTo(right, top, right, top + radius)
        path.lineTo(right, bottom - radius)
        path.quadTo(right, bottom, right - radius, bottom)
        path.lineTo(left + radius, bottom)
        path.quadTo(left, bottom, left, bottom - radius)
        path.lineTo(left, top + radius)
        path.quadTo(left, top, left + radius, top)
        path.close()
    }

    override fun setAlpha(alpha: Int) {
        drawableAlpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(filter: ColorFilter?) {
        colorFilter = filter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getOutline(outline: Outline) {
        val b = bounds
        outline.setRoundRect(b, cornerRadius)
    }
}
