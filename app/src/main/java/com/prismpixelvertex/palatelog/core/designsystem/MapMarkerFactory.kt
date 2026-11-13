package com.prismpixelvertex.palatelog.core.designsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.prismpixelvertex.palatelog.R
import com.prismpixelvertex.palatelog.core.model.RestaurantType
import com.prismpixelvertex.palatelog.core.model.Verdict
import java.util.concurrent.ConcurrentHashMap

object MapMarkerFactory {

    private val markerCache = ConcurrentHashMap<String, Drawable>()

    fun getRestaurantMarker(
        context: Context,
        verdict: Verdict,
        type: RestaurantType
    ): Drawable {
        val cacheKey = "${verdict.name}_${type.name}"
        return markerCache.getOrPut(cacheKey) {
            createRestaurantMarkerBitmap(context, verdict, type)
        }
    }

    fun getUserLocationMarker(context: Context): Drawable {
        return markerCache.getOrPut("USER_LOCATION") {
            createUserLocationMarkerBitmap(context)
        }
    }

    fun getCustomLocationMarker(context: Context): Drawable {
        return markerCache.getOrPut("CUSTOM_LOCATION") {
            createCustomLocationMarkerBitmap(context)
        }
    }

    fun getPoiRestaurantMarker(context: Context, shortLabel: String? = null): Drawable {
        val cacheKey = if (shortLabel.isNullOrBlank()) "POI_PLAIN" else "POI_${shortLabel.trim()}"
        return markerCache.getOrPut(cacheKey) {
            createPoiRestaurantMarkerBitmap(context, shortLabel)
        }
    }

    private fun createRestaurantMarkerBitmap(
        context: Context,
        verdict: Verdict,
        type: RestaurantType
    ): Drawable {
        val width = 100
        val height = 120
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        val pinColor = when (verdict) {
            Verdict.GO -> "#10B981".toColorInt() // Emerald Green
            Verdict.NO_GO -> "#EF4444".toColorInt() // Crimson Red
            Verdict.CAUTION -> "#F59E0B".toColorInt() // Amber Yellow
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinColor
            style = Paint.Style.FILL
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        val centerX = width / 2f
        val circleRadius = 38f
        val circleCenterY = 44f
        val bottomTipY = 114f

        val pinPath = Path().apply {
            addCircle(centerX, circleCenterY, circleRadius, Path.Direction.CW)
            moveTo(centerX - circleRadius + 6f, circleCenterY + circleRadius - 10f)
            lineTo(centerX, bottomTipY)
            lineTo(centerX + circleRadius - 6f, circleCenterY + circleRadius - 10f)
            close()
        }

        canvas.drawPath(pinPath, paint)
        canvas.drawPath(pinPath, strokePaint)

        val iconRes = when (type) {
            RestaurantType.DINE_IN -> R.drawable.ic_marker_restaurant
            RestaurantType.DHABA -> R.drawable.ic_marker_dhaba
            RestaurantType.CLOUD_KITCHEN -> R.drawable.ic_marker_delivery
            RestaurantType.CAFE -> R.drawable.ic_marker_cafe
            RestaurantType.DRIVE_THRU -> R.drawable.ic_marker_takeaway
            RestaurantType.STREET_FOOD -> R.drawable.ic_marker_restaurant
        }

        val iconDrawable = ContextCompat.getDrawable(context, iconRes)
        if (iconDrawable != null) {
            val iconSize = 44
            val left = (centerX - iconSize / 2).toInt()
            val top = (circleCenterY - iconSize / 2).toInt()
            iconDrawable.setBounds(left, top, left + iconSize, top + iconSize)
            iconDrawable.setTint(Color.WHITE)
            iconDrawable.draw(canvas)
        }

        // Draw Star Badge on the top-right shoulder to unmistakably signify "My Saved"
        val badgeRadius = 14f
        val badgeCenterX = centerX + circleRadius - 8f
        val badgeCenterY = circleCenterY - circleRadius + 8f

        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val badgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#CBD5E1".toColorInt()
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#F59E0B".toColorInt() // Gold
            style = Paint.Style.FILL
        }

        canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, badgeBgPaint)
        canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, badgeStrokePaint)
        drawStar(canvas, badgeCenterX, badgeCenterY, outerRadius = 8f, innerRadius = 3.6f, starPaint)

        return bitmap.toDrawable(context.resources)
    }

    private fun createUserLocationMarkerBitmap(context: Context): Drawable {
        val size = 80
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val center = size / 2f

        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#333B82F6".toColorInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, 36f, outerPaint)

        val middlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#2563EB".toColorInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, 22f, middlePaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(center, center, 22f, borderPaint)

        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, 8f, innerPaint)

        return bitmap.toDrawable(context.resources)
    }

    private fun createCustomLocationMarkerBitmap(context: Context): Drawable {
        val width = 90
        val height = 110
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        val centerX = width / 2f
        val circleRadius = 34f
        val circleCenterY = 40f
        val bottomTipY = 104f

        val pinPath = Path().apply {
            addCircle(centerX, circleCenterY, circleRadius, Path.Direction.CW)
            moveTo(centerX - circleRadius + 4f, circleCenterY + circleRadius - 8f)
            lineTo(centerX, bottomTipY)
            lineTo(centerX + circleRadius - 4f, circleCenterY + circleRadius - 8f)
            close()
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#F59E0B".toColorInt()
            style = Paint.Style.FILL
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        canvas.drawPath(pinPath, fillPaint)
        canvas.drawPath(pinPath, strokePaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, circleCenterY, 12f, dotPaint)

        val innerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#F59E0B".toColorInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, circleCenterY, 6f, innerDotPaint)

        return bitmap.toDrawable(context.resources)
    }

    private fun createPoiRestaurantMarkerBitmap(
        context: Context,
        shortLabel: String? = null
    ): Drawable {
        if (shortLabel.isNullOrBlank()) {
            val width = 72
            val height = 86
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)

            val centerX = width / 2f
            val circleRadius = 26f
            val circleCenterY = 32f
            val bottomTipY = 82f

            val pinPath = Path().apply {
                addCircle(centerX, circleCenterY, circleRadius, Path.Direction.CW)
                moveTo(centerX - 10f, circleCenterY + circleRadius - 3f)
                lineTo(centerX, bottomTipY)
                lineTo(centerX + 10f, circleCenterY + circleRadius - 3f)
                close()
            }

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = "#0284C7".toColorInt() // Sky Blue
                style = Paint.Style.FILL
            }

            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }

            canvas.drawPath(pinPath, fillPaint)
            canvas.drawPath(pinPath, strokePaint)

            val iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_marker_restaurant)
            if (iconDrawable != null) {
                val iconSize = 30
                val left = (centerX - iconSize / 2).toInt()
                val top = (circleCenterY - iconSize / 2).toInt()
                iconDrawable.setBounds(left, top, left + iconSize, top + iconSize)
                iconDrawable.setTint(Color.WHITE)
                iconDrawable.draw(canvas)
            }

            return bitmap.toDrawable(context.resources)
        }

        // With Short Name Label Pill floating directly above the Sky Blue circular pin
        val cleanLabel = shortLabel.trim()
        val truncated = if (cleanLabel.length > 12) cleanLabel.take(11).trimEnd() + "…" else cleanLabel

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#0F172A".toColorInt() // Slate 900
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val textWidth = textPaint.measureText(truncated)
        val pillPaddingH = 10f
        val pillWidth = maxOf(textWidth + pillPaddingH * 2f, 60f)
        val pillHeight = 28f

        val circleRadius = 25f
        val pointerHeight = 10f
        val gap = 3f

        val totalHeight = (pillHeight + gap + (circleRadius * 2f) + pointerHeight + 4f).toInt()
        val totalWidth = maxOf(pillWidth + 10f, 72f).toInt()

        val bitmap = createBitmap(totalWidth, totalHeight)
        val canvas = Canvas(bitmap)

        val centerX = totalWidth / 2f

        // 1. Draw Name Pill at Top
        val pillLeft = centerX - pillWidth / 2f
        val pillTop = 2f
        val pillRight = pillLeft + pillWidth
        val pillBottom = pillTop + pillHeight
        val pillRect = RectF(pillLeft, pillTop, pillRight, pillBottom)

        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val pillStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#CBD5E1".toColorInt() // Slate 300
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }

        canvas.drawRoundRect(pillRect, 7f, 7f, pillBgPaint)
        canvas.drawRoundRect(pillRect, 7f, 7f, pillStrokePaint)

        val textY = pillTop + (pillHeight / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(truncated, centerX, textY, textPaint)

        // 2. Draw Circular Sky Blue Pin below pill
        val circleCenterY = pillBottom + gap + circleRadius
        val bottomTipY = totalHeight - 2f

        val pinPath = Path().apply {
            addCircle(centerX, circleCenterY, circleRadius, Path.Direction.CW)
            moveTo(centerX - 9f, circleCenterY + circleRadius - 2f)
            lineTo(centerX, bottomTipY)
            lineTo(centerX + 9f, circleCenterY + circleRadius - 2f)
            close()
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#0284C7".toColorInt() // Sky Blue
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        canvas.drawPath(pinPath, fillPaint)
        canvas.drawPath(pinPath, strokePaint)

        // 3. Draw Fork-and-Knife Icon inside Circle
        val iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_marker_restaurant)
        if (iconDrawable != null) {
            val iconSize = 28
            val left = (centerX - iconSize / 2).toInt()
            val top = (circleCenterY - iconSize / 2).toInt()
            iconDrawable.setBounds(left, top, left + iconSize, top + iconSize)
            iconDrawable.setTint(Color.WHITE)
            iconDrawable.draw(canvas)
        }

        return bitmap.toDrawable(context.resources)
    }

    private fun drawStar(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        outerRadius: Float,
        innerRadius: Float,
        paint: Paint
    ) {
        val path = Path()
        val angle = (Math.PI / 5).toFloat()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val currentAngle = (i * angle) - (Math.PI / 2).toFloat()
            val x = cx + r * kotlin.math.cos(currentAngle)
            val y = cy + r * kotlin.math.sin(currentAngle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }
}
