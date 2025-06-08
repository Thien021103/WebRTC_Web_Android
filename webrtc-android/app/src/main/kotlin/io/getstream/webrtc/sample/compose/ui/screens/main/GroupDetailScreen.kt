package io.getstream.webrtc.sample.compose.ui.screens.main

import android.util.Log
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
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import okhttp3.internal.wait
import org.json.JSONObject

data class GroupDetails(
  val name: String,
  val owner: String,
  val state: String,
  val camera: String,
  val controller: String
)

@Composable
fun GroupDetailScreen(
  accessToken: String,
) {
  var group by remember { mutableStateOf<GroupDetails?>(null) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  var fetchTrigger by remember { mutableIntStateOf(0) } // Trigger for refresh

  val client = OkHttpClient.Builder().build()

  fun performGetGroupStatus() {
    isLoading = true
    errorMessage = ""
    val url = "https://thientranduc.id.vn:444/api/group"

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
          Log.d("GroupDetailScreen", "Response: $responseBody")
        }
        val json = JSONObject(responseBody ?: "{}")
        if (response.isSuccessful) {
          val status = json.optString("status")
          if (status == "success") {
            val groupJson = json.getJSONObject("group")
            val groupDetails = GroupDetails(
              name = groupJson.optString("name", "N/A"),
              owner = groupJson.optString("owner", "N/A"),
              state = groupJson.optString("state", "Impossible"),
              camera = groupJson.optString("camera", "Disconnected"),
              controller = groupJson.optString("controller", "Disconnected")
            )
            CoroutineScope(Dispatchers.Main).launch {
              group = groupDetails
              isLoading = false
            }
          } else {
            CoroutineScope(Dispatchers.Main).launch {
              errorMessage = json.optString("message", "Failed to fetch group status")
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

  // Fetch group status on screen load or refresh
  LaunchedEffect(fetchTrigger) {
    performGetGroupStatus()
  }

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    if (isLoading) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator()
      }
    } else if (group != null) {
      // Main Group Details Card
      Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalAlignment = Alignment.Start
        ) {
          Text(
            text = "Group Details",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.padding(bottom = 16.dp)
          )
          Text(
            text = "Group Name: ${group!!.name}",
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
          )
          Text(
            text = "State: ${group!!.state}",
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
          )
          Text(
            text = "Owner's Email: ${group!!.owner}",
            fontSize = 18.sp
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      // Camera Status Card
      Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalAlignment = Alignment.Start
        ) {
          Text(
            text = "Camera Status",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.padding(bottom = 8.dp)
          )
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (group!!.camera == "Connected") MaterialTheme.colors.primaryVariant else MaterialTheme.colors.error,
            modifier = Modifier.padding(top = 4.dp)
          ) {
            Text(
              text = group!!.camera,
              fontSize = 16.sp,
              color = MaterialTheme.colors.surface,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      // Controller Status Card
      Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalAlignment = Alignment.Start
        ) {
          Text(
            text = "Controller Status",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.padding(bottom = 8.dp)
          )
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (group!!.controller == "Connected") MaterialTheme.colors.primaryVariant else MaterialTheme.colors.error,
            modifier = Modifier.padding(top = 4.dp)
          ) {
            Text(
              text = group!!.controller,
              fontSize = 16.sp,
              color = MaterialTheme.colors.surface,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
          }
        }
      }
      if (errorMessage.isNotEmpty()) {
        Text(
          text = errorMessage,
          fontSize = 16.sp,
          color = MaterialTheme.colors.error,
          modifier = Modifier.padding(top = 8.dp)
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      Button(
        onClick = { fetchTrigger++ },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
        colors = ButtonDefaults.buttonColors(
          backgroundColor = MaterialTheme.colors.secondary,
          contentColor = MaterialTheme.colors.onSecondary
        )
      ) {
        Icon(
          imageVector = Icons.Filled.Refresh,
          contentDescription = "Refresh",
          modifier = Modifier.padding(end = 8.dp)
        )
        Text("Refresh", fontSize = 20.sp)
      }
      Spacer(modifier = Modifier.height(16.dp))
    } else if (errorMessage.isNotEmpty()) {
      Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "Error",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.error,
            modifier = Modifier.padding(bottom = 8.dp)
          )
          Text(
            text = errorMessage,
            fontSize = 16.sp,
            color = MaterialTheme.colors.error
          )
        }
      }
      Spacer(modifier = Modifier.weight(1f))
      Button(
        onClick = { fetchTrigger++ },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
        colors = ButtonDefaults.buttonColors(
          backgroundColor = MaterialTheme.colors.secondary,
          contentColor = MaterialTheme.colors.onSecondary
        )
      ) {
        Icon(
          imageVector = Icons.Filled.Refresh,
          contentDescription = "Retry",
          modifier = Modifier.padding(end = 8.dp)
        )
        Text("Retry", fontSize = 20.sp)
      }
    }
  }
}