// surum: 0.6.0 - M-ONUR Aile Cocuk Kontrol - acilista servisi baslat
package com.monur.ailekontrol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat

class BaslangicAlici : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Settings.canDrawOverlays(context)) {
            ContextCompat.startForegroundService(context, Intent(context, KontrolServisi::class.java))
        }
    }
}
