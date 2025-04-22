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
//import com.google.firebase.FirebaseApp
import io.getstream.webrtc.sample.compose.ui.screens.list.VideoListScreen
import io.getstream.webrtc.sample.compose.ui.screens.list.VideoListViewModel
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
  object Main : Screen()
  object Videos : Screen()
  object Signalling : Screen()
}

class MainActivity : ComponentActivity() {
  @RequiresApi(Build.VERSION_CODES.Q)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

//    FirebaseApp.initializeApp(this)
//    FirebaseMessaging.getInstance().subscribeToTopic("doorbell")

    val permissions = arrayOf(
      Manifest.permission.CAMERA,
      Manifest.permission.RECORD_AUDIO,
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.POST_NOTIFICATIONS
    )
    requestPermissions(permissions, 0)

    val id = 123

    setContent {
      WebrtcSampleComposeTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colors.background
        ) {
          val initialScreen = if (intent.getBooleanExtra("SHOW_SIGNALLING", false)) {
            Screen.Signalling
          } else {
            Screen.Main
          }
          var currentScreen by remember { mutableStateOf(initialScreen) }

          when (currentScreen) {
            Screen.Main -> MainScreen(
              onVideosClick = { currentScreen = Screen.Videos },
              onSignallingClick = { currentScreen = Screen.Signalling }
            )
            Screen.Videos -> VideoListScreen(
              viewModel = VideoListViewModel(this),
              onBack = { currentScreen = Screen.Main }
            )
            Screen.Signalling -> SignallingScreen(
              id = id,
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
  CoroutineScope(Dispatchers.IO).launch {
    try {
      val token = FirebaseMessaging.getInstance().token.await()
      Log.d("FCM Token", "$token")
    } catch (e: Exception) {
      println("Failed to get FCM token: $e")
    }
  }

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

@Composable
fun SignallingScreen(id: Int, onBack: () -> Unit) {
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
              signalingClient = SignalingClient(id),
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
          StageScreen(state = state) {
            onCallScreen = true
          }
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