package com.agustavomartin.botlocalensayoandroid.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.agustavomartin.botlocalensayoandroid.MainActivity
import com.agustavomartin.botlocalensayoandroid.MainActivityIntentKeys
import com.agustavomartin.botlocalensayoandroid.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.agustavomartin.botlocalensayoandroid.ui.AppContainer

class AppFirebaseMessagingService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.initialize(applicationContext)
        ensureChannel(applicationContext)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                AppContainer.authRepository.registerPushToken(token, Build.MODEL)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        ensureChannel(applicationContext)

        val title = message.data["title"]?.takeIf { it.isNotBlank() } ?: message.notification?.title ?: "Bot Ensayo"
        val body = message.data["body"]?.takeIf { it.isNotBlank() } ?: message.notification?.body ?: "Hay contenido nuevo en la biblioteca."

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivityIntentKeys.EXTRA_SCREEN, message.data["screen"] ?: "library")
            message.data["itemId"]?.toIntOrNull()?.let { putExtra(MainActivityIntentKeys.EXTRA_ITEM_ID, it) }
            message.data["itemType"]?.let { putExtra(MainActivityIntentKeys.EXTRA_ITEM_TYPE, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = BitmapFactory.decodeResource(resources, R.drawable.notification_icon)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        }
    }

    companion object {
        const val CHANNEL_ID = "audios_nuevos"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing != null) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audios nuevos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando se suben audios nuevos a la biblioteca"
            }
            manager.createNotificationChannel(channel)
        }
    }
}

