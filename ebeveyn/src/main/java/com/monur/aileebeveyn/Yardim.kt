// surum: 0.1.0 - M-ONUR Aile Kontrol Ebeveyn - ortak yardimcilar
package com.monur.aileebeveyn

import android.content.Context

object Yardim {

    private fun sp(c: Context) = c.getSharedPreferences("ebeveyn", Context.MODE_PRIVATE)

    fun aileId(c: Context): String = sp(c).getString("aile_id", "") ?: ""
    fun aileIdKaydet(c: Context, id: String) = sp(c).edit().putString("aile_id", id).apply()

    fun sureOnce(zaman: Long): String {
        if (zaman <= 0L) return "hic"
        val fark = (System.currentTimeMillis() - zaman) / 60000
        return when {
            fark < 2 -> "az once"
            fark < 60 -> fark.toString() + " dk once"
            fark < 1440 -> (fark / 60).toString() + " sa once"
            else -> (fark / 1440).toString() + " gun once"
        }
    }

    fun dkYaz(m: Int): String {
        if (m < 0) return ""
        val s = m / 60
        val d = m % 60
        return (if (s < 10) "0" else "") + s + ":" + (if (d < 10) "0" else "") + d
    }

    fun dkOku(metin: String): Int {
        val p = metin.trim().split(":")
        if (p.size != 2) return -1
        val s = p[0].trim().toIntOrNull() ?: return -1
        val d = p[1].trim().toIntOrNull() ?: return -1
        if (s !in 0..23 || d !in 0..59) return -1
        return s * 60 + d
    }

    fun sure(dk: Long): String =
        if (dk >= 60) (dk / 60).toString() + " sa " + (dk % 60).toString() + " dk" else dk.toString() + " dk"
}
