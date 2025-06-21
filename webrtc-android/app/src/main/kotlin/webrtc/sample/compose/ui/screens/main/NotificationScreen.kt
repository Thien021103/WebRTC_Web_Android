package webrtc.sample.compose.ui.screens.main

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
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
import java.text.SimpleDateFormat
import java.util.*

data class NotificationEntry(
  val timestamp: String
)

@Composable
fun NotificationScreen(
  accessToken: String,
) {
  var notifications by remember { mutableStateOf<List<NotificationEntry>>(emptyList()) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  var fetchTrigger by remember { mutableStateOf(0) }

  fun performGetNotifications() {
    isLoading = true
    errorMessage = ""

    val client = OkHttpClient()
    val url = "https://thientranduc.id.vn:444/api/notifications"

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
          Log.d("NotificationsScreen", "Response: $responseBody")
        }
        val json = JSONObject(responseBody ?: "{}")
        if (response.isSuccessful) {
          val status = json.optString("status")
          if (status == "success") {
            val notificationsArray = json.getJSONArray("notifications")
            val notificationsList = mutableListOf<NotificationEntry>()
            for (i in 0 until notificationsArray.length()) {
              val entryJson = notificationsArray.getJSONObject(i)
              notificationsList.add(
                NotificationEntry(
                  timestamp = entryJson.getString("time")
                )
              )
            }
            CoroutineScope(Dispatchers.Main).launch {
              notifications = notificationsList
              isLoading = false
            }
          } else {
            CoroutineScope(Dispatchers.Main).launch {
              errorMessage = json.optString("message", "Failed to fetch notifications")
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

  // Fetch notifications on load or refresh
  LaunchedEffect(fetchTrigger) {
    performGetNotifications()
  }

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Refresh button
    Button(
      onClick = { fetchTrigger++ },
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(12.dp),
      elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
      colors = ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.colors.secondary,
        contentColor = MaterialTheme.colors.onSecondary
      ),
      enabled = !isLoading
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(24.dp),
          color = MaterialTheme.colors.onSecondary,
          strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Fetching...", fontSize = 18.sp, fontWeight = FontWeight.Bold)
      } else {
        Icon(
          imageVector = Icons.Filled.Refresh,
          contentDescription = "Refresh",
          modifier = Modifier.padding(end = 8.dp)
        )
        Text("Refresh Notifications", fontSize = 18.sp, fontWeight = FontWeight.Bold)
      }
    }
    Spacer(modifier = Modifier.height(16.dp))

    // Notifications list
    if (isLoading) {
      CircularProgressIndicator()
      Text(
        text = "Fetching notifications...",
        fontSize = 16.sp,
        modifier = Modifier.padding(top = 8.dp)
      )
    } else if (notifications.isNotEmpty()) {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f)
      ) {
        items(notifications) { entry ->
          Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            elevation = 4.dp,
            shape = RoundedCornerShape(8.dp)
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
              Text(
                text = "Time: ${
                  SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                  ).format(
                    SimpleDateFormat(
                      "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                      Locale.getDefault()
                    ).parse(entry.timestamp) ?: entry.timestamp
                  )
                }",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Date: ${
                  SimpleDateFormat(
                    "dd MMM, yyyy ",
                    Locale.getDefault()
                  ).format(
                    SimpleDateFormat(
                      "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                      Locale.getDefault()
                    ).parse(entry.timestamp) ?: entry.timestamp
                  )
                }",
                fontSize = 18.sp
              )
            }
          }
        }
      }
    } else if (errorMessage.isNotEmpty()) {
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
  }
}