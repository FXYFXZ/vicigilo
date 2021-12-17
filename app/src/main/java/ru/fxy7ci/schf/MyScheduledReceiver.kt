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


class MyScheduledReceiver() : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // дергаем через интент
        context.sendBroadcast(Intent("INTERNET_LOST"))


        val scheduledIntent = Intent(context, MainActivity::class.java)
        scheduledIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            scheduledIntent, 0
        )

        val notificationManager = context
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val res = context.resources
        val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification: Notification = Notification.Builder(context)
            .setContentIntent(contentIntent)
            .setContentText("Да накорми кота, наконец") // Текст уведомления
            .setContentTitle("Время кормить кота") // Заголовок уведомления
            .setSmallIcon(R.drawable.sym_def_app_icon)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    res,
                    R.drawable.sym_def_app_icon
                )
            )
            .setTicker("Накорми кота!") // текст в строке состояния
           // .setWhen(System.currentTimeMillis()).setAutoCancel(true)
            .setSound(alarmSound)
            .setLights(0xff00ff, 300, 100)
            .build()

          notificationManager.notify(1, notification)


        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(200)


    }
}

