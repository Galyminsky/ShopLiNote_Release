package com.jobforandroid.shoplinoterelease.ui.ds

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.jobforandroid.shoplinoterelease.R

/**
 * Neumorphic surface with cached outer shadows.
 * The parent must have clipChildren=false, or the shadow will be clipped.
 */
class NeumorphicSurface @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var variant: Int = VARIANT_RAISED
        set(value) {
            if (field != value) {
                field = value
                updateBackground()
                rebuildShadowBitmap()
            }
        }

    var cornerRadiusPx: Float = resources.getDimension(R.dimen.ds_radius_l)
        set(value) {
            if (field != value) {
                field = value
                updateBackground()
                rebuildShadowBitmap()
            }
        }

    private val density = resources.displayMetrics.density
    private val shadowMarginPx = 32f * density
    private val shadowBlurPx = 10f * density
    private val insetShadowOffsetPx = resources.getDimension(R.dimen.ds_inset_shadow_offset)
    private val insetShadowBlurPx = resources.getDimension(R.dimen.ds_inset_shadow_blur)
    private val shadowOffsetXPx = 5f * density
    private val shadowLightOffsetYPx = -5f * density
    private val shadowDarkOffsetYPx = 6f * density
    private var shadowBitmap: Bitmap? = null

    init {
        setWillNotDraw(false)
        val defaultCornerRadius = resources.getDimension(R.dimen.ds_radius_l)
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.DsNeumorphicSurface,
            defStyleAttr,
            0
        )
        val initialVariant: Int
        val initialCornerRadius: Float
        try {
            initialVariant = typedArray.getInt(
                R.styleable.DsNeumorphicSurface_dsVariant,
                VARIANT_RAISED
            )
            initialCornerRadius = typedArray.getDimension(
                R.styleable.DsNeumorphicSurface_dsCornerRadius,
                defaultCornerRadius
            )
        } finally {
            typedArray.recycle()
        }
        variant = initialVariant
        cornerRadiusPx = initialCornerRadius
        updateBackground()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildShadowBitmap()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (ensureShadow()) invalidate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        ensureShadow()
    }

    override fun onDraw(canvas: Canvas) {
        ensureShadow()
        shadowBitmap?.let {
            if (variant == VARIANT_INSET) {
                canvas.drawBitmap(it, 0f, 0f, null)
            } else {
                canvas.drawBitmap(it, -shadowMarginPx, -shadowMarginPx, null)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    private fun updateBackground() {
        val colorResource = when (variant) {
            VARIANT_INSET -> R.color.ds_surface_inset
            else -> R.color.ds_surface
        }
        background = GradientDrawable().apply {
            setColor(ContextCompat.getColor(context, colorResource))
            cornerRadius = cornerRadiusPx
        }
    }

    private fun ensureShadow(): Boolean {
        if (shadowBitmap != null || width <= 0 || height <= 0) return false
        return rebuildShadowBitmap(invalidateAfterBuild = false)
    }

    private fun rebuildShadowBitmap(invalidateAfterBuild: Boolean = true): Boolean {
        shadowBitmap = null
        if (width <= 0 || height <= 0) {
            return false
        }

        if (variant == VARIANT_INSET) {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val bitmapCanvas = Canvas(bitmap)
            val viewRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            val clipPath = Path().apply {
                addRoundRect(
                    viewRect,
                    cornerRadiusPx,
                    cornerRadiusPx,
                    Path.Direction.CW
                )
            }
            bitmapCanvas.clipPath(clipPath)

            drawInsetShadow(bitmapCanvas, width, height, insetShadowOffsetPx, R.color.ds_shadow_inset)
            drawInsetShadow(bitmapCanvas, width, height, -insetShadowOffsetPx, R.color.ds_shadow_light)
            shadowBitmap = bitmap
            if (invalidateAfterBuild) invalidate()
            return true
        }

        val margin = shadowMarginPx.toInt()
        val bitmap = Bitmap.createBitmap(
            width + margin * 2,
            height + margin * 2,
            Bitmap.Config.ARGB_8888
        )
        val bitmapCanvas = Canvas(bitmap)
        val rect = RectF(
            shadowMarginPx,
            shadowMarginPx,
            shadowMarginPx + width,
            shadowMarginPx + height
        )

        if (variant == VARIANT_RAISED) {
            drawShadow(
                bitmapCanvas,
                rect,
                R.color.ds_shadow_light,
                -shadowOffsetXPx,
                shadowLightOffsetYPx
            )
        }
        drawShadow(
            bitmapCanvas,
            rect,
            R.color.ds_shadow_dark,
            shadowOffsetXPx,
            shadowDarkOffsetYPx
        )

        val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.ds_surface)
        }
        bitmapCanvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, surfacePaint)
        shadowBitmap = bitmap
        if (invalidateAfterBuild) invalidate()
        return true
    }

    private fun drawShadow(
        canvas: Canvas,
        rect: RectF,
        colorResource: Int,
        offsetX: Float,
        offsetY: Float
    ) {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, colorResource)
            maskFilter = BlurMaskFilter(shadowBlurPx, BlurMaskFilter.Blur.NORMAL)
        }
        val shadowRect = RectF(rect).apply { offset(offsetX, offsetY) }
        canvas.drawRoundRect(shadowRect, cornerRadiusPx, cornerRadiusPx, shadowPaint)
    }

    private fun drawInsetShadow(canvas: Canvas, width: Int, height: Int, offset: Float, colorResource: Int) {
        val path = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addRect(RectF(-width.toFloat(), -height.toFloat(), width * 2f, height * 2f), Path.Direction.CW)
            addRoundRect(
                RectF(offset, offset, width + offset, height + offset),
                cornerRadiusPx,
                cornerRadiusPx,
                Path.Direction.CW
            )
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, colorResource)
            maskFilter = BlurMaskFilter(insetShadowBlurPx, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, paint)
    }

    private companion object {
        const val VARIANT_RAISED = 0
        const val VARIANT_RAISED_FLAT = 1
        const val VARIANT_INSET = 2
    }
}
