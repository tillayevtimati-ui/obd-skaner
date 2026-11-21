package uz.avtoyordamchi.skaner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import javax.net.SocketFactory

/** Skaner bilan jismoniy aloqa: Wi-Fi yoki Bluetooth. */
interface Ulanish {
    val kirish: InputStream
    val chiqish: OutputStream
    fun yop()
}

class WifiUlanish(factory: SocketFactory, host: String, port: Int) : Ulanish {
    private val socket: Socket = factory.createSocket()
    override val kirish: InputStream
    override val chiqish: OutputStream

    init {
        socket.connect(InetSocketAddress(host, port), 8000)
        socket.soTimeout = 1000
        kirish = socket.getInputStream()
        chiqish = socket.getOutputStream()
    }

    override fun yop() {
        try { socket.close() } catch (_: Exception) {}
    }
}

@SuppressLint("MissingPermission")
class BtUlanish(adapter: BluetoothAdapter, manzil: String) : Ulanish {
    private val socket: BluetoothSocket
    override val kirish: InputStream
    override val chiqish: OutputStream

    init {
        val qurilma: BluetoothDevice = adapter.getRemoteDevice(manzil)
        try { adapter.cancelDiscovery() } catch (_: Exception) {}
        var s = qurilma.createRfcommSocketToServiceRecord(SPP)
        try {
            s.connect()
        } catch (e: Exception) {
            // Ba'zi arzon skanerlar faqat "insecure" ulanishni qabul qiladi
            try { s.close() } catch (_: Exception) {}
            s = qurilma.createInsecureRfcommSocketToServiceRecord(SPP)
            s.connect()
        }
        socket = s
        kirish = socket.inputStream
        chiqish = socket.outputStream
    }

    override fun yop() {
        try { socket.close() } catch (_: Exception) {}
    }

    companion object {
        val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
