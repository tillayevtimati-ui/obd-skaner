package uz.avtoyordamchi.skaner

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Muddatlar (sug'urta, texnik ko'rik, moy...) bo'yicha kunlik eslatmalar. */
object Eslatmalar {
    private const val KANAL = "muddatlar"
    private val ESLATISH_KUNLARI = setOf(30, 14, 7, 3, 1, 0)

    /** Har kuni soat 10:00 atrofida tekshiruvni rejalashtirish. */
    fun rejalashtir(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            ctx, 1, Intent(ctx, EslatmaReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val vaqt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, vaqt, AlarmManager.INTERVAL_DAY, pi)
    }

    fun tekshir(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(KANAL, "Muddat eslatmalari", NotificationManager.IMPORTANCE_DEFAULT))

        val prefs = ctx.getSharedPreferences("eslatmalar", Context.MODE_PRIVATE)
        val bugun = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        var raqam = 100
        for (m in Profil(ctx).mashinalar()) {
            val xabarlar = mutableListOf<Pair<String, String>>()
            for ((nom, sana) in listOf("Sug'urta" to m.sugurta, "Texnik ko'rik" to m.texosmotr,
                "Tonirovka ruxsati" to m.tonirovka, "Ishonchnoma" to m.ishonchnoma)) {
                val kun = Sana.qolganKun(sana) ?: continue
                if (kun in ESLATISH_KUNLARI || kun == -1) {
                    val matn = when {
                        kun < 0 -> "$nom muddati o'tib ketdi"
                        kun == 0 -> "$nom muddati bugun tugaydi"
                        else -> "$nom muddatiga $kun kun qoldi (${Sana.chiroyli(sana)})"
                    }
                    xabarlar.add("${m.id}-$nom" to matn)
                }
            }
            m.moyQoldi?.let { km ->
                if (km in -2000..500) xabarlar.add("${m.id}-moy" to
                    if (km < 0) "Moy almashtirish vaqti ${-km} km o'tib ketdi" else "Moy almashtirishga $km km qoldi")
            }
            for ((kalit, matn) in xabarlar) {
                if (prefs.getString(kalit, "") == bugun) continue
                prefs.edit().putString(kalit, bugun).apply()
                val ochish = PendingIntent.getActivity(
                    ctx, 0, Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                val n = Notification.Builder(ctx, KANAL)
                    .setSmallIcon(R.drawable.ic_bildirishnoma)
                    .setContentTitle(m.nomi + if (m.raqamBor) "  ·  ${m.hudud} ${m.raqam}" else "")
                    .setContentText(matn)
                    .setContentIntent(ochish)
                    .setAutoCancel(true)
                    .build()
                nm.notify(raqam++, n)
            }
        }
    }
}

/** Kunlik signal va telefon qayta yonganda ishlaydi. */
class EslatmaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Eslatmalar.rejalashtir(context)
        try { Eslatmalar.tekshir(context) } catch (_: Exception) {}
    }
}
