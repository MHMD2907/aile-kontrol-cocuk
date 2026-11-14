// surum: 0.7.0 - M-ONUR Aile Cocuk Kontrol - ayarlar, PIN ve aile baglantisi
package com.monur.ailekontrol

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

object Ayarlar {

    private fun sp(c: Context) = c.getSharedPreferences("aile_ayarlar", Context.MODE_PRIVATE)

    fun pin(c: Context): String = sp(c).getString("pin", "") ?: ""
    fun pinKaydet(c: Context, p: String) = sp(c).edit().putString("pin", p).apply()

    // 0 = limitsiz
    fun gunlukLimitDk(c: Context): Int = sp(c).getInt("limit_dk", 0)
    fun limitKaydet(c: Context, dk: Int) = sp(c).edit().putInt("limit_dk", dk).apply()

    // gunun dakikasi, -1 = kapali
    fun yatisBas(c: Context): Int = sp(c).getInt("yatis_bas", -1)
    fun yatisBit(c: Context): Int = sp(c).getInt("yatis_bit", -1)
    fun yatisKaydet(c: Context, bas: Int, bit: Int) =
        sp(c).edit().putInt("yatis_bas", bas).putInt("yatis_bit", bit).apply()

    fun engelliler(c: Context): Set<String> = sp(c).getStringSet("engelli", emptySet()) ?: emptySet()
    fun engelliKaydet(c: Context, set: Set<String>) = sp(c).edit().putStringSet("engelli", HashSet(set)).apply()

    fun geciciIzinBitis(c: Context): Long = sp(c).getLong("gecici_izin", 0L)
    fun geciciIzinVer(c: Context, dk: Int) =
        sp(c).edit().putLong("gecici_izin", System.currentTimeMillis() + dk * 60000L).apply()

    // Uzaktan kilit (ebeveyn uygulamasindan)
    fun kilitli(c: Context): Boolean = sp(c).getBoolean("kilitli", false)
    fun kilitliKaydet(c: Context, k: Boolean) = sp(c).edit().putBoolean("kilitli", k).apply()

    // Aile baglantisi
    fun aileId(c: Context): String = sp(c).getString("aile_id", "") ?: ""
    fun cihazAdi(c: Context): String = sp(c).getString("cihaz_adi", "") ?: ""
    fun bagli(c: Context): Boolean = aileId(c).isNotEmpty()
    fun eslesmeKaydet(c: Context, aileId: String, ad: String) =
        sp(c).edit().putString("aile_id", aileId).putString("cihaz_adi", ad).apply()

    fun cihazId(c: Context): String {
        var id = sp(c).getString("cihaz_id", "") ?: ""
        if (id.isEmpty()) {
            id = java.util.UUID.randomUUID().toString().replace("-", "").take(20)
            sp(c).edit().putString("cihaz_id", id).apply()
        }
        return id
    }

    fun sonKonumIstegi(c: Context): Long = sp(c).getLong("son_konum_istegi", 0L)
    fun sonKonumIstegiKaydet(c: Context, t: Long) = sp(c).edit().putLong("son_konum_istegi", t).apply()
}

fun pinSor(a: AppCompatActivity, basarili: () -> Unit) {
    val et = EditText(a)
    et.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
    AlertDialog.Builder(a)
        .setTitle("Ebeveyn PIN")
        .setView(et)
        .setPositiveButton("Tamam") { _, _ ->
            if (et.text.toString() == Ayarlar.pin(a)) {
                basarili()
            } else {
                Toast.makeText(a, "PIN hatali", Toast.LENGTH_SHORT).show()
            }
        }
        .setNegativeButton("Iptal", null)
        .show()
}

fun pinBelirle(a: AppCompatActivity, bitti: () -> Unit) {
    val et = EditText(a)
    et.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
    AlertDialog.Builder(a)
        .setTitle("Yeni ebeveyn PIN (en az 4 rakam)")
        .setView(et)
        .setPositiveButton("Kaydet") { _, _ ->
            val p = et.text.toString()
            if (p.length >= 4) {
                Ayarlar.pinKaydet(a, p)
                Toast.makeText(a, "PIN kaydedildi", Toast.LENGTH_SHORT).show()
                bitti()
            } else {
                Toast.makeText(a, "En az 4 rakam girin", Toast.LENGTH_SHORT).show()
            }
        }
        .setNegativeButton("Iptal", null)
        .show()
}
