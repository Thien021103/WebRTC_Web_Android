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
import io.getstream.webrtc.sample.compose.ui.screens.users.RegisterUserScreen
import io.getstream.webrtc.sample.compose.ui.screens.users.UserManagementScreen
import com.google.firebase.messaging.FirebaseMessaging
import io.getstream.webrtc.sample.compose.ui.screens.door.DoorHistoryScreen
import io.getstream.webrtc.sample.compose.ui.screens.door.DoorScreen
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
  object UserList : Screen()
  object Door : Screen()
  object DoorHistory : Screen()
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
              onSuccess = { emailOrId, id, token, folder, config ->
                identifier = emailOrId
                groupId = id
                accessToken = token
                cloudFolder = folder
                currentScreen = Screen.Main
                (this.applicationContext as WebRTCApp).initializeMediaManager(config)
              },
              onRegister = { currentScreen = Screen.Register },
              onBack = { currentScreen = Screen.RoleSelection }
            )
            Screen.Register -> RegisterScreen(
              fcmToken = fcmToken,
              onSuccess = { email, id, token, folder, config ->
                identifier = email
                groupId = id
                accessToken = token
                cloudFolder = folder
                currentScreen = Screen.Main
                role = "Owner"
                (this.applicationContext as WebRTCApp).initializeMediaManager(config)
              },
              onLogin = { currentScreen = Screen.Login },
              onBack = { currentScreen = Screen.RoleSelection }
            )
            Screen.Main -> MainScreen(
              role = role,
              identifier = identifier,
              id = groupId,
              token = accessToken,
              onVideosClick = { currentScreen = Screen.Videos },
              onSignallingClick = { currentScreen = Screen.Signalling },
              onDoorClick = { currentScreen = Screen.Door },
              onUserManagementClick = { currentScreen = Screen.UserManagement },
              onLogout = {
                identifier = ""
                groupId = ""
                accessToken = ""
                role = ""
                currentScreen = Screen.RoleSelection
                (this.applicationContext as WebRTCApp).clearConfig()
              }
            )
            Screen.Videos -> VideoListScreen(
              viewModel = VideoListViewModel(token = accessToken),
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
            Screen.Door -> DoorScreen(
              accessToken = accessToken,
              identifier = identifier,
              role = role,
              onHistoryClick = { currentScreen = Screen.DoorHistory },
              onBack = { currentScreen = Screen.Main }
            )
            Screen.DoorHistory -> DoorHistoryScreen(
              accessToken = accessToken,
              onBack = { currentScreen = Screen.Door }
            )
          }
        }
      }
    }
  }
}
