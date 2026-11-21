package uz.avtoyordamchi.skaner

import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.SocketFactory

/** Wi-Fi ELM327 bilan aloqa va OBD-II javoblarini o'qish. */
class Elm(private val factory: SocketFactory) {

    private var socket: Socket? = null
    private lateinit var inp: InputStream
    private lateinit var out: OutputStream
    var isCan = true
        private set

    fun ulan(host: String, port: Int) {
        val s = factory.createSocket()
        s.connect(InetSocketAddress(host, port), 8000)
        s.soTimeout = 1000
        socket = s
        inp = s.getInputStream()
        out = s.getOutputStream()
    }

    fun yop() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }

    val ulanganmi: Boolean get() = socket?.isConnected == true && socket?.isClosed == false

    /** Buyruq yuboradi va '>' belgisigacha kelgan qatorlarni qaytaradi. */
    @Synchronized
    fun cmd(c: String, kutish: Long = 10_000): List<String> {
        out.write((c + "\r").toByteArray(Charsets.US_ASCII))
        out.flush()
        val sb = StringBuilder()
        val buf = ByteArray(512)
        val t0 = System.currentTimeMillis()
        while (!sb.contains('>') && System.currentTimeMillis() - t0 < kutish) {
            try {
                val n = inp.read(buf)
                if (n < 0) break
                sb.append(String(buf, 0, n, Charsets.US_ASCII))
            } catch (_: java.net.SocketTimeoutException) {
            }
        }
        return sb.toString().replace(">", "")
            .replace("\r", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals(c, ignoreCase = true) }
    }

    fun boshlash(): String {
        val versiya = cmd("ATZ", 5000).joinToString(" ")
        for (c in listOf("ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")) cmd(c)
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

    fun pid(p: String): IntArray? {
        val r = cmd(p)
        if (!yaxshi(r)) return null
        val kutilgan = "4" + p[1] + p.substring(2)
        for (q in hexQatorlar(r)) {
            val i = q.indexOf(kutilgan)
            if (i >= 0) {
                val b = q.substring(i + 4)
                return IntArray(b.length / 2) { b.substring(it * 2, it * 2 + 2).toInt(16) }
            }
        }
        return null
    }

    fun vin(): String? {
        val r = cmd("0902", 15_000)
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
        var qatorlar = hexQatorlar(cmd(mode, 15_000))
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

    /** Saqlangan xatolarni o'chiradi ('chek'ni o'chiradi). */
    fun xatolarniOchir(): Boolean = yaxshi(cmd("04", 10_000))

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

/** Datchiklar: PID -> (nomi, birligi, hisoblash). */
data class Datchik(val pid: String, val nom: String, val birlik: String, val f: (IntArray) -> Number)

val DATCHIKLAR = listOf(
    Datchik("010C", "Motor aylanishi", "ayl/min") { a -> (a[0] * 256 + a[1]) / 4 },
    Datchik("0105", "Sovutish suyuqligi", "°C") { a -> a[0] - 40 },
    Datchik("010D", "Tezlik", "km/soat") { a -> a[0] },
    Datchik("0104", "Motor yuklamasi", "%") { a -> Math.round(a[0] * 1000.0 / 255) / 10.0 },
    Datchik("0111", "Drossel holati", "%") { a -> Math.round(a[0] * 1000.0 / 255) / 10.0 },
    Datchik("0106", "Qisqa yoqilg'i tuzatish (STFT)", "%") { a -> Math.round((a[0] - 128) * 1000.0 / 128) / 10.0 },
    Datchik("0107", "Uzoq yoqilg'i tuzatish (LTFT)", "%") { a -> Math.round((a[0] - 128) * 1000.0 / 128) / 10.0 },
    Datchik("010B", "Kollektor bosimi (MAP)", "kPa") { a -> a[0] },
    Datchik("0110", "Havo sarfi (MAF)", "g/s") { a -> (a[0] * 256 + a[1]) / 100.0 },
    Datchik("010F", "Kirish havosi harorati", "°C") { a -> a[0] - 40 },
    Datchik("010E", "O't oldirish burchagi", "°") { a -> a[0] / 2.0 - 64 },
    Datchik("0142", "Blok kuchlanishi", "V") { a -> (a[0] * 256 + a[1]) / 1000.0 },
    Datchik("011F", "Motor ishlagan vaqt", "s") { a -> a[0] * 256 + a[1] },
    Datchik("0121", "'Chek' bilan yurilgan masofa", "km") { a -> a[0] * 256 + a[1] },
    Datchik("0131", "Xatolar o'chirilgandan beri", "km") { a -> a[0] * 256 + a[1] },
)
