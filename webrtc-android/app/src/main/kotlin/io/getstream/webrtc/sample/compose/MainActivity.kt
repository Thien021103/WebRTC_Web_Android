package io.getstream.webrtc.sample.compose

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.cloudinary.android.MediaManager
import io.getstream.webrtc.sample.compose.ui.screens.users.RegisterUserScreen
import io.getstream.webrtc.sample.compose.ui.screens.users.UserManagementScreen
import com.google.firebase.messaging.FirebaseMessaging
import io.getstream.webrtc.sample.compose.ui.screens.list.VideoListScreen
import io.getstream.webrtc.sample.compose.ui.screens.list.VideoListViewModel
import io.getstream.webrtc.sample.compose.ui.screens.stage.LoginScreen
import io.getstream.webrtc.sample.compose.ui.screens.stage.MainScreen
import io.getstream.webrtc.sample.compose.ui.screens.stage.RegisterScreen
import io.getstream.webrtc.sample.compose.ui.screens.stage.RoleSelectionScreen
import io.getstream.webrtc.sample.compose.ui.screens.stage.SignallingScreen
import io.getstream.webrtc.sample.compose.ui.screens.users.UserListScreen
import io.getstream.webrtc.sample.compose.ui.theme.WebrtcSampleComposeTheme
import kotlinx.coroutines.tasks.await

sealed class Screen {
  object Login : Screen()
  object Register : Screen()
  object Main : Screen()
  object Videos : Screen()
  object Signalling : Screen()
  object RoleSelection : Screen()
  object RegisterUser : Screen()
  object UserManagement : Screen()
  object  UserList : Screen()
}

class MainActivity : ComponentActivity() {
  @RequiresApi(Build.VERSION_CODES.Q)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val permissions = arrayOf(
      Manifest.permission.CAMERA,
      Manifest.permission.RECORD_AUDIO,
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.POST_NOTIFICATIONS
    )
    requestPermissions(permissions, 0)

    // Initialize Cloudinary (do this once, e.g., in Application class or before upload)
    val config = mapOf(
      "cloud_name" to "dvarse6wk",
      "api_key" to "573435389774623",
      "api_secret" to "CZmauvR9SiOsysGNak67f9DVTjc"
    )
    MediaManager.init(this, config)

    setContent {
      WebrtcSampleComposeTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colors.background
        ) {
          var fcmToken by remember { mutableStateOf("") }
          LaunchedEffect(Unit) {
            try {
              fcmToken = FirebaseMessaging.getInstance().token.await()
              Log.d("FCM Token", fcmToken)
            } catch (e: Exception) {
              println("Failed to get FCM token: $e")
            }
          }
          var currentScreen: Screen by remember { mutableStateOf(Screen.RoleSelection) }
          var identifier by remember { mutableStateOf("") }
          var groupId by remember { mutableStateOf("") }
          var accessToken by remember { mutableStateOf("") }
          var role by remember { mutableStateOf("") }
          var cloudFolder by remember { mutableStateOf("") }

          when (currentScreen) {
            Screen.RoleSelection -> RoleSelectionScreen(
              onRoleSelected = { selectedRole ->
                role = selectedRole
                currentScreen = Screen.Login
              }
            )
            Screen.Login -> LoginScreen(
              fcmToken = fcmToken,
              role = role,
              onSuccess = { emailOrId, id, token, folder ->
                identifier = emailOrId
                groupId = id
                accessToken = token
                currentScreen = Screen.Main
                cloudFolder = folder
              },
              onRegister = { currentScreen = Screen.Register }
            )
            Screen.Register -> RegisterScreen(
              fcmToken = fcmToken,
              onSuccess = { email, id, token, folder ->
                identifier = email
                groupId = id
                accessToken = token
                currentScreen = Screen.Main
                cloudFolder = folder
                role = "Owner"
              },
              onLogin = { currentScreen = Screen.RoleSelection }
            )
            Screen.Main -> MainScreen(
              role = role,
              identifier = identifier,
              id = groupId,
              token = accessToken,
              onVideosClick = { currentScreen = Screen.Videos },
              onSignallingClick = { currentScreen = Screen.Signalling },
              onUserManagementClick = { currentScreen = Screen.UserManagement },
              onLogout = {
                identifier = ""
                groupId = ""
                accessToken = ""
                role = ""
                currentScreen = Screen.RoleSelection
              }
            )
            Screen.Videos -> VideoListScreen(
              viewModel = VideoListViewModel(
                context = this,
                cameraId = groupId,
                cloudName = "dvarse6wk",
                cloudFolder = cloudFolder,
                token = accessToken
              ),
              onBack = { currentScreen = Screen.Main }
            )
            Screen.Signalling -> SignallingScreen(
              role = role,
              email = identifier,
              accessToken = accessToken,
              cloudFolder = cloudFolder,
              onBack = { currentScreen = Screen.Main }
            )
            Screen.UserManagement -> UserManagementScreen(
              onRegisterUserClick = { currentScreen = Screen.RegisterUser },
              onUserListClick = { currentScreen = Screen.UserList },
              onBack = { currentScreen = Screen.Main }
            )
            Screen.RegisterUser -> RegisterUserScreen(
              accessToken = accessToken,
              onBack = { currentScreen = Screen.UserManagement }
            )
            Screen.UserList -> UserListScreen(
              accessToken = accessToken,
              onBack = { currentScreen = Screen.UserManagement }
            )
          }
        }
      }
    }
  }
}
