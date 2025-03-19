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
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.webrtc.sample.compose.ui.screens.stage.StageScreen
import io.getstream.webrtc.sample.compose.ui.screens.video.VideoCallScreen
import io.getstream.webrtc.sample.compose.ui.theme.WebrtcSampleComposeTheme
import io.getstream.webrtc.sample.compose.webrtc.SignalingClient
import io.getstream.webrtc.sample.compose.webrtc.peer.StreamPeerConnectionFactory
import io.getstream.webrtc.sample.compose.webrtc.sessions.LocalWebRtcSessionManager
import io.getstream.webrtc.sample.compose.webrtc.sessions.WebRtcSessionManager
import io.getstream.webrtc.sample.compose.webrtc.sessions.WebRtcSessionManagerImpl

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 0)

    setContent {
      WebrtcSampleComposeTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colors.background
        ) {
          // UI Layout for IP input and call screen
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp),
            verticalArrangement = Arrangement.Center
          ) {
            // Initialize with default values null (will be updated after user input)
//            var signalingServerIp by remember { mutableStateOf("") }
            var startedSignalling by remember { mutableStateOf(false) }
            var sessionManager by remember { mutableStateOf<WebRtcSessionManager?>(null) }

//            TextField(
//              value = signalingServerIp,
//              onValueChange = { signalingServerIp = it },
//              label = { Text("Signaling Server IP, do not enter wrong!") },
//              modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp)),
//              textStyle = TextStyle(fontSize = 20.sp), // Custom text size here
//            )

            Button(
              onClick = {
                try {
                  // Initialize sessionManager with the user-provided IP
                  sessionManager = WebRtcSessionManagerImpl(
                    context = this@MainActivity,
                    signalingClient = SignalingClient(),
                    peerConnectionFactory = StreamPeerConnectionFactory(this@MainActivity)
                  )
                  // Allow StageScreen and VideoCallScreen
                  startedSignalling = true
                } catch(e: Exception) {
                  // Handle any exceptions and notify the user
                  Toast.makeText(this@MainActivity, "Error starting signaling: ${e.message}", Toast.LENGTH_LONG).show()
                }
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "Start Signalling",
                fontSize = 20.sp
              )
            }
            if(startedSignalling) {
              CompositionLocalProvider(LocalWebRtcSessionManager provides sessionManager!!) {
                var onCallScreen by remember { mutableStateOf(false) }
                val state by sessionManager!!.signalingClient.sessionStateFlow.collectAsState()

                if (!onCallScreen) {
                  StageScreen(state = state) { onCallScreen = true }
                } else {
                  VideoCallScreen() {
                    startedSignalling = false
                    onCallScreen = false
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
