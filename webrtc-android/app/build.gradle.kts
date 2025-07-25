import java.io.FileInputStream
import java.util.Properties

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id(libs.plugins.android.application.get().pluginId)
  id(libs.plugins.kotlin.android.get().pluginId)
  id(libs.plugins.compose.compiler.get().pluginId)

  // Add the ID of the plugin for Firebase
  id("com.google.gms.google-services")
  id("com.google.firebase.appdistribution")
  id("com.google.firebase.crashlytics")
  id("com.google.firebase.firebase-perf")
}

val localProperties = Properties()
localProperties.load(FileInputStream(rootProject.file("local.properties")))

android {
  namespace = "webrtc.sample.compose"
  compileSdk = Configurations.compileSdk

  defaultConfig {
    applicationId = "webrtc.sample.compose"
    minSdk = Configurations.minSdk
    targetSdk = Configurations.targetSdk
    versionCode = Configurations.versionCode
    versionName = Configurations.versionName

    buildConfigField(
      "String",
      "SIGNALING_SERVER_IP_ADDRESS",
      localProperties["SIGNALING_SERVER_IP_ADDRESS"].toString()
    )
  }

  kotlinOptions {
    jvmTarget = "17"
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  packagingOptions {
    resources {
      excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
  }

  lint {
    abortOnError = false
  }
}

dependencies {
  // compose
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling)
  implementation(libs.androidx.compose.material)
  implementation(libs.androidx.compose.material.iconsExtended)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.foundation.layout)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.constraintlayout)
  implementation(libs.androidx.navigation.compose)

  // image loading
  implementation(libs.landscapist.glide)

  // webrtc
  implementation(libs.webrtc)
  implementation(libs.okhttp.logging)

  // coroutines
  implementation(libs.kotlinx.coroutines.android)

  // logger
  implementation(libs.stream.log)
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.firebase.messaging.ktx)

  // firebase
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.perf.ktx)

  // cloudinary
  implementation(libs.cloudinary.android)

  // json
  implementation(libs.kotlinx.serialization.json)
}