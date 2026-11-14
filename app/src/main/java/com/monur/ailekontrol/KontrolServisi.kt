// surum: 0.7.0 - M-ONUR Aile Cocuk Kontrol - arka plan kontrol servisi (bulut baglantili)
package com.monur.ailekontrol

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar

class KontrolServisi : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var mevcut: String? = null
    private var toplamOnbellek = 0L
    private var toplamZaman = 0L

    private var dinleyici: ListenerRegistration? = null
    private var dinleyiciKuruluyor = false
    private var sonDurum = 0L
    private var sonKonum = 0L
    private var sonUygulamaListesi = 0L

    // Limit/uyku saatinde bile acik kalacak paketler (telefon, sistem)
    private val herZamanIzinli = setOf(
        "com.android.systemui",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.android.phone",
        "com.android.incallui",
        "com.android.server.telecom",
        "com.android.emergency"
    )

    // Her zaman ebeveyn PIN'i isteyen paketler (cocuk izinleri/korumayi kapatamasin)
    private val pinKorumali = setOf(
        "com.android.settings",
        "com.samsung.android.settings",
        "com.miui.securitycenter",
        "com.coloros.safecenter",
        "com.huawei.systemmanager"
    )

    private val dongu = object : Runnable {
        override fun run() {
            try {
                kontrol()
            } catch (e: Exception) {
            }
            try {
                bulutIsleri()
            } catch (e: Exception) {
            }
            handler.postDelayed(this, 2000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        bildirimBaslat()
        handler.post(dongu)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(dongu)
        dinleyici?.remove()
        dinleyici = null
        super.onDestroy()
    }

    private fun bildirimBaslat() {
        val kanal = "aile_kontrol"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(kanal, "Aile Cocuk Kontrol", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val bildirim = NotificationCompat.Builder(this, kanal)
            .setContentTitle("Aile Cocuk Kontrol")
            .setContentText("Ebeveyn kontrolu aktif")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, bildirim, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, bildirim)
        }
    }

    // ---------- Bulut (ebeveyn uygulamasi ile baglanti) ----------

    private fun bulutIsleri() {
        if (!Ayarlar.bagli(this)) return
        if (dinleyici == null) {
            if (dinleyiciKuruluyor) return
            dinleyiciKuruluyor = true
            Bulut.girisYap(this, {
                dinleyici = Senkron.cihazRef(this).addSnapshotListener { d, _ ->
                    if (d != null && d.exists()) {
                        Senkron.uzakAyarUygula(this, d)
                        val istek = d.getLong("konumIste") ?: 0L
                        if (istek > Ayarlar.sonKonumIstegi(this)) {
                            Ayarlar.sonKonumIstegiKaydet(this, istek)
                            Senkron.konumGonder(this)
                        }
                    }
                }
                dinleyiciKuruluyor = false
            }, {
                dinleyiciKuruluyor = false
            })
            return
        }
        val simdi = System.currentTimeMillis()
        if (simdi - sonDurum > 60000) {
            sonDurum = simdi
            Senkron.durumGonder(this)
        }
        if (simdi - sonKonum > 15 * 60000) {
            sonKonum = simdi
            Senkron.konumGonder(this)
        }
        if (simdi - sonUygulamaListesi > 6 * 3600000L) {
            sonUygulamaListesi = simdi
            Senkron.uygulamaListesiGonder(this)
        }
    }

    // ---------- Yerel kontrol ----------

    @Suppress("DEPRECATION")
    private fun onPlandakiUygulama(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val simdi = System.currentTimeMillis()
        val olaylar = usm.queryEvents(simdi - 10000, simdi)
        val olay = UsageEvents.Event()
        var son: String? = null
        while (olaylar.hasNextEvent()) {
            olaylar.getNextEvent(olay)
            if (olay.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                son = olay.packageName
            }
        }
        return son
    }

    @Suppress("DEPRECATION")
    private fun anaEkranPaketi(): String? {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.resolveActivity(i, 0)?.activityInfo?.packageName
    }

    private fun kontrol() {
        val yeni = onPlandakiUygulama()
        if (yeni != null) mevcut = yeni
        val pkg = mevcut ?: return
        if (pkg == packageName) return
        if (pkg == anaEkranPaketi()) return
        if (herZamanIzinli.contains(pkg)) return

        val sebep = engelSebebi(pkg) ?: return
        val i = Intent(this, EngelActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        i.putExtra("sebep", sebep)
        startActivity(i)
    }

    private fun engelSebebi(pkg: String): String? {
        if (System.currentTimeMillis() < Ayarlar.geciciIzinBitis(this)) return null
        if (Ayarlar.kilitli(this)) {
            return "Cihaz ebeveyn tarafindan kilitlendi."
        }
        if (Ayarlar.pin(this).isNotEmpty() && pinKorumali.contains(pkg)) {
            return "Ayarlar ebeveyn PIN'i ile korunuyor."
        }
        if (Ayarlar.engelliler(this).contains(pkg)) {
            return "Bu uygulama ebeveyn tarafindan engellendi."
        }
        if (yatisSaatiMi()) {
            return "Uyku saati. Cihaz su an kullanima kapali."
        }
        val limit = Ayarlar.gunlukLimitDk(this)
        if (limit > 0 && bugunToplamDk() >= limit) {
            return "Bugunku ekran suresi doldu (" + limit + " dk)."
        }
        return null
    }

    private fun yatisSaatiMi(): Boolean {
        val bas = Ayarlar.yatisBas(this)
        val bit = Ayarlar.yatisBit(this)
        if (bas < 0 || bit < 0 || bas == bit) return false
        val cal = Calendar.getInstance()
        val simdi = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return if (bas < bit) {
            simdi >= bas && simdi < bit
        } else {
            simdi >= bas || simdi < bit
        }
    }

    private fun bugunToplamDk(): Long {
        val simdi = System.currentTimeMillis()
        if (simdi - toplamZaman > 30000) {
            toplamZaman = simdi
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val ana = anaEkranPaketi()
            var toplam = 0L
            for (u in usm.queryAndAggregateUsageStats(cal.timeInMillis, simdi).values) {
                if (u.packageName != packageName && u.packageName != ana) {
                    toplam += u.totalTimeInForeground
                }
            }
            toplamOnbellek = toplam / 60000
        }
        return toplamOnbellek
    }
}
