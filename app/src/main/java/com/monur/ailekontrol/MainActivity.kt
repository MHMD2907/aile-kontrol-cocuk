// sürüm: 0.1.0 — M-ONUR Aile Kontrol (çocuk)
package com.monur.ailekontrol

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName
    private lateinit var durum: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        admin = ComponentName(this, AdminReceiver::class.java)

        val kok = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        durum = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
        }

        val btn = Button(this).apply {
            text = "Korumayı Etkinleştir"
            setOnClickListener { yoneticiIste() }
        }

        kok.addView(durum)
        kok.addView(btn)
        setContentView(kok)

        durumuGuncelle()
    }

    override fun onResume() {
        super.onResume()
        durumuGuncelle()
    }

    private fun yoneticiIste() {
        if (!dpm.isAdminActive(admin)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.admin_aciklama)
                )
            }
            startActivity(intent)
        }
    }

    private fun durumuGuncelle() {
        durum.text = if (dpm.isAdminActive(admin)) {
            "Koruma AKTİF\nBu uygulama yalnızca ebeveyn parolasıyla kaldırılabilir."
        } else {
            "Koruma PASİF\nEtkinleştirmek için butona dokunun."
        }
    }
}
