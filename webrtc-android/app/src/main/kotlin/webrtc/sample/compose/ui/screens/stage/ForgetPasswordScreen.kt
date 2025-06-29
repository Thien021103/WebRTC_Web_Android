package webrtc.sample.compose.ui.screens.stage

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockReset
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Composable
fun ForgetPasswordScreen(
  role: String,
  onSuccess: () -> Unit,
  onBack: () -> Unit
) {
  val otpUrl = "https://thientranduc.id.vn:444/api/forget-otp"
  val forgetPasswordUrl = "https://thientranduc.id.vn:444/api/reset-password"

  var email by remember { mutableStateOf("") }
  var groupId by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var otp by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var showPassword by remember { mutableStateOf(false) }
  var showConfirmPassword by remember { mutableStateOf(false) }
  var isOtpRequested by remember { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }
  val client = remember { OkHttpClient() }

  fun performRequestOtp() {
    isLoading = true
    errorMessage = ""
    val body = JSONObject().apply {
      put("email", email)
      if (role == "Owner") {
        put("groupId", groupId)
      }
    }.toString()
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
      try {
        val request = Request.Builder()
          .url(otpUrl)
          .post(body.toRequestBody("application/json".toMediaType()))
          .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        if (responseBody != null) {
          Log.d("ForgetPassword", responseBody)
        }
        val json = JSONObject(responseBody ?: "{}")
        withContext(Dispatchers.Main) {
          if (response.isSuccessful && json.optString("status") == "success") {
            isOtpRequested = true
            isLoading = false
            snackbarHostState.showSnackbar("OTP sent to your email")
          } else {
            errorMessage = json.optString("message", "Failed to request OTP")
            isLoading = false
          }
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          errorMessage = "Network error: ${e.message}"
          isLoading = false
        }
      }
    }
  }

  fun performForgetPassword() {
    if (password != confirmPassword) {
      errorMessage = "Passwords do not match"
      return
    }
    isLoading = true
    errorMessage = ""
    val body = JSONObject().apply {
      put("email", email)
      if (role == "Owner") {
        put("groupId", groupId)
      }
      put("password", password)
      put("otp", otp)
    }.toString()
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
      try {
        val request = Request.Builder()
          .url(forgetPasswordUrl)
          .post(body.toRequestBody("application/json".toMediaType()))
          .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        if (responseBody != null) {
          Log.d("ForgetPassword Response", responseBody)
        }
        val json = JSONObject(responseBody ?: "{}")
        withContext(Dispatchers.Main) {
          if (response.isSuccessful && json.optString("status") == "success") {
            errorMessage = json.optString("message")
            isLoading = false
            onSuccess()
          } else {
            errorMessage = json.optString("message", "Password reset failed")
            isOtpRequested = false
            isLoading = false
          }
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
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
          text = "Reset Password",
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = 16.dp)
        )
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email") },
          modifier = Modifier.fillMaxWidth(),
          enabled = !isLoading && !isOtpRequested
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (role == "Owner") {
          OutlinedTextField(
            value = groupId,
            onValueChange = { groupId = it },
            label = { Text("Group ID") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && !isOtpRequested
          )
          Spacer(modifier = Modifier.height(8.dp))
        }
        if (isOtpRequested) {
          OutlinedTextField(
            value = otp,
            onValueChange = { otp = it },
            label = { Text("OTP") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("New Password") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                  imageVector = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                  contentDescription = "New password hide/show"
                )
              }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = {
              if (password != confirmPassword) Text("Passwords do not match")
              else Text("Confirm New Password")
            },
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                Icon(
                  imageVector = if (showConfirmPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                  contentDescription = "Confirm password hide/show"
                )
              }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
          )
          Spacer(modifier = Modifier.height(16.dp))
        }
        if (isLoading) {
          CircularProgressIndicator()
          Text("Requesting OTP...", fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (!isOtpRequested) {
          Button(
            onClick = { performRequestOtp() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
            colors = ButtonDefaults.buttonColors(
              backgroundColor = MaterialTheme.colors.secondary,
              contentColor = MaterialTheme.colors.onSecondary
            ),
            enabled = email.isNotBlank() && (if (role == "Owner") groupId.isNotBlank() else true) && !isLoading
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colors.onSecondary,
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Requesting OTP...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
            } else {
              Icon(
                imageVector = Icons.Filled.LockReset,
                contentDescription = "Request OTP",
                modifier = Modifier.padding(end = 8.dp)
              )
              Text(
                text = "Request OTP",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        } else {
          Button(
            onClick = { performForgetPassword() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
            colors = ButtonDefaults.buttonColors(
              backgroundColor = MaterialTheme.colors.secondary,
              contentColor = MaterialTheme.colors.onSecondary
            ),
            enabled = email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && otp.isNotBlank() && !isLoading
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colors.onSecondary,
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Resetting Password...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
            } else {
              Icon(
                imageVector = Icons.Filled.LockReset,
                contentDescription = "Reset Password",
                modifier = Modifier.padding(end = 8.dp)
              )
              Text(
                text = "Reset Password",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
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