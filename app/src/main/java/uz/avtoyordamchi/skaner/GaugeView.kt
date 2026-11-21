package uz.avtoyordamchi.skaner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.view.View

/** Aylana o'lchagich: 270° yoy, markazda katta raqam va izoh. */
class GaugeView(context: Context) : View(context) {

    var qiymat: Float = 0f          // 0..1
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }
    var matn: String = "—"
        set(v) { field = v; invalidate() }
    var izoh: String = ""
        set(v) { field = v; invalidate() }
    var rang: Int = Color.parseColor("#3D8BFF")
        set(v) { field = v; invalidate() }

    private val d = resources.displayMetrics.density
    private val oval = RectF()

    private val trek = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#1E2A47")
    }
    private val yoy = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val nur = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val chiziq = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A3758")
        strokeWidth = 2f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
    }
    private val katta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
    private val kichik = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B98B8")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        letterSpacing = 0.08f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val s = minOf(w, (240 * d).toInt())
        setMeasuredDimension(w, s)
    }

    override fun onDraw(canvas: Canvas) {
        val s = minOf(width, height).toFloat()
        val qalin = s * 0.075f
        val cx = width / 2f
        val cy = height / 2f
        val r = s / 2f - qalin
        oval.set(cx - r, cy - r, cx + r, cy + r)

        // Shkala chiziqchalari
        for (i in 0..27) {
            val burchak = Math.toRadians(135.0 + i * 270.0 / 27)
            val r1 = r - qalin * 1.1f
            val r2 = r - qalin * (if (i % 3 == 0) 1.7f else 1.4f)
            canvas.drawLine(
                cx + (r1 * Math.cos(burchak)).toFloat(), cy + (r1 * Math.sin(burchak)).toFloat(),
                cx + (r2 * Math.cos(burchak)).toFloat(), cy + (r2 * Math.sin(burchak)).toFloat(),
                chiziq,
            )
        }

        trek.strokeWidth = qalin
        canvas.drawArc(oval, 135f, 270f, false, trek)

        if (qiymat > 0f) {
            val och = Color.argb(255,
                minOf(255, Color.red(rang) + 60), minOf(255, Color.green(rang) + 60), minOf(255, Color.blue(rang) + 60))
            val g = SweepGradient(cx, cy, intArrayOf(rang, och, rang), floatArrayOf(0f, 0.5f, 1f))
            nur.shader = null
            nur.color = Color.argb(60, Color.red(rang), Color.green(rang), Color.blue(rang))
            nur.strokeWidth = qalin * 1.9f
            canvas.drawArc(oval, 135f, 270f * qiymat, false, nur)
            yoy.shader = g
            yoy.strokeWidth = qalin
            canvas.drawArc(oval, 135f, 270f * qiymat, false, yoy)
        }

        katta.textSize = s * 0.26f
        canvas.drawText(matn, cx, cy + katta.textSize * 0.32f, katta)
        kichik.textSize = s * 0.06f
        canvas.drawText(izoh.uppercase(), cx, cy + r * 0.62f, kichik)
    }
}
