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
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.SocketFactory

class MainActivity : Activity() {

    private val KOK = Color.parseColor("#1F4FD8")
    private val FON = Color.parseColor("#F3F5F9")
    private val QIZIL = Color.parseColor("#D93025")
    private val SARIQ = Color.parseColor("#E3A008")
    private val YASHIL = Color.parseColor("#188038")
    private val KULRANG = Color.parseColor("#5F6368")

    private lateinit var hostEdit: EditText
    private lateinit var portEdit: EditText
    private lateinit var holat: TextView
    private lateinit var natijalar: LinearLayout
    private lateinit var tekshirBtn: Button
    private lateinit var jonliBtn: Button
    private lateinit var ochirBtn: Button
    private lateinit var ulashBtn: Button

    @Volatile private var band = false
    @Volatile private var jonli = false
    private var oxirgiHisobot = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("sozlama", Context.MODE_PRIVATE)

        val ildiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(FON)
            setPadding(dp(16), dp(24), dp(16), dp(16))
        }

        ildiz.addView(TextView(this).apply {
            text = "Avto Skaner"
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#111827"))
        })
        ildiz.addView(TextView(this).apply {
            text = "Wi-Fi ELM327 orqali mashinani tekshirish"
            textSize = 14f
            setTextColor(KULRANG)
            setPadding(0, 0, 0, dp(12))
        })

        val manzilQator = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        hostEdit = EditText(this).apply {
            setText(prefs.getString("host", "192.168.0.10"))
            hint = "Skaner manzili"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            textSize = 15f
        }
        portEdit = EditText(this).apply {
            setText(prefs.getInt("port", 35000).toString())
            hint = "Port"
            inputType = InputType.TYPE_CLASS_NUMBER
            textSize = 15f
        }
        manzilQator.addView(hostEdit, LinearLayout.LayoutParams(0, -2, 2f))
        manzilQator.addView(portEdit, LinearLayout.LayoutParams(0, -2, 1f))
        ildiz.addView(manzilQator)

        tekshirBtn = tugma("Tekshirish", KOK) { tekshirish() }
        jonliBtn = tugma("Jonli datchiklar", Color.parseColor("#374151")) { jonliAlmashtir() }
        ochirBtn = tugma("Xatolarni o'chirish", QIZIL) { ochirishSorash() }
        ulashBtn = tugma("Ulashish", Color.parseColor("#374151")) { ulashish() }

        ildiz.addView(qator(tekshirBtn, jonliBtn))
        ildiz.addView(qator(ochirBtn, ulashBtn))

        holat = TextView(this).apply {
            text = "1) Skanerni ulang, kalitni ON qiling.\n2) Telefonni skaner Wi-Fi'siga ulang.\n3) \"Tekshirish\"ni bosing."
            textSize = 14f
            setTextColor(KULRANG)
            setPadding(0, dp(12), 0, dp(8))
        }
        ildiz.addView(holat)

        natijalar = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ildiz.addView(natijalar)

        setContentView(ScrollView(this).apply {
            setBackgroundColor(FON)
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
                    holat("❌ Ulanib bo'lmadi: ${e.message ?: e.javaClass.simpleName}\n" +
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
        ui { natijalar.removeAllViews(); holat("Skanerga ulanmoqda...", KULRANG) }
        val elm = yangiElm()
        try {
            val versiya = elm.boshlash()
            val akk = elm.kuchlanish()
            ui { holat("Skaner: $versiya. Mashina bilan aloqa o'rnatilmoqda (10–20 soniya)...", KULRANG) }
            val protokol = elm.mashinaBilanAloqa()
            if (protokol == null) {
                ui { holat("❌ Mashina javob bermadi. Kalit ON holatidami? Eski mashinalarda OBD-II bo'lmasligi mumkin.", QIZIL) }
                return@fonda
            }
            ui { holat("O'qilmoqda...", KULRANG) }
            val vin = elm.vin()
            val holatBayt = elm.pid("0101")
            val chek = holatBayt?.let { (it[0] and 0x80) != 0 }
            val saqlangan = elm.xatolar("03")
            val kutilayotgan = elm.xatolar("07")
            val doimiy = elm.xatolar("0A")
            val datchiklar = mutableListOf<Pair<String, String>>()
            for (d in DATCHIKLAR) {
                val a = elm.pid(d.pid) ?: continue
                val q = try { d.f(a) } catch (_: Exception) { continue }
                datchiklar.add(d.nom to "$q ${d.birlik}")
            }

            val vaqt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(Date())
            oxirgiHisobot = hisobot(vaqt, versiya, akk, protokol, vin, chek, saqlangan, kutilayotgan, doimiy, datchiklar)

            ui {
                val jami = saqlangan.size + doimiy.size
                when {
                    jami == 0 && kutilayotgan.isEmpty() -> holat("✅ Xato kodlari topilmadi", YASHIL)
                    jami == 0 -> holat("🟡 Kutilayotgan xatolar bor — kuzatib boring", SARIQ)
                    else -> holat("🔴 $jami ta xato kodi topildi", QIZIL)
                }
                karta("Umumiy") {
                    satr(it, "Vaqt", vaqt)
                    satr(it, "VIN", vin ?: "o'qilmadi")
                    satr(it, "Protokol", protokol)
                    satr(it, "Akkumulyator", akk)
                    satr(it, "'Chek' chirog'i", when (chek) { true -> "YONGAN"; false -> "o'chiq"; null -> "noma'lum" })
                }
                xatoKarta("Saqlangan xatolar", saqlangan)
                xatoKarta("Kutilayotgan xatolar", kutilayotgan)
                if (doimiy.isNotEmpty()) xatoKarta("Doimiy xatolar (o'chmaydi)", doimiy)
                karta("Datchiklar") { k ->
                    if (datchiklar.isEmpty()) satr(k, "—", "ma'lumot kelmadi")
                    datchiklar.forEach { satr(k, it.first, it.second) }
                }
            }
        } finally {
            elm.yop()
        }
    }

    // ------------------------------------------------------------ jonli
    private fun jonliAlmashtir() {
        if (jonli) { jonli = false; return }
        fonda {
            ui { natijalar.removeAllViews(); holat("Skanerga ulanmoqda...", KULRANG) }
            val elm = yangiElm()
            try {
                elm.boshlash()
                if (elm.mashinaBilanAloqa() == null) {
                    ui { holat("❌ Mashina javob bermadi.", QIZIL) }
                    return@fonda
                }
                jonli = true
                val tanlangan = DATCHIKLAR.filter {
                    it.pid in setOf("010C", "0105", "010D", "0106", "0107", "0111", "0142")
                }
                val qiymatlar = arrayOfNulls<TextView>(tanlangan.size)
                ui {
                    jonliBtn.text = "To'xtatish"
                    jonliBtn.isEnabled = true
                    holat("🟢 Jonli rejim — har soniyada yangilanadi", YASHIL)
                    karta("Jonli datchiklar") { k ->
                        tanlangan.forEachIndexed { i, d -> qiymatlar[i] = satr(k, d.nom, "...") }
                    }
                }
                while (jonli) {
                    tanlangan.forEachIndexed { i, d ->
                        val a = elm.pid(d.pid)
                        val q = if (a == null) "—" else try { "${d.f(a)} ${d.birlik}" } catch (_: Exception) { "—" }
                        ui { qiymatlar[i]?.text = q }
                    }
                    Thread.sleep(300)
                }
            } finally {
                jonli = false
                elm.yop()
                ui { jonliBtn.text = "Jonli datchiklar"; holat("Jonli rejim to'xtatildi", KULRANG) }
            }
        }
    }

    // ------------------------------------------------------------ o'chirish
    private fun ochirishSorash() {
        AlertDialog.Builder(this)
            .setTitle("Xatolarni o'chirish")
            .setMessage("Saqlangan xato kodlari va 'chek' chirog'i o'chiriladi.\n\n" +
                "Diqqat: sabab tuzatilmagan bo'lsa, chiroq yana yonadi. Ustaga ko'rsatishdan oldin " +
                "o'chirmang — u xatolarni ko'rishi kerak.\n\nMotor o'chiq, kalit ON holatida bo'lsin.")
            .setPositiveButton("O'chirish") { _, _ -> ochirish() }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

    private fun ochirish() = fonda {
        ui { holat("Xatolar o'chirilmoqda...", KULRANG) }
        val elm = yangiElm()
        try {
            elm.boshlash()
            if (elm.mashinaBilanAloqa() == null) {
                ui { holat("❌ Mashina javob bermadi.", QIZIL) }
                return@fonda
            }
            val ok = elm.xatolarniOchir()
            ui {
                if (ok) holat("✅ Xatolar o'chirildi. Qayta tekshirib ko'ring.", YASHIL)
                else holat("❌ Mashina o'chirish buyrug'ini qabul qilmadi. Motorni o'chirib, kalitni ON qilib qayta urinib ko'ring.", QIZIL)
            }
        } finally {
            elm.yop()
        }
    }

    // ------------------------------------------------------------ ulashish
    private fun ulashish() {
        if (oxirgiHisobot.isEmpty()) {
            holat("Avval \"Tekshirish\"ni bosing.", KULRANG)
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
        datchiklar: List<Pair<String, String>>,
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
        datchiklar.forEach { appendLine("  ${it.first}: ${it.second}") }
    }

    // ------------------------------------------------------------ UI yordamchilar
    private fun ui(f: () -> Unit) = runOnUiThread(f)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun holat(matn: String, rang: Int) {
        holat.text = matn
        holat.setTextColor(rang)
    }

    private fun tugmalar(yoq: Boolean) {
        tekshirBtn.isEnabled = yoq
        ochirBtn.isEnabled = yoq
        ulashBtn.isEnabled = yoq
        jonliBtn.isEnabled = yoq || jonli
    }

    private fun tugma(matn: String, rang: Int, bosish: () -> Unit) = Button(this).apply {
        text = matn
        isAllCaps = false
        textSize = 15f
        setTextColor(Color.WHITE)
        background = GradientDrawable().apply { setColor(rang); cornerRadius = dp(10).toFloat() }
        setOnClickListener { bosish() }
    }

    private fun qator(a: View, b: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, dp(8), 0, 0)
        addView(a, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(6) })
        addView(b, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(6) })
    }

    private fun karta(sarlavha: String, ichi: (LinearLayout) -> Unit) {
        val k = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(12).toFloat() }
        }
        k.addView(TextView(this).apply {
            text = sarlavha
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#111827"))
            setPadding(0, 0, 0, dp(6))
        })
        ichi(k)
        natijalar.addView(k, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
    }

    private fun satr(k: LinearLayout, nom: String, qiymat: String): TextView {
        val q = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(4))
        }
        q.addView(TextView(this).apply {
            text = nom; textSize = 14f; setTextColor(KULRANG)
        }, LinearLayout.LayoutParams(0, -2, 1.3f))
        val v = TextView(this).apply {
            text = qiymat; textSize = 15f; gravity = Gravity.END
            setTypeface(typeface, Typeface.BOLD); setTextColor(Color.parseColor("#111827"))
        }
        q.addView(v, LinearLayout.LayoutParams(0, -2, 1f))
        k.addView(q)
        return v
    }

    private fun xatoKarta(sarlavha: String, kodlar: List<String>) = karta(sarlavha) { k ->
        if (kodlar.isEmpty()) {
            k.addView(TextView(this).apply { text = "Yo'q ✅"; textSize = 15f; setTextColor(YASHIL) })
            return@karta
        }
        for (kod in kodlar) {
            val rang = when (Kodlar.jiddiylik(kod)) { 2 -> QIZIL; 1 -> SARIQ; else -> YASHIL }
            k.addView(TextView(this).apply {
                text = kod
                textSize = 17f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(rang)
                setPadding(0, dp(6), 0, 0)
            })
            k.addView(TextView(this).apply {
                text = Kodlar.izoh(kod)
                textSize = 14f
                setTextColor(Color.parseColor("#374151"))
            })
        }
    }
}
