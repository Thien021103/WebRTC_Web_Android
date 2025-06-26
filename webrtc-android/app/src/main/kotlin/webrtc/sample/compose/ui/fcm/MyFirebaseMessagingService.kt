package webrtc.sample.compose.ui.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import webrtc.sample.compose.MainActivity
import webrtc.sample.compose.R

class MyFirebaseMessagingService : FirebaseMessagingService() {
  companion object {
    const val DOORBELL_NOTIFICATION_ID = 1
    const val CHANNEL_ID = "channel_id"
    private var mediaPlayer: MediaPlayer? = null

    fun stopRingingSound(context: Context) {
      try {
        mediaPlayer?.let {
          if (it.isPlaying) it.stop()
          it.release()
        }
        mediaPlayer = null
        Log.d("FCM", "Ringtone stopped")
      } catch (e: Exception) {
        Log.e("FCM", "Error stopping ringtone: ${e.message}")
      }
    }

    fun cancelNotification(context: Context) {
      try {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(DOORBELL_NOTIFICATION_ID)
        Log.d("FCM", "Notification canceled")
      } catch (e: Exception) {
        Log.e("FCM", "Error canceling notification: ${e.message}")
      }
    }
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    Log.d("Notification", "Received message: data=${remoteMessage.data}")

    val type = remoteMessage.data["type"]
    val title = when (type) {
      "notify" -> "You have visitors"
      "human" -> "People detected at your door"
      else -> "Doorbell Alert"
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Doorbell Notifications",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        enableVibration(true)
        enableLights(true)
        setShowBadge(true)
        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        setBypassDnd(true)
      }
      getSystemService(Context.NOTIFICATION_SERVICE).let { it as NotificationManager }.createNotificationChannel(channel)
      Log.d("FCM", "Notification channel created")
    }

    try {
      showNotification(title, "Open the app to see!")
      if (type == "notify") {
        startRinging()
        Handler(Looper.getMainLooper()).postDelayed({
          stopRingingSound(this)
          cancelNotification(this)
        }, 10000)
      }
    } catch (e: Exception) {
      Log.e("FCM", "Error in notification or ringing: ${e.message}")
    }
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.d("FCM Token", "New token: $token")
  }

  private fun showNotification(title: String, message: String) {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val intent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      putExtra("SHOW_SIGNALLING", true)
      putExtra("IS_CALL_NOTIFICATION", true)
    }
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher_2)
      .setContentTitle(title)
      .setContentText(message)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .setVibrate(longArrayOf(0, 1000, 500, 1000))
      .setSound(null)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .build()

    try {
      notificationManager.notify(DOORBELL_NOTIFICATION_ID, notification)
      Log.d("FCM", "Notification posted with ID: $DOORBELL_NOTIFICATION_ID")
    } catch (e: Exception) {
      Log.e("FCM", "Failed to show notification: ${e.message}")
    }
  }

  private fun startRinging() {
    stopRingingSound(this)
    mediaPlayer = MediaPlayer().apply {
      try {
        setDataSource(
          this@MyFirebaseMessagingService,
          RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        )
        isLooping = true
        prepare()
        start()
        Log.d("FCM", "Ringtone started")
      } catch (e: Exception) {
        Log.e("MediaPlayer", "Error playing ringtone: ${e.message}")
      }
    }
  }
}