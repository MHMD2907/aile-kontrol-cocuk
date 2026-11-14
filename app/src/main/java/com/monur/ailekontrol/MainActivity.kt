// surum: 0.5.0 - M-ONUR Aile Cocuk Kontrol (cocuk)
package com.monur.ailekontrol

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.URL
import java.util.Calendar
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName
    private lateinit var durum: TextView
    private lateinit var rapor: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        admin = ComponentName(this, AdminReceiver::class.java)

        val kok = LinearLayout(this)
        kok.orientation = LinearLayout.VERTICAL
        kok.setPadding(48, 48, 48, 48)

        val baslik = TextView(this)
        baslik.text = "Aile Cocuk Kontrol"
        baslik.textSize = 24f
        baslik.gravity = Gravity.CENTER

        durum = TextView(this)
        durum.textSize = 16f
        durum.gravity = Gravity.CENTER
        durum.setPadding(0, 24, 0, 24)

        val btn = Button(this)
        btn.text = "Korumayi Etkinlestir"
        btn.setOnClickListener { yoneticiIste() }

        val raporBtn = Button(this)
        raporBtn.text = "Kullanim Raporunu Yenile"
        raporBtn.setOnClickListener {
            if (kullanimIzniVar()) {
                raporuGoster()
            } else {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }

        rapor = TextView(this)
        rapor.textSize = 16f
        rapor.setPadding(0, 24, 0, 0)

        kok.addView(baslik)
        kok.addView(durum)
        kok.addView(btn)
        kok.addView(raporBtn)
        kok.addView(rapor)

        val kaydirma = ScrollView(this)
        kaydirma.addView(kok)
        setContentView(kaydirma)

        durumuGuncelle()
        guncellemeKontrol()
    }

    override fun onResume() {
        super.onResume()
        durumuGuncelle()
        raporuGoster()
    }

    private fun yoneticiIste() {
        if (!dpm.isAdminActive(admin)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_aciklama))
            startActivity(intent)
        }
    }

    private fun durumuGuncelle() {
        durum.text = if (dpm.isAdminActive(admin))
        "Koruma AKTIF - yalnizca ebeveyn kaldirabilir."
        else
        "Koruma PASIF - etkinlestirmek icin butona dokunun."
    }

    @Suppress("DEPRECATION")
    private fun kullanimIzniVar(): Boolean {
        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mod = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        return mod == AppOpsManager.MODE_ALLOWED
    }

    @Suppress("DEPRECATION")
    private fun raporuGoster() {
        if (!kullanimIzniVar()) {
            rapor.text = "Kullanim raporu icin izin gerekli. 'Kullanim Raporunu Yenile' butonuna basin, listeden Aile Cocuk Kontrol'u bulup izni acin."
            return
        }
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val veriler = usm.queryAndAggregateUsageStats(cal.timeInMillis, System.currentTimeMillis())
        val liste = veriler.values.filter { it.totalTimeInForeground > 60000 }.sortedByDescending { it.totalTimeInForeground }
        var toplam = 0L
        val sb = StringBuilder()
        for (u in liste) {
            toplam += u.totalTimeInForeground
            val ad = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(u.packageName, 0)).toString()
            } catch (e: Exception) {
                u.packageName
            }
            sb.append(ad).append(": ").append(sureYaz(u.totalTimeInForeground)).append("\n")
        }
        val detay = if (sb.isEmpty()) "Henuz kullanim yok." else sb.toString()
        rapor.text = "BUGUN TOPLAM EKRAN SURESI: " + sureYaz(toplam) + "\n\n" + detay
    }

    private fun sureYaz(ms: Long): String {
        val dk = ms / 60000
        return if (dk >= 60) (dk / 60).toString() + " sa " + (dk % 60).toString() + " dk" else dk.toString() + " dk"
    }

    private fun guncellemeKontrol() {
        thread {
            try {
                val api = "https://api.github.com/repos/MHMD2907/aile-kontrol-cocuk/releases/latest"
                val json = URL(api).readText()
                val tag = JSONObject(json).optString("tag_name", "")
                val sonKod = tag.filter { it.isDigit() }.toIntOrNull() ?: return@thread
                if (sonKod > BuildConfig.VERSION_CODE) {
                    runOnUiThread { guncellemeSor() }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun guncellemeSor() {
        AlertDialog.Builder(this)
        .setTitle("Yeni surum var")
        .setMessage("Guncel surum mevcut. Indirip kurmak ister misiniz?")
        .setPositiveButton("Guncelle") { _, _ ->
            val url = "https://github.com/MHMD2907/aile-kontrol-cocuk/releases/latest/download/app-debug.apk"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
        .setNegativeButton("Sonra", null)
        .show()
    }
}
