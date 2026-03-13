package com.example.moonphase

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MoonphaseWatchFaceService : WatchFaceService() {

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val renderer = MoonphaseRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            currentUserStyleRepository = currentUserStyleRepository,
            canvasType = 1 // HARDWARE = 1, SOFTWARE = 2
        )
        return WatchFace(WatchFaceType.ANALOG, renderer)
    }
}

class MoonphaseRenderer(
    context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<Renderer.SharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    16L,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = true
) {
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val tickPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val dateBgPaint = Paint().apply {
        color = Color.DKGRAY
        isAntiAlias = true
    }

    private val hourHandPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val minuteHandPaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val secondHandPaint = Paint().apply {
        color = Color.parseColor("#FF6666")
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    override suspend fun createSharedAssets(): SharedAssets {
        return object : SharedAssets {
            override fun onDestroy() {}
        }
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: SharedAssets
    ) {
        canvas.drawColor(Color.BLACK)

        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val radius = min(width, height) / 2f

        // Draw ticks and Roman numerals
        val romanNumerals = listOf("XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI")
        textPaint.textSize = radius * 0.15f

        for (i in 0 until 12) {
            val angle = i * 30f
            val textRadius = radius * 0.85f
            val textX = centerX + textRadius * cos(Math.toRadians(angle.toDouble() - 90)).toFloat()
            val textY = centerY + textRadius * sin(Math.toRadians(angle.toDouble() - 90)).toFloat()

            val textBounds = Rect()
            textPaint.getTextBounds(romanNumerals[i], 0, romanNumerals[i].length, textBounds)
            val adjustedY = textY + textBounds.height() / 2f

            canvas.drawText(romanNumerals[i], textX, adjustedY, textPaint)
        }

        for (i in 0 until 60) {
            if (i % 5 != 0) {
                val angle = i * 6f
                val tickLength = radius * 0.05f
                val startX = centerX + (radius - tickLength) * cos(Math.toRadians(angle.toDouble() - 90)).toFloat()
                val startY = centerY + (radius - tickLength) * sin(Math.toRadians(angle.toDouble() - 90)).toFloat()
                val endX = centerX + radius * cos(Math.toRadians(angle.toDouble() - 90)).toFloat()
                val endY = centerY + radius * sin(Math.toRadians(angle.toDouble() - 90)).toFloat()

                canvas.drawLine(startX, startY, endX, endY, tickPaint)
            }
        }

        val date = zonedDateTime.toLocalDate()
        val time = zonedDateTime.toLocalTime()

        // Draw Date Box
        val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")
        val dateText = date.format(dateFormatter)

        val dateTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 12f * 2.5f // Convert sp approx
            isAntiAlias = true
        }
        val dateBounds = Rect()
        dateTextPaint.getTextBounds(dateText, 0, dateText.length, dateBounds)

        val datePaddingX = 8f * 2.5f
        val datePaddingY = 4f * 2.5f
        val dateRightMargin = 24f * 2.5f

        val dateBoxRight = width - dateRightMargin
        val dateBoxLeft = dateBoxRight - dateBounds.width() - datePaddingX * 2
        val dateBoxTop = centerY - dateBounds.height() / 2f - datePaddingY
        val dateBoxBottom = centerY + dateBounds.height() / 2f + datePaddingY

        canvas.drawRoundRect(dateBoxLeft, dateBoxTop, dateBoxRight, dateBoxBottom, 8f * 2.5f, 8f * 2.5f, dateBgPaint)
        canvas.drawText(dateText, dateBoxLeft + datePaddingX, centerY + dateBounds.height() / 2f, dateTextPaint)

        // Draw Moonphase Box
        val moonPhaseText = getMoonPhase(date)
        val moonPhaseTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 14f * 2.5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val moonPhaseBounds = Rect()
        moonPhaseTextPaint.getTextBounds(moonPhaseText, 0, moonPhaseText.length, moonPhaseBounds)

        val moonBottomMargin = 36f * 2.5f
        val moonBoxBottom = height - moonBottomMargin
        val moonBoxTop = moonBoxBottom - moonPhaseBounds.height() - datePaddingY * 2
        val moonBoxLeft = centerX - moonPhaseBounds.width() / 2f - datePaddingX
        val moonBoxRight = centerX + moonPhaseBounds.width() / 2f + datePaddingX

        canvas.drawRoundRect(moonBoxLeft, moonBoxTop, moonBoxRight, moonBoxBottom, 8f * 2.5f, 8f * 2.5f, dateBgPaint)
        canvas.drawText(moonPhaseText, centerX, moonBoxBottom - datePaddingY - moonPhaseBounds.bottom, moonPhaseTextPaint)


        // Draw Hands
        val hours = time.hour
        val minutes = time.minute
        val seconds = time.second

        // Hour hand
        val hourAngle = (hours % 12 + minutes / 60f) * 30f
        val hourEndX = centerX + (radius * 0.5f) * cos(Math.toRadians(hourAngle.toDouble() - 90)).toFloat()
        val hourEndY = centerY + (radius * 0.5f) * sin(Math.toRadians(hourAngle.toDouble() - 90)).toFloat()
        canvas.drawLine(centerX, centerY, hourEndX, hourEndY, hourHandPaint)

        // Minute hand
        val minuteAngle = (minutes + seconds / 60f) * 6f
        val minuteEndX = centerX + (radius * 0.7f) * cos(Math.toRadians(minuteAngle.toDouble() - 90)).toFloat()
        val minuteEndY = centerY + (radius * 0.7f) * sin(Math.toRadians(minuteAngle.toDouble() - 90)).toFloat()
        canvas.drawLine(centerX, centerY, minuteEndX, minuteEndY, minuteHandPaint)

        // Second hand
        val secondAngle = seconds * 6f
        val secondEndX = centerX + (radius * 0.9f) * cos(Math.toRadians(secondAngle.toDouble() - 90)).toFloat()
        val secondEndY = centerY + (radius * 0.9f) * sin(Math.toRadians(secondAngle.toDouble() - 90)).toFloat()
        canvas.drawLine(centerX, centerY, secondEndX, secondEndY, secondHandPaint)

        // Center dot
        canvas.drawCircle(centerX, centerY, 6f, secondHandPaint)
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: SharedAssets
    ) {
    }

    private fun getMoonPhase(date: LocalDate): String {
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
