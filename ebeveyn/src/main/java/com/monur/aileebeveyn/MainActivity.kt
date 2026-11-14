// surum: 0.1.0 - M-ONUR Aile Kontrol Ebeveyn - ana ekran
package com.monur.aileebeveyn

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var kok: LinearLayout
    private var liste: LinearLayout? = null
    private var bilgi: TextView? = null
    private var dinleyici: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kok = LinearLayout(this)
        kok.orientation = LinearLayout.VERTICAL
        kok.setPadding(40, 48, 40, 48)
        val kaydirma = ScrollView(this)
        kaydirma.addView(kok)
        setContentView(kaydirma)
        ekraniKur()
        guncellemeKontrol()
    }

    override fun onDestroy() {
        dinleyici?.remove()
        super.onDestroy()
    }

    private fun ekraniKur() {
        dinleyici?.remove()
        dinleyici = null
        kok.removeAllViews()
        val baslik = TextView(this)
        baslik.text = "Aile Kontrol - Ebeveyn"
        baslik.textSize = 24f
        baslik.gravity = Gravity.CENTER
        baslik.setPadding(0, 0, 0, 24)
        kok.addView(baslik)
        if (Yardim.aileId(this).isEmpty()) kurulumEkrani() else anaEkran()
    }

    private fun kurulumEkrani() {
        val aciklama = TextView(this)
        aciklama.textSize = 16f
        aciklama.text = "Hos geldiniz. Ilk kez kullaniyorsaniz 'Yeni Aile Olustur'a basin.\n\n" +
            "Esiniz zaten aile olusturduysa, ondan 'Aile Kodu'nu isteyip 'Esimin Ailesine Katil'a basin."
        aciklama.setPadding(0, 0, 0, 24)
        kok.addView(aciklama)

        val yeni = Button(this)
        yeni.text = "Yeni Aile Olustur"
        yeni.setOnClickListener {
            Bulut.girisYap(this, {
                val ref = Bulut.db().collection("aileler").document()
                ref.set(hashMapOf("olusturma" to System.currentTimeMillis()))
                    .addOnSuccessListener {
                        Yardim.aileIdKaydet(this, ref.id)
                        ekraniKur()
                    }
                    .addOnFailureListener { mesaj("Hata: " + it.message) }
            }, { mesaj(it) })
        }
        kok.addView(yeni)

        val katil = Button(this)
        katil.text = "Esimin Ailesine Katil"
        katil.setOnClickListener { aileyeKatil() }
        kok.addView(katil)
    }

    private fun aileyeKatil() {
        val alan = EditText(this)
        alan.hint = "Aile kodu"
        AlertDialog.Builder(this)
            .setTitle("Aileye Katil")
            .setMessage("Esinizin ebeveyn uygulamasindaki 'Aile Kodunu Paylas' ile gonderdigi kodu yapistirin.")
            .setView(alan)
            .setPositiveButton("Katil") { _, _ ->
                val kod = alan.text.toString().trim().substringAfterLast(" ")
                if (kod.length < 10) {
                    mesaj("Kod gecersiz")
                    return@setPositiveButton
                }
                Bulut.girisYap(this, {
                    Bulut.db().collection("aileler").document(kod).get()
                        .addOnSuccessListener { d ->
                            if (d.exists()) {
                                Yardim.aileIdKaydet(this, kod)
                                ekraniKur()
                            } else {
                                mesaj("Aile bulunamadi")
                            }
                        }
                        .addOnFailureListener { mesaj("Hata: " + it.message) }
                }, { mesaj(it) })
            }
            .setNegativeButton("Iptal", null)
            .show()
    }

    private fun anaEkran() {
        val ekle = Button(this)
        ekle.text = "+ Cocuk Cihazi Ekle"
        ekle.setOnClickListener { kodOlustur() }
        kok.addView(ekle)

        val paylas = Button(this)
        paylas.text = "Aile Kodunu Paylas (esiniz icin)"
        paylas.setOnClickListener {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_TEXT, "Aile Kontrol Ebeveyn uygulamasi aile kodu: " + Yardim.aileId(this))
            startActivity(Intent.createChooser(i, "Aile kodunu paylas"))
        }
        kok.addView(paylas)

        val b = TextView(this)
        b.textSize = 16f
        b.setPadding(0, 32, 0, 16)
        b.text = "Cihazlar yukleniyor..."
        kok.addView(b)
        bilgi = b

        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        kok.addView(l)
        liste = l

        Bulut.girisYap(this, {
            dinleyici = Bulut.db().collection("aileler").document(Yardim.aileId(this))
                .collection("cihazlar")
                .addSnapshotListener { qs, e ->
                    if (e != null) {
                        bilgi?.text = "Baglanti hatasi: " + e.message
                    } else if (qs != null) {
                        listeyiCiz(qs.documents)
                    }
                }
        }, { bilgi?.text = it })
    }

    private fun listeyiCiz(cihazlar: List<DocumentSnapshot>) {
        val l = liste ?: return
        l.removeAllViews()
        bilgi?.text = if (cihazlar.isEmpty())
            "Henuz cihaz yok. '+ Cocuk Cihazi Ekle'ye basin."
        else
            "Cihazlar (" + cihazlar.size + ")"

        for (d in cihazlar.sortedBy { it.getString("ad") ?: "" }) {
            val kart = LinearLayout(this)
            kart.orientation = LinearLayout.VERTICAL
            kart.setPadding(36, 28, 36, 28)
            val arka = GradientDrawable()
            arka.cornerRadius = 24f
            arka.setColor(Color.parseColor("#FFF3E6"))
            arka.setStroke(3, Color.parseColor("#FF8C42"))
            kart.background = arka

            val ad = TextView(this)
            ad.text = d.getString("ad") ?: "Cihaz"
            ad.textSize = 20f
            ad.setTypeface(null, Typeface.BOLD)
            ad.setTextColor(Color.parseColor("#1E3A5F"))

            val ayar = d.get("ayarlar") as? Map<*, *>
            val limit = (ayar?.get("limitDk") as? Number)?.toInt() ?: 0
            val kilitli = ayar?.get("kilitli") as? Boolean ?: false
            val bugun = d.getLong("bugunDk") ?: 0L

            val detay = TextView(this)
            detay.textSize = 15f
            detay.setTextColor(Color.parseColor("#333333"))
            detay.text = "Bugun: " + Yardim.sure(bugun) +
                (if (limit > 0) " / " + limit + " dk" else "") +
                "\nSon gorulme: " + Yardim.sureOnce(d.getLong("sonGorulme") ?: 0L) +
                (if (kilitli) "\nKILITLI" else "")

            kart.addView(ad)
            kart.addView(detay)
            kart.setOnClickListener {
                val i = Intent(this, CihazActivity::class.java)
                i.putExtra("cihazId", d.id)
                startActivity(i)
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 24
            l.addView(kart, lp)
        }
    }

    private fun kodOlustur() {
        val kod = (100000..999999).random().toString()
        Bulut.girisYap(this, {
            Bulut.db().collection("eslesme").document(kod)
                .set(hashMapOf("aileId" to Yardim.aileId(this), "olusturma" to System.currentTimeMillis()))
                .addOnSuccessListener {
                    AlertDialog.Builder(this)
                        .setTitle("Eslestirme Kodu")
                        .setMessage(
                            "Cocugun telefonunda/tabletinde Aile Cocuk Kontrol uygulamasini acin, " +
                                "'Aileye Baglan'a basin ve bu kodu girin:\n\n" + kod + "\n\n15 dakika gecerlidir."
                        )
                        .setPositiveButton("Tamam", null)
                        .show()
                }
                .addOnFailureListener { mesaj("Hata: " + it.message) }
        }, { mesaj(it) })
    }

    private fun mesaj(m: String) {
        Toast.makeText(this, m, Toast.LENGTH_LONG).show()
    }

    private fun guncellemeKontrol() {
        thread {
            try {
                val api = "https://api.github.com/repos/MHMD2907/aile-kontrol-cocuk/releases/latest"
                val json = URL(api).readText()
                val tag = JSONObject(json).optString("tag_name", "")
                val sonKod = tag.filter { it.isDigit() }.toIntOrNull() ?: return@thread
                if (sonKod > BuildConfig.VERSION_CODE) {
                    runOnUiThread {
                        AlertDialog.Builder(this)
                            .setTitle("Yeni surum var")
                            .setMessage("Ebeveyn uygulamasinin guncel surumu mevcut. Indirip kurmak ister misiniz?")
                            .setPositiveButton("Guncelle") { _, _ ->
                                val url = "https://github.com/MHMD2907/aile-kontrol-cocuk/releases/latest/download/ebeveyn-debug.apk"
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                            .setNegativeButton("Sonra", null)
                            .show()
                    }
                }
            } catch (e: Exception) {
            }
        }
    }
}
