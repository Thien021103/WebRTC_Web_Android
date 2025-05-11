package io.getstream.webrtc.sample.compose.ui.screens.stage

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
import androidx.compose.material.icons.filled.PersonAdd
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
fun RegisterScreen(
  fcmToken: String,
  onSuccess: (String, String, String) -> Unit,
  onLogin: () -> Unit
) {

  val registerUrl = "https://thientranduc.id.vn:444/api/register"

  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var cameraId by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var showPassword by remember { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }

  val client = OkHttpClient()

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    content = { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(16.dp),
        verticalArrangement = Arrangement.Center
      ) {

        Text("Register", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email") },
          modifier = Modifier.fillMaxWidth(),
          enabled = !isLoading
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
        OutlinedTextField(
          value = cameraId,
          onValueChange = { cameraId = it },
          label = { Text("Camera ID") },
          modifier = Modifier.fillMaxWidth(),
          enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))
        if (fcmToken.isBlank()) {
          CircularProgressIndicator()
          Text("Fetching FCM token...", fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            isLoading = true
            errorMessage = ""
            val body = JSONObject().apply {
              put("email", email)
              put("password", password)
              put("groupId", cameraId)
              put("fcmToken", fcmToken)
            }.toString()
            CoroutineScope(Dispatchers.IO).launch {
              try {
                val request = Request.Builder()
                  .url(registerUrl) // Replace with your server URL
                  .post(body.toRequestBody("application/json".toMediaType()))
                  .build()

                // Response body will be: { "success": true, "accessToken": "xyz" }
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                  val responseBody = response.body?.string()
                  val json = JSONObject(responseBody ?: "{}")
                  val status = json.optString("status")
                  if (status == "success") {
                    val accessToken = json.optString("message", "")
                    CoroutineScope(Dispatchers.Main).launch {
                      onSuccess(email, cameraId, accessToken)
                      isLoading = false
                    }
                  } else {
                    CoroutineScope(Dispatchers.Main).launch {
                      errorMessage = json.optString("message", "Registration failed")
                      isLoading = false
                    }
                  }
                } else {
                  CoroutineScope(Dispatchers.Main).launch {
                    errorMessage = "Server error: ${response.body?.string()}"
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
          },
          modifier = Modifier.fillMaxWidth().height(56.dp),
          shape = RoundedCornerShape(12.dp),
          elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
          colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.onSecondary
          ),
          enabled = email.isNotBlank() && password.isNotBlank() && cameraId.isNotBlank() && fcmToken.isNotBlank() && !isLoading
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = MaterialTheme.colors.onSecondary,
              strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Registering ...",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
          }
          else {
            Icon(
              imageVector = Icons.Filled.PersonAdd,
              contentDescription = "Register",
              modifier = Modifier.padding(end = 8.dp)
            )
            Text(
              text = "Register",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Change to Login button
        TextButton(
          onClick = onLogin,
          modifier = Modifier.fillMaxWidth(),
          enabled = !isLoading
        ) {
          Text("Already have an account? Login", fontSize = 16.sp)
        }
      }
    }
  )
  // Error snackbar
  if (errorMessage.isNotEmpty()) {
    LaunchedEffect(errorMessage) {
      snackbarHostState.showSnackbar(errorMessage)
    }
  }
}