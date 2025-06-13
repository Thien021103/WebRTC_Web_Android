package io.getstream.webrtc.sample.compose.ui.screens.video

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
fun VideoCallControls(
  context: Context,
  identifier: String,
  role: String,
  accessToken: String,
  modifier: Modifier,
  callMediaState: CallMediaState,
  actions: List<VideoCallControlAction> = buildDefaultCallControlActions(callMediaState = callMediaState),
  onCallAction: (CallAction) -> Unit
) {
  var isUnlocking by remember { mutableStateOf(false) }
  var showUnlockDialog by remember { mutableStateOf(false) }
  var showPassword by remember { mutableStateOf(false) }
  var password by remember { mutableStateOf("") }
  var unlockMessage by remember { mutableStateOf("") }
  var isLightOn by remember { mutableStateOf(false) }
  var isTogglingLight by remember { mutableStateOf(false) }

  val client = OkHttpClient.Builder().build()

  fun performToggleLight() {
    isLightOn = !isLightOn
    isTogglingLight = true

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val request = Request.Builder()
          .url("https://thientranduc.id.vn:444/api/light")
          .addHeader("Authorization", "Bearer $accessToken")
          .get()
          .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        val json = JSONObject(responseBody ?: "{}")
        val status = json.optString("status")
        if (response.isSuccessful) {
          if (status == "success") {
            CoroutineScope(Dispatchers.Main).launch {
              isTogglingLight = false
            }
          }
          else {
            CoroutineScope(Dispatchers.Main).launch {
              isTogglingLight = false
            }
          }
        } else {
          CoroutineScope(Dispatchers.Main).launch {
            isTogglingLight = false
          }
        }
      } catch (e: Exception) {
        CoroutineScope(Dispatchers.Main).launch {
          isTogglingLight = false
        }
      }
    }
  }

  fun performUnlock() {
    isUnlocking = true
    unlockMessage = "Unlocking ..."

    val body = JSONObject().apply {
      put("email", identifier)
      put("password", password)
    }.toString()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val request = Request.Builder()
          .url("https://thientranduc.id.vn:444/api/unlock")
          .addHeader("Authorization", "Bearer $accessToken")
          .post(body.toRequestBody("application/json".toMediaType()))
          .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        val json = JSONObject(responseBody ?: "{}")
        val status = json.optString("status")
        if (response.isSuccessful) {
          if (status == "success") {
            password = ""
            isUnlocking = false
            unlockMessage = "Door ${json.optString("message", "Handled")}"
          } else {
            isUnlocking = false
            unlockMessage = json.optString("message", "Error handling door")
          }
        } else {
          isUnlocking = false
          unlockMessage = "Server error: ${json.optString("message")}"
        }
      } catch (e: Exception) {
        isUnlocking = false
        unlockMessage = "Network error: ${e.message}"
      }
    }
  }

  LazyRow(
    modifier = modifier.padding(bottom = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceEvenly
  ) {
    items(actions) { action ->
      Box(
        modifier = Modifier.size(56.dp).clip(CircleShape).background(action.background)
      ) {
        Icon(
          modifier = Modifier.padding(10.dp).align(Alignment.Center).clickable { onCallAction(action.callAction) },
          tint = action.iconTint,
          painter = action.icon,
          contentDescription = null
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
    }
    item {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colors.surface.copy(alpha = 0.8f))
          .then(
            if (isLightOn) Modifier.shadow(
              elevation = 1.dp,
              shape = CircleShape,
              ambientColor = Color.Yellow,
              spotColor = Color.Yellow
            ) else Modifier
          )
          .clickable(enabled = !isTogglingLight) { performToggleLight() }
      ) {
        Icon(
          imageVector = Icons.Filled.Lightbulb,
          contentDescription = "Light switch",
          tint = if (isLightOn) Color.Yellow else MaterialTheme.colors.onSurface,
          modifier = Modifier.padding(10.dp).align(Alignment.Center)
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
    }
    item {
      Button(
        onClick = { showUnlockDialog = true },
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
          contentDescription = "Unlock button",
          modifier = Modifier.padding(end = 8.dp)
        )
        Text(
          text = "Unlock door",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }

  if (showUnlockDialog) {
    AlertDialog(
      onDismissRequest = {
        showUnlockDialog = false
        password = ""
      },
      title = {
        Text(
          text = stringResource(R.string.unlock_door),
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
          onClick = { performUnlock() },
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
            showUnlockDialog = false
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
