package com.jobforandroid.shoplinoterelease.ui.ds

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
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

    private val shadowMarginPx = resources.getDimension(R.dimen.ds_shadow_margin)
    private val shadowBlurPx = resources.getDimension(R.dimen.ds_shadow_blur)
    private var insetShadowOffsetPx = resources.getDimension(R.dimen.ds_inset_shadow_offset)
    private val insetShadowBlurPx = resources.getDimension(R.dimen.ds_inset_shadow_blur)
    private val shadowOffsetXPx = resources.getDimension(R.dimen.ds_shadow_offset_x)
    private val shadowLightOffsetYPx = -resources.getDimension(R.dimen.ds_shadow_offset_x)
    private val shadowDarkOffsetYPx = resources.getDimension(R.dimen.ds_shadow_offset_y)
    private var shadowBitmap: Bitmap? = null
    private val shadowSourceRect = Rect()
    private val shadowTargetRect = Rect()
    private var fillColor: Int? = null
    private var pressedInset = false
    private var wasPressed = false
    private var shadowOpacity = 1f
    private val renderedVariant get() = if (pressedInset && wasPressed) VARIANT_INSET else variant

    init {
        setWillNotDraw(false)
        val defaultCornerRadius = resources.getDimension(R.dimen.ds_radius_l)
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.NeumorphicSurface,
            defStyleAttr,
            0
        )
        val initialVariant: Int
        val initialCornerRadius: Float
        try {
            if (typedArray.hasValue(R.styleable.NeumorphicSurface_dsSurfaceColor)) fillColor = typedArray.getColor(R.styleable.NeumorphicSurface_dsSurfaceColor, 0)
            insetShadowOffsetPx = typedArray.getDimension(R.styleable.NeumorphicSurface_dsInsetShadowOffset, insetShadowOffsetPx)
            pressedInset = typedArray.getBoolean(R.styleable.NeumorphicSurface_dsPressedInset, false)
            shadowOpacity = typedArray.getFloat(R.styleable.NeumorphicSurface_dsShadowOpacity, 1f).coerceIn(0f, 1f)
            initialVariant = typedArray.getInt(
                R.styleable.NeumorphicSurface_dsVariant,
                VARIANT_RAISED
            )
            initialCornerRadius = typedArray.getDimension(
                R.styleable.NeumorphicSurface_dsCornerRadius,
                defaultCornerRadius
            )
        } finally {
            typedArray.recycle()
        }
        variant = initialVariant
        cornerRadiusPx = initialCornerRadius
        updateBackground()
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        val pressed = drawableState.contains(android.R.attr.state_pressed)
        if (pressedInset && pressed != wasPressed) {
            wasPressed = pressed
            updateBackground()
            rebuildShadowBitmap()
        }
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
            if (renderedVariant == VARIANT_INSET) {
                if (it.height == height) {
                    canvas.drawBitmap(it, 0f, 0f, null)
                } else {
                    // Preserve corner geometry and stretch only the straight side shadows of long fields.
                    val edge = minOf(it.height / 3, kotlin.math.ceil(cornerRadiusPx + insetShadowBlurPx * 2 + insetShadowOffsetPx).toInt())
                    shadowSourceRect.set(0, 0, it.width, edge)
                    shadowTargetRect.set(0, 0, width, edge)
                    canvas.drawBitmap(it, shadowSourceRect, shadowTargetRect, null)
                    shadowSourceRect.set(0, edge, it.width, it.height - edge)
                    shadowTargetRect.set(0, edge, width, height - edge)
                    canvas.drawBitmap(it, shadowSourceRect, shadowTargetRect, null)
                    shadowSourceRect.set(0, it.height - edge, it.width, it.height)
                    shadowTargetRect.set(0, height - edge, width, height)
                    canvas.drawBitmap(it, shadowSourceRect, shadowTargetRect, null)
                }
            } else {
                canvas.drawBitmap(it, -shadowMarginPx, -shadowMarginPx, null)
            }
        }
    }

    private fun updateBackground() {
        val colorResource = when (renderedVariant) {
            VARIANT_INSET -> R.color.ds_surface_inset
            else -> R.color.ds_surface
        }
        background = GradientDrawable().apply {
            setColor(fillColor ?: ContextCompat.getColor(context, colorResource))
            cornerRadius = cornerRadiusPx
        }
    }

    private fun ensureShadow(): Boolean {
        if (shadowBitmap != null || width <= 0 || height <= 0 || renderedVariant == VARIANT_FLAT) return false
        return rebuildShadowBitmap(invalidateAfterBuild = false)
    }

    private fun rebuildShadowBitmap(invalidateAfterBuild: Boolean = true): Boolean {
        shadowBitmap = null
        if (width <= 0 || height <= 0 || renderedVariant == VARIANT_FLAT) {
            return false
        }

        if (renderedVariant == VARIANT_INSET) {
            // An editor can be taller than the screen; shadow memory must not scale with note length.
            val bitmapHeight = minOf(height, maxOf(resources.displayMetrics.heightPixels, (cornerRadiusPx * 4).toInt() + 1))
            val bitmap = Bitmap.createBitmap(width, bitmapHeight, Bitmap.Config.ARGB_8888)
            val bitmapCanvas = Canvas(bitmap)
            val viewRect = RectF(0f, 0f, width.toFloat(), bitmapHeight.toFloat())
            val clipPath = Path().apply {
                addRoundRect(
                    viewRect,
                    cornerRadiusPx,
                    cornerRadiusPx,
                    Path.Direction.CW
                )
            }
            bitmapCanvas.clipPath(clipPath)

            drawInsetShadow(bitmapCanvas, width, bitmapHeight, insetShadowOffsetPx, R.color.ds_shadow_inset)
            drawInsetShadow(bitmapCanvas, width, bitmapHeight, -insetShadowOffsetPx, R.color.ds_shadow_light)
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

        if (renderedVariant == VARIANT_RAISED) {
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
            color = fillColor ?: ContextCompat.getColor(context, R.color.ds_surface)
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
            alpha = (alpha * shadowOpacity).toInt()
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
            alpha = (alpha * shadowOpacity).toInt()
            maskFilter = BlurMaskFilter(insetShadowBlurPx, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, paint)
    }

    private companion object {
        const val VARIANT_RAISED = 0
        const val VARIANT_RAISED_FLAT = 1
        const val VARIANT_INSET = 2
        const val VARIANT_FLAT = 3
    }
}
