package com.example.moonphase.presentation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import java.time.ZonedDateTime
import android.graphics.Paint
import android.graphics.Color
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

class MyRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int,
    interactiveDrawModeUpdateDelayMillis: Long
) : Renderer.CanvasRenderer2<MyRenderer.SharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    interactiveDrawModeUpdateDelayMillis,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = true
) {

    class SharedAssets : Renderer.SharedAssets {
        override fun onDestroy() {}
    }

    override suspend fun createSharedAssets(): SharedAssets {
        return SharedAssets()
    }

    private val whitePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val fillPaint = Paint().apply {
        color = Color.DKGRAY
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val lightRedPaint = Paint().apply {
        color = Color.parseColor("#FF6666")
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
    }

    private val lightRedFillPaint = Paint().apply {
        color = Color.parseColor("#FF6666")
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val lightGrayPaint = Paint().apply {
        color = Color.LTGRAY
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    private val backgroundPaint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: SharedAssets
    ) {
        // Draw background
        canvas.drawRect(bounds, backgroundPaint)

        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val radius = minOf(width, height) / 2f

        // Draw roman numerals
        val romanNumerals = listOf("XII", "I", "II", "", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI")
        textPaint.textSize = radius * 0.15f

        for (i in 0 until 12) {
            val angle = i * 30f
            val textRadius = radius * 0.80f

            val textBounds = Rect()
            textPaint.getTextBounds(romanNumerals[i], 0, romanNumerals[i].length, textBounds)

            canvas.save()
            canvas.translate(centerX, centerY)
            canvas.rotate(angle)

            canvas.drawText(romanNumerals[i], 0f, -textRadius + textBounds.height() / 2f, textPaint)
            canvas.restore()
        }

        // Draw ticks
        for (i in 0 until 60) {
            val angle = i * 6f
            val isHour = i % 5 == 0
            val tickLength = if (isHour) radius * 0.1f else radius * 0.05f
            whitePaint.strokeWidth = if (isHour) 4f else 2f

            val startX = centerX + (radius - tickLength) * cos(Math.toRadians(angle.toDouble() - 90)).toFloat()
            val startY = centerY + (radius - tickLength) * sin(Math.toRadians(angle.toDouble() - 90)).toFloat()
            val endX = centerX + radius * cos(Math.toRadians(angle.toDouble() - 90)).toFloat()
            val endY = centerY + radius * sin(Math.toRadians(angle.toDouble() - 90)).toFloat()

            canvas.drawLine(startX, startY, endX, endY, whitePaint)
        }

        // Date
        val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")
        val dateText = zonedDateTime.format(dateFormatter)
        textPaint.textSize = 12f * context.resources.displayMetrics.scaledDensity
        val textBounds = Rect()
        textPaint.getTextBounds(dateText, 0, dateText.length, textBounds)

        val paddingHorizontal = 8f * context.resources.displayMetrics.density
        val paddingVertical = 4f * context.resources.displayMetrics.density
        val dateBgLeft = width - 24f * context.resources.displayMetrics.density - textBounds.width() - paddingHorizontal * 2
        val dateBgTop = centerY - textBounds.height() / 2f - paddingVertical
        val dateBgRight = width - 24f * context.resources.displayMetrics.density
        val dateBgBottom = centerY + textBounds.height() / 2f + paddingVertical

        canvas.drawRoundRect(dateBgLeft, dateBgTop, dateBgRight, dateBgBottom, 8f * context.resources.displayMetrics.density, 8f * context.resources.displayMetrics.density, fillPaint)
        canvas.drawText(dateText, dateBgLeft + paddingHorizontal + textBounds.width() / 2f, centerY + textBounds.height() / 2f, textPaint)

        // Moon phase
        val moonPhase = getMoonPhase(zonedDateTime.toLocalDate())
        textPaint.textSize = 14f * context.resources.displayMetrics.scaledDensity
        textPaint.getTextBounds(moonPhase, 0, moonPhase.length, textBounds)

        val moonBgLeft = centerX - textBounds.width() / 2f - paddingHorizontal
        val moonBgBottom = height - 36f * context.resources.displayMetrics.density
        val moonBgTop = moonBgBottom - textBounds.height() - paddingVertical * 2
        val moonBgRight = centerX + textBounds.width() / 2f + paddingHorizontal

        canvas.drawRoundRect(moonBgLeft, moonBgTop, moonBgRight, moonBgBottom, 8f * context.resources.displayMetrics.density, 8f * context.resources.displayMetrics.density, fillPaint)
        canvas.drawText(moonPhase, centerX, moonBgBottom - paddingVertical, textPaint)

        // Hands
        val hours = zonedDateTime.hour
        val minutes = zonedDateTime.minute
        val seconds = zonedDateTime.second
        val millis = zonedDateTime.nano / 1000000

        // Hour hand
        val hourAngle = (hours % 12 + minutes / 60f) * 30f
        val hourEndX = centerX + (radius * 0.5f) * cos(Math.toRadians(hourAngle.toDouble() - 90)).toFloat()
        val hourEndY = centerY + (radius * 0.5f) * sin(Math.toRadians(hourAngle.toDouble() - 90)).toFloat()
        whitePaint.strokeWidth = 8f
        whitePaint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(centerX, centerY, hourEndX, hourEndY, whitePaint)

        // Minute hand
        val minuteAngle = (minutes + seconds / 60f) * 6f
        val minuteEndX = centerX + (radius * 0.7f) * cos(Math.toRadians(minuteAngle.toDouble() - 90)).toFloat()
        val minuteEndY = centerY + (radius * 0.7f) * sin(Math.toRadians(minuteAngle.toDouble() - 90)).toFloat()
        canvas.drawLine(centerX, centerY, minuteEndX, minuteEndY, lightGrayPaint)

        // Second hand
        if (renderParameters.drawMode == androidx.wear.watchface.DrawMode.INTERACTIVE) {
            val secondAngle = (seconds + millis / 1000f) * 6f
            val secondEndX = centerX + (radius * 0.9f) * cos(Math.toRadians(secondAngle.toDouble() - 90)).toFloat()
            val secondEndY = centerY + (radius * 0.9f) * sin(Math.toRadians(secondAngle.toDouble() - 90)).toFloat()
            canvas.drawLine(centerX, centerY, secondEndX, secondEndY, lightRedPaint)

            // Center dot
            canvas.drawCircle(centerX, centerY, 6f, lightRedFillPaint)
        } else {
            canvas.drawCircle(centerX, centerY, 6f, whitePaint)
        }
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: SharedAssets
    ) {}

    private fun getMoonPhase(date: java.time.LocalDate): String {
        var year = date.year
        var month = date.monthValue
        val day = date.dayOfMonth

        if (month < 3) {
            year--
            month += 12
        }
        month++
        val c = 365.25 * year
        val e = 30.6 * month
        val jd = c + e + day - 694039.09 // known new moon epoch
        val phase = jd / 29.5305882
        var normalizedPhase = phase - phase.toLong()
        if (normalizedPhase < 0) {
            normalizedPhase += 1.0
        }

        val index = Math.round(normalizedPhase * 8).toInt() % 8
        val phases = listOf("🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘")
        return if (index >= 0 && index < phases.size) phases[index] else "🌑"
    }
}
