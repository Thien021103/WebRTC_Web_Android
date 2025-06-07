package io.getstream.webrtc.sample.compose.ui.screens.users

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
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
fun RegisterUserScreen(
  accessToken: String,
  onBack: () -> Unit
) {
  val snackbarHostState = remember { SnackbarHostState() }

  var userName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }

  var registerMessage by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var showPassword by remember { mutableStateOf(false) }
  var showConfirmPassword by remember { mutableStateOf(false) }

  fun performRegisterUser() {
    if (password != confirmPassword) {
      CoroutineScope(Dispatchers.Main).launch {
        registerMessage = "Passwords do not match"
      }
      return
    }
    isLoading = true
    registerMessage = ""
    val client = OkHttpClient()
    val registerUrl = "https://thientranduc.id.vn:444/api/register"

    val body = JSONObject().apply {
      put("userName", userName)
      put("email", email)
      put("password", password)
      put("ownerToken", accessToken)
    }.toString()

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val request = Request.Builder()
          .url(registerUrl)
          .post(body.toRequestBody("application/json".toMediaType()))
          .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        if (responseBody != null) {
          Log.d("RegisterUser Response", responseBody)
        }
        val json = JSONObject(responseBody ?: "{}")
        if (response.isSuccessful) {
          val status = json.optString("status")
          if (status == "success") {
            CoroutineScope(Dispatchers.Main).launch {
              registerMessage = "User registered successfully"
              isLoading = false
              userName = ""
              password = ""
              confirmPassword = ""
            }
          } else {
            CoroutineScope(Dispatchers.Main).launch {
              registerMessage = json.optString("message", "Registration failed")
              isLoading = false
            }
          }
        } else {
          CoroutineScope(Dispatchers.Main).launch {
            registerMessage = json.optString("message", "Registration failed")
            isLoading = false
          }
        }
      } catch (e: Exception) {
        CoroutineScope(Dispatchers.Main).launch {
          registerMessage = "Network error: ${e.message.toString()}"
          isLoading = false
        }
      }
    }
  }
  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text("Register New User", fontSize = 20.sp) },
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
        modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Register New User",
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
          value = userName,
          onValueChange = { userName = it },
          label = { Text("User name") },
          modifier = Modifier.fillMaxWidth(),
          enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email") },
          modifier = Modifier.fillMaxWidth(),
          enabled = !isLoading,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
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
          value = confirmPassword,
          onValueChange = { confirmPassword = it },
          label = {
            if (password != confirmPassword) Text("Passwords do not match")
            else Text("Confirm Password")
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
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = { performRegisterUser() },
          modifier = Modifier.fillMaxWidth().height(56.dp),
          shape = RoundedCornerShape(12.dp),
          elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
          colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.onSecondary
          ),
          enabled = userName.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && !isLoading
        ) {
          if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Registering...",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
          } else {
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
      }
    }
  )

  if (registerMessage.isNotEmpty()) {
    LaunchedEffect(registerMessage) {
      snackbarHostState.showSnackbar(registerMessage)
    }
  }
}