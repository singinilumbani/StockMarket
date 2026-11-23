package com.example.stockmarket.background

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.stockmarket.R

// Shows system notifications. Used by the background worker.
object Notifier {

    const val CHANNEL_ID = "account_updates"

    // Android 8+ needs a channel. The user can switch it off in the phone's settings.
    // Creating it again is harmless, so we simply do it before every use.
    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Account updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Order fills and registration decisions"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // From Android 13 the user must have allowed notifications
    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")   // checked in canNotify above
    fun show(context: Context, id: Int, title: String, text: String) {
        if (!canNotify(context)) return

        // Tapping the notification opens the app
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val tap = PendingIntent.getActivity(
            context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // What anyone sees on a LOCKED screen: no amounts, no company names
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("MSE Invest")
            .setContentText("You have an account update")
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}

