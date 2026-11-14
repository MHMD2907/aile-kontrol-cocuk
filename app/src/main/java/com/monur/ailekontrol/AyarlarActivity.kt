// surum: 0.7.0 - M-ONUR Aile Cocuk Kontrol - ebeveyn ayarlari (bulut senkron)
package com.monur.ailekontrol

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AyarlarActivity : AppCompatActivity() {

    private val kutular = mutableListOf<Pair<String, CheckBox>>()
    private lateinit var limitAlan: EditText
    private lateinit var basAlan: EditText
    private lateinit var bitAlan: EditText

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val kok = LinearLayout(this)
        kok.orientation = LinearLayout.VERTICAL
        kok.setPadding(40, 40, 40, 40)

        val ustKaydet = Button(this)
        ustKaydet.text = "Kaydet"
        ustKaydet.setOnClickListener { kaydet() }
        kok.addView(ustKaydet)

        kok.addView(baslik("Gunluk ekran suresi limiti (dakika, 0 = limitsiz)"))
        limitAlan = EditText(this)
        limitAlan.inputType = InputType.TYPE_CLASS_NUMBER
        limitAlan.setText(Ayarlar.gunlukLimitDk(this).toString())
        kok.addView(limitAlan)

        kok.addView(baslik("Uyku saati baslangic (SS:DD, bos = kapali)"))
        basAlan = EditText(this)
        basAlan.hint = "21:30"
        basAlan.setText(dkYaz(Ayarlar.yatisBas(this)))
        kok.addView(basAlan)

        kok.addView(baslik("Uyku saati bitis (SS:DD)"))
        bitAlan = EditText(this)
        bitAlan.hint = "07:00"
        bitAlan.setText(dkYaz(Ayarlar.yatisBit(this)))
        kok.addView(bitAlan)

        kok.addView(baslik("Engellenecek uygulamalar (isaretle)"))
        val pm = packageManager
        val ana = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val uygulamalar = pm.queryIntentActivities(ana, 0)
            .map { Pair(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .distinctBy { it.first }
            .filter { it.first != packageName }
            .sortedBy { it.second.lowercase() }
        val engelli = Ayarlar.engelliler(this)
        for (u in uygulamalar) {
            val cb = CheckBox(this)
            cb.text = u.second
            cb.isChecked = engelli.contains(u.first)
            kok.addView(cb)
            kutular.add(Pair(u.first, cb))
        }

        val pinDegis = Button(this)
        pinDegis.text = "PIN Degistir"
        pinDegis.setOnClickListener { pinBelirle(this) { Senkron.ayarlariYukle(this) } }
        kok.addView(pinDegis)

        val altKaydet = Button(this)
        altKaydet.text = "Kaydet"
        altKaydet.setOnClickListener { kaydet() }
        kok.addView(altKaydet)

        val kaydirma = ScrollView(this)
        kaydirma.addView(kok)
        setContentView(kaydirma)
    }

    private fun baslik(t: String): TextView {
        val tv = TextView(this)
        tv.text = t
        tv.textSize = 17f
        tv.setPadding(0, 32, 0, 8)
        return tv
    }

    private fun kaydet() {
        Ayarlar.limitKaydet(this, limitAlan.text.toString().trim().toIntOrNull() ?: 0)
        Ayarlar.yatisKaydet(this, dkOku(basAlan.text.toString()), dkOku(bitAlan.text.toString()))
        Ayarlar.engelliKaydet(this, kutular.filter { it.second.isChecked }.map { it.first }.toSet())
        Senkron.ayarlariYukle(this)
        Toast.makeText(this, "Ayarlar kaydedildi", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun dkYaz(m: Int): String {
        if (m < 0) return ""
        val s = m / 60
        val d = m % 60
        return (if (s < 10) "0" else "") + s + ":" + (if (d < 10) "0" else "") + d
    }

    private fun dkOku(metin: String): Int {
        val p = metin.trim().split(":")
        if (p.size != 2) return -1
        val s = p[0].trim().toIntOrNull() ?: return -1
        val d = p[1].trim().toIntOrNull() ?: return -1
        if (s !in 0..23 || d !in 0..59) return -1
        return s * 60 + d
    }
}
