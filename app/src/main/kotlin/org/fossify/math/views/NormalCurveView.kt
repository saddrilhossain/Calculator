package org.fossify.math.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import org.fossify.math.glassbox.NormalMath
import org.fossify.math.glassbox.NormalQuestion
import org.fossify.math.glassbox.RegionType
import java.util.Locale

/**
 * Draws the normal curve for a question, shades the region the student asked for,
 * and (optionally) shows where the student's own z-value lands.
 * It never prints the probability — the picture is the feedback.
 */
class NormalCurveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var question: NormalQuestion? = null
    private var studentZ: Double? = null

    private val density = resources.displayMetrics.density

    private val curvePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3 * density
    }
    private val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1.5f * density }
    private val meanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f * density
        pathEffect = DashPathEffect(floatArrayOf(8 * density, 6 * density), 0f)
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 152, 0) // orange: "this is YOUR z"
        strokeWidth = 3 * density
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12 * density; textAlign = Paint.Align.CENTER
    }
    private val markerTextPaint = Paint(textPaint).apply { color = Color.rgb(255, 152, 0) }

    fun setColors(textColor: Int, primaryColor: Int) {
        curvePaint.color = textColor
        axisPaint.color = textColor
        meanPaint.color = textColor
        textPaint.color = textColor
        shadePaint.color = primaryColor
        shadePaint.alpha = 190
        invalidate()
    }

    fun showQuestion(q: NormalQuestion) {
        question = q
        studentZ = null
        invalidate()
    }

    fun showStudentZ(z: Double?) {
        studentZ = z
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, (w * 0.6f).toInt()) // keep a nice 5:3 shape
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val q = question ?: run {
            textPaint.let { canvas.drawText("Enter a question and tap 'Show region'", width / 2f, height / 2f, it) }
            return
        }

        val left = 12 * density
        val right = width - 12 * density
        val baseline = height - 34 * density
        val top = 34 * density

        // We draw from μ − 4σ to μ + 4σ (the curve is basically zero beyond that)
        val xMin = q.mean - 4 * q.sd
        val xMax = q.mean + 4 * q.sd
        val peak = NormalMath.pdf(0.0)

        fun px(x: Double) = (left + (x - xMin) / (xMax - xMin) * (right - left)).toFloat()
        fun py(x: Double) = (baseline - NormalMath.pdf((x - q.mean) / q.sd) / peak * (baseline - top)).toFloat()

        // 1. Shaded region
        val (from, to) = when (q.type) {
            RegionType.LESS_THAN -> xMin to q.a.coerceIn(xMin, xMax)
            RegionType.GREATER_THAN -> q.a.coerceIn(xMin, xMax) to xMax
            RegionType.BETWEEN -> q.a.coerceIn(xMin, xMax) to q.b.coerceIn(xMin, xMax)
        }
        if (to > from) {
            val shade = Path()
            shade.moveTo(px(from), baseline)
            val steps = 200
            for (i in 0..steps) {
                val x = from + (to - from) * i / steps
                shade.lineTo(px(x), py(x))
            }
            shade.lineTo(px(to), baseline)
            shade.close()
            canvas.drawPath(shade, shadePaint)
        }

        // 2. The curve
        val curve = Path()
        val steps = 400
        for (i in 0..steps) {
            val x = xMin + (xMax - xMin) * i / steps
            if (i == 0) curve.moveTo(px(x), py(x)) else curve.lineTo(px(x), py(x))
        }
        canvas.drawPath(curve, curvePaint)

        // 3. Axis, mean line and boundary labels
        canvas.drawLine(left, baseline, right, baseline, axisPaint)
        canvas.drawLine(px(q.mean), baseline, px(q.mean), top, meanPaint)
        canvas.drawText("μ = ${fmt(q.mean)}", px(q.mean), top - 12 * density, textPaint)

        drawBoundary(canvas, px(q.a), baseline, "a = ${fmt(q.a)}")
        if (q.type == RegionType.BETWEEN) drawBoundary(canvas, px(q.b), baseline, "b = ${fmt(q.b)}")

        // 4. The student's z, converted back to an x-value so they can SEE where it lands
        studentZ?.let { z ->
            val x = (q.mean + z * q.sd).coerceIn(xMin, xMax)
            val mx = px(x)
            canvas.drawLine(mx, baseline, mx, top + 10 * density, markerPaint)
            // Put the label beside the line, halfway up, so it never covers the μ label
            val labelY = baseline - (baseline - top) * 0.55f
            val onRight = mx < width * 0.6f
            markerTextPaint.textAlign = if (onRight) Paint.Align.LEFT else Paint.Align.RIGHT
            val labelX = if (onRight) mx + 6 * density else mx - 6 * density
            canvas.drawText("your z = ${String.format(Locale.US, "%.3g", z)}", labelX, labelY, markerTextPaint)
        }
    }

    private fun drawBoundary(canvas: Canvas, x: Float, baseline: Float, label: String) {
        canvas.drawLine(x, baseline, x, baseline + 6 * density, axisPaint)
        canvas.drawText(label, x, baseline + 20 * density, textPaint)
    }

    private fun fmt(v: Double): String =
        if (v == Math.floor(v)) v.toLong().toString() else String.format(Locale.US, "%.3g", v)
}
