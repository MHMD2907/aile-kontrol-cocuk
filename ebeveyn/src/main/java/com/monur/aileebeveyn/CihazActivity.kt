// surum: 0.1.0 - M-ONUR Aile Kontrol Ebeveyn - cihaz detay ve kurallar
package com.monur.aileebeveyn

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration

class CihazActivity : AppCompatActivity() {

    private lateinit var ref: DocumentReference
    private var dinleyici: ListenerRegistration? = null
    private var formDolduruldu = false
    private var kilitli = false
    private var sonLat = 0.0
    private var sonLng = 0.0
    private var konumVar = false

    private lateinit var adText: TextView
    private lateinit var bilgiText: TextView
    private lateinit var konumText: TextView
    private lateinit var kullanimText: TextView
    private lateinit var kilitBtn: Button
    private lateinit var limitAlan: EditText
    private lateinit var basAlan: EditText
    private lateinit var bitAlan: EditText
    private lateinit var pinAlan: EditText
    private lateinit var uygulamaKutu: LinearLayout
    private val kutular = mutableListOf<Pair<String, CheckBox>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cihazId = intent.getStringExtra("cihazId") ?: run { finish(); return }
        if (!Bulut.baslat(this)) {
            finish()
            return
        }
        ref =Bulut.db().collection("aileler").document(Yardim.aileId(this))
            .collection("cihazlar").document(cihazId)

        val kok = LinearLayout(this)
        kok.orientation = LinearLayout.VERTICAL
        kok.setPadding(40, 40, 40, 40)

        adText = TextView(this)
        adText.textSize = 24f
        adText.setTypeface(null, Typeface.BOLD)
        kok.addView(adText)

        bilgiText = TextView(this)
        bilgiText.textSize = 15f
        bilgiText.setPadding(0, 12, 0, 12)
        kok.addView(bilgiText)

        kilitBtn = Button(this)
        kilitBtn.text = "Simdi Kilitle"
        kilitBtn.setOnClickListener {
            ref.update("ayarlar.kilitli", !kilitli)
                .addOnSuccessListener { mesaj(if (!kilitli) "Kilit acildi" else "Cihaz kilitlendi") }
                .addOnFailureListener { mesaj("Hata: " + it.message) }
        }
        kok.addView(kilitBtn)

        kok.addView(bolum("Konum"))
        konumText = TextView(this)
        konumText.textSize = 15f
        kok.addView(konumText)

        val konumIste = Button(this)
        konumIste.text = "Konumu Guncelle"
        konumIste.setOnClickListener {
            ref.update("konumIste", System.currentTimeMillis())
                .addOnSuccessListener { mesaj("Cihazdan konum istendi, birkac saniye bekleyin") }
                .addOnFailureListener { mesaj("Hata: " + it.message) }
        }
        kok.addView(konumIste)

        val harita = Button(this)
        harita.text = "Haritada Ac"
        harita.setOnClickListener {
            if (konumVar) {
                val url = "https://maps.google.com/?q=" + sonLat + "," + sonLng
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } else {
                mesaj("Henuz konum yok")
            }
        }
        kok.addView(harita)

        kok.addView(bolum("Bugunku kullanim"))
        kullanimText = TextView(this)
        kullanimText.textSize = 15f
        kok.addView(kullanimText)

        kok.addView(bolum("Kurallar"))
        kok.addView(etiket("Gunluk ekran suresi limiti (dakika, 0 = limitsiz)"))
        limitAlan = EditText(this)
        limitAlan.inputType = InputType.TYPE_CLASS_NUMBER
        kok.addView(limitAlan)

        kok.addView(etiket("Uyku saati baslangic (SS:DD, bos = kapali)"))
        basAlan = EditText(this)
        basAlan.hint = "21:30"
        kok.addView(basAlan)

        kok.addView(etiket("Uyku saati bitis (SS:DD)"))
        bitAlan = EditText(this)
        bitAlan.hint = "07:00"
        kok.addView(bitAlan)

        kok.addView(etiket("Cocuk cihazindaki ebeveyn PIN'i (bos = degistirme)"))
        pinAlan = EditText(this)
        pinAlan.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        kok.addView(pinAlan)

        val kaydet1 = Button(this)
        kaydet1.text = "Kurallari Kaydet ve Gonder"
        kaydet1.setOnClickListener { kaydet() }
        kok.addView(kaydet1)

        kok.addView(bolum("Engellenecek uygulamalar"))
        uygulamaKutu = LinearLayout(this)
        uygulamaKutu.orientation = LinearLayout.VERTICAL
        kok.addView(uygulamaKutu)

        val kaydet2 = Button(this)
        kaydet2.text = "Kurallari Kaydet ve Gonder"
        kaydet2.setOnClickListener { kaydet() }
        kok.addView(kaydet2)

        val sil = Button(this)
        sil.text = "Bu Cihazi Aileden Cikar"
        sil.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Emin misiniz?")
                .setMessage("Cihaz listeden kaldirilir. Cocuk cihazindaki uygulama yerel kurallarla calismaya devam eder.")
                .setPositiveButton("Cikar") { _, _ ->
                    ref.delete().addOnSuccessListener { finish() }
                }
                .setNegativeButton("Vazgec", null)
                .show()
        }
        kok.addView(sil)

        val kaydirma = ScrollView(this)
        kaydirma.addView(kok)
        setContentView(kaydirma)

        Bulut.girisYap(this, {
            dinleyici = ref.addSnapshotListener { d, e ->
                if (e != null) {
                    bilgiText.text = "Baglanti hatasi: " + e.message
                } else if (d != null && d.exists()) {
                    guncelle(d)
                } else if (d != null) {
                    finish()
                }
            }
        }, { bilgiText.text = it })
    }

    override fun onDestroy() {
        dinleyici?.remove()
        super.onDestroy()
    }

    private fun guncelle(d: DocumentSnapshot) {
        val ad = d.getString("ad") ?: "Cihaz"
        adText.text = ad
        title = ad

        val ayar = d.get("ayarlar") as? Map<*, *> ?: emptyMap<String, Any>()
        kilitli = ayar["kilitli"] as? Boolean ?: false
        kilitBtn.text = if (kilitli) "Kilidi Ac" else "Simdi Kilitle"
        val limit = (ayar["limitDk"] as? Number)?.toInt() ?: 0
        val bugun = d.getLong("bugunDk") ?: 0L

        bilgiText.text = "Model: " + (d.getString("model") ?: "-") +
            "\nSon gorulme: " + Yardim.sureOnce(d.getLong("sonGorulme") ?: 0L) +
            "\nBugun: " + Yardim.sure(bugun) + (if (limit > 0) " / limit " + limit + " dk" else " (limit yok)") +
            "\nDurum: " + (if (kilitli) "KILITLI" else "acik") +
            "\nUygulama surumu: v" + (d.getLong("surum") ?: 0L)

        val konum = d.get("konum") as? Map<*, *>
        if (konum != null) {
            sonLat = (konum["lat"] as? Number)?.toDouble() ?: 0.0
            sonLng = (konum["lng"] as? Number)?.toDouble() ?: 0.0
            val dogruluk = (konum["dogruluk"] as? Number)?.toInt() ?: 0
            val zaman = (konum["zaman"] as? Number)?.toLong() ?: 0L
            konumVar = true
            konumText.text = "Son konum: " + Yardim.sureOnce(zaman) + " (yaklasik " + dogruluk + " m)" +
                "\n" + String.format("%.5f, %.5f", sonLat, sonLng)
        } else {
            konumVar = false
            konumText.text = "Henuz konum gelmedi. Cocuk cihazinda konum izni verilmis olmali."
        }

        val kullanim = d.get("kullanim") as? List<*>
        val sb = StringBuilder()
        kullanim?.forEach { k ->
            val m = k as? Map<*, *> ?: return@forEach
            sb.append(m["ad"] ?: "?").append(": ").append(Yardim.sure((m["dk"] as? Number)?.toLong() ?: 0L)).append("\n")
        }
        kullanimText.text = if (sb.isEmpty()) "Henuz veri yok." else sb.toString()

        if (!formDolduruldu) {
            formDolduruldu = true
            limitAlan.setText(limit.toString())
            basAlan.setText(Yardim.dkYaz((ayar["yatisBas"] as? Number)?.toInt() ?: -1))
            bitAlan.setText(Yardim.dkYaz((ayar["yatisBit"] as? Number)?.toInt() ?: -1))
        }

        if (kutular.isEmpty()) {
            val engelli = (ayar["engelli"] as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
            val uygulamalar = d.get("uygulamalar") as? List<*>
            if (uygulamalar.isNullOrEmpty()) {
                if (uygulamaKutu.childCount == 0) {
                    uygulamaKutu.addView(etiket("Uygulama listesi cocuk cihazindan bekleniyor..."))
                }
            } else {
                uygulamaKutu.removeAllViews()
                for (u in uygulamalar) {
                    val m = u as? Map<*, *> ?: continue
                    val p = m["p"] as? String ?: continue
                    val cb = CheckBox(this)
                    cb.text = (m["ad"] as? String) ?: p
                    cb.isChecked = engelli.contains(p)
                    uygulamaKutu.addView(cb)
                    kutular.add(Pair(p, cb))
                }
            }
        }
    }

    private fun kaydet() {
        val guncel = hashMapOf<String, Any>(
            "ayarlar.limitDk" to (limitAlan.text.toString().trim().toIntOrNull() ?: 0),
            "ayarlar.yatisBas" to Yardim.dkOku(basAlan.text.toString()),
            "ayarlar.yatisBit" to Yardim.dkOku(bitAlan.text.toString())
        )
        if (kutular.isNotEmpty()) {
            guncel["ayarlar.engelli"] = kutular.filter { it.second.isChecked }.map { it.first }
        }
        val pin = pinAlan.text.toString().trim()
        if (pin.isNotEmpty()) {
            if (pin.length < 4) {
                mesaj("PIN en az 4 rakam olmali")
                return
            }
            guncel["ayarlar.pin"] = pin
        }
        ref.update(guncel)
            .addOnSuccessListener {
                pinAlan.setText("")
                mesaj("Kurallar kaydedildi ve cihaza gonderildi")
            }
            .addOnFailureListener { mesaj("Hata: " + it.message) }
    }

    private fun bolum(t: String): TextView {
        val tv = TextView(this)
        tv.text = t
        tv.textSize = 19f
        tv.setTypeface(null, Typeface.BOLD)
        tv.setPadding(0, 40, 0, 8)
        return tv
    }

    private fun etiket(t: String): TextView {
        val tv = TextView(this)
        tv.text = t
        tv.textSize = 15f
        tv.setPadding(0, 16, 0, 4)
        return tv
    }

    private fun mesaj(m: String) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    }
}
