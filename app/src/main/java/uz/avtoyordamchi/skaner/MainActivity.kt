package uz.avtoyordamchi.skaner

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar
import java.util.Locale
import javax.net.SocketFactory

class MainActivity : Activity() {

    private lateinit var k: Kit
    private lateinit var profil: Profil
    private lateinit var kontent: FrameLayout
    private val navlar = mutableListOf<LinearLayout>()

    private var ekran = 0
    private var joriy: Tekshiruv? = null
    private var holatMatni: TextView? = null
    private var ulanishChip: TextView? = null
    private var chipMatn = "Ulanmagan"
    private var chipRang = R2.XIRA

    @Volatile private var band = false
    @Volatile private var jonli = false
    @Volatile private var sinov100 = false

    private val NAV = listOf(
        "uy" to "Asosiy", "lupa" to "Diagnostika", "grafik" to "Jonli", "royxat" to "Tarix", "asboblar" to "Garaj",
    )

    // ============================================================ hayot sikli
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = R2.FON1
        window.navigationBarColor = R2.KARTA
        k = Kit(this)
        profil = Profil(this)
        joriy = Tarix.royxat(this).firstOrNull()

        val ildiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(R2.FON1, R2.FON2))
        }
        kontent = FrameLayout(this)
        ildiz.addView(kontent, LinearLayout.LayoutParams(-1, 0, 1f))
        ildiz.addView(navPanel(), LinearLayout.LayoutParams(-1, -2))
        setContentView(ildiz)
        korsat(0)
    }

    override fun onDestroy() {
        jonli = false; sinov100 = false
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (ekran != 0) korsat(0) else super.onBackPressed()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            xabar("Ruxsat berildi. Qaytadan urinib ko'ring.")
        } else {
            xabar("Bluetooth ruxsatisiz skanerga ulanib bo'lmaydi")
        }
    }

    // ============================================================ navigatsiya
    private fun navPanel(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(k.dp(8), k.dp(8), k.dp(8), k.dp(10))
            background = GradientDrawable().apply {
                setColor(R2.KARTA)
                setStroke(k.dp(1), R2.CHEGARA)
            }
            elevation = k.dpf(16)
        }
        NAV.forEachIndexed { i, (ikonka, nom) ->
            val t = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, k.dp(7), 0, k.dp(6))
                addView(k.ikonka(ikonka, 26))
                addView(k.matn(nom, 10.5f, R2.XIRA, Kit.Vazn.QALIN).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, k.dp(4), 0, 0)
                })
                isClickable = true
                setOnClickListener { korsat(i) }
            }
            navlar.add(t)
            panel.addView(t, LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginStart = k.dp(2); marginEnd = k.dp(2)
            })
        }
        return panel
    }

    private fun korsat(i: Int) {
        if (i != 2) { jonli = false; sinov100 = false }
        ekran = i
        navlar.forEachIndexed { j, t ->
            val faol = j == i
            t.background = if (faol) k.shakl(R2.shaffof(R2.KOK, 46), 16, R2.shaffof(R2.KOK, 90)) else null
            t.getChildAt(0).alpha = if (faol) 1f else 0.55f
            (t.getChildAt(1) as TextView).setTextColor(if (faol) R2.FIRUZA else R2.XIRA)
        }
        holatMatni = null
        ulanishChip = null
        val ichki = when (i) {
            0 -> asosiyEkran()
            1 -> diagnostikaEkran()
            2 -> jonliEkran()
            3 -> tarixEkran()
            else -> garajEkran()
        }
        ichki.setPadding(k.dp(18), k.dp(22), k.dp(18), k.dp(28))
        kontent.removeAllViews()
        kontent.addView(ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            addView(ichki)
        })
        ichki.alpha = 0f
        ichki.translationY = k.dpf(12)
        ichki.animate().alpha(1f).translationY(0f).setDuration(260).start()
    }

    // ============================================================ umumiy qismlar
    private fun ui(f: () -> Unit) = runOnUiThread(f)

    private fun xabar(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun holat(m: String, rang: Int) {
        holatMatni?.let { it.text = m; it.setTextColor(rang); it.visibility = View.VISIBLE }
    }

    private fun holatYasa(): TextView = k.matn("", 13.5f, R2.XIRA, Kit.Vazn.ORTA).apply {
        visibility = View.GONE
        setLineSpacing(k.dpf(3), 1f)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = k.dp(14) }
        holatMatni = this
    }

    private fun chip(m: String, rang: Int) {
        chipMatn = m; chipRang = rang
        ulanishChip?.let {
            it.text = "●  $m"
            it.setTextColor(rang)
            it.background = k.shakl(R2.shaffof(rang, 36), 14, R2.shaffof(rang, 100))
        }
    }

    private fun sarlavha(kichik: String, katta: String, ong: View? = null): View {
        val q = k.qator()
        val u = k.ustun()
        u.addView(k.matn(kichik.uppercase(), 11f, R2.FIRUZA, Kit.Vazn.QALIN).apply { letterSpacing = 0.18f })
        u.addView(k.matn(katta, 27f, R2.OQ, Kit.Vazn.QALIN).apply { setPadding(0, k.dp(4), 0, 0) })
        q.addView(u, k.lp(0, -2, 1f))
        if (ong != null) q.addView(ong)
        return q
    }

    private fun ulanishChipYasa(): TextView = k.chip("●  $chipMatn", chipRang, 12f).also {
        ulanishChip = it
        chip(chipMatn, chipRang)
    }

    private fun bosh(): View = k.bosh(1)

    private fun dialog(): AlertDialog.Builder = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)

    // ============================================================ 0. ASOSIY
    private fun asosiyEkran(): LinearLayout {
        val c = k.ustun()
        c.addView(sarlavha("Assalomu alaykum", "Avto Skaner", ulanishChipYasa()))

        // Mashina kartasi
        val mk = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(k.dp(16), k.dp(16), k.dp(16), k.dp(16))
            background = k.bosiladigan(k.gradient(intArrayOf(Color.parseColor("#1B2B5C"), R2.KARTA), 24, chegara = R2.CHEGARA), 24)
            isClickable = true
            setOnClickListener { korsat(4) }
        }
        mk.addView(k.ikonka("mashina", 78))
        val mu = k.ustun().apply { setPadding(k.dp(14), 0, 0, 0) }
        mu.addView(k.matn(profil.mashinaNomi, 19f, R2.OQ, Kit.Vazn.QALIN))
        val tafsilot = if (profil.toldirilgan) {
            listOfNotNull(if (profil.probeg > 0) "%,d km".format(Locale.US, profil.probeg).replace(',', ' ') else null, profil.yoqilgi)
                .joinToString("  ·  ")
        } else "Mashinangiz ma'lumotlarini kiriting"
        mu.addView(k.matn(tafsilot, 13f, R2.XIRA, Kit.Vazn.ORTA).apply { setPadding(0, k.dp(6), 0, 0) })
        mu.addView(k.chip(if (profil.toldirilgan) "Garaj  ›" else "Qo'shish  ›", R2.FIRUZA, 11f).apply {
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = k.dp(10) }
        })
        mk.addView(mu, k.lp(0, -2, 1f))
        c.addView(mk, LinearLayout.LayoutParams(-1, -2).apply { topMargin = k.dp(20) })

        // Holat o'lchagichi
        val hk = k.karta(18, R2.KARTA, 26).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            background = k.gradient(intArrayOf(Color.parseColor("#15224A"), R2.KARTA), 26, chegara = R2.CHEGARA)
        }
        hk.addView(k.matn("MASHINA HOLATI", 11f, R2.XIRA, Kit.Vazn.QALIN).apply { letterSpacing = 0.16f })
        val g = GaugeView(this, 250)
        hk.addView(g, LinearLayout.LayoutParams(-1, -2).apply { topMargin = k.dp(4) })
        val t = joriy
        if (t == null) {
            g.matn = "?"; g.izoh = "tekshirilmagan"; g.rang = R2.KOK
            hk.addView(k.matn("Skanerni ulang va birinchi tekshiruvni boshlang", 13f, R2.XIRA, Kit.Vazn.ORTA).apply { gravity = Gravity.CENTER })
        } else {
            g.matn = t.ball.toString(); g.izoh = R2.ballSozi(t.ball); g.rang = R2.ballRangi(t.ball)
            g.post { g.qiymat = t.ball / 100f }
            hk.addView(k.matn("Oxirgi tekshiruv: ${t.vaqtMatn}", 12.5f, R2.XIRA, Kit.Vazn.ORTA).apply { gravity = Gravity.CENTER })
            val st = k.qator().apply { setPadding(0, k.dp(16), 0, 0) }
            fun statistika(qiymat: String, nom: String, rang: Int) = k.ustun().apply {
                gravity = Gravity.CENTER
                addView(k.matn(qiymat, 18f, rang, Kit.Vazn.RAQAM).apply { gravity = Gravity.CENTER })
                addView(k.matn(nom, 11.5f, R2.XIRA, Kit.Vazn.ORTA).apply { gravity = Gravity.CENTER; setPadding(0, k.dp(4), 0, 0) })
            }
            st.addView(statistika(t.xatoSoni.toString(), "xato", if (t.xatoSoni > 0) R2.QIZIL else R2.YASHIL), k.lp(0, -2, 1f))
            st.addView(statistika(t.kutilayotgan.size.toString(), "kutilayotgan", if (t.kutilayotgan.isNotEmpty()) R2.SARIQ else R2.YASHIL), k.lp(0, -2, 1f))
            val v = t.akk.filter { it.isDigit() || it == '.' }
            st.addView(statistika(if (v.isBlank()) "—" else v, "volt", R2.FIRUZA), k.lp(0, -2, 1f))
            hk.addView(st, LinearLayout.LayoutParams(-1, -2))
        }
        c.addView(hk, LinearLayout.LayoutParams(-1, -2).apply { topMargin = k.dp(14) })

        c.addView(k.katta("Tekshirishni boshlash", "lupa") { tekshirish() },
            LinearLayout.LayoutParams(-1, k.dp(62)).apply { topMargin = k.dp(16) })
        c.addView(holatYasa())

        // Tez amallar
        c.addView(k.bolim("Tez amallar"))
        c.addView(k.panjara(listOf(
            k.amal("grafik", "Jonli datchiklar") { korsat(2) },
            k.amal("sekundomer", "0–100 sinovi") { korsat(2) },
            k.amal("hisobot", "PDF hisobot") { pdfUlash() },
            k.amal("ai", "AI xulosa") { aiYubor() },
        ), 2, 12))

        // Eslatmalar
        c.addView(k.bolim("Eslatmalar"))
        val eslatmalar = mutableListOf<View>()
        profil.moyQoldi?.let { km ->
            val r = when { km < 0 -> R2.QIZIL; km < 1000 -> R2.SARIQ; else -> R2.YASHIL }
            eslatmalar.add(eslatma("moy", "Moy almashtirish", if (km < 0) "${-km} km o'tib ketdi" else "$km km qoldi", r))
        }
        profil.qolganKun(profil.sugurta)?.let { kun ->
            val r = when { kun < 0 -> R2.QIZIL; kun < 15 -> R2.SARIQ; else -> R2.YASHIL }
            eslatmalar.add(eslatma("qalqon", "Sug'urta", if (kun < 0) "muddati o'tgan" else "$kun kun qoldi", r))
        }
        profil.qolganKun(profil.texosmotr)?.let { kun ->
            val r = when { kun < 0 -> R2.QIZIL; kun < 15 -> R2.SARIQ; else -> R2.YASHIL }
            eslatmalar.add(eslatma("kalendar", "Texnik ko'rik", if (kun < 0) "muddati o'tgan" else "$kun kun qoldi", r))
        }
        if (eslatmalar.isEmpty()) {
            val e = k.karta(16).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = k.bosiladigan(k.shakl(R2.KARTA, 22, R2.CHEGARA), 22)
                isClickable = true
                setOnClickListener { korsat(4) }
            }
            e.addView(k.ikonka("qongiroq", 40))
            val u = k.ustun().apply { setPadding(k.dp(14), 0, 0, 0) }
            u.addView(k.matn("Eslatmalarni yoqing", 15f, R2.OQ, Kit.Vazn.QALIN))
            u.addView(k.matn("Moy, sug'urta va texnik ko'rik muddatlarini Garajda kiriting", 12.5f, R2.XIRA, Kit.Vazn.ORTA).apply {
                setPadding(0, k.dp(4), 0, 0)
            })
            e.addView(u, k.lp(0, -2, 1f))
            c.addView(e)
        } else {
            eslatmalar.forEachIndexed { i, v ->
                c.addView(v, LinearLayout.LayoutParams(-1, -2).apply { if (i > 0) topMargin = k.dp(10) })
            }
        }

        // Maslahat
        c.addView(k.bolim("Kun maslahati"))
        val m = k.karta(16).apply {
            orientation = LinearLayout.HORIZONTAL
            background = k.gradient(intArrayOf(R2.shaffof(R2.BINAFSHA, 60), R2.KARTA), 22, chegara = R2.shaffof(R2.BINAFSHA, 90))
        }
        m.addView(k.ikonka("ai", 40))
        m.addView(k.matn(MASLAHATLAR[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % MASLAHATLAR.size], 13.5f, R2.MATN2, Kit.Vazn.ORTA).apply {
            setPadding(k.dp(14), 0, 0, 0)
            setLineSpacing(k.dpf(3), 1f)
        }, k.lp(0, -2, 1f))
        c.addView(m)
        return c
    }

    private fun eslatma(ikonka: String, nom: String, qiymat: String, rang: Int): View {
        val e = k.karta(14).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        e.addView(k.ikonkaDoira(ikonka, 46, rang))
        e.addView(k.matn(nom, 15f, R2.OQ, Kit.Vazn.QALIN).apply { setPadding(k.dp(14), 0, 0, 0) }, k.lp(0, -2, 1f))
        e.addView(k.chip(qiymat, rang, 12f))
        return e
    }

    // ============================================================ 1. DIAGNOSTIKA
    private fun diagnostikaEkran(): LinearLayout {
        val c = k.ustun()
        val t = joriy
        c.addView(sarlavha("Tekshiruv natijasi", "Diagnostika", ulanishChipYasa()))

        if (t == null) {
            c.addView(boshHolat("lupa", "Hali tekshiruv yo'q", "Skanerni mashinaga ulang, kalitni ON qiling va tekshiruvni boshlang."))
            c.addView(k.katta("Tekshirishni boshlash", "lupa") { tekshirish() },
                LinearLayout.LayoutParams(-1, k.dp(60)).apply { topMargin = k.dp(18) })
            c.addView(holatYasa())
            return c
        }

        // Xulosa kartasi
        val xk = k.karta(16, R2.KARTA, 26).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = k.gradient(intArrayOf(R2.shaffof(R2.ballRangi(t.ball), 50), R2.KARTA), 26, chegara = R2.CHEGARA)
        }
        val g = GaugeView(this, 130).apply {
            matn = t.ball.toString(); rang = R2.ballRangi(t.ball); izoh = ""
        }
        g.post { g.qiymat = t.ball / 100f }
        xk.addView(g, LinearLayout.LayoutParams(k.dp(130), -2))
        val xu = k.ustun().apply { setPadding(k.dp(12), 0, 0, 0) }
        xu.addView(k.matn(R2.ballSozi(t.ball).replaceFirstChar { it.uppercase() }, 20f, R2.ballRangi(t.ball), Kit.Vazn.QALIN))
        xu.addView(k.matn(t.mashina, 13f, R2.OQ, Kit.Vazn.ORTA).apply { setPadding(0, k.dp(6), 0, 0) })
        xu.addView(k.matn(t.vaqtMatn, 12f, R2.XIRA, Kit.Vazn.ORTA).apply { setPadding(0, k.dp(2), 0, 0) })
        val chiplar = k.qator().apply { setPadding(0, k.dp(10), 0, 0) }
        chiplar.addView(k.chip("${t.xatoSoni} xato", if (t.xatoSoni > 0) R2.QIZIL else R2.YASHIL))
        if (t.chek == true) chiplar.addView(k.chip("Chek yongan", R2.SARIQ).apply {
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginStart = k.dp(6) }
        })
        xu.addView(chiplar)
        xk.addView(xu, k.lp(0, -2, 1f))
        c.addView(xk, LinearLayout.LayoutParams(-1, -2).apply { topMargin = k.dp(20) })

        // Amallar
        c.addView(k.panjara(listOf(
            k.tugma("Qayta tekshirish", "lupa") { tekshirish() },
            k.tugma("PDF hisobot", "hisobot") { pdfUlash() },
            k.tugma("AI xulosa", "ai") { aiYubor() },
            k.tugma("Tozalash", "supurgi", R2.QIZIL) { tozalashSorash() },
        ).onEach { it.minimumHeight = k.dp(52) }, 2, 10).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = k.dp(14) }
        })
        c.addView(holatYasa())

        // Xato kodlari
        c.addView(k.bolim("Xato kodlari"))
        if (t.saqlangan.isEmpty() && t.kutilayotgan.isEmpty() && t.doimiy.isEmpty()) {
            val ok = k.karta(16).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = k.shakl(R2.shaffof(R2.YASHIL, 28), 22, R2.shaffof(R2.YASHIL, 90))
            }
            ok.addView(k.ikonka("tayyor", 44))
            val u = k.ustun().apply { setPadding(k.dp(14), 0, 0, 0) }
            u.addView(k.matn("Xato kodlari yo'q", 16f, R2.OQ, Kit.Vazn.QALIN))
            u.addView(k.matn("Motor bloki hech qanday xato saqlamagan", 12.5f, R2.XIRA, Kit.Vazn.ORTA).apply { setPadding(0, k.dp(4), 0, 0) })
            ok.addView(u, k.lp(0, -2, 1f))
            c.addView(ok)
        } else {
            t.saqlangan.forEach { c.addView(xatoKarta(it, "Saqlangan")) }
            t.doimiy.forEach { c.addView(xatoKarta(it, "Doimiy")) }
            t.kutilayotgan.forEach { c.addView(xatoKarta(it, "Kutilayotgan")) }
        }

        // Freeze frame
        if (t.muzlatilgan.isNotEmpty()) {
            c.addView(k.bolim("Xato paydo bo'lgan payt", t.muzlatilganKod?.let { k.chip(it, R2.SARIQ) }))
            val fk = k.karta(16).apply { setPadding(k.dp(16), k.dp(4), k.dp(16), k.dp(4)) }
            fk.addView(k.matn("Motor bloki xatoni yozgan lahzadagi holat (freeze frame)", 12.5f, R2.XIRA, Kit.Vazn.ORTA).apply {
                setPadding(0, k.dp(12), 0, k.dp(4))
            })
            t.muzlatilgan.forEach { (n, v) -> fk.addView(k.ajratgich()); fk.addView(k.juft(n, v)) }
            c.addView(fk)
        }

        // Tayyorlik monitorlari
        if (t.tayyorlik.isNotEmpty()) {
            val tugallanmagan = t.tayyorlik.count { !it.tayyor }
            c.addView(k.bolim("Tayyorlik monitorlari",
                k.chip("${t.tayyorlik.size - tugallanmagan}/${t.tayyorlik.size}", if (tugallanmagan == 0) R2.YASHIL else R2.SARIQ)))
            val mk = k.karta(16).apply { setPadding(k.dp(16), k.dp(4), k.dp(16), k.dp(4)) }
            t.tayyorlik.forEachIndexed { i, m ->
                if (i > 0) mk.addView(k.ajratgich())
                val q = k.qator().apply { setPadding(0, k.dp(11), 0, k.dp(11)) }
                q.addView(k.matn(m.nom, 13.5f, R2.MATN2, Kit.Vazn.ORTA), k.lp(0, -2, 1f))
                q.addView(k.chip(if (m.tayyor) "Tayyor" else "Tugallanmagan", if (m.tayyor) R2.YASHIL else R2.SARIQ, 11f))
                mk.addView(q)
            }
            c.addView(mk)
            if (tugallanmagan >= 3) {
                c.addView(k.matn("Ko'p monitor tugallanmagan: xatolar yaqinda o'chirilgan yoki akkumulyator uzilgan bo'lishi mumkin. " +
                    "Ishlatilgan mashina olayotgan bo'lsangiz, bunga e'tibor bering.", 12.5f, R2.SARIQ, Kit.Vazn.ORTA).apply {
                    setPadding(k.dp(4), k.dp(10), k.dp(4), 0); setLineSpacing(k.dpf(2), 1f)
                })
            }
        }

        // Asosiy ko'rsatkichlar
        if (t.datchiklar.isNotEmpty()) {
            c.addView(k.bolim("Asosiy ko'rsatkichlar"))
            val asosiy = listOf("010C", "0105", "0142", "0104", "0106", "0107")
                .mapNotNull { p -> datchik(p)?.let { d -> t.datchiklar[d.nom]?.let { v -> k.plitka(d.ikonka, d.nom, v).first } } }
            c.addView(k.panjara(asosiy))

            c.addView(k.bolim("Barcha datchiklar"))
            val bk = k.karta(16).apply { setPadding(k.dp(16), k.dp(2), k.dp(16), k.dp(2)) }
            t.datchiklar.entries.forEachIndexed { i, (n, v) ->
                if (i > 0) bk.addView(k.ajratgich())
                bk.addView(k.juft(n, v))
            }
            c.addView(bk)
        }

        // Mashina
        c.addView(k.bolim("Mashina"))
        val mk2 = k.karta(16).apply { setPadding(k.dp(16), k.dp(2), k.dp(16), k.dp(2)) }
        listOf("VIN" to (t.vin ?: "o'qilmadi"), "Protokol" to t.protokol, "Akkumulyator" to t.akk.ifBlank { "—" },
            "Skaner" to t.skaner.ifBlank { "—" }).forEachIndexed { i, (n, v) ->
            if (i > 0) mk2.addView(k.ajratgich())
            mk2.addView(k.juft(n, v))
        }
        c.addView(mk2)
        return c
    }

    private fun xatoKarta(kod: String, turi: String): View {
        val j = Kodlar.jiddiylik(kod)
        val rang = when (j) { 2 -> R2.QIZIL; 1 -> R2.SARIQ; else -> R2.YASHIL }
        val daraja = when (j) { 2 -> "Jiddiy"; 1 -> "Tekshirtiring"; else -> "Yengil" }
        val ikonka = when (j) { 2 -> "chiroq"; 1 -> "ogohlantirish"; else -> "kalit" }
        val e = k.karta(16).apply {
            orientation = LinearLayout.HORIZONTAL
            background = k.gradient(intArrayOf(R2.shaffof(rang, 34), R2.KARTA), 22, GradientDrawable.Orientation.LEFT_RIGHT, R2.shaffof(rang, 80))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = k.dp(10) }
        }
        e.addView(k.ikonka(ikonka, 40))
        val u = k.ustun().apply { setPadding(k.dp(14), 0, 0, 0) }
        val q = k.qator()
        q.addView(k.matn(kod, 20f, R2.OQ, Kit.Vazn.RAQAM), k.lp(0, -2, 1f))
        q.addView(k.chip(daraja, rang, 11f))
        u.addView(q)
        u.addView(k.matn(Kodlar.izoh(kod), 13.5f, R2.MATN2, Kit.Vazn.ORTA).apply {
            setPadding(0, k.dp(8), 0, k.dp(6)); setLineSpacing(k.dpf(2), 1f)
        })
        u.addView(k.matn("$turi xato", 11.5f, R2.XIRA, Kit.Vazn.QALIN))
        e.addView(u, k.lp(0, -2, 1f))
        return e
    }

    private fun boshHolat(ikonka: String, sarlavha: String, izoh: String): View = k.karta(24, R2.KARTA, 26).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = k.dp(20) }
        addView(k.ikonka(ikonka, 96))
        addView(k.matn(sarlavha, 18f, R2.OQ, Kit.Vazn.QALIN).apply { gravity = Gravity.CENTER; setPadding(0, k.dp(14), 0, 0) })
        addView(k.matn(izoh, 13.5f, R2.XIRA, Kit.Vazn.ORTA).apply {
            gravity = Gravity.CENTER; setPadding(0, k.dp(8), 0, 0); setLineSpacing(k.dpf(2), 1f)
        })
    }

    // ============================================================ 2. JONLI
    private fun jonliEkran(): LinearLayout {
        val c = k.ustun()
        c.addView(sarlavha("Real vaqt", "Jonli", ulanishChipYasa()))

        val gq = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val rpm = GaugeView(this, 175).apply { matn = "0"; birlik = "AYL/MIN"; izoh = "aylanish"; rang = R2.FIRUZA }
        val tezlik = GaugeView(this, 175).apply { matn = "0"; birlik = "KM/SOAT"; izoh = "tezlik"; rang = R2.KOK }
        val gk1 = k.karta(8, R2.KARTA, 24).apply { addView(rpm) }
        val gk2 = k.karta(8, R2.KARTA, 24).apply { addView(tezlik) }
        gq.addView(gk1, k.lp(0, -2, 1f))
        gq.addView(gk2, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = k.dp(10) })
        c.addView(gq, LinearLayout.LayoutParams(-1, -2).apply { topMargin = k.dp(20) })

        val boshlashBtn = k.katta(if (jonli) "To'xtatish" else "Jonli rejimni boshlash", "grafik") {}
        c.addView(boshlashBtn, LinearLayout.LayoutParams(-1, k.dp(60)).apply { topMargin = k.dp(14) })
        c.addView(holatYasa())

        // Grafikli plitkalar
        c.addView(k.bolim("Ko'rsatkichlar"))
        data class Jonli(val pid: String, val nom: String, val birlik: String, val ikonka: String, val rang: Int,
                         val qiymat: TextView, val grafik: SparkView)
        val royxat = listOf(
            Triple("0105", R2.SARIQ, "harorat"), Triple("0142", R2.FIRUZA, "akkumulyator"),
            Triple("0106", R2.BINAFSHA, "tomchi"), Triple("0107", R2.BINAFSHA, "tomchi"),
            Triple("0111", R2.KOK, "shamol"), Triple("0104", R2.YASHIL, "kuchlanish"),
            Triple("SARF", R2.QIZIL, "yoqilgi"), Triple("0110", R2.KOK, "shamol"),
        )
        val jonlilar = mutableListOf<Jonli>()
        val plitkalar = royxat.map { (pid, rang, ik) ->
            val d = datchik(pid)
            val nom = d?.nom ?: "Yoqilg'i sarfi"
            val birlik = d?.birlik ?: "L/100 km"
            val pl = k.karta(14, R2.KARTA, 20)
            val tepa = k.qator()
            tepa.addView(k.ikonka(ik, 22).apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = k.dp(8) })
            tepa.addView(k.matn(nom, 11.5f, R2.XIRA, Kit.Vazn.ORTA).apply { maxLines = 1 }, k.lp(0, -2, 1f))
            pl.addView(tepa)
            val qv = k.matn("—", 19f, R2.OQ, Kit.Vazn.RAQAM).apply { setPadding(0, k.dp(10), 0, k.dp(2)) }
            pl.addView(qv)
            pl.addView(k.matn(birlik, 10.5f, R2.XIRA, Kit.Vazn.QALIN))
            val sp = SparkView(this, rang)
            pl.addView(sp, LinearLayout.LayoutParams(-1, -2).apply { topMargin = k.dp(8) })
            jonlilar.add(Jonli(pid, nom, birlik, ik, rang, qv, sp))
            pl
        }
        c.addView(k.panjara(plitkalar))

        // 0–100 sinovi
        c.addView(k.bolim("0–100 km/soat sinovi"))
        val sk = k.karta(18, R2.KARTA, 26).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            background = k.gradient(intArrayOf(R2.shaffof(R2.QIZIL, 40), R2.KARTA), 26, chegara = R2.CHEGARA)
        }
        sk.addView(k.ikonka("sekundomer", 64))
        val vaqtMatn = k.matn("—.— s", 38f, R2.OQ, Kit.Vazn.RAQAM).apply { gravity = Gravity.CENTER; setPadding(0, k.dp(10), 0, 0) }
        sk.addView(vaqtMatn)
        val engMatn = k.matn(if (profil.eng100 > 0) "Eng yaxshi natija: %.2f s".format(Locale.US, profil.eng100) else "Hali natija yo'q",
            12.5f, R2.XIRA, Kit.Vazn.ORTA).apply { gravity = Gravity.CENTER; setPadding(0, k.dp(6), 0, k.dp(14)) }
        sk.addView(engMatn)
        val sinovBtn = k.tugma(if (sinov100) "Sinovni to'xtatish" else "Sinovni boshlash", "raketa") {}
        sk.addView(sinovBtn, LinearLayout.LayoutParams(-1, k.dp(52)))
        sk.addView(k.matn("Faqat yopiq, bo'sh va xavfsiz joyda sinang. Yo'l harakati qoidalariga rioya qiling.", 11.5f, R2.XIRA, Kit.Vazn.ORTA).apply {
            gravity = Gravity.CENTER; setPadding(0, k.dp(12), 0, 0)
        })
        c.addView(sk)

        // --- Jonli rejim boshqaruvi
        boshlashBtn.setOnClickListener {
            if (jonli) { jonli = false; return@setOnClickListener }
            if (sinov100) { xabar("Avval 0–100 sinovini to'xtating"); return@setOnClickListener }
            if (!ulanishTayyor()) return@setOnClickListener
            fonda {
                ui { holat("Skanerga ulanmoqda...", R2.XIRA); chip("Ulanmoqda", R2.SARIQ) }
                val elm = Elm(ulanishOch())
                try {
                    elm.boshlash()
                    if (elm.mashinaBilanAloqa() == null) {
                        ui { chip("Mashina javob bermadi", R2.QIZIL); holat("Mashina javob bermadi. Kalit ON holatidami?", R2.QIZIL) }
                        return@fonda
                    }
                    jonli = true
                    ui {
                        chip("Jonli", R2.YASHIL)
                        (boshlashBtn.getChildAt(1) as TextView).text = "To'xtatish"
                        holat("Qiymatlar uzluksiz yangilanmoqda", R2.YASHIL)
                    }
                    var maf: Double? = null
                    var tez: Int? = null
                    while (jonli) {
                        val ayl = elm.pid("010C")?.let { (it[0] * 256 + it[1]) / 4 }
                        tez = elm.pid("010D")?.let { it[0] }
                        ui {
                            if (ayl != null) { rpm.matn = ayl.toString(); rpm.qiymat = ayl / 7000f; rpm.rang = if (ayl > 5000) R2.QIZIL else R2.FIRUZA }
                            tez?.let { tezlik.matn = it.toString(); tezlik.qiymat = it / 200f }
                        }
                        for (j in jonlilar) {
                            if (!jonli) break
                            if (j.pid == "SARF") {
                                val m = maf; val s = tez
                                if (m != null && s != null) {
                                    val litrSoat = m * 3600.0 / (14.7 * 740.0)
                                    val qiymat = if (s > 3) litrSoat / s * 100.0 else litrSoat
                                    val birlik = if (s > 3) "L/100 km" else "L/soat"
                                    ui {
                                        j.qiymat.text = "%.1f".format(Locale.US, qiymat)
                                        ((j.qiymat.parent as LinearLayout).getChildAt(2) as TextView).text = birlik
                                        j.grafik.qosh(qiymat.toFloat())
                                    }
                                }
                                continue
                            }
                            val d = datchik(j.pid) ?: continue
                            val a = elm.pid(j.pid)
                            val q = if (a == null) null else try { d.f(a) } catch (_: Exception) { null }
                            if (j.pid == "0110") maf = q?.toDouble()
                            ui {
                                j.qiymat.text = q?.toString() ?: "—"
                                if (q != null) j.grafik.qosh(q.toFloat())
                            }
                        }
                    }
                } finally {
                    jonli = false
                    elm.yop()
                    ui {
                        (boshlashBtn.getChildAt(1) as TextView).text = "Jonli rejimni boshlash"
                        chip("Uzildi", R2.XIRA)
                        holat("Jonli rejim to'xtatildi", R2.XIRA)
                    }
                }
            }
        }

        // --- 0–100 sinovi boshqaruvi
        sinovBtn.setOnClickListener {
            if (sinov100) { sinov100 = false; return@setOnClickListener }
            if (jonli) { xabar("Avval jonli rejimni to'xtating"); return@setOnClickListener }
            if (!ulanishTayyor()) return@setOnClickListener
            fonda {
                ui { holat("Skanerga ulanmoqda...", R2.XIRA); chip("Ulanmoqda", R2.SARIQ) }
                val elm = Elm(ulanishOch())
                try {
                    elm.boshlash()
                    if (elm.mashinaBilanAloqa() == null) {
                        ui { chip("Mashina javob bermadi", R2.QIZIL); holat("Mashina javob bermadi.", R2.QIZIL) }
                        return@fonda
                    }
                    sinov100 = true
                    ui {
                        chip("Sinov", R2.YASHIL)
                        (sinovBtn.getChildAt(1) as TextView).text = "Sinovni to'xtatish"
                        holat("To'xtang (0 km/soat), keyin gazni bosing", R2.SARIQ)
                        vaqtMatn.text = "0.00 s"
                    }
                    var toxtadi = false
                    var t0 = 0L
                    while (sinov100) {
                        val s = elm.pid("010D")?.let { it[0] } ?: continue
                        val hozir = System.currentTimeMillis()
                        ui { tezlik.matn = s.toString(); tezlik.qiymat = s / 200f }
                        if (!toxtadi) {
                            if (s == 0) { toxtadi = true; ui { holat("Tayyor! Gazni bosing", R2.YASHIL) } }
                            continue
                        }
                        if (t0 == 0L) {
                            if (s > 0) t0 = hozir
                            continue
                        }
                        val otdi = (hozir - t0) / 1000f
                        ui { vaqtMatn.text = "%.2f s".format(Locale.US, otdi) }
                        if (s >= 100) {
                            val eng = profil.eng100
                            if (eng == 0f || otdi < eng) profil.eng100 = otdi
                            ui {
                                vaqtMatn.text = "%.2f s".format(Locale.US, otdi)
                                engMatn.text = "Eng yaxshi natija: %.2f s".format(Locale.US, profil.eng100)
                                holat(if (eng == 0f || otdi < eng) "Yangi rekord!" else "Sinov tugadi", R2.YASHIL)
                            }
                            sinov100 = false
                        }
                        if (otdi > 60f) { sinov100 = false; ui { holat("Sinov bekor qilindi (60 soniyadan oshdi)", R2.SARIQ) } }
                    }
                } finally {
                    sinov100 = false
                    elm.yop()
                    ui {
                        (sinovBtn.getChildAt(1) as TextView).text = "Sinovni boshlash"
                        chip("Uzildi", R2.XIRA)
                    }
                }
            }
        }
        return c
    }

    // ============================================================ 3. TARIX
    private fun tarixEkran(): LinearLayout {
        val c = k.ustun()
        val royxat = Tarix.royxat(this)
        c.addView(sarlavha("Barcha tekshiruvlar", "Tarix", k.chip("${royxat.size} ta", R2.FIRUZA, 12f)))

        if (royxat.isEmpty()) {
            c.addView(boshHolat("royxat", "Tarix bo'sh", "Har bir tekshiruv shu yerda saqlanadi. Mashina holati qanday o'zgarayotganini kuzatib borasiz."))
            return c
        }

        if (royxat.size >= 2) {
            c.addView(k.bolim("Holat dinamikasi"))
            val dk = k.karta(16)
            val q = k.qator()
            q.addView(k.matn("Ball o'zgarishi", 13.5f, R2.MATN2, Kit.Vazn.ORTA), k.lp(0, -2, 1f))
            val farq = royxat.first().ball - royxat.last().ball
            q.addView(k.chip(if (farq >= 0) "+$farq" else "$farq", if (farq >= 0) R2.YASHIL else R2.QIZIL))
            dk.addView(q)
            val sp = SparkView(this, R2.FIRUZA)
            royxat.reversed().forEach { sp.qosh(it.ball.toFloat()) }
            dk.addView(sp, LinearLayout.LayoutParams(-1, -2).apply { topMargin = k.dp(12) })
            c.addView(dk)
        }

        c.addView(k.bolim("Tekshiruvlar"))
        royxat.forEach { t ->
            val r = R2.ballRangi(t.ball)
            val e = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(k.dp(14), k.dp(14), k.dp(16), k.dp(14))
                background = k.bosiladigan(k.shakl(R2.KARTA, 22, R2.CHEGARA), 22)
                isClickable = true
                setOnClickListener { joriy = t; korsat(1) }
                setOnLongClickListener {
                    dialog().setTitle("Tekshiruvni o'chirish")
                        .setMessage("${t.vaqtMatn} dagi tekshiruv tarixdan o'chirilsinmi?")
                        .setPositiveButton("O'chirish") { _, _ ->
                            Tarix.ochir(this@MainActivity, t.vaqt)
                            if (joriy?.vaqt == t.vaqt) joriy = Tarix.royxat(this@MainActivity).firstOrNull()
                            korsat(3)
                        }
                        .setNegativeButton("Bekor qilish", null).show()
                    true
                }
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = k.dp(10) }
            }
            val doira = k.matn(t.ball.toString(), 16f, r, Kit.Vazn.RAQAM).apply {
                gravity = Gravity.CENTER
                background = k.shakl(R2.shaffof(r, 36), 26, R2.shaffof(r, 120))
            }
            e.addView(doira, LinearLayout.LayoutParams(k.dp(52), k.dp(52)))
            val u = k.ustun().apply { setPadding(k.dp(14), 0, 0, 0) }
            u.addView(k.matn(t.vaqtMatn, 15f, R2.OQ, Kit.Vazn.QALIN))
            u.addView(k.matn("${t.mashina}  ·  ${t.xatoSoni} xato", 12.5f, R2.XIRA, Kit.Vazn.ORTA).apply { setPadding(0, k.dp(4), 0, 0) })
            e.addView(u, k.lp(0, -2, 1f))
            e.addView(k.matn("›", 26f, R2.XIRA, Kit.Vazn.QALIN))
            c.addView(e)
        }
        c.addView(k.matn("Tekshiruvni o'chirish uchun ustiga bosib turing", 12f, R2.XIRA, Kit.Vazn.ORTA).apply {
            gravity = Gravity.CENTER; setPadding(0, k.dp(6), 0, 0)
        })
        return c
    }

    // ============================================================ 4. GARAJ
    private fun garajEkran(): LinearLayout {
        val c = k.ustun()
        c.addView(sarlavha("Mashina va sozlamalar", "Garaj"))

        val hero = k.karta(20, R2.KARTA, 26).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            background = k.gradient(intArrayOf(Color.parseColor("#1B2B5C"), R2.KARTA), 26, chegara = R2.CHEGARA)
        }
        hero.addView(k.ikonka("mashina", 110))
        hero.addView(k.matn(profil.mashinaNomi, 20f, R2.OQ, Kit.Vazn.QALIN).apply { gravity = Gravity.CENTER; setPadding(0, k.dp(8), 0, 0) })
        c.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { topMargin = k.dp(20) })

        // Mashina
        c.addView(k.bolim("Mashina"))
        val mk = k.karta(16)
        var model = profil.model
        var yoqilgi = profil.yoqilgi
        val modelT = k.tanlash(model.ifBlank { "Modelni tanlang  ›" }) {}
        modelT.setOnClickListener {
            dialog().setTitle("Mashina modeli").setItems(Profil.MODELLAR.toTypedArray()) { _, i ->
                model = Profil.MODELLAR[i]; modelT.text = model
            }.show()
        }
        mk.addView(k.maydonBlok("Model", modelT).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = 0 })
        val yilE = k.maydon(profil.yil, "Masalan: 2021", true)
        mk.addView(k.maydonBlok("Ishlab chiqarilgan yil", yilE))
        val probegE = k.maydon(if (profil.probeg > 0) profil.probeg.toString() else "", "Masalan: 85000", true)
        mk.addView(k.maydonBlok("Probeg (km)", probegE))
        val yoqT = k.tanlash(yoqilgi) {}
        yoqT.setOnClickListener {
            dialog().setTitle("Yoqilg'i turi").setItems(Profil.YOQILGILAR.toTypedArray()) { _, i ->
                yoqilgi = Profil.YOQILGILAR[i]; yoqT.text = yoqilgi
            }.show()
        }
        mk.addView(k.maydonBlok("Yoqilg'i turi", yoqT))
        c.addView(mk)

        // Xizmat
        c.addView(k.bolim("Xizmat eslatmalari"))
        val xk = k.karta(16)
        val moyE = k.maydon(if (profil.moyKm > 0) profil.moyKm.toString() else "", "Masalan: 80000", true)
        xk.addView(k.maydonBlok("Oxirgi moy almashtirilgan probeg (km)", moyE).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = 0 })
        val intE = k.maydon(profil.moyInterval.toString(), "8000", true)
        xk.addView(k.maydonBlok("Moy almashtirish oralig'i (km)", intE))
        var sugurta = profil.sugurta
        var texosmotr = profil.texosmotr
        val sugT = k.tanlash(sugurta.ifBlank { "Sanani tanlang  ›" }) {}
        sugT.setOnClickListener { sanaTanla(sugurta) { s -> sugurta = s; sugT.text = s } }
        xk.addView(k.maydonBlok("Sug'urta tugash sanasi", sugT))
        val texT = k.tanlash(texosmotr.ifBlank { "Sanani tanlang  ›" }) {}
        texT.setOnClickListener { sanaTanla(texosmotr) { s -> texosmotr = s; texT.text = s } }
        xk.addView(k.maydonBlok("Texnik ko'rik tugash sanasi", texT))
        c.addView(xk)

        // Ulanish
        c.addView(k.bolim("Skaner ulanishi"))
        val uk = k.karta(16)
        var turi = profil.ulanishTuri
        val wifiBlok = k.ustun()
        val hostE = k.maydon(profil.host, "192.168.0.10")
        val portE = k.maydon(profil.port.toString(), "35000", true)
        val wq = k.qator(false)
        wq.addView(k.maydonBlok("Manzil (IP)", hostE), k.lp(0, -2, 2f))
        wq.addView(k.maydonBlok("Port", portE), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = k.dp(10) })
        wifiBlok.addView(wq)
        wifiBlok.addView(k.matn("Ko'pchilik Wi-Fi skanerlar: 192.168.0.10 : 35000. Ba'zilarida 192.168.4.1.", 12f, R2.XIRA, Kit.Vazn.ORTA).apply {
            setPadding(k.dp(2), k.dp(10), 0, 0)
        })
        val btBlok = k.ustun()
        var btManzil = profil.btManzil
        var btNom = profil.btNom
        val btT = k.tanlash(if (btNom.isBlank()) "Skanerni tanlang  ›" else "$btNom  ›") {}
        btT.setOnClickListener { btTanla { nom, manzil -> btNom = nom; btManzil = manzil; btT.text = "$nom  ›" } }
        btBlok.addView(k.maydonBlok("Bluetooth skaner", btT))
        btBlok.addView(k.matn("Avval telefon sozlamalarida skanerni juftlang (PIN odatda 1234 yoki 0000).", 12f, R2.XIRA, Kit.Vazn.ORTA).apply {
            setPadding(k.dp(2), k.dp(10), 0, 0)
        })
        fun korinish() {
            wifiBlok.visibility = if (turi == "wifi") View.VISIBLE else View.GONE
            btBlok.visibility = if (turi == "bt") View.VISIBLE else View.GONE
        }
        uk.addView(k.segment(listOf("Wi-Fi", "Bluetooth"), if (turi == "bt") 1 else 0) { i ->
            turi = if (i == 1) "bt" else "wifi"; korinish()
        })
        uk.addView(wifiBlok)
        uk.addView(btBlok)
        korinish()
        c.addView(uk)

        c.addView(k.katta("Saqlash", "tayyor") {
            profil.model = model
            profil.yil = yilE.text.toString().trim()
            profil.probeg = probegE.text.toString().trim().toIntOrNull() ?: 0
            profil.yoqilgi = yoqilgi
            profil.moyKm = moyE.text.toString().trim().toIntOrNull() ?: 0
            profil.moyInterval = intE.text.toString().trim().toIntOrNull() ?: 8000
            profil.sugurta = sugurta
            profil.texosmotr = texosmotr
            profil.ulanishTuri = turi
            profil.host = hostE.text.toString().trim().ifBlank { "192.168.0.10" }
            profil.port = portE.text.toString().trim().toIntOrNull() ?: 35000
            profil.btManzil = btManzil
            profil.btNom = btNom
            xabar("Saqlandi")
            korsat(0)
        }, LinearLayout.LayoutParams(-1, k.dp(60)).apply { topMargin = k.dp(20) })

        // Ilova haqida
        c.addView(k.bolim("Ilova haqida"))
        val ak = k.karta(16).apply { setPadding(k.dp(16), k.dp(2), k.dp(16), k.dp(2)) }
        listOf("Versiya" to "0.3", "Shriftlar" to "Manrope, Orbitron (OFL)", "3D ikonkalar" to "Microsoft Fluent Emoji (MIT)")
            .forEachIndexed { i, (n, v) -> if (i > 0) ak.addView(k.ajratgich()); ak.addView(k.juft(n, v)) }
        c.addView(ak)
        return c
    }

    private fun sanaTanla(joriySana: String, tanlandi: (String) -> Unit) {
        val cal = Calendar.getInstance()
        if (joriySana.length == 10) try {
            cal.set(joriySana.substring(0, 4).toInt(), joriySana.substring(5, 7).toInt() - 1, joriySana.substring(8, 10).toInt())
        } catch (_: Exception) {}
        DatePickerDialog(this, android.R.style.Theme_Material_Dialog_Alert, { _, y, m, d ->
            tanlandi("%04d-%02d-%02d".format(Locale.US, y, m + 1, d))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    @SuppressLint("MissingPermission")
    private fun btTanla(tanlandi: (String, String) -> Unit) {
        if (!btRuxsat()) return
        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter == null) { xabar("Bu telefonda Bluetooth yo'q"); return }
        if (!adapter.isEnabled) { xabar("Avval Bluetooth'ni yoqing"); return }
        val qurilmalar = adapter.bondedDevices.toList()
        if (qurilmalar.isEmpty()) { xabar("Juftlangan qurilma yo'q. Telefon sozlamalarida skanerni juftlang."); return }
        val nomlar = qurilmalar.map { (it.name ?: "Noma'lum") + "\n" + it.address }.toTypedArray()
        dialog().setTitle("Skanerni tanlang").setItems(nomlar) { _, i ->
            tanlandi(qurilmalar[i].name ?: qurilmalar[i].address, qurilmalar[i].address)
        }.show()
    }

    // ============================================================ aloqa
    private fun btRuxsat(): Boolean {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 7)
            return false
        }
        return true
    }

    private fun ulanishTayyor(): Boolean {
        if (band) return false
        if (profil.ulanishTuri == "bt") {
            if (!btRuxsat()) return false
            if (profil.btManzil.isBlank()) {
                xabar("Garaj bo'limida Bluetooth skanerni tanlang")
                korsat(4)
                return false
            }
        }
        return true
    }

    @Suppress("DEPRECATION")
    private fun wifiFactory(): SocketFactory {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        for (n in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(n) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return n.socketFactory
        }
        return SocketFactory.getDefault()
    }

    private fun ulanishOch(): Ulanish {
        return if (profil.ulanishTuri == "bt") {
            val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                ?: throw Exception("Bu telefonda Bluetooth yo'q")
            if (!adapter.isEnabled) throw Exception("Bluetooth o'chiq")
            BtUlanish(adapter, profil.btManzil)
        } else {
            WifiUlanish(wifiFactory(), profil.host, profil.port)
        }
    }

    private fun fonda(ish: () -> Unit) {
        if (band) return
        band = true
        Thread {
            try {
                ish()
            } catch (e: Exception) {
                ui {
                    chip("Ulanmagan", R2.QIZIL)
                    val sabab = e.message ?: e.javaClass.simpleName
                    holat("Ulanib bo'lmadi: $sabab\n" + if (profil.ulanishTuri == "bt")
                        "Skaner yoqilganini va juftlanganini tekshiring." else
                        "Telefon skaner Wi-Fi'siga ulanganini, manzil va portni tekshiring.", R2.QIZIL)
                    if (holatMatni == null) xabar("Ulanib bo'lmadi: $sabab")
                }
            } finally {
                band = false
            }
        }.start()
    }

    // ============================================================ to'liq tekshiruv
    private fun tekshirish() {
        if (!ulanishTayyor()) return
        fonda {
            ui { holat("Skanerga ulanmoqda...", R2.XIRA); chip("Ulanmoqda", R2.SARIQ) }
            val elm = Elm(ulanishOch())
            try {
                val versiya = elm.boshlash()
                val akk = elm.kuchlanish()
                ui { chip("Skaner ulandi", R2.FIRUZA); holat("Mashina bilan aloqa o'rnatilmoqda (10–20 soniya)...", R2.XIRA) }
                val protokol = elm.mashinaBilanAloqa()
                if (protokol == null) {
                    ui {
                        chip("Mashina javob bermadi", R2.QIZIL)
                        holat("Mashina javob bermadi. Kalit ON holatidami? Eski mashinalarda OBD-II bo'lmasligi mumkin.", R2.QIZIL)
                    }
                    return@fonda
                }
                ui { chip("Mashina ulandi", R2.YASHIL); holat("Xato kodlari o'qilmoqda...", R2.XIRA) }
                val vin = elm.vin()
                val holatB = elm.pid("0101")
                val chek = holatB?.let { (it[0] and 0x80) != 0 }
                val saqlangan = elm.xatolar("03")
                val kutilayotgan = elm.xatolar("07")
                val doimiy = elm.xatolar("0A")

                ui { holat("Datchiklar o'qilmoqda...", R2.XIRA) }
                val datchiklar = linkedMapOf<String, String>()
                val sonlar = hashMapOf<String, Double>()
                for (d in DATCHIKLAR) {
                    val a = elm.pid(d.pid) ?: continue
                    val q = try { d.f(a) } catch (_: Exception) { continue }
                    datchiklar[d.nom] = "$q ${d.birlik}"
                    sonlar[d.pid] = q.toDouble()
                }

                ui { holat("Freeze frame va monitorlar o'qilmoqda...", R2.XIRA) }
                val mKod = if (saqlangan.isNotEmpty() || doimiy.isNotEmpty()) elm.muzlatilganKod() else null
                val muzlatilgan = linkedMapOf<String, String>()
                if (mKod != null) {
                    for (p in listOf("0105", "010C", "010D", "0104", "0106", "0107", "010B", "0111", "010F")) {
                        val d = datchik(p) ?: continue
                        val a = elm.muzlatilgan(p) ?: continue
                        val q = try { d.f(a) } catch (_: Exception) { continue }
                        muzlatilgan[d.nom] = "$q ${d.birlik}"
                    }
                }
                val tayyorlik = elm.tayyorlik()

                val akkV = akk.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                val ball = Tekshiruv.ballHisobla(saqlangan, kutilayotgan, doimiy, akkV, sonlar["0105"])
                val t = Tekshiruv(
                    vaqt = System.currentTimeMillis(), mashina = profil.mashinaNomi, vin = vin, protokol = protokol,
                    skaner = versiya, akk = akk, chek = chek, ball = ball, saqlangan = saqlangan,
                    kutilayotgan = kutilayotgan, doimiy = doimiy, datchiklar = datchiklar,
                    muzlatilganKod = mKod, muzlatilgan = muzlatilgan, tayyorlik = tayyorlik,
                )
                Tarix.saqla(this, t)
                joriy = t
                ui { korsat(1) }
            } finally {
                elm.yop()
            }
        }
    }

    // ============================================================ tozalash
    private fun tozalashSorash() {
        dialog()
            .setTitle("Xatolarni o'chirish")
            .setMessage("Saqlangan xato kodlari va 'chek' chirog'i o'chiriladi.\n\n" +
                "Sabab tuzatilmagan bo'lsa, chiroq yana yonadi. Ustaga ko'rsatishdan oldin o'chirmang: " +
                "u xatolarni ko'rishi kerak.\n\nMotor o'chiq, kalit ON holatida bo'lsin.")
            .setPositiveButton("O'chirish") { _, _ -> tozalash() }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

    private fun tozalash() {
        if (!ulanishTayyor()) return
        fonda {
            ui { holat("Xatolar o'chirilmoqda...", R2.XIRA); chip("Ulanmoqda", R2.SARIQ) }
            val elm = Elm(ulanishOch())
            try {
                elm.boshlash()
                if (elm.mashinaBilanAloqa() == null) {
                    ui { chip("Mashina javob bermadi", R2.QIZIL); holat("Mashina javob bermadi.", R2.QIZIL) }
                    return@fonda
                }
                val ok = elm.xatolarniOchir()
                ui {
                    chip("Mashina ulandi", R2.YASHIL)
                    if (ok) holat("Xatolar o'chirildi. Natijani ko'rish uchun qayta tekshiring.", R2.YASHIL)
                    else holat("Mashina buyruqni qabul qilmadi. Motorni o'chirib, kalitni ON qilib qayta urinib ko'ring.", R2.QIZIL)
                }
            } finally {
                elm.yop()
            }
        }
    }

    // ============================================================ ulashish
    private fun matnHisobot(t: Tekshiruv): String = buildString {
        appendLine("AVTO SKANER HISOBOTI — ${t.vaqtMatn}")
        appendLine("Mashina: ${t.mashina}" + if (profil.probeg > 0) ", probeg ${profil.probeg} km" else "")
        appendLine("Yoqilg'i: ${profil.yoqilgi}")
        appendLine("Holat bali: ${t.ball}/100 (${R2.ballSozi(t.ball)})")
        appendLine("VIN: ${t.vin ?: "o'qilmadi"} | Protokol: ${t.protokol}")
        appendLine("Akkumulyator: ${t.akk} | 'Chek': ${when (t.chek) { true -> "yongan"; false -> "o'chiq"; null -> "noma'lum" }}")
        appendLine()
        fun bolim(s: String, l: List<String>) {
            appendLine("$s: ${if (l.isEmpty()) "yo'q" else ""}")
            l.forEach { appendLine("  $it — ${Kodlar.izoh(it)}") }
        }
        bolim("Saqlangan xatolar", t.saqlangan)
        bolim("Kutilayotgan xatolar", t.kutilayotgan)
        if (t.doimiy.isNotEmpty()) bolim("Doimiy xatolar", t.doimiy)
        if (t.muzlatilgan.isNotEmpty()) {
            appendLine()
            appendLine("Freeze frame (${t.muzlatilganKod ?: "?"}):")
            t.muzlatilgan.forEach { (n, v) -> appendLine("  $n: $v") }
        }
        if (t.tayyorlik.isNotEmpty()) {
            appendLine()
            appendLine("Tayyorlik monitorlari:")
            t.tayyorlik.forEach { appendLine("  ${it.nom}: ${if (it.tayyor) "tayyor" else "tugallanmagan"}") }
        }
        appendLine()
        appendLine("Datchiklar:")
        t.datchiklar.forEach { (n, v) -> appendLine("  $n: $v") }
    }

    private fun pdfUlash() {
        val t = joriy ?: run { xabar("Avval tekshiruv o'tkazing"); return }
        try {
            val fayl = PdfHisobot.yarat(this, t, profil)
            val uri = PdfHisobot.uri(fayl)
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Avto Skaner hisoboti — ${t.mashina}")
                clipData = ClipData.newRawUri("", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(i, "PDF hisobotni yuborish"))
        } catch (e: Exception) {
            xabar("PDF yaratib bo'lmadi: ${e.message}")
        }
    }

    private fun aiYubor() {
        val t = joriy ?: run { xabar("Avval tekshiruv o'tkazing"); return }
        val savol = "Sen O'zbekistondagi tajribali avtomexaniksan. Quyidagi OBD skaner hisobotini oddiy o'zbek tilida tahlil qil: " +
            "umumiy xulosa va jiddiylik (xavfsiz / tez orada tekshirtirish / haydamaslik), har bir xato kodining shu modeldagi " +
            "eng ehtimoliy sabablari (arzonidan qimmatiga), so'mda taxminiy narx, datchiklardagi g'ayrioddiy qiymatlar va " +
            "ustaga nima deyish kerak.\n\n" + matnHisobot(t)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, savol)
        }, "AI ga yuborish (masalan, Claude)"))
    }

    companion object {
        private val MASLAHATLAR = listOf(
            "Skanerni mashinada uzoq ulab qoldirmang: u akkumulyatorni sekin-asta o'tirgizadi.",
            "'Chek' chirog'i miltillasa, motor jiddiy tutashmayapti: tezlikni pasaytiring va tezroq ustaga boring.",
            "Ishlatilgan mashina olishda tayyorlik monitorlariga qarang: ko'pi tugallanmagan bo'lsa, xatolar yaqinda o'chirilgan.",
            "Uzoq yoqilg'i tuzatish (LTFT) doimo +10% dan yuqori bo'lsa, ko'pincha havo so'rilishi yoki havo datchigi aybdor.",
            "Motor qizigan holatda radiator qopqog'ini ochmang: issiq suyuqlik bosim ostida otilib chiqadi.",
            "Metan uskunasi bor mashinada yoqilg'i tuzatish benzindagidan farq qiladi; tahlilda buni hisobga oling.",
            "Akkumulyator kuchlanishi motor ishlaganda 13.8–14.6 V bo'lishi kerak. Pastroq bo'lsa, generatorni tekshirtiring.",
            "Xatoni o'chirishdan oldin PDF hisobotni saqlang: usta keyin uni ko'rishi kerak bo'ladi.",
        )
    }
}
