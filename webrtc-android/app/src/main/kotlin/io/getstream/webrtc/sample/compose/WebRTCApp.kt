package io.getstream.webrtc.sample.compose

import android.app.Application
import android.util.Log
import io.getstream.log.android.AndroidStreamLogger
import com.cloudinary.android.MediaManager

class WebRTCApp : Application() {

  companion object {
    private var isMediaManagerInitialized = false
    private lateinit var cloudinaryConfig: Map<String, String>
  }
  override fun onCreate() {
    super.onCreate()

    cloudinaryConfig = mapOf(
      "cloud_name" to "",
      "api_key" to "",
      "api_secret" to ""
    )

    AndroidStreamLogger.installOnDebuggableApp(this)
  }

  fun initializeMediaManager(config: Map<String, String>) {
    synchronized(this) {
      if (!isMediaManagerInitialized) {
        if (config["cloud_name"].isNullOrEmpty() || config["api_key"].isNullOrEmpty() || config["api_secret"].isNullOrEmpty()) {
          Log.e("MyApplication", "Invalid Cloudinary config")
          return
        }
        cloudinaryConfig = config
        MediaManager.init(this, config)
        isMediaManagerInitialized = true
        Log.d("MyApplication", "MediaManager initialized with cloud_name: ${config["cloud_name"]}")
      } else {
        Log.w("MyApplication", "MediaManager already initialized, skipping")
      }
    }
  }

  fun clearConfig() {
    synchronized(this) {
      cloudinaryConfig = mapOf(
        "cloud_name" to "",
        "api_key" to "",
        "api_secret" to ""
      )
      // MediaManager cannot be deinitialized, but config is cleared
      Log.d("MyApplication", "Cloudinary config cleared")
    }
  }

  fun getCloudinaryConfig(): Map<String, String> = cloudinaryConfig
}
