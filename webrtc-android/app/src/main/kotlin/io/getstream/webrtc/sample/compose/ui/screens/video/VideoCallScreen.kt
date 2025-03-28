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

package io.getstream.webrtc.sample.compose.ui.screens.video

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.webrtc.sample.compose.ui.components.VideoRenderer
import io.getstream.webrtc.sample.compose.webrtc.sessions.LocalWebRtcSessionManager
import io.getstream.log.taggedLogger
import io.getstream.webrtc.sample.compose.ui.components.AudioRecorder
import kotlinx.coroutines.delay

@Composable
fun VideoCallScreen(
  onCancelCall: () -> Unit
) {
  val sessionManager = LocalWebRtcSessionManager.current

  val remoteVideoTrackState by sessionManager.remoteVideoTrackFlow.collectAsState(null)
  val remoteVideoTrack = remoteVideoTrackState

//  val localVideoTrackState by sessionManager.localVideoTrackFlow.collectAsState(null)
//  val localVideoTrack = localVideoTrackState

  val remoteAudioTrackState by sessionManager.remoteAudioTrackFlow.collectAsState(initial = null)
  val remoteAudioTrack = remoteAudioTrackState


  var callMediaState by remember { mutableStateOf(CallMediaState()) }
  LaunchedEffect(key1 = Unit) {
    sessionManager.onSessionScreenReady()
  }

  Box(
    modifier = Modifier.fillMaxSize()
  ) {
    var parentSize: IntSize by remember { mutableStateOf(IntSize(0, 0)) }
    if (remoteVideoTrack != null) {
      VideoRenderer(
        videoTrack = remoteVideoTrack,
        modifier = Modifier
          .fillMaxSize()
          .absolutePadding(0.dp, 0.dp, 0.dp, 100.dp)
          .clip(RoundedCornerShape(5.dp))
          .onSizeChanged { parentSize = it }
      )
    }
    if(remoteAudioTrack != null) {
      AudioRecorder(audioTrack = remoteAudioTrack)
    }
// Enhanced logging for local video condition
//    if (localVideoTrack != null) {
////      if (callMediaState.isCameraEnabled) {
//        Log.d("VideoCallScreen", "Local video track available and camera enabled")
//        FloatingVideoRenderer(
//          modifier = Modifier
//            .size(width = 150.dp, height = 210.dp)
//            .clip(RoundedCornerShape(16.dp))
//            .align(Alignment.TopEnd),
//          videoTrack = localVideoTrack,
//          parentBounds = parentSize,
//          paddingValues = PaddingValues(0.dp)
//        )
////      } else {
////        Log.d ( "VideoCallScreen", "Local video track exists but camera is disabled: isCameraEnabled=${callMediaState.isCameraEnabled}" )
////      }
//    } else {
//      Log.d ( "VideoCallScreen", "Local video track null" )
//    }

//    val activity = (LocalContext.current as? Activity)

    VideoCallControls(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter),
      callMediaState = callMediaState,
      onCallAction = {
        when (it) {
          is CallAction.ToggleMicroPhone -> {
            val enabled = callMediaState.isMicrophoneEnabled.not()
            callMediaState = callMediaState.copy(isMicrophoneEnabled = enabled)
            sessionManager.enableMicrophone(enabled)
          }
          is CallAction.ToggleCamera -> {
            val enabled = callMediaState.isCameraEnabled.not()
            callMediaState = callMediaState.copy(isCameraEnabled = enabled)
            sessionManager.enableCamera(enabled)
          }
          CallAction.FlipCamera -> sessionManager.flipCamera()
          CallAction.LeaveCall -> {
            sessionManager.disconnect()
            onCancelCall.invoke()
//            activity?.finish()
          }
        }
      }
    )
  }
}
