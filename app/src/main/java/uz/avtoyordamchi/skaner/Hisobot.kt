package uz.avtoyordamchi.skaner

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

/** Tekshiruv natijasidan A4 PDF hisobot yasaydi. */
object PdfHisobot {

    fun yarat(ctx: Context, t: Tekshiruv, profil: Profil): File {
        val doc = PdfDocument()
        val W = 595f
        val H = 842f
        val chap = 40f
        val ong = W - 40f
        var sahifaRaqam = 1
        var sahifa = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, sahifaRaqam).create())
        var c: Canvas = sahifa.canvas
        var y = 0f

        val qalin = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Shrift.qalin(ctx); color = Color.parseColor("#0B1226") }
        val orta = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Shrift.orta(ctx); color = Color.parseColor("#1F2A44") }
        val oddiy = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Shrift.oddiy(ctx); color = Color.parseColor("#4A5675") }
        val raqam = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Shrift.raqam(ctx) }
        val chiziq = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E3E8F2"); strokeWidth = 1f }
        val fon = Paint(Paint.ANTI_ALIAS_FLAG)

        fun sarlavhaChiz() {
            fon.shader = LinearGradient(0f, 0f, W, 0f, Color.parseColor("#0E1730"), Color.parseColor("#1E3A8A"), Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, W, 110f, fon)
            fon.shader = null
            Ikonka.ol(ctx, "mashina")?.let { c.drawBitmap(it, null, RectF(chap, 24f, chap + 62f, 86f), null) }
            qalin.color = Color.WHITE; qalin.textSize = 22f
            c.drawText("Avto Skaner hisoboti", chap + 78f, 52f, qalin)
            oddiy.color = Color.parseColor("#B8C4E0"); oddiy.textSize = 11f
            c.drawText("${t.mashina}  ·  ${t.vaqtMatn}", chap + 78f, 74f, oddiy)
            qalin.color = Color.parseColor("#0B1226"); oddiy.color = Color.parseColor("#4A5675")
        }

        fun yangiSahifa() {
            doc.finishPage(sahifa)
            sahifaRaqam++
            sahifa = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, sahifaRaqam).create())
            c = sahifa.canvas
            y = 50f
        }

        fun joy(kerak: Float) { if (y + kerak > H - 50f) yangiSahifa() }

        fun bolim(m: String) {
            joy(50f)
            y += 26f
            qalin.textSize = 13f; qalin.color = Color.parseColor("#3D8BFF")
            c.drawText(m.uppercase(), chap, y, qalin)
            qalin.color = Color.parseColor("#0B1226")
            y += 8f
            c.drawLine(chap, y, ong, y, chiziq)
            y += 18f
        }

        fun juft(nom: String, qiymat: String) {
            joy(20f)
            oddiy.textSize = 11f; orta.textSize = 11f
            c.drawText(nom, chap, y, oddiy)
            orta.textAlign = Paint.Align.RIGHT
            c.drawText(qiymat, ong, y, orta)
            orta.textAlign = Paint.Align.LEFT
            y += 18f
        }

        sarlavhaChiz()

        // Ball doirasi
        y = 140f
        val rang = R2.ballRangi(t.ball)
        val doira = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 8f; color = Color.parseColor("#E3E8F2") }
        c.drawArc(RectF(chap, y, chap + 90f, y + 90f), 135f, 270f, false, doira)
        doira.color = rang; doira.strokeCap = Paint.Cap.ROUND
        c.drawArc(RectF(chap, y, chap + 90f, y + 90f), 135f, 270f * t.ball / 100f, false, doira)
        raqam.textSize = 26f; raqam.color = Color.parseColor("#0B1226"); raqam.textAlign = Paint.Align.CENTER
        c.drawText(t.ball.toString(), chap + 45f, y + 54f, raqam)
        qalin.textSize = 18f
        c.drawText("Holat: ${R2.ballSozi(t.ball)}", chap + 115f, y + 32f, qalin)
        oddiy.textSize = 11.5f
        val xulosa = when {
            t.xatoSoni == 0 && t.kutilayotgan.isEmpty() -> "Xato kodlari topilmadi."
            t.xatoSoni == 0 -> "Kutilayotgan xatolar bor, kuzatib boring."
            else -> "${t.xatoSoni} ta xato kodi topildi."
        }
        c.drawText(xulosa, chap + 115f, y + 54f, oddiy)
        c.drawText("'Chek' chirog'i: ${when (t.chek) { true -> "yongan"; false -> "o'chiq"; null -> "noma'lum" }}", chap + 115f, y + 72f, oddiy)
        y += 110f

        bolim("Xato kodlari")
        val hammasi = t.saqlangan.map { it to "Saqlangan" } + t.doimiy.map { it to "Doimiy" } + t.kutilayotgan.map { it to "Kutilayotgan" }
        if (hammasi.isEmpty()) {
            orta.textSize = 12f; c.drawText("Xato kodlari yo'q", chap, y, orta); y += 18f
        }
        for ((kod, turi) in hammasi) {
            joy(44f)
            val kr = when (Kodlar.jiddiylik(kod)) { 2 -> R2.QIZIL; 1 -> R2.SARIQ; else -> R2.YASHIL }
            fon.color = kr
            c.drawRoundRect(RectF(chap, y - 12f, chap + 4f, y + 20f), 2f, 2f, fon)
            qalin.textSize = 14f; c.drawText(kod, chap + 14f, y, qalin)
            oddiy.textSize = 10f; c.drawText(turi, chap + 80f, y, oddiy)
            orta.textSize = 11f; c.drawText(Kodlar.izoh(kod).take(95), chap + 14f, y + 16f, orta)
            y += 38f
        }

        if (t.muzlatilgan.isNotEmpty()) {
            bolim("Xato paydo bo'lgan payt (freeze frame)" + (t.muzlatilganKod?.let { " — $it" } ?: ""))
            t.muzlatilgan.forEach { (k, v) -> juft(k, v) }
        }

        if (t.tayyorlik.isNotEmpty()) {
            bolim("Tayyorlik monitorlari")
            t.tayyorlik.forEach { juft(it.nom, if (it.tayyor) "Tayyor" else "Tugallanmagan") }
        }

        if (t.datchiklar.isNotEmpty()) {
            bolim("Datchiklar")
            t.datchiklar.forEach { (k, v) -> juft(k, v) }
        }

        bolim("Mashina")
        juft("Mashina", t.mashina)
        if (profil.probeg > 0) juft("Probeg (egasi kiritgan)", "${profil.probeg} km")
        juft("Yoqilg'i", profil.yoqilgi)
        juft("VIN", t.vin ?: "o'qilmadi")
        juft("Protokol", t.protokol)
        juft("Akkumulyator", t.akk)

        joy(40f)
        y += 24f
        oddiy.textSize = 9f
        c.drawText("Avto Skaner tomonidan avtomatik yaratildi. Yakuniy tashxis uchun malakali ustaga murojaat qiling.", chap, y, oddiy)

        doc.finishPage(sahifa)
        val papka = File(ctx.cacheDir, "hisobotlar").apply { mkdirs() }
        papka.listFiles()?.forEach { it.delete() }
        val fayl = File(papka, "AvtoSkaner_${t.vaqt}.pdf")
        FileOutputStream(fayl).use { doc.writeTo(it) }
        doc.close()
        return fayl
    }

    fun uri(fayl: File): Uri = Uri.parse("content://${HisobotProvider.AVTORITET}/${fayl.name}")
}

/** PDF hisobotni boshqa ilovalarga (Telegram va h.k.) berish uchun oddiy provayder. */
class HisobotProvider : ContentProvider() {
    override fun onCreate() = true

    private fun fayl(uri: Uri): File? {
        val nom = uri.lastPathSegment ?: return null
        if (nom.contains("/") || nom.contains("..")) return null
        val f = File(File(context!!.cacheDir, "hisobotlar"), nom)
        return if (f.exists()) f else null
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val f = fayl(uri) ?: return null
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri) = "application/pdf"

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val f = fayl(uri)
        val c = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
        if (f != null) c.addRow(arrayOf<Any>(f.name, f.length()))
        return c
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    companion object {
        const val AVTORITET = "uz.avtoyordamchi.skaner.hisobot"
    }
}
