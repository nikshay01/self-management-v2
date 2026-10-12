package com.selfmanagement.samsungbridge.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.selfmanagement.samsungbridge.R
import java.util.Locale
import kotlin.math.max

class DashboardChartView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    enum class ChartType { BAR, LINE, DONUT, HORIZONTAL_BAR }
    data class Entry(val label: String, val value: Double, val color: Int? = null)
    private val points = mutableListOf<Entry>()
    private var type = ChartType.BAR
    private var color = context.getColor(R.color.accent_teal)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labels = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.text_secondary); textSize = dp(10f); textAlign = Paint.Align.CENTER }
    private val bounds = RectF()

    fun setData(data: List<Entry>, chartType: ChartType, chartColor: Int = context.getColor(R.color.accent_teal)) {
        points.clear(); points.addAll(data.filter { it.value.isFinite() && it.value >= 0 })
        type = chartType; color = chartColor
        contentDescription = if (points.isEmpty()) "No chart data" else points.joinToString { "${it.label}: ${it.value}" }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) { labels.textSize = dp(12f); canvas.drawText("Sync data to see this chart", width / 2f, height / 2f, labels); labels.textSize = dp(10f); return }
        val left = dp(8f); val right = width - dp(8f); val top = dp(8f)
        val bottom = height - dp(if (type == ChartType.BAR || type == ChartType.LINE) 28f else 8f)
        when (type) {
            ChartType.BAR -> drawBars(canvas, left, top, right, bottom)
            ChartType.LINE -> drawLine(canvas, left, top, right, bottom)
            ChartType.DONUT -> drawDonut(canvas)
            ChartType.HORIZONTAL_BAR -> drawHorizontal(canvas, left, top, right, bottom)
        }
    }

    private fun labelPositions(size: Int): Set<Int> = if (size <= 8) (0 until size).toSet() else setOf(0, size / 4, size / 2, size * 3 / 4, size - 1)

    private fun grid(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        fill.color = context.getColor(R.color.card_stroke)
        for (i in 0..3) { val y = t + (b - t) * i / 3f; canvas.drawRect(l, y, r, y + dp(1f), fill) }
    }

    private fun drawBars(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        grid(canvas, l, t, r, b); val maxValue = max(1.0, points.maxOf { it.value }); val slot = (r - l) / points.size
        val barWidth = minOf(dp(26f), slot * .58f)
        val labelPositions = labelPositions(points.size)
        points.forEachIndexed { i, p ->
            val x = l + slot * (i + .5f); val y = b - (b - t) * (p.value / maxValue).toFloat()
            fill.color = p.color ?: color; bounds.set(x - barWidth / 2, y, x + barWidth / 2, b); canvas.drawRoundRect(bounds, dp(5f), dp(5f), fill)
            if (i in labelPositions) canvas.drawText(p.label.takeLast(5), x, height - dp(7f), labels)
        }
    }

    private fun drawLine(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        grid(canvas, l, t, r, b); val minValue = points.minOf { it.value }; val span = max(1.0, points.maxOf { it.value } - minValue)
        val step = if (points.size == 1) 0f else (r - l) / (points.size - 1); val path = Path(); val labelPositions = labelPositions(points.size)
        points.forEachIndexed { i, p ->
            val x = l + step * i; val y = b - (((p.value - minValue) / span) * (b - t)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            fill.color = color; canvas.drawCircle(x, y, dp(3f), fill)
            if (i in labelPositions) canvas.drawText(p.label.takeLast(5), x, height - dp(7f), labels)
        }
        line.color = color; line.strokeWidth = dp(2.5f); canvas.drawPath(path, line)
    }

    private fun drawHorizontal(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val rows = points.take(5); val maxValue = max(1.0, rows.maxOf { it.value }); val rowHeight = (b - t) / rows.size
        labels.textAlign = Paint.Align.LEFT
        rows.forEachIndexed { i, p ->
            val y = t + rowHeight * i; canvas.drawText(p.label.take(18), l, y + dp(12f), labels)
            val start = l + dp(2f); val end = r - dp(42f)
            bounds.set(start, y + dp(19f), start + (end - start) * (p.value / maxValue).toFloat(), y + dp(30f))
            fill.color = p.color ?: color; canvas.drawRoundRect(bounds, dp(5f), dp(5f), fill)
            labels.textAlign = Paint.Align.RIGHT; canvas.drawText(short(p.value), r, y + dp(28f), labels); labels.textAlign = Paint.Align.LEFT
        }
        labels.textAlign = Paint.Align.CENTER
    }

    private fun drawDonut(canvas: Canvas) {
        val total = points.sumOf { it.value }; if (total <= 0) return
        val size = minOf(width * .62f, height - dp(38f)); val cx = width / 2f; val cy = (height - dp(26f)) / 2f
        bounds.set(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2)
        line.style = Paint.Style.STROKE; line.strokeWidth = dp(22f); var start = -90f
        points.forEach { p -> val sweep = (p.value / total * 360).toFloat(); line.color = p.color ?: color; canvas.drawArc(bounds, start, sweep, false, line); start += sweep }
        labels.color = context.getColor(R.color.text_primary); labels.textSize = dp(16f); labels.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText(short(total), cx, cy + dp(5f), labels)
        labels.color = context.getColor(R.color.text_secondary); labels.textSize = dp(9f); labels.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawText("minutes", cx, cy + dp(20f), labels)
        var x = dp(4f); labels.textAlign = Paint.Align.LEFT
        points.forEach { p -> fill.color = p.color ?: color; canvas.drawCircle(x + dp(3f), height - dp(12f), dp(3f), fill); x += dp(10f); val name = p.label.take(8); canvas.drawText(name, x, height - dp(9f), labels); x += labels.measureText(name) + dp(12f) }
        labels.textAlign = Paint.Align.CENTER
    }

    private fun short(v: Double) = when { v >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fm", v / 1_000_000); v >= 10_000 -> String.format(Locale.getDefault(), "%.0fk", v / 1_000); v >= 1_000 -> String.format(Locale.getDefault(), "%.1fk", v / 1_000); else -> String.format(Locale.getDefault(), "%.0f", v) }
    private fun dp(v: Float) = v * resources.displayMetrics.density
}
