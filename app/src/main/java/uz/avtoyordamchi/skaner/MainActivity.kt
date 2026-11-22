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
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.view.MotionEvent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
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
    private var tahrirNusxa: Mashina? = null
    private var tahrirYangi = false
    private var rasmKorinish: ImageView? = null
    private var ulanishChip: TextView? = null
    private var chipMatn = "Ulanmagan"
    private var chipRang = R2.XIRA

    @Volatile private var band = false
    @Volatile private var jonli = false
    @Volatile private var sinov100 = false

    private val NAV = listOf(
        "uy" to "Asosiy", "lupa" to "Diagnostika", "grafik" to "Jonli", "royxat" to "Tarix", "garaj" to "Garaj",
    )

    // ============================================================ hayot sikli
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profil = Profil(this)
        R2.mavzu(profil.qorongi)
        window.statusBarColor = R2.FON1
        window.navigationBarColor = R2.KARTA
        if (!R2.qorongi) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        k = Kit(this)
        Eslatmalar.rejalashtir(this)
        Thread { try { Eslatmalar.tekshir(this) } catch (_: Exception) {} }.start()
        if (Build.VERSION.SDK_INT >= 33 && !profil.bildirishnomaSorandi &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            profil.bildirishnomaSorandi = true
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 8)
        }
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
        if (ekran == 5) korsat(4) else if (ekran != 0) korsat(0) else super.onBackPressed()
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
                addView(k.ikonka(ikonka, 24, R2.XIRA))
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
        val navIndeks = if (i == 5) 4 else i
        navlar.forEachIndexed { j, t ->
            val faol = j == navIndeks
            t.background = if (faol) k.shakl(R2.shaffof(R2.KOK, if (R2.qorongi) 46 else 22), 16) else null
            (t.getChildAt(0) as ImageView).setColorFilter(if (faol) R2.KOK else R2.XIRA, PorterDuff.Mode.SRC_IN)
            (t.getChildAt(1) as TextView).setTextColor(if (faol) R2.KOK else R2.XIRA)
        }
        if (i != 5) rasmKorinish = null
        holatMatni = null
        ulanishChip = null
        val ichki = when (i) {
            0 -> asosiyEkran()
            1 -> diagnostikaEkran()
            2 -> jonliEkran()
            3 -> tarixEkran()
            4 -> garajEkran()
            else -> tahrirEkran()
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

    private fun dialogMavzu() = if (R2.qorongi) android.R.style.Theme_Material_Dialog_Alert else android.R.style.Theme_Material_Light_Dialog_Alert

    private fun dialog(): AlertDialog.Builder = AlertDialog.Builder(this, dialogMavzu())

    // ============================================================ 0. ASOSIY
    private fun asosiyEkran(): LinearLayout {
        val c = k.ustun()
        c.addView(sarlavha("Assalomu alaykum", "Avto Skaner", ulanishChipYasa()))

        // Mashinalar karuseli
        val royxat = profil.mashinalar()
        val muddatlar = k.ustun()
        c.addView(karusel(royxat) { m -> muddatlarniYangila(muddatlar, m) }, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = k.dp(18)
        })

        // Holat kartasi
        val hk = k.karta(14, R2.KARTA, 24).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val g = GaugeView(this, 120)
        val t = joriy
        if (t == null) {
            g.matn = "?"; g.izoh = ""; g.rang = R2.KOK
        } else {
            g.matn = t.ball.toString(); g.izoh = ""; g.rang = R2.ballRangi(t.ball)
            g.post { g.qiymat = t.ball / 100f }
        }
        hk.addView(g, LinearLayout.LayoutParams(k.dp(120), -2))
        val hu = k.ustun().apply { setPadding(k.dp(12), 0, 0, 0) }
        hu.addView(k.matn("Mashina holati", 12.5f, R2.XIRA, Kit.Vazn.ORTA))
        if (t == null) {
            hu.addView(k.matn("Tekshirilmagan", 19f, R2.OQ, Kit.Vazn.QALIN).apply { setPadding(0, k.dp(4), 0, 0) })
            hu.addView(k.matn("Skanerni ulang va birinchi tekshiruvni boshlang", 12.5f, R2.XIRA, Kit.Vazn.ORTA).apply {
                setPadding(0, k.dp(4), 0, 0)
            })
        } else {
            hu.addView(k.matn(R2.ballSozi(t.ball).replaceFirstChar { it.uppercase() }, 19f, R2.ballRangi(t.ball), Kit.Vazn.QALIN).apply {
                setPadding(0, k.dp(4), 0, 0)
            })
            hu.addView(k.matn("${t.xatoSoni} xato  ·  ${t.vaqtMatn}", 12.5f, R2.XIRA, Kit.Vazn.ORTA).apply { setPadding(0, k.dp(4), 0, 0) })
            hu.addView(k.chip("Batafsil  ›", R2.KOK, 11f).apply {
                layoutParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = k.dp(8) }
                setOnClickListener { korsat(1) }
            })
        }
        hk.addView(hu, k.lp(0, -2, 1f))
        c.addView(hk, LinearLayout.LayoutParams(-1, -2).apply { topMargin = k.dp(14) })

        c.addView(k.katta("Tekshirishni boshlash", "lupa") { tekshirish() },
            LinearLayout.LayoutParams(-1, k.dp(58)).apply { topMargin = k.dp(12) })
        c.addView(holatYasa())

        // Muddatlar
        c.addView(k.bolim("Muddatlar"))
        c.addView(muddatlar)
        muddatlarniYangila(muddatlar, profil.faol)

        // Tez amallar
        c.addView(k.bolim("Tez amallar"))
        c.addView(k.panjara(listOf(
            k.amal("grafik", "Jonli datchiklar", R2.KOK) { korsat(2) },
            k.amal("sekundomer", "0–100 sinovi", R2.QIZIL) { korsat(2) },
            k.amal("hisobot", "PDF hisobot", R2.YASHIL) { pdfUlash() },
            k.amal("ai", "AI xulosa", R2.BINAFSHA) { aiYubor() },
        ), 2, 10))

        // Maslahat
        c.addView(k.bolim("Kun maslahati"))
        val mk = k.karta(16).apply { orientation = LinearLayout.HORIZONTAL }
        mk.addView(k.ikonkaDoira("ai", 44, R2.BINAFSHA))
        mk.addView(k.matn(MASLAHATLAR[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % MASLAHATLAR.size], 13.5f, R2.MATN2, Kit.Vazn.ORTA).apply {
            setPadding(k.dp(14), 0, 0, 0)
            setLineSpacing(k.dpf(3), 1f)
        }, k.lp(0, -2, 1f))
        c.addView(mk)
        return c
    }

    /** Mashinalar karuseli: har sahifada bitta mashina, oxirida "qo'shish" sahifasi. */
    private fun karusel(royxat: List<Mashina>, tanlandi: (Mashina?) -> Unit): View {
        val kenglik = resources.displayMetrics.widthPixels - k.dp(36)
        val balandlik = k.dp(250)
        val hsv = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val qator = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        royxat.forEach { qator.addView(mashinaKarta(it), LinearLayout.LayoutParams(kenglik, balandlik)) }
        qator.addView(qoshishKarta(), LinearLayout.LayoutParams(kenglik, balandlik))
        hsv.addView(qator)

        val nuqtalar = k.qator().apply { gravity = Gravity.CENTER; setPadding(0, k.dp(12), 0, 0) }
        val sahifalar = royxat.size + 1
        repeat(sahifalar) { nuqtalar.addView(View(this), LinearLayout.LayoutParams(k.dp(7), k.dp(7)).apply { marginStart = k.dp(4); marginEnd = k.dp(4) }) }

        var joriySahifa = maxOf(0, royxat.indexOfFirst { it.id == profil.faolId })
        fun nuqtalarniBelgila() {
            for (i in 0 until sahifalar) {
                val v = nuqtalar.getChildAt(i)
                val faol = i == joriySahifa
                v.background = k.shakl(if (faol) R2.KOK else R2.CHEGARA, 4)
                v.layoutParams = (v.layoutParams as LinearLayout.LayoutParams).apply { width = k.dp(if (faol) 20 else 7) }
            }
        }
        nuqtalarniBelgila()

        hsv.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_CANCEL) {
                val farq = hsv.scrollX - joriySahifa * kenglik
                var yangi = joriySahifa
                if (farq > kenglik / 6) yangi++ else if (farq < -kenglik / 6) yangi--
                yangi = yangi.coerceIn(0, sahifalar - 1)
                hsv.post { hsv.smoothScrollTo(yangi * kenglik, 0) }
                if (yangi != joriySahifa) {
                    joriySahifa = yangi
                    nuqtalarniBelgila()
                    val m = royxat.getOrNull(yangi)
                    if (m != null) profil.faolId = m.id
                    tanlandi(m)
                }
            }
            false
        }
        hsv.post { hsv.scrollTo(joriySahifa * kenglik, 0) }

        return k.ustun().apply {
            addView(hsv, LinearLayout.LayoutParams(-1, balandlik))
            addView(nuqtalar)
        }
    }

    private fun mashinaKarta(m: Mashina): View {
        val e = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(k.dp(16), k.dp(14), k.dp(16), k.dp(16))
            background = k.bosiladigan(k.shakl(R2.KARTA, 28, R2.CHEGARA), 28)
            isClickable = true
            setOnClickListener { tahrirOch(m) }
        }
        val tepa = k.qator()
        tepa.addView(k.ikonkaDoira("mashina", 40, R2.MATN2))
        tepa.addView(k.matn(m.brend, 15f, R2.MATN2, Kit.Vazn.ORTA).apply { setPadding(k.dp(12), 0, 0, 0) }, k.lp(0, -2, 1f))
        if (m.id == profil.faol?.id && profil.mashinalar().size > 1) tepa.addView(k.chip("Faol", R2.KOK, 11f))
        e.addView(tepa)
        val rasm = ImageView(this).apply {
            setImageBitmap(MashinaRasm.ol(this@MainActivity, m))
            if (m.rasm.isNotBlank()) {
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = k.shakl(R2.KARTA2, 18)
                clipToOutline = true
            } else scaleType = ImageView.ScaleType.FIT_CENTER
        }
        e.addView(rasm, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = k.dp(8); bottomMargin = k.dp(8) })
        val past = k.qator().apply { gravity = Gravity.CENTER }
        if (m.raqamBor) past.addView(RaqamView(this, m.hudud, m.raqam, 170))
        else past.addView(k.matn(m.qisqaNom + "  ·  raqam qo'shish ›", 13.5f, R2.XIRA, Kit.Vazn.ORTA))
        e.addView(past, LinearLayout.LayoutParams(-1, -2))
        return e
    }

    private fun qoshishKarta(): View {
        val e = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(k.dp(16), k.dp(20), k.dp(16), k.dp(20))
            background = k.bosiladigan(k.shakl(R2.KARTA, 28, R2.CHEGARA), 28)
            isClickable = true
            setOnClickListener { tahrirOch(null) }
        }
        val rasm = ImageView(this).apply {
            setImageBitmap(MashinaRasm.siluet(this@MainActivity, "sedan", R2.CHEGARA))
            alpha = 0.45f
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        e.addView(rasm, LinearLayout.LayoutParams(-1, 0, 1f))
        val tugma = k.qator().apply {
            gravity = Gravity.CENTER
            setPadding(k.dp(18), k.dp(12), k.dp(20), k.dp(12))
            background = k.shakl(R2.KARTA2, 24, R2.CHEGARA)
            addView(k.ikonka("plus", 18, R2.OQ).apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = k.dp(8) })
            addView(k.matn("Avtomobil qo'shish", 15f, R2.OQ, Kit.Vazn.QALIN))
        }
        e.addView(tugma, LinearLayout.LayoutParams(-2, -2).apply { topMargin = k.dp(10) })
        return e
    }

    /** Muddatlar kartochkalari (tanlangan mashina uchun). */
    private fun muddatlarniYangila(kont: LinearLayout, m: Mashina?) {
        kont.removeAllViews()
        if (m == null) {
            val e = k.karta(16).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = k.bosiladigan(k.shakl(R2.KARTA, 22, R2.CHEGARA), 22)
                isClickable = true
                setOnClickListener { tahrirOch(null) }
            }
            e.addView(k.ikonkaDoira("qongiroq", 44, R2.KOK))
            val u = k.ustun().apply { setPadding(k.dp(14), 0, 0, 0) }
            u.addView(k.matn("Mashinangizni qo'shing", 15f, R2.OQ, Kit.Vazn.QALIN))
            u.addView(k.matn("Sug'urta, texnik ko'rik, tonirovka va moy muddatlarini eslatib turamiz", 12.5f, R2.XIRA, Kit.Vazn.ORTA).apply {
                setPadding(0, k.dp(4), 0, 0)
            })
            e.addView(u, k.lp(0, -2, 1f))
            kont.addView(e)
            return
        }
        fun sanaKarta(ikonka: String, nom: String, sana: String): View {
            val kun = Sana.qolganKun(sana)
            return if (kun == null) muddatKarta(ikonka, nom, "Ma'lumot yo'q", "Kiritish uchun bosing", R2.XIRA) { tahrirOch(m) }
            else muddatKarta(ikonka, nom, Sana.qolganMatn(kun), "${Sana.chiroyli(sana)} gacha",
                when { kun < 0 -> R2.QIZIL; kun < 15 -> R2.SARIQ; else -> R2.OQ }) { tahrirOch(m) }
        }
        val km = m.moyQoldi
        val moy = if (km == null) muddatKarta("moy", "Moy almashtirish", "Ma'lumot yo'q", "Probeg va oxirgi almashtirishni kiriting", R2.XIRA) { tahrirOch(m) }
        else muddatKarta("moy", "Moy almashtirish", if (km < 0) "${-km} km o'tdi" else "$km km qoldi", "har ${m.moyInterval} km",
            when { km < 0 -> R2.QIZIL; km < 1000 -> R2.SARIQ; else -> R2.OQ }) { tahrirOch(m) }
        kont.addView(k.panjara(listOf(
            sanaKarta("qalqon", "Sug'urta", m.sugurta),
            sanaKarta("kalendar", "Texnik ko'rik", m.texosmotr),
            sanaKarta("tonirovka", "Tonirovka", m.tonirovka),
            sanaKarta("ishonchnoma", "Ishonchnoma", m.ishonchnoma),
            moy,
            muddatKarta("lupa", "Diagnostika", joriy?.let { "${it.ball}/100" } ?: "Tekshirilmagan",
                joriy?.vaqtMatn ?: "Skaner bilan tekshiring", joriy?.let { R2.ballRangi(it.ball) } ?: R2.XIRA) { korsat(1) },
        ), 2, 10))
    }

    private fun muddatKarta(ikonka: String, nom: String, asosiy: String, izoh: String, rang: Int, bosish: () -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(k.dp(16), k.dp(16), k.dp(14), k.dp(16))
            minimumHeight = k.dp(136)
            background = k.bosiladigan(k.shakl(R2.KARTA, 24, R2.CHEGARA), 24)
            isClickable = true
            setOnClickListener { bosish() }
            val q = k.qator()
            q.addView(k.ikonka(ikonka, 20, R2.XIRA).apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = k.dp(8) })
            q.addView(k.matn(nom, 13.5f, R2.XIRA, Kit.Vazn.ORTA))
            addView(q)
            addView(View(this@MainActivity), LinearLayout.LayoutParams(1, 0, 1f))
            addView(k.matn(asosiy, 19f, if (rang == R2.XIRA) R2.OQ else rang, Kit.Vazn.QALIN).apply { setPadding(0, k.dp(18), 0, 0) })
            addView(k.matn(izoh, 12.5f, R2.XIRA, Kit.Vazn.ORTA).apply { setPadding(0, k.dp(4), 0, 0); maxLines = 2 })
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
            ok.addView(k.ikonkaDoira("tayyor", 46, R2.YASHIL))
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
        e.addView(k.ikonkaDoira(ikonka, 46, rang))
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
        addView(k.ikonkaDoira(ikonka, 88, R2.KOK))
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
            tepa.addView(k.ikonka(ik, 20, rang).apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = k.dp(8) })
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
        sk.addView(k.ikonkaDoira("sekundomer", 64, R2.QIZIL))
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
        val royxat = profil.mashinalar()
        c.addView(sarlavha("Mashinalar va sozlamalar", "Garaj", k.chip("${royxat.size} ta", R2.KOK, 12f)))

        // Mashinalar ro'yxati
        c.addView(k.bolim("Mashinalarim"))
        royxat.forEach { m ->
            val e = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(k.dp(12), k.dp(12), k.dp(14), k.dp(12))
                background = k.bosiladigan(k.shakl(R2.KARTA, 22, R2.CHEGARA), 22)
                isClickable = true
                setOnClickListener { tahrirOch(m) }
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = k.dp(10) }
            }
            val rasm = ImageView(this).apply {
                setImageBitmap(MashinaRasm.ol(this@MainActivity, m))
                scaleType = if (m.rasm.isNotBlank()) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
                background = k.shakl(R2.KARTA2, 14)
                clipToOutline = true
            }
            e.addView(rasm, LinearLayout.LayoutParams(k.dp(112), k.dp(64)))
            val u = k.ustun().apply { setPadding(k.dp(14), 0, 0, 0) }
            u.addView(k.matn(m.nomi, 15.5f, R2.OQ, Kit.Vazn.QALIN))
            if (m.raqamBor) u.addView(RaqamView(this, m.hudud, m.raqam, 118).apply {
                layoutParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = k.dp(8) }
            }) else u.addView(k.matn("Davlat raqami kiritilmagan", 12.5f, R2.XIRA, Kit.Vazn.ORTA).apply { setPadding(0, k.dp(6), 0, 0) })
            e.addView(u, k.lp(0, -2, 1f))
            e.addView(k.ikonka("oldinga", 20, R2.XIRA))
            c.addView(e)
        }
        c.addView(k.tugma("Mashina qo'shish", "plus") { tahrirOch(null) },
            LinearLayout.LayoutParams(-1, k.dp(54)))

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
        uk.addView(k.katta("Ulanishni saqlash", "tayyor") {
            profil.ulanishTuri = turi
            profil.host = hostE.text.toString().trim().ifBlank { "192.168.0.10" }
            profil.port = portE.text.toString().trim().toIntOrNull() ?: 35000
            profil.btManzil = btManzil
            profil.btNom = btNom
            xabar("Saqlandi")
        }, LinearLayout.LayoutParams(-1, k.dp(52)).apply { topMargin = k.dp(16) })
        c.addView(uk)

        // Ko'rinish
        c.addView(k.bolim("Ko'rinish"))
        val kk = k.karta(16)
        kk.addView(k.segment(listOf("Och", "To'q"), if (profil.qorongi) 1 else 0) { i ->
            val yangi = i == 1
            if (yangi != profil.qorongi) {
                profil.qorongi = yangi
                R2.mavzu(yangi)
                MashinaRasm.tozalaKesh()
                recreate()
            }
        })
        c.addView(kk)

        // Bildirishnomalar
        c.addView(k.bolim("Bildirishnomalar"))
        val bk = k.karta(16).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        bk.addView(k.ikonkaDoira("qongiroq", 44, R2.KOK))
        val ruxsat = Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val bu = k.ustun().apply { setPadding(k.dp(14), 0, 0, 0) }
        bu.addView(k.matn(if (ruxsat) "Eslatmalar yoqilgan" else "Eslatmalar o'chiq", 15f, R2.OQ, Kit.Vazn.QALIN))
        bu.addView(k.matn("Muddat tugashiga 30, 14, 7, 3 va 1 kun qolganda telefoningizga xabar keladi", 12.5f, R2.XIRA, Kit.Vazn.ORTA).apply {
            setPadding(0, k.dp(4), 0, 0)
        })
        bk.addView(bu, k.lp(0, -2, 1f))
        if (!ruxsat) bk.addView(k.chip("Yoqish", R2.KOK, 12f).apply {
            setOnClickListener { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 8) }
        })
        c.addView(bk)

        // Ilova haqida
        c.addView(k.bolim("Ilova haqida"))
        val ak = k.karta(16).apply { setPadding(k.dp(16), k.dp(2), k.dp(16), k.dp(2)) }
        listOf("Versiya" to "0.4", "Shriftlar" to "Manrope, Orbitron (OFL)", "Ikonkalar" to "Lucide (ISC)")
            .forEachIndexed { i, (n, v) -> if (i > 0) ak.addView(k.ajratgich()); ak.addView(k.juft(n, v)) }
        c.addView(ak)
        return c
    }

    // ============================================================ 5. MASHINA TAHRIRLASH
    private fun tahrirOch(m: Mashina?) {
        tahrirNusxa = m?.copy() ?: Mashina(id = System.currentTimeMillis())
        tahrirYangi = m == null
        korsat(5)
    }

    private fun tahrirEkran(): LinearLayout {
        val c = k.ustun()
        val m = tahrirNusxa ?: Mashina(id = System.currentTimeMillis()).also { tahrirNusxa = it; tahrirYangi = true }
        c.addView(sarlavha(if (tahrirYangi) "Yangi mashina" else "Tahrirlash", if (tahrirYangi) "Mashina qo'shish" else m.qisqaNom,
            k.chip("‹ Garaj", R2.KOK, 12f).apply { setOnClickListener { korsat(4) } }))

        // Ko'rinish
        val kk = k.karta(16, R2.KARTA, 28).apply { gravity = Gravity.CENTER_HORIZONTAL }
        val rasm = ImageView(this).apply { background = k.shakl(R2.KARTA2, 20); clipToOutline = true }
        rasmKorinish = rasm
        kk.addView(rasm, LinearLayout.LayoutParams(-1, k.dp(160)))
        val raqamV = RaqamView(this, m.hudud, m.raqam, 190)
        kk.addView(raqamV, LinearLayout.LayoutParams(-2, -2).apply { topMargin = k.dp(12) })
        val rq = k.qator().apply { setPadding(0, k.dp(14), 0, 0) }
        rq.addView(k.tugma("Rasm tanlash", "rasm") {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), 21)
        }, k.lp(0, k.dp(48), 1f))
        rq.addView(k.tugma("Siluet", "mashina") {
            m.rasm = ""; rasmniYangila()
        }, LinearLayout.LayoutParams(0, k.dp(48), 1f).apply { marginStart = k.dp(10) })
        kk.addView(rq, LinearLayout.LayoutParams(-1, -2))
        c.addView(kk, LinearLayout.LayoutParams(-1, -2).apply { topMargin = k.dp(18) })
        rasmniYangila()

        // Asosiy ma'lumotlar
        c.addView(k.bolim("Mashina"))
        val mk = k.karta(16)
        val modelT = k.tanlash(m.model.ifBlank { "Modelni tanlang  ›" }) {}
        modelT.setOnClickListener {
            dialog().setTitle("Mashina modeli").setItems(Profil.MODELLAR.toTypedArray()) { _, i ->
                m.model = Profil.MODELLAR[i]; modelT.text = m.model; rasmniYangila()
            }.show()
        }
        mk.addView(k.maydonBlok("Model", modelT).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = 0 })

        // Rang tanlash
        val ranglar = k.qator()
        val doiralar = mutableListOf<View>()
        fun ranglarniBelgila() {
            doiralar.forEachIndexed { i, v ->
                (v.background as? android.graphics.drawable.GradientDrawable)?.setStroke(k.dp(if (i == m.rang) 3 else 1),
                    if (i == m.rang) R2.KOK else R2.CHEGARA)
            }
        }
        Mashina.RANGLAR.forEachIndexed { i, (_, rang) ->
            val d = View(this).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(rang)
                }
                setOnClickListener { m.rang = i; ranglarniBelgila(); rasmniYangila() }
            }
            doiralar.add(d)
            ranglar.addView(d, LinearLayout.LayoutParams(k.dp(34), k.dp(34)).apply { marginEnd = k.dp(10) })
        }
        ranglarniBelgila()
        mk.addView(k.maydonBlok("Rang", HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(ranglar)
        }))

        val yilE = k.maydon(m.yil, "Masalan: 2021", true)
        val probegE = k.maydon(if (m.probeg > 0) m.probeg.toString() else "", "Masalan: 85000", true)
        val yq = k.qator(false)
        yq.addView(k.maydonBlok("Yil", yilE), k.lp(0, -2, 1f))
        yq.addView(k.maydonBlok("Probeg (km)", probegE), LinearLayout.LayoutParams(0, -2, 1.4f).apply { marginStart = k.dp(10) })
        mk.addView(yq)
        val yoqT = k.tanlash(m.yoqilgi) {}
        yoqT.setOnClickListener {
            dialog().setTitle("Yoqilg'i turi").setItems(Profil.YOQILGILAR.toTypedArray()) { _, i ->
                m.yoqilgi = Profil.YOQILGILAR[i]; yoqT.text = m.yoqilgi
            }.show()
        }
        mk.addView(k.maydonBlok("Yoqilg'i turi", yoqT))

        // Davlat raqami
        val hududE = k.maydon(m.hudud, "40", true)
        val raqamE = k.maydon(m.raqam, "404 KCA")
        val rqq = k.qator(false)
        rqq.addView(k.maydonBlok("Hudud", hududE), k.lp(0, -2, 1f))
        rqq.addView(k.maydonBlok("Davlat raqami", raqamE), LinearLayout.LayoutParams(0, -2, 2.6f).apply { marginStart = k.dp(10) })
        mk.addView(rqq)
        val kuzatuvchi = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                raqamV.hudud = hududE.text.toString().trim().take(2)
                raqamV.raqam = raqamE.text.toString().trim().uppercase()
                raqamV.invalidate()
            }
        }
        hududE.addTextChangedListener(kuzatuvchi)
        raqamE.addTextChangedListener(kuzatuvchi)
        c.addView(mk)

        // Muddatlar
        c.addView(k.bolim("Muddatlar va xizmat"))
        val xk = k.karta(16)
        val moyE = k.maydon(if (m.moyKm > 0) m.moyKm.toString() else "", "Masalan: 80000", true)
        val intE = k.maydon(m.moyInterval.toString(), "8000", true)
        val mq = k.qator(false)
        mq.addView(k.maydonBlok("Oxirgi moy (km)", moyE), k.lp(0, -2, 1.3f))
        mq.addView(k.maydonBlok("Oralig'i (km)", intE), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = k.dp(10) })
        xk.addView(mq)
        fun sanaMaydon(nom: String, ol: () -> String, qoy: (String) -> Unit) {
            val t = k.tanlash(if (ol().isBlank()) "Sanani tanlang  ›" else Sana.chiroyli(ol())) {}
            t.setOnClickListener { sanaTanla(ol()) { s -> qoy(s); t.text = Sana.chiroyli(s) } }
            xk.addView(k.maydonBlok(nom, t))
        }
        sanaMaydon("Sug'urta tugash sanasi", { m.sugurta }) { m.sugurta = it }
        sanaMaydon("Texnik ko'rik tugash sanasi", { m.texosmotr }) { m.texosmotr = it }
        sanaMaydon("Tonirovka ruxsati tugash sanasi", { m.tonirovka }) { m.tonirovka = it }
        sanaMaydon("Ishonchnoma tugash sanasi", { m.ishonchnoma }) { m.ishonchnoma = it }
        c.addView(xk)

        c.addView(k.katta("Saqlash", "tayyor") {
            m.yil = yilE.text.toString().trim()
            m.probeg = probegE.text.toString().trim().toIntOrNull() ?: 0
            m.hudud = hududE.text.toString().trim().take(2)
            m.raqam = raqamE.text.toString().trim().uppercase()
            m.moyKm = moyE.text.toString().trim().toIntOrNull() ?: 0
            m.moyInterval = intE.text.toString().trim().toIntOrNull() ?: 8000
            if (m.model.isBlank()) { xabar("Modelni tanlang"); return@katta }
            profil.saqla(m)
            if (tahrirYangi) profil.faolId = m.id
            tahrirNusxa = null
            xabar("Saqlandi")
            korsat(0)
        }, LinearLayout.LayoutParams(-1, k.dp(58)).apply { topMargin = k.dp(20) })

        if (!tahrirYangi) {
            c.addView(k.tugma("Mashinani o'chirish", "ochirish", R2.QIZIL) {
                dialog().setTitle("Mashinani o'chirish")
                    .setMessage("${m.nomi} garajdan o'chirilsinmi? Tekshiruvlar tarixi saqlanib qoladi.")
                    .setPositiveButton("O'chirish") { _, _ ->
                        if (m.rasm.isNotBlank()) java.io.File(m.rasm).delete()
                        profil.ochir(m.id)
                        tahrirNusxa = null
                        korsat(4)
                    }
                    .setNegativeButton("Bekor qilish", null).show()
            }, LinearLayout.LayoutParams(-1, k.dp(52)).apply { topMargin = k.dp(10) })
        }
        return c
    }

    private fun rasmniYangila() {
        val m = tahrirNusxa ?: return
        val r = rasmKorinish ?: return
        r.setImageBitmap(MashinaRasm.ol(this, m))
        r.scaleType = if (m.rasm.isNotBlank()) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 21 || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val m = tahrirNusxa ?: return
        Thread {
            try {
                val chegaralar = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, chegaralar) }
                var namuna = 1
                while (chegaralar.outWidth / namuna > 1600) namuna *= 2
                val bmp = contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = namuna })
                } ?: throw Exception("Rasm o'qilmadi")
                val fayl = java.io.File(filesDir, "mashina_${m.id}_${System.currentTimeMillis()}.jpg")
                java.io.FileOutputStream(fayl).use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
                ui { m.rasm = fayl.absolutePath; rasmniYangila() }
            } catch (e: Exception) {
                ui { xabar("Rasmni yuklab bo'lmadi: ${e.message}") }
            }
        }.start()
    }

    private fun sanaTanla(joriySana: String, tanlandi: (String) -> Unit) {
        val cal = Calendar.getInstance()
        if (joriySana.length == 10) try {
            cal.set(joriySana.substring(0, 4).toInt(), joriySana.substring(5, 7).toInt() - 1, joriySana.substring(8, 10).toInt())
        } catch (_: Exception) {}
        DatePickerDialog(this, dialogMavzu(), { _, y, m, d ->
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
        val fm = profil.faol
        appendLine("Mashina: ${t.mashina}" + if (fm != null && fm.probeg > 0) ", probeg ${fm.probeg} km" else "")
        appendLine("Yoqilg'i: ${fm?.yoqilgi ?: "noma'lum"}")
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
