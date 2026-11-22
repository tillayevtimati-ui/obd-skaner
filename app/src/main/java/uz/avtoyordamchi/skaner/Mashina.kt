package uz.avtoyordamchi.skaner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.view.View
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Garajdagi bitta mashina. */
data class Mashina(
    val id: Long,
    var model: String = "",
    var yil: String = "",
    var probeg: Int = 0,
    var yoqilgi: String = "Benzin",
    var rang: Int = 0,
    var hudud: String = "",
    var raqam: String = "",
    var rasm: String = "",
    var moyKm: Int = 0,
    var moyInterval: Int = 8000,
    var sugurta: String = "",
    var texosmotr: String = "",
    var tonirovka: String = "",
    var ishonchnoma: String = "",
) {
    val nomi: String get() = if (model.isBlank()) "Mashina" else listOf(model, yil).filter { it.isNotBlank() }.joinToString(" ")
    val brend: String get() = model.substringBefore(' ').ifBlank { "Avto" }
    val qisqaNom: String get() = model.substringAfter(' ', model).ifBlank { "Mashina" }
    val kuzov: String get() = Kuzov.turi(model)
    val raqamBor: Boolean get() = raqam.isNotBlank()

    /** Moy almashtirishgacha qolgan km, ma'lumot yo'q bo'lsa null. */
    val moyQoldi: Int? get() = if (moyKm <= 0 || probeg <= 0) null else moyKm + moyInterval - probeg

    fun json(): JSONObject = JSONObject().apply {
        put("id", id); put("model", model); put("yil", yil); put("probeg", probeg); put("yoqilgi", yoqilgi)
        put("rang", rang); put("hudud", hudud); put("raqam", raqam); put("rasm", rasm)
        put("moyKm", moyKm); put("moyInterval", moyInterval); put("sugurta", sugurta); put("texosmotr", texosmotr)
        put("tonirovka", tonirovka); put("ishonchnoma", ishonchnoma)
    }

    companion object {
        fun oqi(o: JSONObject) = Mashina(
            id = o.getLong("id"),
            model = o.optString("model"), yil = o.optString("yil"), probeg = o.optInt("probeg"),
            yoqilgi = o.optString("yoqilgi", "Benzin"), rang = o.optInt("rang"),
            hudud = o.optString("hudud"), raqam = o.optString("raqam"), rasm = o.optString("rasm"),
            moyKm = o.optInt("moyKm"), moyInterval = o.optInt("moyInterval", 8000),
            sugurta = o.optString("sugurta"), texosmotr = o.optString("texosmotr"),
            tonirovka = o.optString("tonirovka"), ishonchnoma = o.optString("ishonchnoma"),
        )

        val RANGLAR = listOf(
            "Oq" to Color.parseColor("#F1F3F6"), "Qora" to Color.parseColor("#1F2228"),
            "Kumush" to Color.parseColor("#B7BEC9"), "Kulrang" to Color.parseColor("#6B7280"),
            "Ko'k" to Color.parseColor("#2563EB"), "Moviy" to Color.parseColor("#7FA8D1"),
            "Qizil" to Color.parseColor("#C81E2D"), "Yashil" to Color.parseColor("#1F7A4D"),
            "Jigarrang" to Color.parseColor("#7A4E2D"), "Sariq" to Color.parseColor("#E8B22A"),
        )
    }
}

/** Model nomidan kuzov turini aniqlash. */
object Kuzov {
    private val XETCHBEK = listOf("Spark", "Matiz", "Onix Hatch")
    private val KROSSOVER = listOf("Tracker", "Captiva", "Equinox", "Tahoe", "Sportage", "Song", "Tiggo", "Jolion", "Traverse", "Tucson", "Santa Fe", "RAV4")
    private val MINIVEN = listOf("Damas", "Labo", "Orlando")

    fun turi(model: String): String = when {
        XETCHBEK.any { model.contains(it, true) } -> "xetchbek"
        KROSSOVER.any { model.contains(it, true) } -> "krossover"
        MINIVEN.any { model.contains(it, true) } -> "miniven"
        else -> "sedan"
    }
}

/** Mashina rasmi: foydalanuvchi surati yoki rangli kuzov siluet. */
object MashinaRasm {
    private val kesh = HashMap<String, Bitmap>()

    fun siluet(ctx: Context, kuzov: String, rang: Int): Bitmap? {
        val kalit = "$kuzov-$rang"
        kesh[kalit]?.let { return it }
        return try {
            val k = ctx.assets.open("cars/${kuzov}_kuzov.png").use { BitmapFactory.decodeStream(it) }
            val u = ctx.assets.open("cars/${kuzov}_ustki.png").use { BitmapFactory.decodeStream(it) }
            val natija = Bitmap.createBitmap(k.width, k.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(natija)
            c.drawBitmap(k, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = PorterDuffColorFilter(rang, PorterDuff.Mode.SRC_IN) })
            c.drawBitmap(u, 0f, 0f, null)
            kesh[kalit] = natija
            natija
        } catch (_: Exception) { null }
    }

    fun ol(ctx: Context, m: Mashina): Bitmap? {
        if (m.rasm.isNotBlank()) {
            val f = File(m.rasm)
            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.let { return it }
        }
        return siluet(ctx, m.kuzov, Mashina.RANGLAR.getOrNull(m.rang)?.second ?: Mashina.RANGLAR[0].second)
    }

    fun tozalaKesh() = kesh.clear()
}

/** Sana yordamchilari. */
object Sana {
    private val OYLAR = listOf("yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr")

    fun qolganKun(sana: String): Int? {
        if (sana.isBlank()) return null
        return try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(sana) ?: return null
            val bugun = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            Math.round((d.time - bugun) / 86_400_000.0).toInt()
        } catch (_: Exception) { null }
    }

    /** "2027-02-04" -> "4 fevral 2027" */
    fun chiroyli(sana: String): String {
        if (sana.length != 10) return sana
        return try {
            val oy = sana.substring(5, 7).toInt()
            "${sana.substring(8, 10).toInt()} ${OYLAR[oy - 1]} ${sana.substring(0, 4)}"
        } catch (_: Exception) { sana }
    }

    /** Qolgan muddat: "Yana 3 oy", "Yana 12 kun", "Bugun", "Muddati o'tgan". */
    fun qolganMatn(kun: Int): String = when {
        kun < 0 -> "Muddati o'tgan"
        kun == 0 -> "Bugun tugaydi"
        kun < 60 -> "Yana $kun kun"
        kun < 730 -> "Yana ${kun / 30} oy"
        else -> "Yana ${kun / 365} yil"
    }
}

/** O'zbekiston davlat raqami ko'rinishi: "40 | 404 KCA | UZ". */
class RaqamView(context: Context, var hudud: String, var raqam: String, private val kenglikDp: Int = 200) : View(context) {
    private val d = resources.displayMetrics.density
    private val chegara = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.parseColor("#111418") }
    private val fon = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val yozuv = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111418"); textAlign = Paint.Align.CENTER; typeface = Shrift.qalin(context)
    }
    private val bayroq = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = (kenglikDp * d).toInt()
        setMeasuredDimension(w, (w * 0.215f).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val q = h * 0.06f
        chegara.strokeWidth = q
        r.set(q / 2, q / 2, w - q / 2, h - q / 2)
        canvas.drawRoundRect(r, h * 0.12f, h * 0.12f, fon)
        canvas.drawRoundRect(r, h * 0.12f, h * 0.12f, chegara)
        val ajrat = w * 0.17f
        canvas.drawLine(ajrat, q, ajrat, h - q, chegara)
        // Hudud kodi
        yozuv.textSize = h * 0.5f
        yozuv.textScaleX = 0.9f
        canvas.drawText(hudud.ifBlank { "--" }, ajrat / 2, h / 2 + yozuv.textSize * 0.36f, yozuv)
        // Asosiy raqam
        val bayroqX = w - w * 0.1f
        yozuv.textSize = h * 0.62f
        yozuv.textScaleX = 0.88f
        canvas.drawText(raqam.uppercase().ifBlank { "--- ---" }, (ajrat + bayroqX) / 2, h / 2 + yozuv.textSize * 0.36f, yozuv)
        // Bayroq va UZ
        val bx = bayroqX + w * 0.015f
        val bw = w * 0.055f
        val bh = h * 0.28f
        val by = h * 0.18f
        bayroq.color = Color.parseColor("#0099B5"); canvas.drawRect(bx, by, bx + bw, by + bh / 3, bayroq)
        bayroq.color = Color.WHITE; canvas.drawRect(bx, by + bh / 3, bx + bw, by + bh * 2 / 3, bayroq)
        bayroq.color = Color.parseColor("#1EB53A"); canvas.drawRect(bx, by + bh * 2 / 3, bx + bw, by + bh, bayroq)
        bayroq.color = Color.parseColor("#CE1126")
        canvas.drawRect(bx, by + bh / 3 - h * 0.012f, bx + bw, by + bh / 3, bayroq)
        canvas.drawRect(bx, by + bh * 2 / 3, bx + bw, by + bh * 2 / 3 + h * 0.012f, bayroq)
        yozuv.textSize = h * 0.24f
        yozuv.textScaleX = 1f
        yozuv.color = Color.parseColor("#0072BC")
        canvas.drawText("UZ", bx + bw / 2, h * 0.82f, yozuv)
        yozuv.color = Color.parseColor("#111418")
    }
}
