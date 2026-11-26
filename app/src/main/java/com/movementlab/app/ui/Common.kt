package com.movementlab.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movementlab.app.data.Tracking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

val Orange = Color(0xFFFF7A1A)
val Green = Color(0xFF4CC38A)

val AppColors = darkColorScheme(
    primary = Orange,
    onPrimary = Color(0xFF1B1B1B),
    primaryContainer = Color(0xFF4A2A12),
    onPrimaryContainer = Color(0xFFFFDBC7),
    secondary = Green,
    secondaryContainer = Color(0xFF1F3A2D),
    onSecondaryContainer = Color(0xFFBFF0D6),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF242424),
    surfaceContainer = Color(0xFF1D1D1D),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF2E2E2E),
)

const val KG_PER_LB = 0.45359237

fun kgToDisplay(kg: Double, useLb: Boolean): Double = if (useLb) kg / KG_PER_LB else kg
fun displayToKg(v: Double, useLb: Boolean): Double = if (useLb) v * KG_PER_LB else v

fun fmtNum(v: Double): String {
    val r = (v * 10).roundToInt() / 10.0
    return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
}

/** "BW", "+10 kg", "−15 lb (assist)". */
fun fmtWeight(kg: Double, useLb: Boolean): String {
    if (abs(kg) < 0.01) return "BW"
    val unit = if (useLb) "lb" else "kg"
    val v = kgToDisplay(kg, useLb)
    return if (v > 0) "+${fmtNum(v)} $unit" else "−${fmtNum(-v)} $unit assist"
}

fun fmtValue(v: Int, tracking: Tracking): String = if (tracking == Tracking.SECONDS) "${v}s" else "$v"

fun fmtTarget(sets: Int, target: Int, tracking: Tracking): String =
    if (tracking == Tracking.SECONDS) "$sets × ${target}s" else "$sets × $target"

fun fmtDate(millis: Long): String = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(millis))
fun fmtDateShort(millis: Long): String = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))

fun fmtDuration(sec: Int): String {
    val m = sec / 60
    return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
}

fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        Toast.makeText(context, "Couldn't open the video link", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

/** Big −/+ control, easy to hit mid-set. */
@Composable
fun Stepper(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = onMinus, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp)) {
                Text("−", fontSize = 28.sp)
            }
            Text(
                valueText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp).widthIn(min = 96.dp),
            )
            FilledTonalButton(onClick = onPlus, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp)) {
                Text("+", fontSize = 28.sp)
            }
        }
    }
}

/** Minimal line chart; draws dots for each point plus a connecting line. */
@Composable
fun LineChart(values: List<Float>, modifier: Modifier = Modifier, color: Color = Orange) {
    val grid = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier.fillMaxWidth().height(160.dp)) {
        if (values.isEmpty()) return@Canvas
        val maxV = values.max()
        val minV = values.min()
        val range = (maxV - minV).takeIf { it > 0f } ?: 1f
        val padY = 12.dp.toPx()
        val h = size.height - padY * 2
        for (i in 0..3) {
            val y = padY + h * i / 3f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        val step = if (values.size > 1) size.width / (values.size - 1) else 0f
        fun pt(i: Int) = Offset(
            if (values.size > 1) i * step else size.width / 2,
            padY + h - (values[i] - minV) / range * h,
        )
        if (values.size > 1) {
            val path = Path().apply {
                moveTo(pt(0).x, pt(0).y)
                for (i in 1 until values.size) lineTo(pt(i).x, pt(i).y)
            }
            drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
        }
        values.indices.forEach { drawCircle(color, radius = 4.dp.toPx(), center = pt(it)) }
    }
}

@Composable
fun StatBlock(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
