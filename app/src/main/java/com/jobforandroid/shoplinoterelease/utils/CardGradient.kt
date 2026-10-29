package com.jobforandroid.shoplinoterelease.utils

import android.graphics.drawable.GradientDrawable
import android.view.View
import com.google.android.material.color.MaterialColors
import com.jobforandroid.shoplinoterelease.R

object CardGradient {
    fun apply(view: View, position: Int) {
        val startColor = MaterialColors.getColor(
            view,
            com.google.android.material.R.attr.colorSurface
        )
        val endColor = MaterialColors.getColor(view, R.attr.theme_card_gradient_end)
        val direction = if (position % 2 == 0) {
            GradientDrawable.Orientation.LEFT_RIGHT
        } else {
            GradientDrawable.Orientation.RIGHT_LEFT
        }

        val gradient = GradientDrawable(direction, intArrayOf(startColor, endColor))
        val density = view.resources.displayMetrics.density
        view.background = NeumorphicSurfaceDrawable(
            surface = gradient,
            cornerRadius = 12f * density,
            depth = 16f * density,
            highlightColor = MaterialColors.getColor(view, R.attr.theme_surface_highlight),
            shadowColor = MaterialColors.getColor(view, R.attr.theme_surface_shadow)
        )
    }
}
