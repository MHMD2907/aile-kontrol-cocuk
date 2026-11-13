// surum: 0.4.0 - M-ONUR Aile Cocuk Kontrol (cocuk)
package com.monur.ailekontrol

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName
    private lateinit var durum: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        admin = ComponentName(this, AdminReceiver::class.java)

        val kok = LinearLayout(this)
        kok.orientation = LinearLayout.VERTICAL
        kok.gravity = Gravity.CENTER
        kok.setPadding(48, 48, 48, 48)

        durum = TextView(this)
        durum.textSize = 18f
        durum.gravity = Gravity.CENTER

        val btn = Button(this)
        btn.text = "Korumayi Etkinlestir"
        btn.setOnClickListener { yoneticiIste() }

        kok.addView(durum)
        kok.addView(btn)
        setContentView(kok)

        durumuGuncelle()
        guncellemeKontrol()
    }

    override fun onResume() {
        super.onResume()
        durumuGuncelle()
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
        "Koruma AKTIF - yalnizca ebeveyn parolasiyla kaldirilabilir."
        else
        "Koruma PASIF - etkinlestirmek icin butona dokunun."
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
