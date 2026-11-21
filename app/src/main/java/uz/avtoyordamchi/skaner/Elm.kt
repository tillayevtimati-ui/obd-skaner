package uz.avtoyordamchi.skaner

/** ELM327 bilan gaplashish va OBD-II javoblarini o'qish. */
class Elm(private val ulanish: Ulanish) {

    var isCan = true
        private set

    fun yop() = ulanish.yop()

    /** Buyruq yuboradi va '>' belgisigacha kelgan qatorlarni qaytaradi. */
    @Synchronized
    fun cmd(c: String, kutish: Long = 6_000): List<String> {
        val inp = ulanish.kirish
        // Oldingi javobdan qolgan baytlarni tozalash
        try { while (inp.available() > 0) inp.read() } catch (_: Exception) {}
        ulanish.chiqish.write((c + "\r").toByteArray(Charsets.US_ASCII))
        ulanish.chiqish.flush()
        val sb = StringBuilder()
        val buf = ByteArray(512)
        val t0 = System.currentTimeMillis()
        while (!sb.contains('>') && System.currentTimeMillis() - t0 < kutish) {
            val bor = try { inp.available() } catch (_: Exception) { 0 }
            if (bor > 0) {
                val n = inp.read(buf, 0, minOf(bor, buf.size))
                if (n < 0) break
                sb.append(String(buf, 0, n, Charsets.US_ASCII))
            } else {
                Thread.sleep(8)
            }
        }
        return sb.toString().replace(">", "")
            .replace("\r", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals(c, ignoreCase = true) }
    }

    fun boshlash(): String {
        val versiya = cmd("ATZ", 4000).joinToString(" ")
        for (c in listOf("ATE0", "ATL0", "ATS0", "ATH0", "ATAT1", "ATSP0")) cmd(c, 2000)
        return versiya
    }

    /** Mashina bilan aloqani o'rnatadi; protokolni qaytaradi yoki null. */
    fun mashinaBilanAloqa(): String? {
        val r = cmd("0100", 20_000)
        if (!yaxshi(r)) return null
        val dpn = cmd("ATDPN").joinToString("").uppercase().replace("A", "")
        val oxirgi = dpn.lastOrNull()
        isCan = oxirgi != null && oxirgi in "6789"
        return cmd("ATDP").joinToString(" ")
    }

    fun kuchlanish(): String = cmd("ATRV").joinToString(" ")

    /** Javobdan kutilgan sarlavhadan keyingi baytlarni ajratadi. */
    private fun baytlar(r: List<String>, sarlavha: String, otkazish: Int = 0): IntArray? {
        if (!yaxshi(r)) return null
        for (q in hexQatorlar(r)) {
            val i = q.indexOf(sarlavha)
            if (i >= 0) {
                val b = q.substring(minOf(q.length, i + sarlavha.length + otkazish * 2))
                if (b.length < 2) return null
                return IntArray(b.length / 2) { b.substring(it * 2, it * 2 + 2).toInt(16) }
            }
        }
        return null
    }

    /** Joriy qiymat (mode 01). p = "010C" kabi. */
    fun pid(p: String): IntArray? = baytlar(cmd(p), "41" + p.substring(2))

    /** Freeze frame qiymati (mode 02, 0-kadr). p = "010C" kabi. */
    fun muzlatilgan(p: String): IntArray? = baytlar(cmd("02" + p.substring(2) + "00"), "42" + p.substring(2), 1)

    /** Freeze frame'ni yozib qoldirgan xato kodi. */
    fun muzlatilganKod(): String? {
        val b = muzlatilgan("0102") ?: return null
        if (b.size < 2 || (b[0] == 0 && b[1] == 0)) return null
        return dtcNomi(b[0], b[1])
    }

    fun vin(): String? {
        val r = cmd("0902", 10_000)
        if (!yaxshi(r)) return null
        val data = hexQatorlar(r).joinToString("")
        val i = data.indexOf("4902")
        if (i < 0) return null
        val d = data.substring(i + 4)
        val sb = StringBuilder()
        var k = 0
        while (k + 1 < d.length) {
            val ch = d.substring(k, k + 2).toInt(16).toChar()
            if (ch.isLetterOrDigit()) sb.append(ch)
            k += 2
        }
        val v = sb.toString()
        return when {
            v.length >= 17 -> v.takeLast(17)
            v.isNotEmpty() -> v
            else -> null
        }
    }

    /** mode: "03" saqlangan, "07" kutilayotgan, "0A" doimiy xatolar. */
    fun xatolar(mode: String): List<String> {
        val javobKodi = "4" + mode.substring(1)
        var qatorlar = hexQatorlar(cmd(mode, 10_000))
        if (isCan && qatorlar.size > 1 && qatorlar[0].length <= 3) qatorlar = qatorlar.drop(1)
        val data = qatorlar.joinToString("")
        val i = data.indexOf(javobKodi)
        if (i < 0) return emptyList()
        val bloklar = if (isCan) listOf(data.substring(minOf(i + 4, data.length)))
        else qatorlar.filter { it.startsWith(javobKodi) }.map { it.substring(2) }
        val kodlar = mutableListOf<String>()
        for (blok in bloklar) {
            var j = 0
            while (j + 3 < blok.length) {
                val b1 = blok.substring(j, j + 2).toInt(16)
                val b2 = blok.substring(j + 2, j + 4).toInt(16)
                j += 4
                if (b1 == 0 && b2 == 0) continue
                val kod = dtcNomi(b1, b2)
                if (kod !in kodlar) kodlar.add(kod)
            }
        }
        return kodlar
    }

    /** Ekologik o'z-o'zini tekshirish monitorlari (PID 0101). */
    fun tayyorlik(): List<Monitor> {
        val a = pid("0101") ?: return emptyList()
        if (a.size < 4) return emptyList()
        val b = a[1]; val c = a[2]; val d = a[3]
        val natija = mutableListOf<Monitor>()
        fun qosh(nom: String, mavjud: Boolean, tugallanmagan: Boolean) {
            if (mavjud) natija.add(Monitor(nom, true, !tugallanmagan))
        }
        qosh("Tutashmaslik nazorati", b and 0x01 != 0, b and 0x10 != 0)
        qosh("Yoqilg'i tizimi", b and 0x02 != 0, b and 0x20 != 0)
        qosh("Qismlar nazorati", b and 0x04 != 0, b and 0x40 != 0)
        val dizel = b and 0x08 != 0
        if (!dizel) {
            qosh("Katalizator", c and 0x01 != 0, d and 0x01 != 0)
            qosh("Isitiladigan katalizator", c and 0x02 != 0, d and 0x02 != 0)
            qosh("Bug'larni ushlash (EVAP)", c and 0x04 != 0, d and 0x04 != 0)
            qosh("Ikkilamchi havo", c and 0x08 != 0, d and 0x08 != 0)
            qosh("Konditsioner", c and 0x10 != 0, d and 0x10 != 0)
            qosh("Kislorod datchigi", c and 0x20 != 0, d and 0x20 != 0)
            qosh("Kislorod datchigi isitgichi", c and 0x40 != 0, d and 0x40 != 0)
            qosh("EGR tizimi", c and 0x80 != 0, d and 0x80 != 0)
        } else {
            qosh("NMHC katalizatori", c and 0x01 != 0, d and 0x01 != 0)
            qosh("NOx tozalash", c and 0x02 != 0, d and 0x02 != 0)
            qosh("Turbo bosimi", c and 0x08 != 0, d and 0x08 != 0)
            qosh("Kuyindi filtri (DPF)", c and 0x40 != 0, d and 0x40 != 0)
            qosh("EGR/VVT tizimi", c and 0x80 != 0, d and 0x80 != 0)
        }
        return natija
    }

    /** Saqlangan xatolarni o'chiradi ('chek'ni o'chiradi). */
    fun xatolarniOchir(): Boolean = yaxshi(cmd("04", 8_000))

    companion object {
        private val XATOLAR = listOf("NO DATA", "ERROR", "?", "UNABLE TO CONNECT", "STOPPED", "CAN ERROR", "BUS INIT")

        fun yaxshi(r: List<String>): Boolean {
            if (r.isEmpty()) return false
            val j = r.joinToString(" ").uppercase()
            return XATOLAR.none { j.contains(it) }
        }

        fun hexQatorlar(r: List<String>): List<String> {
            val natija = mutableListOf<String>()
            for (l in r) {
                var u = l.uppercase().replace(" ", "")
                if (u.isEmpty() || u.contains("SEARCHING")) continue
                if (u.length >= 2 && u[1] == ':' && u[0] in "0123456789ABCDEF") u = u.substring(2)
                if (u.isNotEmpty() && u.all { it in "0123456789ABCDEF" }) natija.add(u)
            }
            return natija
        }

        fun dtcNomi(b1: Int, b2: Int): String {
            val harf = "PCBU"[(b1 shr 6) and 3]
            return "%c%d%X%X%X".format(harf, (b1 shr 4) and 3, b1 and 0xF, b2 shr 4, b2 and 0xF)
        }
    }
}

data class Monitor(val nom: String, val mavjud: Boolean, val tayyor: Boolean)

/** Datchiklar: PID -> (nomi, birligi, hisoblash). */
data class Datchik(val pid: String, val nom: String, val birlik: String, val ikonka: String, val f: (IntArray) -> Number)

private fun yaxlit(x: Double) = Math.round(x * 10) / 10.0

val DATCHIKLAR = listOf(
    Datchik("010C", "Motor aylanishi", "ayl/min", "shesterna") { a -> (a[0] * 256 + a[1]) / 4 },
    Datchik("0105", "Sovutish suyuqligi", "°C", "harorat") { a -> a[0] - 40 },
    Datchik("010D", "Tezlik", "km/soat", "raketa") { a -> a[0] },
    Datchik("0104", "Motor yuklamasi", "%", "kuchlanish") { a -> yaxlit(a[0] * 100.0 / 255) },
    Datchik("0111", "Drossel holati", "%", "shamol") { a -> yaxlit(a[0] * 100.0 / 255) },
    Datchik("0106", "Qisqa yoqilg'i tuzatish", "%", "tomchi") { a -> yaxlit((a[0] - 128) * 100.0 / 128) },
    Datchik("0107", "Uzoq yoqilg'i tuzatish", "%", "tomchi") { a -> yaxlit((a[0] - 128) * 100.0 / 128) },
    Datchik("010B", "Kollektor bosimi", "kPa", "shamol") { a -> a[0] },
    Datchik("0110", "Havo sarfi (MAF)", "g/s", "shamol") { a -> (a[0] * 256 + a[1]) / 100.0 },
    Datchik("010F", "Kirish havosi", "°C", "qor") { a -> a[0] - 40 },
    Datchik("0146", "Tashqi havo", "°C", "qor") { a -> a[0] - 40 },
    Datchik("010E", "O't oldirish burchagi", "°", "olov") { a -> a[0] / 2.0 - 64 },
    Datchik("012F", "Yoqilg'i darajasi", "%", "yoqilgi") { a -> yaxlit(a[0] * 100.0 / 255) },
    Datchik("0142", "Blok kuchlanishi", "V", "akkumulyator") { a -> (a[0] * 256 + a[1]) / 1000.0 },
    Datchik("011F", "Motor ishlagan vaqt", "s", "taymer") { a -> a[0] * 256 + a[1] },
    Datchik("0121", "'Chek' bilan yurilgan", "km", "chiroq") { a -> a[0] * 256 + a[1] },
    Datchik("0131", "Xatolar o'chirilgandan beri", "km", "supurgi") { a -> a[0] * 256 + a[1] },
)

fun datchik(pid: String): Datchik? = DATCHIKLAR.firstOrNull { it.pid == pid }
