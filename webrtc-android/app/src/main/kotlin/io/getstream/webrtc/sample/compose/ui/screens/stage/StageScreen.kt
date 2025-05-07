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

package io.getstream.webrtc.sample.compose.ui.screens.stage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.webrtc.sample.compose.R
import io.getstream.webrtc.sample.compose.webrtc.WebRTCSessionState

@Composable
fun StageScreen(
  state: WebRTCSessionState,
  onJoinCall: () -> Unit,
  onBack: () -> Unit
) {
  var enabledCall by remember { mutableStateOf(false) }

  val (text, statusText) = when (state) {
    WebRTCSessionState.Offline -> {
      enabledCall = false
      stringResource(id = R.string.button_start_session) to "Offline"
    }
    WebRTCSessionState.Impossible -> {
      enabledCall = false
      stringResource(id = R.string.session_impossible) to "Impossible"
    }
    WebRTCSessionState.Ready -> {
      enabledCall = false
      stringResource(id = R.string.session_ready) to "Ready"
    }
    WebRTCSessionState.Creating -> {
      enabledCall = true
      stringResource(id = R.string.session_creating) to "Creating"
    }
    WebRTCSessionState.Active -> {
      enabledCall = false
      stringResource(id = R.string.session_active) to "Active"
    }
  }

  Scaffold(
    content = { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Session Status: $statusText",
          fontSize = 24.sp,
          color = MaterialTheme.colors.primary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(
          onClick = onJoinCall,
          enabled = enabledCall,
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
          shape = RoundedCornerShape(12.dp),
          elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
          colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.onSecondary
          )
        ) {
          Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Join Session",
            modifier = Modifier.padding(end = 8.dp)
          )
          Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
          onClick = onBack,
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
          shape = RoundedCornerShape(12.dp),
          elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
          colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.onPrimary
          )
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            modifier = Modifier.padding(end = 8.dp)
          )
          Text(
            text = "Back",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  )
}
