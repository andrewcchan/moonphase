package com.example.moonphase.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.moonphase.presentation.theme.MoonphaseTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    MoonphaseTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            WatchFace()
        }
    }
}

@Composable
fun WatchFace() {
    var time by remember { mutableStateOf(LocalTime.now()) }
    var date by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            time = LocalTime.now()
            date = LocalDate.now()
            delay(1000)
        }
    }

    val moonPhase = getMoonPhase(date)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = min(size.width, size.height) / 2

            // Draw ticks
            for (i in 0 until 60) {
                val angle = i * 6f
                val isHour = i % 5 == 0
                val tickLength = if (isHour) radius * 0.1f else radius * 0.05f
                val tickStroke = if (isHour) 4f else 2f
                val startX = center.x + (radius - tickLength) * cos(Math.toRadians(angle.toDouble() - 90)).toFloat()
                val startY = center.y + (radius - tickLength) * sin(Math.toRadians(angle.toDouble() - 90)).toFloat()
                val endX = center.x + radius * cos(Math.toRadians(angle.toDouble() - 90)).toFloat()
                val endY = center.y + radius * sin(Math.toRadians(angle.toDouble() - 90)).toFloat()

                drawLine(
                    color = Color.White,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = tickStroke
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
                .background(Color.DarkGray, shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = date.format(dateFormatter),
                color = Color.White,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .background(Color.DarkGray, shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = moonPhase,
                color = Color.White,
                fontSize = 14.sp
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = min(size.width, size.height) / 2

            val hours = time.hour
            val minutes = time.minute
            val seconds = time.second

            // Hour hand
            val hourAngle = (hours % 12 + minutes / 60f) * 30f
            val hourEndX = center.x + (radius * 0.5f) * cos(Math.toRadians(hourAngle.toDouble() - 90)).toFloat()
            val hourEndY = center.y + (radius * 0.5f) * sin(Math.toRadians(hourAngle.toDouble() - 90)).toFloat()
            drawLine(
                color = Color.White,
                start = center,
                end = Offset(hourEndX, hourEndY),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )

            // Minute hand
            val minuteAngle = (minutes + seconds / 60f) * 6f
            val minuteEndX = center.x + (radius * 0.7f) * cos(Math.toRadians(minuteAngle.toDouble() - 90)).toFloat()
            val minuteEndY = center.y + (radius * 0.7f) * sin(Math.toRadians(minuteAngle.toDouble() - 90)).toFloat()
            drawLine(
                color = Color.LightGray,
                start = center,
                end = Offset(minuteEndX, minuteEndY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            // Second hand
            val secondAngle = seconds * 6f
            val secondEndX = center.x + (radius * 0.9f) * cos(Math.toRadians(secondAngle.toDouble() - 90)).toFloat()
            val secondEndY = center.y + (radius * 0.9f) * sin(Math.toRadians(secondAngle.toDouble() - 90)).toFloat()
            drawLine(
                color = Color.Blue,
                start = center,
                end = Offset(secondEndX, secondEndY),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )

            // Center dot
            drawCircle(color = Color.Blue, radius = 6f, center = center)
        }
    }
}

fun getMoonPhase(date: LocalDate): String {
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

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}
