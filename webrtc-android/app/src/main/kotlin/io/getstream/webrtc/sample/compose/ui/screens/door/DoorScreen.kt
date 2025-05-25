package io.getstream.webrtc.sample.compose.ui.screens.door

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
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.util.Log
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import java.text.SimpleDateFormat
import java.util.Locale

data class DoorStatus(
  val lock: String,
  val user: String?,
  val time: String
)

@Composable
fun DoorScreen(
  accessToken: String,
  onHistoryClick: () -> Unit,
  onBack: () -> Unit
) {
  val snackbarHostState = remember { SnackbarHostState() }
  var door by remember { mutableStateOf<DoorStatus?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf("") }

  fun performGetDoorStatus() {
    val client = OkHttpClient()
    val url = "https://thientranduc.id.vn:444/api/door"

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val request = Request.Builder()
          .url(url)
          .addHeader("Authorization", "Bearer $accessToken")
          .get()
          .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        if (responseBody != null) {
          Log.d("DoorStatus Response", responseBody)
        }
        val json = JSONObject(responseBody ?: "{}")
        if (response.isSuccessful) {
          val status = json.optString("status")
          if (status == "success") {
            val doorJson = json.getJSONObject("door")
            val doorStatus = DoorStatus(
              lock = doorJson.getString("lock"),
              user = doorJson.optString("user", null),
              time = doorJson.getString("time")
            )
            CoroutineScope(Dispatchers.Main).launch {
              door = doorStatus
              isLoading = false
            }
          } else {
            CoroutineScope(Dispatchers.Main).launch {
              errorMessage = json.optString("message", "Failed to fetch door status")
              isLoading = false
            }
          }
        } else {
          CoroutineScope(Dispatchers.Main).launch {
            errorMessage = json.optString("message", "Server error")
            isLoading = false
          }
        }
      } catch (e: Exception) {
        CoroutineScope(Dispatchers.Main).launch {
          errorMessage = "Network error: ${e.message}"
          isLoading = false
        }
      }
    }
  }

  // Fetch door status on screen load
  LaunchedEffect(Unit) {
    performGetDoorStatus()
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text("Door Status", fontSize = 20.sp) },
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary
      )
    },
    content = { padding ->
      Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        if (isLoading) {
          CircularProgressIndicator()
          Text(
            text = "Fetching door status...",
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
          )
        } else if (door != null) {
          Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            elevation = 4.dp,
            shape = RoundedCornerShape(8.dp)
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
              Text(
                text = "Lock Status: ${door!!.lock}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "User: ${door!!.user ?: "N/A"}",
                fontSize = 16.sp
              )
              Text(
                text = "Time: ${
                  SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(
                    SimpleDateFormat(
                      "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 
                      Locale.getDefault()
                    ).parse(door!!.time) ?: door!!.time
                  )
                }",
                fontSize = 14.sp
              )
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
          Button(
            onClick = onHistoryClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
            colors = ButtonDefaults.buttonColors(
              backgroundColor = MaterialTheme.colors.secondary,
              contentColor = MaterialTheme.colors.onSecondary
            )
          ) {
            Icon(
              imageVector = Icons.Filled.History,
              contentDescription = "View Door History",
              modifier = Modifier.padding(end = 8.dp)
            )
            Text("View Door History", fontSize = 20.sp)
          }
        } else if (errorMessage.isNotEmpty()) {
          Text(
            text = errorMessage,
            fontSize = 16.sp,
            color = MaterialTheme.colors.error,
            modifier = Modifier.padding(top = 8.dp)
          )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
          onClick = onBack,
          modifier = Modifier.fillMaxWidth().height(56.dp),
          shape = RoundedCornerShape(12.dp),
          elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
          colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.error,
            contentColor = MaterialTheme.colors.onError
          )
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            modifier = Modifier.padding(end = 8.dp)
          )
          Text(text = "Back", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  )

  if (errorMessage.isNotEmpty()) {
    LaunchedEffect(errorMessage) {
      snackbarHostState.showSnackbar(errorMessage)
    }
  }
}