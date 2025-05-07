package io.getstream.webrtc.sample.compose.ui.screens.stage

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
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScreen(
  id: String,
  onVideosClick: () -> Unit,
  onSignallingClick: () -> Unit,
  onLogout: () -> Unit
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (id.isNotBlank()) "Welcome to camera group: $id!" else "Welcome!",
            fontSize = 20.sp
          )
        },
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary
      )
    },
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
          text = "You are logged in!",
          fontSize = 24.sp,
          color = MaterialTheme.colors.primary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(
          onClick = onSignallingClick,
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
            contentDescription = "Start Signalling",
            modifier = Modifier.padding(end = 8.dp)
          )
          Text("Start Signalling", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = onVideosClick,
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
          shape = RoundedCornerShape(12.dp),
          elevation = ButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.VideoLibrary,
            contentDescription = "View Videos",
            modifier = Modifier.padding(end = 8.dp)
          )
          Text("View Videos", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
          onClick = onLogout,
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
          shape = RoundedCornerShape(12.dp),
          elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
          colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.error,
            contentColor = MaterialTheme.colors.onError
          )
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = "Logout",
            modifier = Modifier.padding(end = 8.dp)
          )
          Text("Logout", fontSize = 20.sp)
        }
      }
    }
  )
}