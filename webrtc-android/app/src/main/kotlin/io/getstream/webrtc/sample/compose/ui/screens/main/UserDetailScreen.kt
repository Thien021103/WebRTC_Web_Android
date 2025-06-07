package io.getstream.webrtc.sample.compose.ui.screens.main

import android.util.Log
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
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun UserDetailScreen(
  role: String,
  identifier: String,
  groupName: String,
  accessToken: String,
  onLogout: () -> Unit
) {
  var askLogout by remember { mutableStateOf(false) }
  var askChangePassword by remember { mutableStateOf(false) }

  var showNewPassword by remember { mutableStateOf(false) }
  var showOldPassword by remember { mutableStateOf(false) }
  var showConfirmPassword by remember { mutableStateOf(false) }

  var confirmPassword by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var oldPassword by remember { mutableStateOf("") }

  var isLoading by remember { mutableStateOf(false) }
  var message by remember { mutableStateOf("") }

  fun performChangePassword () {
    if (newPassword != confirmPassword) {
      CoroutineScope(Dispatchers.Main).launch {
        message = "Passwords do not match"
      }
      return
    }
    isLoading = true
    message = ""
    val client = OkHttpClient()
    val changePassUrl = "https://thientranduc.id.vn:444/api/change-password"

    val body = JSONObject().apply {
      put("password", oldPassword)
      put("newPassword", newPassword)
    }.toString()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val request = Request.Builder()
          .url(changePassUrl)
          .addHeader("Authorization", "Bearer $accessToken")
          .post(body.toRequestBody("application/json".toMediaType()))
          .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        if (responseBody != null) {
          Log.d("Change Password Response", responseBody)
        }
        val json = JSONObject(responseBody ?: "{}")
        if (response.isSuccessful) {
          val status = json.optString("status")
          if (status == "success") {
            CoroutineScope(Dispatchers.Main).launch {
              message = "Changed password successfully"
              isLoading = false
              oldPassword = ""
              confirmPassword = ""
              newPassword = ""
            }
          } else {
            CoroutineScope(Dispatchers.Main).launch {
              message = json.optString("message", "Password change failed")
              isLoading = false
            }
          }
        } else {
          CoroutineScope(Dispatchers.Main).launch {
            message = json.optString("message", "Password change failed")
            isLoading = false
          }
        }
      } catch (e: Exception) {
        CoroutineScope(Dispatchers.Main).launch {
          message = "Network error: ${e.message.toString()}"
          isLoading = false
        }
      }
    }
  }

  fun performLogOut () {
    val client = OkHttpClient()

    val logOutUrl = "https://thientranduc.id.vn:444/api/logout"

    val body = JSONObject().apply {
      if (role == "Owner") {
        put("email", identifier)
        put("groupName", groupName)
      } else {
        put("email", identifier)
      }
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

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Card(
      modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
      elevation = 4.dp,
      shape = RoundedCornerShape(12.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.Start
      ) {
        Text(
          text = "Profile Details",
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colors.primary,
          modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
          text = "Role: $role",
          fontSize = 18.sp,
          modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
          text = "Identifier: $identifier",
          fontSize = 18.sp,
          modifier = Modifier.padding(bottom = 8.dp)
        )
        if (role == "Owner") {
          Text(
            text = "Group name: $groupName",
            fontSize = 18.sp
          )
        }
      }
    }
    Spacer(modifier = Modifier.weight(1f))
    Button(
      onClick = { askChangePassword = true },
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(12.dp),
      elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
      colors = ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.colors.secondaryVariant,
        contentColor = MaterialTheme.colors.onSecondary
      )
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.Redo,
        contentDescription = "Change password",
        modifier = Modifier.padding(end = 8.dp)
      )
      Text("Change password", fontSize = 20.sp)
    }
    Spacer(modifier = Modifier.height(16.dp))
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
            performLogOut()
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

  if (askChangePassword) {
    AlertDialog(
      onDismissRequest = {
        askChangePassword = false
        newPassword = ""
        oldPassword = ""
        confirmPassword = ""
      },
      title = { Text(text = "Enter old and new password", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text(
            text = message,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = oldPassword,
            onValueChange = { oldPassword = it },
            label = { Text("Old password") },
            visualTransformation = if (showOldPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(
                onClick = { showOldPassword = !showOldPassword },
                content = {
                  Icon(
                    imageVector = if (showOldPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = "Password hide/show"
                  )
                }
              )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New password") },
            visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(
                onClick = { showNewPassword = !showNewPassword },
                content = {
                  Icon(
                    imageVector = if (showNewPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = "Password hide/show"
                  )
                }
              )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = {
              if (newPassword != confirmPassword) Text("Passwords do not match")
              else Text("Confirm password")
            },
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(
                onClick = { showConfirmPassword = !showConfirmPassword },
                content = {
                  Icon(
                    imageVector = if (showConfirmPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = "Confirm password hide/show"
                  )
                }
              )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
          )
        }
      },
      confirmButton = {
        Button(
          onClick = { performChangePassword() },
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.onSecondary
          ),
          enabled = oldPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank() && !isLoading
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 8.dp)
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colors.onSecondary,
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Changing ...",
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
            askChangePassword = false
            newPassword = ""
            oldPassword = ""
            confirmPassword = ""
          },
          enabled = !isLoading
        ) {
          Text(stringResource(R.string.cancel), fontSize = 14.sp)
        }
      },
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.height(500.dp).padding(25.dp, 25.dp),
      backgroundColor = MaterialTheme.colors.surface
    )
  }
}