package io.getstream.webrtc.sample.compose.ui.screens.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import java.text.SimpleDateFormat
import java.util.Locale

data class User(
  val id: String,
  val groupId: String,
  val createdAt: String,
  val updatedAt: String
)

@Composable
fun UserListScreen(
  accessToken: String,
  onBack: () -> Unit
) {
  val snackbarHostState = remember { SnackbarHostState() }
  var users by remember { mutableStateOf<List<User>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf("") }

  // Fetch users on screen load
  LaunchedEffect(Unit) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val client = OkHttpClient()
        val request = Request.Builder()
          .url("https://thientranduc.id.vn:444/api/users")
          .addHeader("Authorization", "Bearer $accessToken")
          .get()
          .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        if (responseBody != null) {
          Log.d("GetUsers Response", responseBody)
        }
        val json = JSONObject(responseBody ?: "{}")
        if (response.isSuccessful) {
          val status = json.optString("status")
          if (status == "success") {
            val usersArray = json.getJSONArray("users")
            val userList = mutableListOf<User>()
            for (i in 0 until usersArray.length()) {
              val userJson = usersArray.getJSONObject(i)
              userList.add(
                User(
                  id = userJson.getString("id"),
                  groupId = userJson.getString("groupId"),
                  createdAt = userJson.getString("createdAt"),
                  updatedAt = userJson.getString("updatedAt")
                )
              )
            }
            CoroutineScope(Dispatchers.Main).launch {
              users = userList
              isLoading = false
            }
          } else {
            CoroutineScope(Dispatchers.Main).launch {
              errorMessage = json.optString("message", "Failed to fetch users")
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

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text("User List", fontSize = 20.sp) },
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
            text = "Fetching users...",
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
          )
        } else if (users.isNotEmpty()) {
          LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(users) { user ->
              Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp)
              ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                  Text(text = "User ID: ${user.id}", fontSize = 18.sp, fontWeight = FontWeight.Bold
                  )
                  Text(text = "Group ID: ${user.groupId}", fontSize = 16.sp)
                  Text(
                    text = "Created: ${
                      SimpleDateFormat(
                        "MMM dd, yyyy HH:mm",
                        Locale.getDefault()
                      ).format(
                        SimpleDateFormat(
                          "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                          Locale.getDefault()
                        ).parse(user.createdAt) ?: user.createdAt
                      )
                    }",
                    fontSize = 14.sp
                  )
                  Text(
                    text = "Updated: ${
                      SimpleDateFormat(
                        "MMM dd, yyyy HH:mm",
                        Locale.getDefault()
                      ).format(
                        SimpleDateFormat(
                          "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                          Locale.getDefault()
                        ).parse(user.updatedAt) ?: user.updatedAt
                      )
                    }",
                    fontSize = 14.sp
                  )
                }
              }
            }
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

  if (errorMessage.isNotEmpty()) {
    LaunchedEffect(errorMessage) {
      snackbarHostState.showSnackbar(errorMessage)
    }
  }
}