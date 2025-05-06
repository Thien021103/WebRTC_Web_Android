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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.messaging.FirebaseMessaging
import io.getstream.webrtc.sample.compose.ui.screens.list.VideoListScreen
import io.getstream.webrtc.sample.compose.ui.screens.list.VideoListViewModel
import io.getstream.webrtc.sample.compose.ui.screens.stage.LoginScreen
import io.getstream.webrtc.sample.compose.ui.screens.stage.RegisterScreen
import io.getstream.webrtc.sample.compose.ui.screens.stage.StageScreen
import io.getstream.webrtc.sample.compose.ui.screens.video.VideoCallScreen
import io.getstream.webrtc.sample.compose.ui.theme.WebrtcSampleComposeTheme
import io.getstream.webrtc.sample.compose.webrtc.SignalingClient
import io.getstream.webrtc.sample.compose.webrtc.peer.StreamPeerConnectionFactory
import io.getstream.webrtc.sample.compose.webrtc.sessions.LocalWebRtcSessionManager
import io.getstream.webrtc.sample.compose.webrtc.sessions.WebRtcSessionManager
import io.getstream.webrtc.sample.compose.webrtc.sessions.WebRtcSessionManagerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
          var cameraId by remember { mutableStateOf("") }
          var accessToken by remember { mutableStateOf("") }

          when (currentScreen) {
            Screen.Login -> LoginScreen(
              fcmToken = fcmToken,
              onSuccess = { id, token ->
                cameraId = id
                accessToken = token
                currentScreen = Screen.Main
              },
              onRegister = { currentScreen = Screen.Register }
            )
            Screen.Register -> RegisterScreen(
              fcmToken = fcmToken,
              onSuccess = { id, token ->
                cameraId = id
                accessToken = token
                currentScreen = Screen.Main
              },
              onLogin = { currentScreen = Screen.Login }
            )
            Screen.Main -> MainScreen(
              onVideosClick = { currentScreen = Screen.Videos },
              onSignallingClick = { currentScreen = Screen.Signalling }
            )
            Screen.Videos -> VideoListScreen(
              viewModel = VideoListViewModel(this),
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

@Composable
fun MainScreen(onVideosClick: () -> Unit, onSignallingClick: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Center
  ) {
    Button(onClick = onVideosClick, modifier = Modifier.fillMaxWidth()) {
      Text("View Videos", fontSize = 20.sp)
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onSignallingClick, modifier = Modifier.fillMaxWidth()) {
      Text("Start Signalling", fontSize = 20.sp)
    }
  }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SignallingScreen(
  id: String,
  accessToken: String,
  onBack: () -> Unit
) {
  var startedSignalling by remember { mutableStateOf(false) }
  var sessionManager by remember { mutableStateOf<WebRtcSessionManager?>(null) }
  val context = LocalContext.current
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Center
  ) {
    if (!startedSignalling) {
      Button(
        onClick = {
          sessionManager = WebRtcSessionManagerImpl(
            context = context,
            signalingClient = SignalingClient(id = id, accessToken = accessToken),
            peerConnectionFactory = StreamPeerConnectionFactory(context)
          )
          startedSignalling = true
        },
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Start Signalling", fontSize = 20.sp)
      }
    }
    if (startedSignalling && sessionManager != null) {
      CompositionLocalProvider(LocalWebRtcSessionManager provides sessionManager!!) {
        var onCallScreen by remember { mutableStateOf(false) }
        val state by sessionManager!!.signalingClient.sessionStateFlow.collectAsState()
        if (!onCallScreen) {
          StageScreen(
            state = state,
            onJoinCall = { onCallScreen = true},
            onBack = {
              startedSignalling = false
              sessionManager!!.disconnect()
            }
          )
        } else {
          VideoCallScreen() {
            onCallScreen = false
            startedSignalling = false
            sessionManager = null
          }
        }
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
      Text("Back", fontSize = 20.sp)
    }
  }
}