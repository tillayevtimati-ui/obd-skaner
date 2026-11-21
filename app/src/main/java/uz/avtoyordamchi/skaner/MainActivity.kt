package uz.avtoyordamchi.skaner

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.SocketFactory

class MainActivity : Activity() {

    // ------------------------------------------------------------ ranglar
    private val FON1 = Color.parseColor("#070B16")
    private val FON2 = Color.parseColor("#101A33")
    private val KARTA = Color.parseColor("#141E38")
    private val CHEGARA = Color.parseColor("#23305A")
    private val OQ = Color.parseColor("#F4F7FF")
    private val XIRA = Color.parseColor("#8B98B8")
    private val KOK = Color.parseColor("#3D8BFF")
    private val FIRUZA = Color.parseColor("#22D3EE")
    private val YASHIL = Color.parseColor("#22C55E")
    private val SARIQ = Color.parseColor("#F59E0B")
    private val QIZIL = Color.parseColor("#EF4444")

    private lateinit var hostEdit: EditText
    private lateinit var portEdit: EditText
    private lateinit var gauge: GaugeView
    private lateinit var gaugeOsti: TextView
    private lateinit var ulanishChip: TextView
    private lateinit var holat: TextView
    private lateinit var natijalar: LinearLayout
    private lateinit var asosiyBtn: TextView
    private lateinit var jonliBtn: LinearLayout
    private lateinit var tozalashBtn: LinearLayout
    private lateinit var ulashBtn: LinearLayout
    private lateinit var jonliMatn: TextView

    @Volatile private var band = false
    @Volatile private var jonli = false
    private var oxirgiHisobot = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = FON1
        window.navigationBarColor = FON2
        val prefs = getSharedPreferences("sozlama", Context.MODE_PRIVATE)

        val ildiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(28))
        }

        // ---------- Sarlavha
        val tepa = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val sarlavhaBlok = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        sarlavhaBlok.addView(matn("AVTO DIAGNOSTIKA", 11f, FIRUZA, "sans-serif-medium").apply { letterSpacing = 0.18f })
        sarlavhaBlok.addView(matn("Avto Skaner", 26f, OQ, "sans-serif-medium"))
        tepa.addView(sarlavhaBlok, LinearLayout.LayoutParams(0, -2, 1f))
        ulanishChip = matn("●  Kutilmoqda", 12f, XIRA, "sans-serif-medium").apply {
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = shakl(Color.parseColor("#1A2442"), dp(20).toFloat(), CHEGARA)
        }
        tepa.addView(ulanishChip)
        ildiz.addView(tepa)

        // ---------- Holat o'lchagichi
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(18), dp(16), dp(18))
            background = gradient(Color.parseColor("#16234A"), KARTA, dp(24).toFloat())
        }
        hero.addView(matn("MASHINA HOLATI", 11f, XIRA, "sans-serif-medium").apply { letterSpacing = 0.14f })
        gauge = GaugeView(this).apply { matn = "?"; izoh = "tekshirilmagan"; qiymat = 0f }
        hero.addView(gauge, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        gaugeOsti = matn("Skanerni ulang va tekshiruvni boshlang", 13f, XIRA).apply { gravity = Gravity.CENTER }
        hero.addView(gaugeOsti)
        ildiz.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })

        // ---------- Asosiy tugma
        asosiyBtn = matn("Tekshirishni boshlash", 16f, Color.WHITE, "sans-serif-medium").apply {
            gravity = Gravity.CENTER
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(KOK, FIRUZA)).apply {
                cornerRadius = dp(18).toFloat()
            }
            elevation = dp(6).toFloat()
            setOnClickListener { tekshirish() }
        }
        ildiz.addView(asosiyBtn, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(16) })

        // ---------- Uchta kichik amal
        val amallar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        jonliBtn = amal("📈", "Jonli") { jonliAlmashtir() }
        tozalashBtn = amal("🧹", "Tozalash") { ochirishSorash() }
        ulashBtn = amal("📤", "Ulashish") { ulashish() }
        jonliMatn = (jonliBtn.getChildAt(1) as TextView)
        listOf(jonliBtn, tozalashBtn, ulashBtn).forEachIndexed { i, v ->
            amallar.addView(v, LinearLayout.LayoutParams(0, dp(76), 1f).apply {
                if (i > 0) marginStart = dp(10)
            })
        }
        ildiz.addView(amallar, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        // ---------- Holat matni
        holat = matn("1) Skanerni ulang, kalitni ON qiling\n2) Telefonni skaner Wi-Fi'siga ulang\n3) \"Tekshirishni boshlash\"ni bosing",
            13f, XIRA).apply { setLineSpacing(dp(3).toFloat(), 1f) }
        ildiz.addView(holat, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })

        natijalar = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ildiz.addView(natijalar)

        // ---------- Sozlamalar
        ildiz.addView(bolimSarlavha("Skaner sozlamalari"))
        val sozlama = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = shakl(KARTA, dp(18).toFloat(), CHEGARA)
        }
        hostEdit = maydon(prefs.getString("host", "192.168.0.10") ?: "192.168.0.10", "Manzil",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        portEdit = maydon(prefs.getInt("port", 35000).toString(), "Port", InputType.TYPE_CLASS_NUMBER)
        sozlama.addView(maydonBlok("Manzil (IP)", hostEdit), LinearLayout.LayoutParams(0, -2, 2f))
        sozlama.addView(maydonBlok("Port", portEdit), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
        ildiz.addView(sozlama)
        ildiz.addView(matn("Ko'pchilik Wi-Fi skanerlar: 192.168.0.10 : 35000", 12f, XIRA).apply {
            setPadding(dp(4), dp(8), 0, 0)
        })

        setContentView(ScrollView(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(FON1, FON2))
            isFillViewport = true
            addView(ildiz)
        })
    }

    override fun onDestroy() {
        jonli = false
        super.onDestroy()
    }

    // ------------------------------------------------------------ aloqa
    @Suppress("DEPRECATION")
    private fun wifiFactory(): SocketFactory {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        for (n in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(n) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return n.socketFactory
        }
        return SocketFactory.getDefault()
    }

    private fun yangiElm(): Elm {
        val host = hostEdit.text.toString().trim()
        val port = portEdit.text.toString().trim().toIntOrNull() ?: 35000
        getSharedPreferences("sozlama", Context.MODE_PRIVATE).edit()
            .putString("host", host).putInt("port", port).apply()
        val elm = Elm(wifiFactory())
        elm.ulan(host, port)
        return elm
    }

    private fun fonda(ish: () -> Unit) {
        if (band) return
        band = true
        tugmalar(false)
        Thread {
            try {
                ish()
            } catch (e: Exception) {
                ui {
                    chip("Ulanmagan", QIZIL)
                    holat("Ulanib bo'lmadi: ${e.message ?: e.javaClass.simpleName}\n" +
                        "Telefon skaner Wi-Fi'siga ulanganini, manzil va portni tekshiring.", QIZIL)
                }
            } finally {
                band = false
                ui { tugmalar(true) }
            }
        }.start()
    }

    // ------------------------------------------------------------ to'liq tekshiruv
    private fun tekshirish() = fonda {
        ui {
            natijalar.removeAllViews()
            holat("Skanerga ulanmoqda...", XIRA)
            chip("Ulanmoqda", SARIQ)
            gauge.rang = KOK; gauge.qiymat = 0.08f; gauge.matn = "…"; gauge.izoh = "ulanmoqda"
        }
        val elm = yangiElm()
        try {
            val versiya = elm.boshlash()
            val akk = elm.kuchlanish()
            ui {
                chip("Skaner ulandi", FIRUZA)
                holat("Mashina bilan aloqa o'rnatilmoqda (10–20 soniya)...", XIRA)
                gauge.qiymat = 0.3f; gauge.izoh = "aloqa"
            }
            val protokol = elm.mashinaBilanAloqa()
            if (protokol == null) {
                ui {
                    chip("Mashina javob bermadi", QIZIL)
                    gauge.qiymat = 0f; gauge.matn = "?"; gauge.izoh = "javob yo'q"
                    holat("Mashina javob bermadi. Kalit ON holatidami? Eski mashinalarda OBD-II bo'lmasligi mumkin.", QIZIL)
                }
                return@fonda
            }
            ui { chip("Mashina ulandi", YASHIL); holat("Ma'lumotlar o'qilmoqda...", XIRA); gauge.qiymat = 0.55f; gauge.izoh = "o'qilmoqda" }
            val vin = elm.vin()
            val holatBayt = elm.pid("0101")
            val chek = holatBayt?.let { (it[0] and 0x80) != 0 }
            val saqlangan = elm.xatolar("03")
            val kutilayotgan = elm.xatolar("07")
            val doimiy = elm.xatolar("0A")
            val datchiklar = linkedMapOf<String, String>()
            val sonlar = hashMapOf<String, Double>()
            for (d in DATCHIKLAR) {
                val a = elm.pid(d.pid) ?: continue
                val q = try { d.f(a) } catch (_: Exception) { continue }
                datchiklar[d.nom] = "$q ${d.birlik}"
                sonlar[d.pid] = q.toDouble()
            }

            val vaqt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(Date())
            oxirgiHisobot = hisobot(vaqt, versiya, akk, protokol, vin, chek, saqlangan, kutilayotgan, doimiy, datchiklar)

            // Ball: 100 dan boshlab kamaytiriladi
            var ball = 100
            (saqlangan + doimiy).forEach {
                ball -= when (Kodlar.jiddiylik(it)) { 2 -> 35; 1 -> 15; else -> 6 }
            }
            ball -= kutilayotgan.size * 5
            val kuchV = akk.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
            if (kuchV != null && kuchV < 12.0) ball -= 10
            val harorat = sonlar["0105"]
            if (harorat != null && harorat > 105) ball -= 20
            ball = ball.coerceIn(0, 100)
            val (rang, soz) = when {
                ball >= 85 -> YASHIL to "a'lo holat"
                ball >= 60 -> SARIQ to "e'tibor bering"
                else -> QIZIL to "jiddiy"
            }

            ui {
                gauge.rang = rang; gauge.qiymat = ball / 100f; gauge.matn = ball.toString(); gauge.izoh = soz
                gaugeOsti.text = "Oxirgi tekshiruv: $vaqt"
                val jami = saqlangan.size + doimiy.size
                when {
                    jami == 0 && kutilayotgan.isEmpty() -> holat("Xato kodlari topilmadi", YASHIL)
                    jami == 0 -> holat("Kutilayotgan xatolar bor: kuzatib boring", SARIQ)
                    else -> holat("$jami ta xato kodi topildi", QIZIL)
                }

                // Asosiy ko'rsatkichlar
                natijalar.addView(bolimSarlavha("Asosiy ko'rsatkichlar"))
                val asosiy = listOf(
                    Triple("🔋", "Akkumulyator", akk.ifBlank { "—" }),
                    Triple("🌡️", "Sovutish suyuqligi", datchiklar["Sovutish suyuqligi"] ?: "—"),
                    Triple("⚙️", "Aylanish", datchiklar["Motor aylanishi"] ?: "—"),
                    Triple("🚨", "'Chek' chirog'i", when (chek) { true -> "Yongan"; false -> "O'chiq"; null -> "—" }),
                )
                natijalar.addView(panjara(asosiy.map { plitka(it.first, it.second, it.third).first }))

                // Xatolar
                natijalar.addView(bolimSarlavha("Xato kodlari"))
                if (saqlangan.isEmpty() && kutilayotgan.isEmpty() && doimiy.isEmpty()) {
                    natijalar.addView(boshKarta("✅", "Xato kodlari yo'q", "Motor bloki hech qanday xato saqlamagan"))
                } else {
                    saqlangan.forEach { natijalar.addView(xatoKarta(it, "Saqlangan")) }
                    doimiy.forEach { natijalar.addView(xatoKarta(it, "Doimiy")) }
                    kutilayotgan.forEach { natijalar.addView(xatoKarta(it, "Kutilayotgan")) }
                }

                // Barcha datchiklar
                if (datchiklar.isNotEmpty()) {
                    natijalar.addView(bolimSarlavha("Barcha datchiklar"))
                    natijalar.addView(panjara(datchiklar.map { (nom, q) -> plitka("", nom, q).first }))
                }

                // Mashina
                natijalar.addView(bolimSarlavha("Mashina"))
                natijalar.addView(royxatKarta(listOf(
                    "VIN" to (vin ?: "o'qilmadi"),
                    "Protokol" to protokol,
                    "Skaner" to versiya.ifBlank { "—" },
                )))
            }
        } finally {
            elm.yop()
        }
    }

    // ------------------------------------------------------------ jonli
    private fun jonliAlmashtir() {
        if (jonli) { jonli = false; return }
        fonda {
            ui { natijalar.removeAllViews(); holat("Skanerga ulanmoqda...", XIRA); chip("Ulanmoqda", SARIQ) }
            val elm = yangiElm()
            try {
                elm.boshlash()
                if (elm.mashinaBilanAloqa() == null) {
                    ui { chip("Mashina javob bermadi", QIZIL); holat("Mashina javob bermadi.", QIZIL) }
                    return@fonda
                }
                jonli = true
                val tanlangan = DATCHIKLAR.filter {
                    it.pid in setOf("0105", "010D", "0111", "0104", "0106", "0107", "0142", "010F")
                }
                val qiymatlar = arrayOfNulls<TextView>(tanlangan.size)
                ui {
                    chip("Jonli", YASHIL)
                    jonliMatn.text = "To'xtatish"
                    jonliBtn.isEnabled = true
                    jonliBtn.alpha = 1f
                    holat("Jonli rejim: qiymatlar uzluksiz yangilanadi", YASHIL)
                    gauge.rang = FIRUZA; gauge.izoh = "ayl/min"; gauge.matn = "0"; gauge.qiymat = 0f
                    natijalar.addView(bolimSarlavha("Jonli datchiklar"))
                    val plitkalar = tanlangan.mapIndexed { i, d ->
                        val (v, q) = plitka("", d.nom, "…")
                        qiymatlar[i] = q
                        v
                    }
                    natijalar.addView(panjara(plitkalar))
                }
                while (jonli) {
                    val rpm = elm.pid("010C")?.let { (it[0] * 256 + it[1]) / 4 }
                    ui {
                        if (rpm != null) {
                            gauge.matn = rpm.toString()
                            gauge.qiymat = rpm / 7000f
                            gauge.rang = if (rpm > 5000) QIZIL else FIRUZA
                        }
                    }
                    tanlangan.forEachIndexed { i, d ->
                        if (!jonli) return@forEachIndexed
                        val a = elm.pid(d.pid)
                        val q = if (a == null) "—" else try { "${d.f(a)} ${d.birlik}" } catch (_: Exception) { "—" }
                        ui { qiymatlar[i]?.text = q }
                    }
                }
            } finally {
                jonli = false
                elm.yop()
                ui {
                    jonliMatn.text = "Jonli"
                    chip("Uzildi", XIRA)
                    gauge.qiymat = 0f; gauge.matn = "?"; gauge.izoh = "to'xtatildi"; gauge.rang = KOK
                    holat("Jonli rejim to'xtatildi", XIRA)
                }
            }
        }
    }

    // ------------------------------------------------------------ o'chirish
    private fun ochirishSorash() {
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Xatolarni o'chirish")
            .setMessage("Saqlangan xato kodlari va 'chek' chirog'i o'chiriladi.\n\n" +
                "Sabab tuzatilmagan bo'lsa, chiroq yana yonadi. Ustaga ko'rsatishdan oldin o'chirmang: " +
                "u xatolarni ko'rishi kerak.\n\nMotor o'chiq, kalit ON holatida bo'lsin.")
            .setPositiveButton("O'chirish") { _, _ -> ochirish() }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

    private fun ochirish() = fonda {
        ui { holat("Xatolar o'chirilmoqda...", XIRA); chip("Ulanmoqda", SARIQ) }
        val elm = yangiElm()
        try {
            elm.boshlash()
            if (elm.mashinaBilanAloqa() == null) {
                ui { chip("Mashina javob bermadi", QIZIL); holat("Mashina javob bermadi.", QIZIL) }
                return@fonda
            }
            val ok = elm.xatolarniOchir()
            ui {
                chip("Mashina ulandi", YASHIL)
                if (ok) holat("Xatolar o'chirildi. Qayta tekshirib ko'ring.", YASHIL)
                else holat("Mashina o'chirish buyrug'ini qabul qilmadi. Motorni o'chirib, kalitni ON qilib qayta urinib ko'ring.", QIZIL)
            }
        } finally {
            elm.yop()
        }
    }

    // ------------------------------------------------------------ ulashish
    private fun ulashish() {
        if (oxirgiHisobot.isEmpty()) {
            holat("Avval tekshiruv o'tkazing.", SARIQ)
            return
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, oxirgiHisobot)
        }, "Hisobotni yuborish"))
    }

    private fun hisobot(
        vaqt: String, versiya: String, akk: String, protokol: String, vin: String?, chek: Boolean?,
        saqlangan: List<String>, kutilayotgan: List<String>, doimiy: List<String>,
        datchiklar: Map<String, String>,
    ): String = buildString {
        appendLine("AVTO SKANER HISOBOTI — $vaqt")
        appendLine("VIN: ${vin ?: "o'qilmadi"}")
        appendLine("Protokol: $protokol")
        appendLine("Akkumulyator: $akk | Skaner: $versiya")
        appendLine("'Chek' chirog'i: ${when (chek) { true -> "YONGAN"; false -> "o'chiq"; null -> "noma'lum" }}")
        appendLine()
        fun bolim(sarlavha: String, l: List<String>) {
            appendLine("$sarlavha: ${if (l.isEmpty()) "yo'q" else ""}")
            l.forEach { appendLine("  $it — ${Kodlar.izoh(it)}") }
        }
        bolim("Saqlangan xatolar", saqlangan)
        bolim("Kutilayotgan xatolar", kutilayotgan)
        if (doimiy.isNotEmpty()) bolim("Doimiy xatolar", doimiy)
        appendLine()
        appendLine("Datchiklar:")
        datchiklar.forEach { (k, v) -> appendLine("  $k: $v") }
    }

    // ------------------------------------------------------------ UI yordamchilar
    private fun ui(f: () -> Unit) = runOnUiThread(f)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun holat(m: String, rang: Int) {
        holat.text = m
        holat.setTextColor(rang)
    }

    private fun chip(m: String, rang: Int) {
        ulanishChip.text = "●  $m"
        ulanishChip.setTextColor(rang)
        ulanishChip.background = shakl(Color.argb(40, Color.red(rang), Color.green(rang), Color.blue(rang)),
            dp(20).toFloat(), Color.argb(110, Color.red(rang), Color.green(rang), Color.blue(rang)))
    }

    private fun tugmalar(yoq: Boolean) {
        for (v in listOf<View>(asosiyBtn, tozalashBtn, ulashBtn)) {
            v.isEnabled = yoq
            v.alpha = if (yoq) 1f else 0.45f
        }
        asosiyBtn.text = if (yoq) "Tekshirishni boshlash" else "Bajarilmoqda..."
        jonliBtn.isEnabled = yoq || jonli
        jonliBtn.alpha = if (yoq || jonli) 1f else 0.45f
    }

    private fun matn(m: String, size: Float, rang: Int, shrift: String = "sans-serif") = TextView(this).apply {
        text = m
        textSize = size
        setTextColor(rang)
        typeface = Typeface.create(shrift, Typeface.NORMAL)
    }

    private fun shakl(rang: Int, radius: Float, chegara: Int? = null) = GradientDrawable().apply {
        setColor(rang)
        cornerRadius = radius
        if (chegara != null) setStroke(dp(1), chegara)
    }

    private fun gradient(a: Int, b: Int, radius: Float) =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(a, b)).apply {
            cornerRadius = radius
            setStroke(dp(1), CHEGARA)
        }

    private fun amal(belgi: String, nom: String, bosish: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        background = shakl(KARTA, dp(18).toFloat(), CHEGARA)
        isClickable = true
        addView(matn(belgi, 20f, OQ).apply { gravity = Gravity.CENTER })
        addView(matn(nom, 13f, OQ, "sans-serif-medium").apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        })
        setOnClickListener { bosish() }
    }

    private fun bolimSarlavha(m: String) = matn(m.uppercase(), 12f, XIRA, "sans-serif-medium").apply {
        letterSpacing = 0.14f
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24); bottomMargin = dp(10) }
    }

    /** Ko'rsatkich plitkasi: (plitka, qiymat matni). */
    private fun plitka(belgi: String, nom: String, qiymat: String): Pair<View, TextView> {
        val k = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = shakl(KARTA, dp(18).toFloat(), CHEGARA)
        }
        if (belgi.isNotEmpty()) k.addView(matn(belgi, 18f, OQ))
        k.addView(matn(nom, 12f, XIRA).apply { setPadding(0, if (belgi.isNotEmpty()) dp(6) else 0, 0, dp(4)) })
        val v = matn(qiymat, 19f, OQ, "sans-serif-medium")
        k.addView(v)
        return k to v
    }

    private fun panjara(plitkalar: List<View>): LinearLayout {
        val p = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        plitkalar.chunked(2).forEachIndexed { i, juft ->
            val q = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            juft.forEachIndexed { j, v ->
                q.addView(v, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (j == 1) marginStart = dp(10)
                })
            }
            if (juft.size == 1) q.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f).apply { marginStart = dp(10) })
            p.addView(q, LinearLayout.LayoutParams(-1, -2).apply { if (i > 0) topMargin = dp(10) })
        }
        return p
    }

    private fun xatoKarta(kod: String, turi: String): View {
        val rang = when (Kodlar.jiddiylik(kod)) { 2 -> QIZIL; 1 -> SARIQ; else -> YASHIL }
        val darajasi = when (Kodlar.jiddiylik(kod)) { 2 -> "Jiddiy"; 1 -> "Tekshirtiring"; else -> "Yengil" }
        val tashqi = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = shakl(KARTA, dp(18).toFloat(), CHEGARA)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
        }
        tashqi.addView(View(this).apply { background = shakl(rang, dp(3).toFloat()) },
            LinearLayout.LayoutParams(dp(5), ViewGroup.LayoutParams.MATCH_PARENT).apply {
                setMargins(dp(10), dp(14), 0, dp(14))
            })
        val ichi = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        val qator = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        qator.addView(matn(kod, 22f, OQ, "sans-serif-medium"), LinearLayout.LayoutParams(0, -2, 1f))
        qator.addView(matn(darajasi, 11f, rang, "sans-serif-medium").apply {
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = shakl(Color.argb(40, Color.red(rang), Color.green(rang), Color.blue(rang)), dp(12).toFloat())
        })
        ichi.addView(qator)
        ichi.addView(matn(Kodlar.izoh(kod), 14f, Color.parseColor("#C9D3EA")).apply {
            setPadding(0, dp(6), 0, dp(4)); setLineSpacing(dp(2).toFloat(), 1f)
        })
        ichi.addView(matn("$turi xato", 11f, XIRA))
        tashqi.addView(ichi, LinearLayout.LayoutParams(0, -2, 1f))
        return tashqi
    }

    private fun boshKarta(belgi: String, sarlavha: String, izoh: String) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = shakl(Color.argb(30, 34, 197, 94), dp(18).toFloat(), Color.argb(90, 34, 197, 94))
        addView(matn(belgi, 22f, OQ))
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
            addView(matn(sarlavha, 16f, OQ, "sans-serif-medium"))
            addView(matn(izoh, 13f, XIRA))
        })
    }

    private fun royxatKarta(qatorlar: List<Pair<String, String>>) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(8), dp(16), dp(8))
        background = shakl(KARTA, dp(18).toFloat(), CHEGARA)
        qatorlar.forEachIndexed { i, (k, v) ->
            if (i > 0) addView(View(this@MainActivity).apply { setBackgroundColor(CHEGARA) },
                LinearLayout.LayoutParams(-1, dp(1)))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(12), 0, dp(12))
                addView(matn(k, 13f, XIRA), LinearLayout.LayoutParams(0, -2, 1f))
                addView(matn(v, 13f, OQ, "sans-serif-medium").apply { gravity = Gravity.END },
                    LinearLayout.LayoutParams(0, -2, 2f))
            })
        }
    }

    private fun maydon(qiymat: String, ishora: String, tur: Int) = EditText(this).apply {
        setText(qiymat)
        hint = ishora
        inputType = tur
        textSize = 15f
        setTextColor(OQ)
        setHintTextColor(XIRA)
        background = null
        setPadding(0, dp(2), 0, dp(2))
        setSingleLine(true)
    }

    private fun maydonBlok(nom: String, e: EditText) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(matn(nom, 11f, XIRA, "sans-serif-medium"))
        addView(e)
    }
}
