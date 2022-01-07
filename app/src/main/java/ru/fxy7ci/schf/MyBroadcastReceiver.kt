package ru.fxy7ci.schf

import android.R
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Vibrator
import android.util.Log

class MyBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // дергаем через интент
        context.sendBroadcast(Intent(StoreVals.MAIN_BRD_ALARM))
    }
}

