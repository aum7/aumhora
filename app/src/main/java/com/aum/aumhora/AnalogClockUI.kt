package com.aum.aumhora

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogClockAum(
    modifier: Modifier = Modifier,
    time: Calendar,
    // default clock colors
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    hourHandColor: Color = MaterialTheme.colorScheme.primary,
    minuteHandColor: Color = MaterialTheme.colorScheme.onSurface,
    secondHandColor: Color = MaterialTheme.colorScheme.error,
    dialColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    centerDotColor: Color = backgroundColor,
    nextSubHoraTime: Calendar?,
    secondsBeforeNotification: Int = 59,
    notificationMarkerColor: Color = Color.Red
) {
    Canvas(modifier = modifier) {
        Log.d("aclockaum", "canvas size w x h : ${size.width} x ${size.height}")
        Log.d("aclockaum", "canvas min dimension : ${size.minDimension}")
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val clockRadius = size.minDimension / 2f
        // clock background
//        drawCircle(
//            color = backgroundColor,
//            radius = clockRadius,
//            center = Offset(centerX, centerY)
//        )
        // clock outer circle
//        drawCircle(
//            color = dialColor,
//            radius = clockRadius,
//            center = Offset(centerX, centerY),
//            style = androidx.compose.ui.graphics.drawscope.Stroke(
//                width = 0.dp.toPx()
//            )
//        )
        // clock dial ticks
        for (i in 0 until 60) {
            val angle = Math.toRadians((i * 6 - 90).toDouble())
            val isHourTick = i % 5 == 0
            val tickStart = if (isHourTick) clockRadius * 0.82f else clockRadius * 0.88f
            val tickEnd = clockRadius * 0.95f
            val tickColorToUse = if (isHourTick) dialColor else dialColor.copy(alpha = 0.8f)
            val strokeWidthToUse = if (isHourTick) 2.dp.toPx() else 1.dp.toPx()
            drawLine(
                color = tickColorToUse,
                start = Offset(
                    centerX + tickStart * cos(angle).toFloat(),
                    centerY + tickStart * sin(angle).toFloat()
                ),
                end = Offset(
                    centerX + tickEnd * cos(angle).toFloat(),
                    centerY + tickEnd * sin(angle).toFloat()
                ),
                strokeWidth = strokeWidthToUse,
                cap = StrokeCap.Round
            )
        }
        // clock hands
        val displayHours = time.get(Calendar.HOUR)
        val displayMinutes = time.get(Calendar.MINUTE)
        val displaySeconds = time.get(Calendar.SECOND)
        // draw hour hand
        drawHand(
            angle = (
                    displayHours + displayMinutes / 60f +
                            displaySeconds / 3600f) * 30f - 90f,
            length = clockRadius * 0.6f,
            color = hourHandColor,
            strokeWidth = 10.dp.toPx()
        )
        // draw minute hand
        drawHand(
            angle = (displayMinutes + displaySeconds / 60f) * 6f - 90f,
            length = clockRadius * 0.75f,
            color = minuteHandColor,
            strokeWidth = 6.dp.toPx()
        )
        // draw second hand
        if (secondHandColor != Color.Transparent) {
            drawHand(
                angle = displaySeconds * 6f - 90f,
                length = clockRadius * 0.85f,
                color = secondHandColor,
                strokeWidth = 4.dp.toPx()
            )
        }
        // upcoming subhora marker
        nextSubHoraTime?.let { nextChangeCal ->
            val nowMillis = time.timeInMillis
            val nextChangeStartMillis = nextChangeCal.timeInMillis
            val notifyFromMillis = nextChangeStartMillis - (
                    secondsBeforeNotification * 1000L)

            if (nowMillis >= notifyFromMillis && nowMillis < nextChangeStartMillis) {
                // determine marker position
                val changeSecond = nextChangeCal.get(Calendar.SECOND)

                val markerAngleDegrees = (changeSecond * 6f) - 90f
                val markerAngleRadians = Math.toRadians(
                    markerAngleDegrees.toDouble()
                )

                val markerVisualRadius = clockRadius * 0.93f
                val markerX = centerX + markerVisualRadius * cos(
                    markerAngleRadians
                ).toFloat()
                val markerY = centerY + markerVisualRadius * sin(
                    markerAngleRadians
                ).toFloat()

                drawCircle(
                    color = notificationMarkerColor,
                    radius = 7.dp.toPx(),
                    center = Offset(markerX, markerY)
                )
            }
        }
        // center dot
//        drawCircle(
//            color = minuteHandColor.copy(alpha = 0.5f),
//            radius = 5.dp.toPx(),
//            center = Offset(centerX, centerY)
//        )
        drawCircle(
            color = centerDotColor,
            radius = 12.dp.toPx(),
            center = Offset(centerX, centerY)
        )
    }
}

// drawhand func
fun DrawScope.drawHand(
    angle: Float,
    length: Float,
    color: Color,
    strokeWidth: Float,
    strokeCap: StrokeCap = StrokeCap.Round
) {
    val angleInRadians = Math.toRadians(angle.toDouble())
    val startX = center.x
    val startY = center.y

    val endX = center.x + length * cos(angleInRadians).toFloat()
    val endY = center.y + length * sin(angleInRadians).toFloat()

    drawLine(
        color = color,
        start = Offset(startX, startY),
        end = Offset(endX, endY),
        strokeWidth = strokeWidth,
        cap = strokeCap
    )
}
