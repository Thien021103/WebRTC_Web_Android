package io.getstream.webrtc.sample.compose.ui.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.getstream.webrtc.sample.compose.MainActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {
  companion object {
    const val DOORBELL_NOTIFICATION_ID = 1
    const val CHANNEL_ID = "channel_id"
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    Log.d("Notification", "Received message")
    showNotification(remoteMessage.notification?.title, remoteMessage.notification?.body)
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    println("FCM Token: $token")
    // Gửi token lên server nếu cần
  }

  private fun showNotification(title: String?, message: String?) {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Doorbell Notifications",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        enableVibration(true)
        setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null)
      }
      notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      putExtra("SHOW_SIGNALLING", true) // Báo hiệu mở SignallingScreen
      Log.d("Intent", "Signalling after notification")
    }
    val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setContentTitle(title ?: "Có khách!")
      .setContentText(message ?: "Mở ứng dụng để xem.")
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setVibrate(longArrayOf(0, 1000, 500, 1000))
      .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
      .build()

    notificationManager.notify(DOORBELL_NOTIFICATION_ID, notification)
  }
}