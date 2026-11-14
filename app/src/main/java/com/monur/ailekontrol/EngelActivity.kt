// surum: 0.6.0 - M-ONUR Aile Cocuk Kontrol - engel ekrani
package com.monur.ailekontrol

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class EngelActivity : AppCompatActivity() {

    private lateinit var mesaj: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val kok = LinearLayout(this)
        kok.orientation = LinearLayout.VERTICAL
        kok.gravity = Gravity.CENTER
        kok.setPadding(64, 64, 64, 64)
        kok.setBackgroundColor(Color.parseColor("#1E3A5F"))

        val baslik = TextView(this)
        baslik.text = "AILE COCUK KONTROL"
        baslik.textSize = 20f
        baslik.setTextColor(Color.parseColor("#FFB347"))
        baslik.gravity = Gravity.CENTER

        mesaj = TextView(this)
        mesaj.textSize = 22f
        mesaj.setTextColor(Color.WHITE)
        mesaj.gravity = Gravity.CENTER
        mesaj.setPadding(0, 48, 0, 48)
        mesaj.text = intent.getStringExtra("sebep") ?: "Bu uygulama su an kullanilamaz."

        val ev = Button(this)
        ev.text = "Ana Ekrana Don"
        ev.setOnClickListener { eveDon() }

        val ebeveyn = Button(this)
        ebeveyn.text = "Ebeveyn: 15 dk izin ver"
        ebeveyn.setOnClickListener {
            pinSor(this) {
                Ayarlar.geciciIzinVer(this, 15)
                finish()
            }
        }

        kok.addView(baslik)
        kok.addView(mesaj)
        kok.addView(ev)
        kok.addView(ebeveyn)
        setContentView(kok)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                eveDon()
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mesaj.text = intent.getStringExtra("sebep") ?: mesaj.text
    }

    private fun eveDon() {
        val i = Intent(Intent.ACTION_MAIN)
        i.addCategory(Intent.CATEGORY_HOME)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
        finish()
    }
}
