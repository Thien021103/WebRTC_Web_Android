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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextButton
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import io.getstream.webrtc.sample.compose.R
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
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
  role: String,
  identifier: String,
  onHistoryClick: () -> Unit,
  onBack: () -> Unit
) {
  val snackbarHostState = remember { SnackbarHostState() }
  var door by remember { mutableStateOf<DoorStatus?>(null) }

  var showDialog by remember { mutableStateOf(false) }

  var isLoading by remember { mutableStateOf(true) }
  var isUnlocking by remember { mutableStateOf(false) }

  var showPassword by remember { mutableStateOf(false) }
  var password by remember { mutableStateOf("") }

  var unlockMessage by remember { mutableStateOf("") }

  var errorMessage by remember { mutableStateOf("") }
  val client = OkHttpClient.Builder().build()

  fun performGetDoorStatus() {
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
              lock = doorJson.optString("lock", "Locked"),
              user = doorJson.optString("user", "N/A"),
              time = doorJson.optString("time", "N/A")
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

  fun performUnlock() {
    val url = if (door!!.lock == "Locked") {
      "https://thientranduc.id.vn:444/api/unlock"
    } else {
      "https://thientranduc.id.vn:444/api/lock"
    }
    isUnlocking = true
    unlockMessage = "Unlocking ..."

    val body = JSONObject().apply {
      if(role == "Owner") {
        put("email", identifier)
      } else {
        put("id", identifier)
      }
      put("password", password)
    }.toString()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val request = Request.Builder()
          .url(url)
          .addHeader("Authorization", "Bearer $accessToken")
          .post(body.toRequestBody("application/json".toMediaType()))
          .build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
          val responseBody = response.body?.string()
          val json = JSONObject(responseBody ?: "{}")
          val status = json.optString("status")
          unlockMessage = json.optString("message")
          if (status == "success") {
            CoroutineScope(Dispatchers.Main).launch {
              password = ""
              isUnlocking = false
              showDialog = false
              errorMessage = "Door $unlockMessage"
            }
          } else {
            CoroutineScope(Dispatchers.Main).launch {
              isUnlocking = false
              showDialog = false
              errorMessage = unlockMessage
            }
          }
        } else {
          CoroutineScope(Dispatchers.Main).launch {
            isUnlocking = false
            showDialog = false
            unlockMessage = "Server error: ${response.body?.string()}"
            errorMessage = unlockMessage
          }
        }
      } catch (e: Exception) {
        CoroutineScope(Dispatchers.Main).launch {
          isUnlocking = false
          showDialog = false
          unlockMessage = "Network error: ${e.message}"
          errorMessage = unlockMessage
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
        verticalArrangement = Arrangement.Center,
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
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "User: ${door!!.user ?: "N/A"}",
                fontSize = 18.sp
              )
              Text(
                text = "Time:\n${
                  if (door!!.time == "N/A") {"N/A"}
                  else {
                    SimpleDateFormat("\tdd MMM, yyyy\n\tHH:mm", Locale.getDefault()).format(
                      SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        Locale.getDefault()
                      ).parse(door!!.time) ?: door!!.time
                    )
                  }
                }",
                fontSize = 18.sp
              )
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
          Button(
            onClick = {  showDialog = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
            colors = ButtonDefaults.buttonColors(
              backgroundColor = MaterialTheme.colors.primary,
              contentColor = MaterialTheme.colors.onPrimary
            )
          ) {
            Icon(
              imageVector = Icons.Filled.LockOpen,
              contentDescription = "Door modify",
              modifier = Modifier.padding(end = 8.dp)
            )
            Text(
              text = if (door!!.lock == "Locked") {
                "Unlock door"
              } else {
                "Lock door?"
              },
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
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

  if (showDialog) {
    AlertDialog(
      onDismissRequest = {
        showDialog = false
        password = ""
      },
      title = {
        if (door!!.lock == "Locked") { Text(text = "Unlock door?", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        else { Text(text = "Lock door?", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
      },
      text = {
        Column {
          Text(
            text = stringResource(R.string.enter_password),
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
          )
          Text(
            text = unlockMessage,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
          )
          OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(
                onClick = { showPassword = !showPassword },
                content = {
                  Icon(
                    imageVector = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = "Password hide/show"
                  )
                }
              )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isUnlocking
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            performUnlock()
            performGetDoorStatus()
          },
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.onSecondary
          ),
          enabled = password.isNotBlank() && !isUnlocking
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 8.dp)
          ) {
            if (isUnlocking) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colors.onSecondary,
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Unlocking...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            } else {
              Text(
                text = stringResource(R.string.confirm),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showDialog = false
            password = ""
          },
          enabled = !isUnlocking
        ) {
          Text(stringResource(R.string.cancel), fontSize = 14.sp)
        }
      },
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.height(390.dp).padding(25.dp, 25.dp),
      backgroundColor = MaterialTheme.colors.surface
    )
  }
}