package com.lagradost.cloudstream3.utils

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.view.ViewGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.ShapeAppearanceModel
import android.content.Context
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.utils.UIHelper.colorFromAttribute
import kotlin.math.roundToInt

enum class GroupPosition {
    Single,
    First,
    Middle,
    Last
}

object GroupCardHelper {
    const val CARD_BACKGROUND_COLOR = 0xFF17171A.toInt()
    const val APP_BACKGROUND_COLOR = 0xFF0F0F10.toInt()
    const val ICON_TILE_BACKGROUND_COLOR = 0xFF222328.toInt()

    fun getAccentContainerColor(context: Context): Int {
        val accent = context.colorFromAttribute(R.attr.colorPrimary)
        val pureBlack = CommonActivity.isPureBlack(context)
        val base = if (pureBlack) Color.BLACK else 0xFF101012.toInt()
        val composeAccent = androidx.compose.ui.graphics.Color(accent)
        val composeBase = androidx.compose.ui.graphics.Color(base)
        val container = com.lagradost.cloudstream3.ui.liquidglass.computeAccentContainerColor(composeAccent, composeBase)
        val a = (container.alpha * 255.0f + 0.5f).toInt()
        val r = (container.red * 255.0f + 0.5f).toInt()
        val g = (container.green * 255.0f + 0.5f).toInt()
        val b = (container.blue * 255.0f + 0.5f).toInt()
        return Color.argb(a, r, g, b)
    }

    fun getGroupPosition(index: Int, total: Int): GroupPosition {
        return when {
            total <= 1 -> GroupPosition.Single
            index <= 0 -> GroupPosition.First
            index >= total - 1 -> GroupPosition.Last
            else -> GroupPosition.Middle
        }
    }

    fun applyGroupCardStyle(
        view: View,
        position: GroupPosition,
        backgroundColor: Int = getAccentContainerColor(view.context),
        applyMargins: Boolean = true,
        horizontalMarginDp: Float = 16f,
        gapDp: Float = 3f,
    ) {
        val density = view.resources.displayMetrics.density
        val r28 = 28f * density
        val r12 = 12f * density

        if (view is MaterialCardView) {
            val shapeModel = ShapeAppearanceModel.builder().apply {
                when (position) {
                    GroupPosition.Single -> {
                        setAllCornerSizes(r28)
                    }
                    GroupPosition.First -> {
                        setTopLeftCornerSize(r28)
                        setTopRightCornerSize(r28)
                        setBottomLeftCornerSize(r12)
                        setBottomRightCornerSize(r12)
                    }
                    GroupPosition.Middle -> {
                        setAllCornerSizes(r12)
                    }
                    GroupPosition.Last -> {
                        setTopLeftCornerSize(r12)
                        setTopRightCornerSize(r12)
                        setBottomLeftCornerSize(r28)
                        setBottomRightCornerSize(r28)
                    }
                }
            }.build()

            view.shapeAppearanceModel = shapeModel
            view.setCardBackgroundColor(ColorStateList.valueOf(backgroundColor))
            view.rippleColor = ColorStateList.valueOf(0x1FFFFFFF)
            view.cardElevation = 0f
            view.maxCardElevation = 0f
            view.strokeWidth = 0
            view.radius = 0f
        } else {
            val radii = when (position) {
                GroupPosition.Single -> floatArrayOf(r28, r28, r28, r28, r28, r28, r28, r28)
                GroupPosition.First -> floatArrayOf(r28, r28, r28, r28, r12, r12, r12, r12)
                GroupPosition.Middle -> floatArrayOf(r12, r12, r12, r12, r12, r12, r12, r12)
                GroupPosition.Last -> floatArrayOf(r12, r12, r12, r12, r28, r28, r28, r28)
            }
            val contentDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = radii
                setColor(backgroundColor)
            }
            val maskDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = radii
                setColor(Color.WHITE)
            }
            val rippleColor = ColorStateList.valueOf(0x1FFFFFFF)
            val drawable = RippleDrawable(rippleColor, contentDrawable, maskDrawable)
            view.background = drawable
            view.clipToOutline = true
        }

        if (applyMargins) {
            val lp = view.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null) {
                val horizPx = (horizontalMarginDp * density).roundToInt()
                val gapPx = (gapDp * density).roundToInt()
                val targetBottom = if (position == GroupPosition.First || position == GroupPosition.Middle) gapPx else 0

                var changed = false
                if (lp.marginStart != horizPx || lp.marginEnd != horizPx) {
                    lp.marginStart = horizPx
                    lp.marginEnd = horizPx
                    changed = true
                }
                if (lp.bottomMargin != targetBottom) {
                    lp.bottomMargin = targetBottom
                    changed = true
                }
                if (changed) {
                    view.layoutParams = lp
                }
            }
        }
    }
}
