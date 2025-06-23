package webrtc.sample.compose.ui.screens.door

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import webrtc.sample.compose.R
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Lock

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
  var dialogAction by remember { mutableStateOf("") } // "unlock" or "lock"
  var isLoading by remember { mutableStateOf(false) }
  var isUnlocking by remember { mutableStateOf(false) }

  var showPassword by remember { mutableStateOf(false) }
  var password by remember { mutableStateOf("") }

  var unlockMessage by remember { mutableStateOf("") }

  var errorMessage by remember { mutableStateOf("") }
  var snackbarTrigger by remember { mutableStateOf(0) } // Add counter

  val client = OkHttpClient.Builder().build()

  fun performGetDoorStatus() {
    isLoading = true
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
              snackbarTrigger++ // Increment trigger
              isLoading = false
            }
          }
        } else {
          CoroutineScope(Dispatchers.Main).launch {
            errorMessage = json.optString("message", "Server error")
            snackbarTrigger++ // Increment trigger
            isLoading = false
          }
        }
      } catch (e: Exception) {
        CoroutineScope(Dispatchers.Main).launch {
          errorMessage = "Network error: ${e.message}"
          snackbarTrigger++ // Increment trigger
          isLoading = false
        }
      }
    }
  }

  fun performUnlock(action: String) {
    val url = "https://thientranduc.id.vn:444/api/$action"
    isUnlocking = true
    unlockMessage = "Working ..."

    val body = JSONObject().apply {
      put("email", identifier)
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
        val responseBody = response.body?.string()
        val json = JSONObject(responseBody ?: "{}")
        if (response.isSuccessful) {
          val status = json.optString("status")
          unlockMessage = json.optString("message")
          if (status == "success") {
            CoroutineScope(Dispatchers.Main).launch {
              password = ""
              isUnlocking = false
              showDialog = false
              errorMessage = "Door $unlockMessage"
              snackbarTrigger++ // Increment trigger
            }
          } else {
            CoroutineScope(Dispatchers.Main).launch {
              isUnlocking = false
              showDialog = false
              errorMessage = unlockMessage
              snackbarTrigger++ // Increment trigger
            }
          }
        } else {
          CoroutineScope(Dispatchers.Main).launch {
            isUnlocking = false
            showDialog = false
            unlockMessage = "Server error: ${json.optString("message", "Unknown error")}"
            errorMessage = unlockMessage
            snackbarTrigger++ // Increment trigger
          }
        }
      } catch (e: Exception) {
        CoroutineScope(Dispatchers.Main).launch {
          isUnlocking = false
          showDialog = false
          unlockMessage = "Network error: ${e.message}"
          errorMessage = unlockMessage
          snackbarTrigger++ // Increment trigger
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
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        if (isLoading) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator()
          }
        } else if (door != null) {
          Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            elevation = 4.dp,
            shape = RoundedCornerShape(8.dp)
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
              Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                color = if (door!!.lock == "Locked") MaterialTheme.colors.primaryVariant else MaterialTheme.colors.error,
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = "Lock Status: ${door!!.lock}",
                  fontSize = 24.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  modifier = Modifier.padding(8.dp)
                )
              }
              Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Filled.Person,
                  contentDescription = "User",
                  modifier = Modifier.size(24.dp).padding(end = 4.dp)
                )
                Text(
                  text = door!!.user ?: "N/A",
                  fontSize = 18.sp
                )
              }
              Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Filled.AccessTime,
                  contentDescription = "Time",
                  modifier = Modifier.size(24.dp).padding(end = 4.dp)
                )
                Text(
                  text = if (door!!.time == "N/A") "N/A" else {
                    SimpleDateFormat("\tdd MMM, yyyy\n\tHH:mm", Locale.getDefault()).format(
                      SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        Locale.getDefault()
                      ).parse(door!!.time) ?: door!!.time
                    )
                  },
                  fontSize = 18.sp
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Button(
              onClick = {
                dialogAction = "unlock"
                showDialog = true
              },
              modifier = Modifier.weight(1f).height(56.dp).padding(end = 8.dp),
              shape = RoundedCornerShape(12.dp),
              elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
              colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = MaterialTheme.colors.onSecondary
              )
            ) {
              Icon(
                imageVector = Icons.Filled.LockOpen,
                contentDescription = "Unlock Door",
                modifier = Modifier.padding(end = 8.dp)
              )
              Text(
                text = "Unlock Door",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Button(
              onClick = {
                dialogAction = "lock"
                showDialog = true
              },
              modifier = Modifier.weight(1f).height(56.dp).padding(start = 8.dp),
              shape = RoundedCornerShape(12.dp),
              elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
              colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = MaterialTheme.colors.onSecondary
              )
            ) {
              Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Lock Door",
                modifier = Modifier.padding(end = 8.dp)
              )
              Text(
                text = "Lock Door",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
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
              backgroundColor = MaterialTheme.colors.primaryVariant,
              contentColor = MaterialTheme.colors.onPrimary
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
            fontSize = 20.sp,
            color = MaterialTheme.colors.error,
            modifier = Modifier.padding(top = 8.dp)
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = { performGetDoorStatus() },
          modifier = Modifier.fillMaxWidth().height(56.dp),
          shape = RoundedCornerShape(12.dp),
          elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
          colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondaryVariant,
            contentColor = MaterialTheme.colors.onSecondary
          )
        ) {
          Icon(
            imageVector = Icons.Filled.Loop,
            contentDescription = "Reload Door History",
            modifier = Modifier.padding(end = 8.dp)
          )
          Text("Reload Door History", fontSize = 20.sp)
        }
      }
    }
  )

  LaunchedEffect(snackbarTrigger) {
    if (errorMessage.isNotEmpty()) {
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
        Text(
          text = if (dialogAction == "unlock") "Unlock Door?" else "Lock Door?",
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )
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
            performUnlock(dialogAction)
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
                text = if (dialogAction == "unlock") "Unlocking..." else "Locking...",
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