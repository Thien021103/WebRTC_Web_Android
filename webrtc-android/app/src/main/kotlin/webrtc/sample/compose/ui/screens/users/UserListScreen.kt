package webrtc.sample.compose.ui.screens.users

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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.ui.graphics.Color
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Locale

data class User(
  val email: String,
  val name: String,
  val groupId: String,
  val createdAt: String
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
  var showDeleteDialog by remember { mutableStateOf(false) }
  var deleteEmail by remember { mutableStateOf("") }

  // Fetch users function
  fun fetchUsers() {
    isLoading = true
    errorMessage = ""
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val client = OkHttpClient()
        val request = Request.Builder()
          .url("https://thientranduc.id.vn:444/api/get-users")
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
                  email = userJson.getString("email"),
                  name = userJson.getString("name"),
                  groupId = userJson.getString("groupId"),
                  createdAt = userJson.getString("createdAt")
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

  fun performDeleteUser(
    email: String,
  ) {
    errorMessage = ""
    val client = OkHttpClient()
    val deleteUrl = "https://thientranduc.id.vn:444/api/delete-users"

    val body = JSONObject().apply {
      put("email", email)
    }.toString()

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val request = Request.Builder()
          .url(deleteUrl)
          .addHeader("Authorization", "Bearer $accessToken")
          .delete(body.toRequestBody("application/json".toMediaType()))
          .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        if (responseBody != null) {
          Log.d("DeleteUser Response", responseBody)
        }
        val json = JSONObject(responseBody ?: "{}")
        if (response.isSuccessful) {
          val status = json.optString("status")
          if (status == "success") {
            CoroutineScope(Dispatchers.Main).launch {
              isLoading = true
              errorMessage = "User deleted successfully"
              deleteEmail = ""
              fetchUsers() // Refresh list
            }
          } else {
            CoroutineScope(Dispatchers.Main).launch {
              errorMessage = json.optString("message", "Failed to delete user")
              deleteEmail = ""
            }
          }
        } else {
          CoroutineScope(Dispatchers.Main).launch {
            errorMessage = json.optString("message", "Server error")
            deleteEmail = ""
          }
        }
      } catch (e: Exception) {
        CoroutineScope(Dispatchers.Main).launch {
          errorMessage = "Network error: ${e.message}"
          deleteEmail = ""
        }
      }
    }
  }

  // Initial fetch on screen load
  LaunchedEffect(Unit) {
    fetchUsers()
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
    bottomBar = {
      Button(
        onClick = onBack,
        modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 32.dp).height(56.dp),
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
        Text(text = "Back", fontSize = 18.sp, fontWeight = FontWeight.Bold)
      }
    },
    content = { padding ->
      Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        if (isLoading) {
          CircularProgressIndicator()
          Text(text = "Fetching users...", fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        } else if (users.isNotEmpty()) {
          LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(users) { user ->
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
                  .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                backgroundColor = MaterialTheme.colors.surface
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colors.secondary,
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(8.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "User name",
                        modifier = Modifier.size(22.dp).padding(end = 4.dp),
                        tint = Color.White
                      )
                      Text(
                        text = user.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                      )
                    }
                  }
                  Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Email,
                      contentDescription = "User email",
                      modifier = Modifier.size(22.dp).padding(end = 4.dp)
                    )
                    Text(
                      text = user.email,
                      fontSize = 18.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                  Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Group,
                      contentDescription = "Group ID",
                      modifier = Modifier.size(22.dp).padding(end = 4.dp)
                    )
                    Text(
                      text = user.groupId,
                      fontSize = 14.sp
                    )
                  }
                  Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Filled.CalendarToday,
                      contentDescription = "Created",
                      modifier = Modifier.size(22.dp).padding(end = 4.dp)
                    )
                    Text(
                      text = SimpleDateFormat(
                        "MMM dd, yyyy HH:mm",
                        Locale.getDefault()
                      ).format(
                        SimpleDateFormat(
                          "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                          Locale.getDefault()
                        ).parse(user.createdAt) ?: user.createdAt
                      ),
                      fontSize = 14.sp
                    )
                  }
                  TextButton(
                    onClick = {
                      deleteEmail = user.email
                      showDeleteDialog = true
                    },
                    modifier = Modifier.align(Alignment.End)
                  ) {
                    Text(text = "Delete", color = MaterialTheme.colors.error, fontSize = 18.sp)
                  }
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
      }
    }
  )

  if (errorMessage.isNotEmpty()) {
    LaunchedEffect(errorMessage) {
      snackbarHostState.showSnackbar(errorMessage)
    }
  }

  // Delete Confirmation Dialog
  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = {
        showDeleteDialog = false
        deleteEmail = ""
      },
      title = { Text("Confirm Delete") },
      text = { Text("Are you sure you want to delete this user?") },
      confirmButton = {
        TextButton(
          onClick = {
            performDeleteUser(deleteEmail)
            showDeleteDialog = false
          }
        ) {
          Text("Delete", color = MaterialTheme.colors.error)
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showDeleteDialog = false
            deleteEmail = ""
          }
        ) {
          Text("Cancel")
        }
      }
    )
  }
}