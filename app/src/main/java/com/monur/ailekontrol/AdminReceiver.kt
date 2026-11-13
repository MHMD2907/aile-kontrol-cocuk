// sürüm: 0.1.0 — M-ONUR Aile Kontrol (çocuk)
package com.monur.ailekontrol

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Aile Kontrol etkinleştirildi", Toast.LENGTH_SHORT).show()
    }

    // Çocuk yöneticiyi kapatıp uygulamayı silmeye çalışırsa bu uyarı çıkar.
    // Sonraki adım: burada ebeveyne anında bildirim (FCM) gönderilecek.
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Dikkat: Bu korumayı kapatmak için ebeveyn parolası gerekir. " +
            "İşlem ebeveyne bildirilecektir."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Aile Kontrol kapatıldı", Toast.LENGTH_SHORT).show()
    }
}
