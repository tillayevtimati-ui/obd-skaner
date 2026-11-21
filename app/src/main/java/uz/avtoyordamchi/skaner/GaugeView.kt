package uz.avtoyordamchi.skaner

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.view.View
import android.view.animation.DecelerateInterpolator

/** Aylana o'lchagich: 270° yoy, markazda katta raqam va izoh. */
class GaugeView(context: Context, private val maxOlcham: Int = 250) : View(context) {

    private var korinadigan = 0f
    private var animator: ValueAnimator? = null

    /** 0..1 qiymat; o'zgarganda yumshoq animatsiya bilan o'tadi. */
    var qiymat: Float = 0f
        set(v) {
            field = v.coerceIn(0f, 1f)
            animator?.cancel()
            animator = ValueAnimator.ofFloat(korinadigan, field).apply {
                duration = 650
                interpolator = DecelerateInterpolator()
                addUpdateListener { korinadigan = it.animatedValue as Float; invalidate() }
                start()
            }
        }
    var matn: String = "—"
        set(v) { field = v; invalidate() }
    var izoh: String = ""
        set(v) { field = v; invalidate() }
    var birlik: String = ""
        set(v) { field = v; invalidate() }
    var rang: Int = R2.KOK
        set(v) { field = v; invalidate() }

    private val d = resources.displayMetrics.density
    private val oval = RectF()

    private val trek = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = Color.parseColor("#1B2747")
    }
    private val yoy = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val nur = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val chiziq = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val nuqta = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val katta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Shrift.raqam(context)
    }
    private val kichik = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = R2.XIRA; textAlign = Paint.Align.CENTER; typeface = Shrift.qalin(context); letterSpacing = 0.12f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val s = minOf(w, (maxOlcham * d).toInt())
        setMeasuredDimension(w, s)
    }

    override fun onDraw(canvas: Canvas) {
        val s = minOf(width, height).toFloat()
        val qalin = s * 0.07f
        val cx = width / 2f
        val cy = height / 2f
        val r = s / 2f - qalin * 1.2f
        oval.set(cx - r, cy - r, cx + r, cy + r)

        // Shkala chiziqchalari
        for (i in 0..40) {
            val burchak = Math.toRadians(135.0 + i * 270.0 / 40)
            val katt = i % 5 == 0
            val r1 = r - qalin * 1.15f
            val r2 = r - qalin * (if (katt) 1.85f else 1.5f)
            val faol = i / 40f <= korinadigan && korinadigan > 0f
            chiziq.color = if (faol) R2.shaffof(rang, 220) else Color.parseColor("#2A3758")
            chiziq.strokeWidth = (if (katt) 2.4f else 1.4f) * d
            canvas.drawLine(
                cx + (r1 * Math.cos(burchak)).toFloat(), cy + (r1 * Math.sin(burchak)).toFloat(),
                cx + (r2 * Math.cos(burchak)).toFloat(), cy + (r2 * Math.sin(burchak)).toFloat(),
                chiziq,
            )
        }

        trek.strokeWidth = qalin
        canvas.drawArc(oval, 135f, 270f, false, trek)

        if (korinadigan > 0.002f) {
            val och = Color.argb(255,
                minOf(255, Color.red(rang) + 70), minOf(255, Color.green(rang) + 70), minOf(255, Color.blue(rang) + 70))
            nur.color = R2.shaffof(rang, 55)
            nur.strokeWidth = qalin * 2.2f
            canvas.drawArc(oval, 135f, 270f * korinadigan, false, nur)
            yoy.shader = SweepGradient(cx, cy, intArrayOf(rang, och, rang), floatArrayOf(0f, 0.5f, 1f))
            yoy.strokeWidth = qalin
            canvas.drawArc(oval, 135f, 270f * korinadigan, false, yoy)
            // Uchidagi yorqin nuqta
            val a = Math.toRadians(135.0 + 270.0 * korinadigan)
            canvas.drawCircle(cx + (r * Math.cos(a)).toFloat(), cy + (r * Math.sin(a)).toFloat(), qalin * 0.32f, nuqta)
        }

        katta.textSize = s * (if (matn.length > 4) 0.17f else 0.22f)
        canvas.drawText(matn, cx, cy + katta.textSize * 0.36f, katta)
        if (birlik.isNotEmpty()) {
            kichik.textSize = s * 0.055f
            kichik.color = R2.XIRA
            canvas.drawText(birlik, cx, cy + katta.textSize * 0.36f + s * 0.1f, kichik)
        }
        kichik.textSize = s * 0.058f
        kichik.color = if (izoh.isNotEmpty()) R2.shaffof(rang, 255) else R2.XIRA
        canvas.drawText(izoh.uppercase(), cx, cy + r * 0.78f, kichik)
    }
}

/** Kichik chiziqli grafik (oxirgi N qiymat). */
class SparkView(context: Context, private val rang: Int) : View(context) {
    private val qiymatlar = ArrayList<Float>()
    private val d = resources.displayMetrics.density
    private val chiziq = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2.2f * d; color = rang; strokeJoin = Paint.Join.ROUND
    }
    private val soya = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = R2.shaffof(rang, 40) }
    private val yol = android.graphics.Path()
    private val toldirish = android.graphics.Path()

    fun qosh(v: Float) {
        qiymatlar.add(v)
        if (qiymatlar.size > 60) qiymatlar.removeAt(0)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (40 * d).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        if (qiymatlar.size < 2) return
        val mn = qiymatlar.min()
        val mx = qiymatlar.max()
        val oraliq = if (mx - mn < 0.0001f) 1f else mx - mn
        val w = width.toFloat()
        val h = height.toFloat() - 4 * d
        yol.reset(); toldirish.reset()
        qiymatlar.forEachIndexed { i, v ->
            val x = w * i / (qiymatlar.size - 1)
            val y = 2 * d + h - (v - mn) / oraliq * h
            if (i == 0) { yol.moveTo(x, y); toldirish.moveTo(x, height.toFloat()); toldirish.lineTo(x, y) }
            else { yol.lineTo(x, y); toldirish.lineTo(x, y) }
        }
        toldirish.lineTo(w, height.toFloat()); toldirish.close()
        canvas.drawPath(toldirish, soya)
        canvas.drawPath(yol, chiziq)
    }
}
