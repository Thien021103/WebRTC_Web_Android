package io.getstream.webrtc.sample.compose.ui.screens.stage

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Composable
fun MainScreen(
  role: String,
  identifier: String,
  id: String,
  token: String,
  onVideosClick: () -> Unit,
  onSignallingClick: () -> Unit,
  onDoorClick: () -> Unit,
  onUserManagementClick: () -> Unit,
  onLogout: () -> Unit
) {
  var askLogout by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Welcome!",
            fontSize = 20.sp
          )
        },
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary
      )
    },
    content = { padding ->
      Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
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
          elevation = ButtonDefaults.elevation(defaultElevation = 4.dp)
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
            backgroundColor = MaterialTheme.colors.primary,
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
              backgroundColor = MaterialTheme.colors.primary,
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
        Spacer(modifier = Modifier.height(35.dp))
        Button(
          onClick = { askLogout = true },
          modifier = Modifier.fillMaxWidth().height(56.dp),
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
  if (askLogout) {
    AlertDialog(
      onDismissRequest = { askLogout = false },
      title = {
        Text(
          text = stringResource(R.string.confirm_logout),
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Text(
          text = stringResource(R.string.logout_message),
          fontSize = 16.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            performLogOut(role = role, identifier = identifier, groupId = id, accessToken = token)
            onLogout()
            askLogout = false
          },
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.height(50.dp),
          colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.error,
            contentColor = MaterialTheme.colors.onError
          )
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 8.dp)
          ) {
            Text(
              text = stringResource(R.string.confirm),
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { askLogout = false }) {
          Text(stringResource(R.string.cancel), fontSize = 14.sp)
        }
      },
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.height(170.dp),
      backgroundColor = MaterialTheme.colors.surface
    )
  }
}

fun performLogOut (
  role: String,
  identifier: String,
  groupId: String,
  accessToken: String
) {
  val client = OkHttpClient()

  val logOutUrl = "https://thientranduc.id.vn:444/api/logout"

  val body = JSONObject().apply {
    if (role == "Owner") {
      put("email", identifier)
    } else {
      put("id", identifier)
    }
    put("groupId", groupId)
  }.toString()
  CoroutineScope(Dispatchers.IO).launch {
    try {
      val request = Request.Builder()
        .url(logOutUrl) // Replace with your server URL
        .addHeader("Authorization", "Bearer $accessToken")
        .post(body.toRequestBody("application/json".toMediaType()))
        .build()

      // Response body will be: { "success": true, "accessToken": "xyz" }
      val response = client.newCall(request).execute()
      Log.d("Logout", "Server response: ${response.body?.string()}")
    } catch (e: Exception) {
      Log.d("Logout error", "Server error: ${e.message}")
    }
  }
}