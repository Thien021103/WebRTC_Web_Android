package io.getstream.webrtc.sample.compose

import android.app.Application
import android.util.Log
import com.cloudinary.android.MediaManager
import io.getstream.log.android.AndroidStreamLogger

class WebRTCApp : Application() {
  private var isMediaManagerInitialized = false

  override fun onCreate() {
    super.onCreate()
    AndroidStreamLogger.installOnDebuggableApp(this)
  }

  fun initializeMediaManager(config: Map<String, String>) {
    synchronized(this) {
      if (!isMediaManagerInitialized) {
        if (config["cloud_name"].isNullOrEmpty() || config["api_key"].isNullOrEmpty() || config["api_secret"].isNullOrEmpty()) {
          Log.e("WebRTCApp", "Invalid Cloudinary config")
          return
        }
        MediaManager.init(this, config)
        isMediaManagerInitialized = true
        Log.d("WebRTCApp", "MediaManager initialized with cloud_name: ${config["cloud_name"]}")
      } else {
        Log.w("WebRTCApp", "MediaManager already initialized, skipping")
      }
    }
  }

  fun clearConfig() {
    synchronized(this) {
      isMediaManagerInitialized = false
      Log.d("WebRTCApp", "Cloudinary config cleared")
    }
  }
}