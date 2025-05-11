/*
 * Copyright 2023 Stream.IO, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.webrtc.sample.compose

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.firebase.messaging.FirebaseMessaging
import io.getstream.webrtc.sample.compose.ui.screens.list.VideoListScreen
import io.getstream.webrtc.sample.compose.ui.screens.list.VideoListViewModel
import io.getstream.webrtc.sample.compose.ui.screens.stage.LoginScreen
import io.getstream.webrtc.sample.compose.ui.screens.stage.MainScreen
import io.getstream.webrtc.sample.compose.ui.screens.stage.RegisterScreen
import io.getstream.webrtc.sample.compose.ui.screens.stage.SignallingScreen
import io.getstream.webrtc.sample.compose.ui.theme.WebrtcSampleComposeTheme
import kotlinx.coroutines.tasks.await

sealed class Screen {
  object Login : Screen()
  object Register : Screen()
  object Main : Screen()
  object Videos : Screen()
  object Signalling : Screen()
}

class MainActivity : ComponentActivity() {
  @RequiresApi(Build.VERSION_CODES.Q)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val permissions = arrayOf(
      Manifest.permission.CAMERA,
      Manifest.permission.RECORD_AUDIO,
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.POST_NOTIFICATIONS
    )
    requestPermissions(permissions, 0)

    setContent {
      WebrtcSampleComposeTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colors.background
        ) {
          var fcmToken by remember { mutableStateOf("") }
          LaunchedEffect(Unit) {
            try {
              fcmToken = FirebaseMessaging.getInstance().token.await()
              Log.d("FCM Token", fcmToken)
            } catch (e: Exception) {
              println("Failed to get FCM token: $e")
            }
          }
          var currentScreen: Screen by remember { mutableStateOf(Screen.Login) }
          var userEmail by remember { mutableStateOf("") }
          var cameraId by remember { mutableStateOf("") }
          var accessToken by remember { mutableStateOf("") }

          when (currentScreen) {
            Screen.Login -> LoginScreen(
              fcmToken = fcmToken,
              onSuccess = { email, id, token ->
                userEmail = email
                cameraId = id
                accessToken = token
                currentScreen = Screen.Main
              },
              onRegister = { currentScreen = Screen.Register }
            )
            Screen.Register -> RegisterScreen(
              fcmToken = fcmToken,
              onSuccess = { email, id, token ->
                userEmail = email
                cameraId = id
                accessToken = token
                currentScreen = Screen.Main
              },
              onLogin = { currentScreen = Screen.Login }
            )
            Screen.Main -> MainScreen(
              email = userEmail,
              id = cameraId,
              token = accessToken,
              onVideosClick = { currentScreen = Screen.Videos },
              onSignallingClick = { currentScreen = Screen.Signalling },
              onLogout = {
                userEmail = ""
                cameraId = ""
                accessToken = ""
                currentScreen = Screen.Login
              }
            )
            Screen.Videos -> VideoListScreen(
              viewModel = VideoListViewModel(context = this, cameraId = cameraId),
              onBack = { currentScreen = Screen.Main }
            )
            Screen.Signalling -> SignallingScreen(
              id = cameraId,
              accessToken = accessToken,
              onBack = { currentScreen = Screen.Main }
            )
          }
        }
      }
    }
  }
}