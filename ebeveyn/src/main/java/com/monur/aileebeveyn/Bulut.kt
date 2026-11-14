// surum: 0.1.0 - M-ONUR Aile Kontrol Ebeveyn - Firebase baglantisi
package com.monur.aileebeveyn

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object Bulut {
    const val PROJE_ID = "aile-kontrol-6b80f"
    const val API_KEY = "AIzaSyDGnRYCTZwxKp0VwjS-alNaN5GmDnyJOo0"
    const val APP_ID = "1:1062664872952:android:3b24523a87a9bf8b4eec0d"

    @Volatile
    private var hazir = false

    fun baslat(c: Context): Boolean {
        if (hazir) return true
        return try {
            if (FirebaseApp.getApps(c).isEmpty()) {
                val secenek = FirebaseOptions.Builder()
                    .setApiKey(API_KEY)
                    .setApplicationId(APP_ID)
                    .setProjectId(PROJE_ID)
                    .build()
                FirebaseApp.initializeApp(c.applicationContext, secenek)
            }
            hazir = true
            true
        } catch (e: Exception) {
            false
        }
    }

    fun girisYap(c: Context, sonra: () -> Unit, hata: (String) -> Unit = {}) {
        if (!baslat(c)) {
            hata("Firebase baslatilamadi")
            return
        }
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            sonra()
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener { sonra() }
            .addOnFailureListener { hata(it.message ?: "Giris hatasi") }
    }

    fun db(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
