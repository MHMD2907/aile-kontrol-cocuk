// surum: 0.7.0 - M-ONUR Aile Cocuk Kontrol - bulut senkronizasyonu
package com.monur.ailekontrol

import android.Manifest
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import java.util.Calendar

object Senkron {

    fun cihazRef(c: Context): DocumentReference =
        Bulut.db().collection("aileler").document(Ayarlar.aileId(c))
            .collection("cihazlar").document(Ayarlar.cihazId(c))

    fun eslestir(c: Context, kod: String, ad: String, sonuc: (Boolean, String) -> Unit) {
        Bulut.girisYap(c, {
            val eslesme = Bulut.db().collection("eslesme").document(kod)
            eslesme.get().addOnSuccessListener { d ->
                val aileId = d.getString("aileId")
                val zaman = d.getLong("olusturma") ?: 0L
                if (aileId.isNullOrEmpty()) {
                    sonuc(false, "Kod bulunamadi. Ebeveyn uygulamasindan yeni kod alin.")
                } else if (System.currentTimeMillis() - zaman > 15 * 60000L) {
                    sonuc(false, "Kodun suresi dolmus. Yeni kod alin.")
                } else {
                    Ayarlar.eslesmeKaydet(c, aileId, ad)
                    val veri = hashMapOf<String, Any>(
                        "ad" to ad,
                        "model" to (Build.MANUFACTURER + " " + Build.MODEL),
                        "sonGorulme" to System.currentTimeMillis(),
                        "surum" to BuildConfig.VERSION_CODE,
                        "ayarlar" to yerelAyarlar(c)
                    )
                    cihazRef(c).set(veri, SetOptions.merge())
                        .addOnSuccessListener {
                            eslesme.delete()
                            uygulamaListesiGonder(c)
                            sonuc(true, "Aileye baglandi.")
                        }
                        .addOnFailureListener {
                            Ayarlar.eslesmeKaydet(c, "", "")
                            sonuc(false, it.message ?: "Yazma hatasi")
                        }
                }
            }.addOnFailureListener { sonuc(false, it.message ?: "Okuma hatasi") }
        }, { sonuc(false, it) })
    }

    fun yerelAyarlar(c: Context): HashMap<String, Any> = hashMapOf(
        "limitDk" to Ayarlar.gunlukLimitDk(c),
        "yatisBas" to Ayarlar.yatisBas(c),
        "yatisBit" to Ayarlar.yatisBit(c),
        "engelli" to Ayarlar.engelliler(c).toList(),
        "kilitli" to Ayarlar.kilitli(c),
        "pin" to Ayarlar.pin(c)
    )

    fun ayarlariYukle(c: Context) {
        if (!Ayarlar.bagli(c)) return
        Bulut.girisYap(c, {
            cihazRef(c).set(hashMapOf("ayarlar" to yerelAyarlar(c)), SetOptions.merge())
        })
    }

    fun uzakAyarUygula(c: Context, d: DocumentSnapshot) {
        val ham = d.get("ayarlar") as? Map<*, *> ?: return
        (ham["limitDk"] as? Number)?.let { Ayarlar.limitKaydet(c, it.toInt()) }
        val bas = (ham["yatisBas"] as? Number)?.toInt()
        val bit = (ham["yatisBit"] as? Number)?.toInt()
        if (bas != null && bit != null) Ayarlar.yatisKaydet(c, bas, bit)
        (ham["engelli"] as? List<*>)?.let { l -> Ayarlar.engelliKaydet(c, l.filterIsInstance<String>().toSet()) }
        (ham["kilitli"] as? Boolean)?.let { Ayarlar.kilitliKaydet(c, it) }
        (ham["pin"] as? String)?.let { if (it.length >= 4) Ayarlar.pinKaydet(c, it) }
    }

    @Suppress("DEPRECATION")
    fun durumGonder(c: Context) {
        if (!Ayarlar.bagli(c)) return
        val usm = c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val ana = c.packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0
        )?.activityInfo?.packageName
        val veriler = try {
            usm.queryAndAggregateUsageStats(cal.timeInMillis, System.currentTimeMillis()).values
        } catch (e: Exception) {
            emptyList()
        }
        var toplam = 0L
        val liste = ArrayList<HashMap<String, Any>>()
        for (u in veriler.sortedByDescending { it.totalTimeInForeground }) {
            if (u.packageName == c.packageName || u.packageName == ana) continue
            toplam += u.totalTimeInForeground
            if (u.totalTimeInForeground >= 60000 && liste.size < 20) {
                liste.add(hashMapOf<String, Any>("ad" to etiket(c, u.packageName), "dk" to u.totalTimeInForeground / 60000))
            }
        }
        val veri = hashMapOf<String, Any>(
            "sonGorulme" to System.currentTimeMillis(),
            "bugunDk" to toplam / 60000,
            "kullanim" to liste,
            "surum" to BuildConfig.VERSION_CODE
        )
        cihazRef(c).set(veri, SetOptions.merge())
    }

    @Suppress("DEPRECATION")
    fun uygulamaListesiGonder(c: Context) {
        if (!Ayarlar.bagli(c)) return
        val pm = c.packageManager
        val ana = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val liste = pm.queryIntentActivities(ana, 0)
            .map { Pair(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .distinctBy { it.first }
            .filter { it.first != c.packageName }
            .sortedBy { it.second.lowercase() }
            .map { hashMapOf("p" to it.first, "ad" to it.second) }
        cihazRef(c).set(hashMapOf("uygulamalar" to liste), SetOptions.merge())
    }

    fun konumGonder(c: Context) {
        if (!Ayarlar.bagli(c)) return
        val ince = ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION)
        val kaba = ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (ince != PackageManager.PERMISSION_GRANTED && kaba != PackageManager.PERMISSION_GRANTED) return
        try {
            LocationServices.getFusedLocationProviderClient(c)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { l ->
                    if (l != null) {
                        val konum = hashMapOf<String, Any>(
                            "lat" to l.latitude,
                            "lng" to l.longitude,
                            "dogruluk" to l.accuracy.toDouble(),
                            "zaman" to System.currentTimeMillis()
                        )
                        cihazRef(c).set(hashMapOf("konum" to konum), SetOptions.merge())
                    }
                }
        } catch (e: SecurityException) {
        }
    }

    @Suppress("DEPRECATION")
    private fun etiket(c: Context, pkg: String): String = try {
        c.packageManager.getApplicationLabel(c.packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }
}
