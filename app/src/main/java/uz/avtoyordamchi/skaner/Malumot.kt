package uz.avtoyordamchi.skaner

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Bitta to'liq tekshiruv natijasi. */
data class Tekshiruv(
    val vaqt: Long,
    val mashina: String,
    val vin: String?,
    val protokol: String,
    val skaner: String,
    val akk: String,
    val chek: Boolean?,
    val ball: Int,
    val saqlangan: List<String>,
    val kutilayotgan: List<String>,
    val doimiy: List<String>,
    val datchiklar: Map<String, String>,
    val muzlatilganKod: String?,
    val muzlatilgan: Map<String, String>,
    val tayyorlik: List<Monitor>,
) {
    val xatoSoni: Int get() = saqlangan.size + doimiy.size
    val vaqtMatn: String get() = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(Date(vaqt))

    fun json(): JSONObject = JSONObject().apply {
        put("vaqt", vaqt); put("mashina", mashina); put("vin", vin ?: ""); put("protokol", protokol)
        put("skaner", skaner); put("akk", akk); put("chek", if (chek == null) -1 else if (chek) 1 else 0)
        put("ball", ball)
        put("saqlangan", JSONArray(saqlangan)); put("kutilayotgan", JSONArray(kutilayotgan)); put("doimiy", JSONArray(doimiy))
        put("datchiklar", juftlar(datchiklar)); put("muzlatilgan", juftlar(muzlatilgan))
        put("muzlatilganKod", muzlatilganKod ?: "")
        put("tayyorlik", JSONArray().apply {
            tayyorlik.forEach { put(JSONObject().put("nom", it.nom).put("tayyor", it.tayyor)) }
        })
    }

    companion object {
        private fun juftlar(m: Map<String, String>) = JSONArray().apply {
            m.forEach { (k, v) -> put(JSONArray().put(k).put(v)) }
        }

        private fun royxat(a: JSONArray?): List<String> =
            if (a == null) emptyList() else List(a.length()) { a.getString(it) }

        private fun juftlarOqi(a: JSONArray?): Map<String, String> {
            val m = linkedMapOf<String, String>()
            if (a != null) for (i in 0 until a.length()) {
                val j = a.getJSONArray(i); m[j.getString(0)] = j.getString(1)
            }
            return m
        }

        fun oqi(o: JSONObject): Tekshiruv {
            val ch = o.optInt("chek", -1)
            val t = o.optJSONArray("tayyorlik")
            val mon = if (t == null) emptyList() else List(t.length()) {
                val m = t.getJSONObject(it); Monitor(m.getString("nom"), true, m.getBoolean("tayyor"))
            }
            return Tekshiruv(
                vaqt = o.getLong("vaqt"),
                mashina = o.optString("mashina"),
                vin = o.optString("vin").ifBlank { null },
                protokol = o.optString("protokol"),
                skaner = o.optString("skaner"),
                akk = o.optString("akk"),
                chek = if (ch < 0) null else ch == 1,
                ball = o.optInt("ball"),
                saqlangan = royxat(o.optJSONArray("saqlangan")),
                kutilayotgan = royxat(o.optJSONArray("kutilayotgan")),
                doimiy = royxat(o.optJSONArray("doimiy")),
                datchiklar = juftlarOqi(o.optJSONArray("datchiklar")),
                muzlatilganKod = o.optString("muzlatilganKod").ifBlank { null },
                muzlatilgan = juftlarOqi(o.optJSONArray("muzlatilgan")),
                tayyorlik = mon,
            )
        }

        /** Ball: 100 dan boshlab nosozliklarga qarab kamaytiriladi. */
        fun ballHisobla(
            saqlangan: List<String>, kutilayotgan: List<String>, doimiy: List<String>,
            akkV: Double?, harorat: Double?,
        ): Int {
            var ball = 100
            (saqlangan + doimiy).forEach {
                ball -= when (Kodlar.jiddiylik(it)) { 2 -> 35; 1 -> 15; else -> 6 }
            }
            ball -= kutilayotgan.size * 5
            if (akkV != null && akkV < 12.0) ball -= 10
            if (harorat != null && harorat > 105) ball -= 20
            return ball.coerceIn(0, 100)
        }
    }
}

/** Tekshiruvlar tarixi: ilova xotirasidagi JSON fayllar. */
object Tarix {
    private fun papka(ctx: Context) = File(ctx.filesDir, "tarix").apply { mkdirs() }

    fun saqla(ctx: Context, t: Tekshiruv) {
        File(papka(ctx), "${t.vaqt}.json").writeText(t.json().toString())
    }

    fun royxat(ctx: Context): List<Tekshiruv> =
        (papka(ctx).listFiles() ?: emptyArray())
            .filter { it.name.endsWith(".json") }
            .mapNotNull { f -> try { Tekshiruv.oqi(JSONObject(f.readText())) } catch (_: Exception) { null } }
            .sortedByDescending { it.vaqt }

    fun ochir(ctx: Context, vaqt: Long) {
        File(papka(ctx), "$vaqt.json").delete()
    }
}

/** Mashina profili va sozlamalar. */
class Profil(ctx: Context) {
    private val p = ctx.getSharedPreferences("profil", Context.MODE_PRIVATE)

    var model: String
        get() = p.getString("model", "") ?: ""
        set(v) = p.edit().putString("model", v).apply()
    var yil: String
        get() = p.getString("yil", "") ?: ""
        set(v) = p.edit().putString("yil", v).apply()
    var probeg: Int
        get() = p.getInt("probeg", 0)
        set(v) = p.edit().putInt("probeg", v).apply()
    var yoqilgi: String
        get() = p.getString("yoqilgi", "Benzin") ?: "Benzin"
        set(v) = p.edit().putString("yoqilgi", v).apply()
    var moyKm: Int
        get() = p.getInt("moyKm", 0)
        set(v) = p.edit().putInt("moyKm", v).apply()
    var moyInterval: Int
        get() = p.getInt("moyInterval", 8000)
        set(v) = p.edit().putInt("moyInterval", v).apply()
    var sugurta: String
        get() = p.getString("sugurta", "") ?: ""
        set(v) = p.edit().putString("sugurta", v).apply()
    var texosmotr: String
        get() = p.getString("texosmotr", "") ?: ""
        set(v) = p.edit().putString("texosmotr", v).apply()
    var ulanishTuri: String
        get() = p.getString("ulanishTuri", "wifi") ?: "wifi"
        set(v) = p.edit().putString("ulanishTuri", v).apply()
    var host: String
        get() = p.getString("host", "192.168.0.10") ?: "192.168.0.10"
        set(v) = p.edit().putString("host", v).apply()
    var port: Int
        get() = p.getInt("port", 35000)
        set(v) = p.edit().putInt("port", v).apply()
    var btManzil: String
        get() = p.getString("btManzil", "") ?: ""
        set(v) = p.edit().putString("btManzil", v).apply()
    var btNom: String
        get() = p.getString("btNom", "") ?: ""
        set(v) = p.edit().putString("btNom", v).apply()
    var eng100: Float
        get() = p.getFloat("eng100", 0f)
        set(v) = p.edit().putFloat("eng100", v).apply()

    val mashinaNomi: String get() = if (model.isBlank()) "Mashina" else listOf(model, yil).filter { it.isNotBlank() }.joinToString(" ")
    val toldirilgan: Boolean get() = model.isNotBlank()

    /** "yyyy-MM-dd" sanagacha qolgan kunlar, sana yo'q bo'lsa null. */
    fun qolganKun(sana: String): Int? {
        if (sana.isBlank()) return null
        return try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(sana) ?: return null
            val bugun = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            ((d.time - bugun) / 86_400_000L).toInt()
        } catch (_: Exception) { null }
    }

    /** Moy almashtirishgacha qolgan km, ma'lumot yo'q bo'lsa null. */
    val moyQoldi: Int? get() = if (moyKm <= 0 || probeg <= 0) null else moyKm + moyInterval - probeg

    companion object {
        val MODELLAR = listOf(
            "Chevrolet Cobalt", "Chevrolet Gentra", "Chevrolet Nexia 3", "Chevrolet Spark", "Chevrolet Malibu",
            "Chevrolet Tracker", "Chevrolet Onix", "Chevrolet Damas", "Chevrolet Labo", "Chevrolet Lacetti",
            "Chevrolet Captiva", "Chevrolet Equinox", "Chevrolet Tahoe", "Daewoo Nexia", "Daewoo Matiz",
            "BYD Song Plus", "BYD Chazor", "BYD Han", "Kia K5", "Kia Sportage", "Hyundai Sonata", "Hyundai Elantra",
            "Toyota Camry", "Chery Tiggo", "Haval Jolion", "Boshqa",
        )
        val YOQILGILAR = listOf("Benzin", "Metan", "Propan", "Dizel", "Gibrid", "Elektr")
    }
}
