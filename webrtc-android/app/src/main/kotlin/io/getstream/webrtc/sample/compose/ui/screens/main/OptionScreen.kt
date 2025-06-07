package io.getstream.webrtc.sample.compose.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.webrtc.sample.compose.ui.theme.Cyan1200
import io.getstream.webrtc.sample.compose.ui.theme.Cyan500
import io.getstream.webrtc.sample.compose.ui.theme.Cyan900

@Composable
fun OptionScreen(
  role: String,
  identifier: String,
  onVideosClick: () -> Unit,
  onSignallingClick: () -> Unit,
  onDoorClick: () -> Unit,
  onUserManagementClick: () -> Unit,
) {

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "You are logged in as\n$role $identifier!",
      fontSize = 24.sp,
      color = MaterialTheme.colors.primary,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(bottom = 32.dp)
    )
    Button(
      onClick = onSignallingClick,
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(12.dp),
      elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
      colors = ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.colors.secondary,
        contentColor = MaterialTheme.colors.onSecondary
      )
    ) {
      Icon(
        imageVector = Icons.Filled.PlayArrow,
        contentDescription = "Start Signalling",
        modifier = Modifier.padding(end = 8.dp)
      )
      Text("Start Signalling", fontSize = 20.sp)
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
      onClick = onVideosClick,
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(12.dp),
      elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
      colors = ButtonDefaults.buttonColors(
        backgroundColor = Cyan1200,
        contentColor = MaterialTheme.colors.onPrimary
      )
    ) {
      Icon(
        imageVector = Icons.Filled.VideoLibrary,
        contentDescription = "View Videos",
        modifier = Modifier.padding(end = 8.dp)
      )
      Text("View Videos", fontSize = 20.sp)
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
      onClick = onDoorClick,
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(12.dp),
      elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
      colors = ButtonDefaults.buttonColors(
        backgroundColor = Cyan900,
        contentColor = MaterialTheme.colors.onPrimary
      )
    ) {
      Icon(
        imageVector = Icons.Filled.History,
        contentDescription = "Door History",
        modifier = Modifier.padding(end = 8.dp)
      )
      Text("Door Management", fontSize = 20.sp)
    }
    if (role == "Owner") {
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = onUserManagementClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
        colors = ButtonDefaults.buttonColors(
          backgroundColor = Cyan500,
          contentColor = MaterialTheme.colors.onPrimary
        )
      ) {
        Icon(
          imageVector = Icons.Filled.Group,
          contentDescription = "Manage Users",
          modifier = Modifier.padding(end = 8.dp)
        )
        Text("Manage Users", fontSize = 20.sp)
      }
    }
  }
}