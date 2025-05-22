package io.getstream.webrtc.sample.compose.ui.screens.stage

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

@Composable
fun LoginScreen(
  fcmToken: String,
  role: String,
  onSuccess: (String, String, String, String) -> Unit,
  onRegister: () -> Unit
) {
  val loginUrl = "https://thientranduc.id.vn:444/api/login"

  var emailOrId by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var groupId by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var showPassword by remember { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }
  val client = OkHttpClient()

  fun performLogin () {
    isLoading = true
    errorMessage = ""
    val body = JSONObject().apply {
      if (role == "Owner") {
        put("email", emailOrId)
      } else {
        put("id", emailOrId)
      }
      put("password", password)
      put("groupId", groupId)
      put("fcmToken", fcmToken)
    }.toString()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val request = Request.Builder()
          .url(loginUrl)
          .post(body.toRequestBody("application/json".toMediaType()))
          .build()
        Log.d("Login Request", body)

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        if (responseBody != null) {
          Log.d("Login Response", responseBody)
        }
        val json = JSONObject(responseBody ?: "{}")
        if (response.isSuccessful) {
          val status = json.optString("status")
          if (status == "success") {
            val accessToken = json.optString("message", "")
            val folder = json.optString("cloud", "")
            CoroutineScope(Dispatchers.Main).launch {
              onSuccess(emailOrId, groupId, accessToken, folder)
              isLoading = false
            }
          } else {
            CoroutineScope(Dispatchers.Main).launch {
              errorMessage = json.optString("message", "Login failed")
              isLoading = false
            }
          }
        } else {
          CoroutineScope(Dispatchers.Main).launch {
            errorMessage = json.optString("message", "Login failed")
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
    content = { padding ->
      Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = "$role Login",
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
          value = emailOrId,
          onValueChange = { emailOrId = it },
          label = { Text(if (role == "Owner") "Email" else "User ID") },
          modifier = Modifier.fillMaxWidth(),
          enabled = !isLoading,
          keyboardOptions = KeyboardOptions(
            keyboardType = if (role == "Owner") KeyboardType.Email else KeyboardType.Text
          )
        )
        Spacer(modifier = Modifier.height(8.dp))
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
          enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (role == "Owner") {
          OutlinedTextField(
            value = groupId,
            onValueChange = { groupId = it },
            label = { Text("Group ID") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
          )
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (fcmToken.isBlank()) {
          CircularProgressIndicator()
          Text(
            text = "Fetching FCM token...",
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
          )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = { performLogin() },
          modifier = Modifier.fillMaxWidth().height(56.dp),
          shape = RoundedCornerShape(12.dp),
          elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
          colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.onSecondary
          ),
          enabled = emailOrId.isNotBlank()
            && password.isNotBlank()
            && (if (role == "Owner") groupId.isNotBlank() else true)
            && fcmToken.isNotBlank() && !isLoading
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = MaterialTheme.colors.onSecondary,
              strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Logging in...",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
          } else {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Login,
              contentDescription = "Login",
              modifier = Modifier.padding(end = 8.dp)
            )
            Text(
              text = "Login",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        if (role == "Owner") {
          Spacer(modifier = Modifier.height(8.dp))
          TextButton(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
          ) {
            Text("Don't have an account? Register", fontSize = 16.sp)
          }
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