package io.getstream.webrtc.sample.compose

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import io.getstream.webrtc.sample.compose.ui.screens.users.RegisterUserScreen
import io.getstream.webrtc.sample.compose.ui.screens.users.UserManagementScreen
import com.google.firebase.messaging.FirebaseMessaging
import io.getstream.webrtc.sample.compose.ui.screens.door.DoorHistoryScreen
import io.getstream.webrtc.sample.compose.ui.screens.door.DoorScreen
import io.getstream.webrtc.sample.compose.ui.screens.list.VideoListScreen
import io.getstream.webrtc.sample.compose.ui.screens.list.VideoListViewModel
import io.getstream.webrtc.sample.compose.ui.screens.stage.LoginScreen
import io.getstream.webrtc.sample.compose.ui.screens.main.MainScreen
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
  @RequiresApi(Build.VERSION_CODES.R)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Define permissions based on API level
    val requiredPermissions = mutableListOf(
      Manifest.permission.CAMERA,
      Manifest.permission.RECORD_AUDIO,
//      Manifest.permission.MANAGE_EXTERNAL_STORAGE
    ).apply {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
      }
    }.toTypedArray()

    setContent {
      WebrtcSampleComposeTheme {
        var showSettingsDialog by remember { mutableStateOf(false) }
        var permissionsGranted by remember { mutableStateOf(false) }

        // Register permission request launcher
        val permissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
          Log.d("MainActivity", "Permission results: $permissions")
          val allGranted = permissions.all { it.value }
          if (allGranted) {
            Log.d("MainActivity", "All permissions granted")
            permissionsGranted = true
            showSettingsDialog = false
          } else {
            // Check for POST_NOTIFICATIONS separately
            val postNotificationsDenied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              permissions[Manifest.permission.POST_NOTIFICATIONS] == false
            } else {
              false
            }
            val otherPermissionsDenied = permissions.any { (permission, granted) ->
              permission != Manifest.permission.POST_NOTIFICATIONS && !granted
            }
            Log.d("MainActivity", "Post Notifications denied: $postNotificationsDenied, Other permissions denied: $otherPermissionsDenied")

            if (postNotificationsDenied && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
              Log.d("MainActivity", "POST_NOTIFICATIONS denied, showing settings dialog")
              showSettingsDialog = true
            } else if (otherPermissionsDenied) {
              Log.d("MainActivity", "Other permissions denied, showing rationale")
              showSettingsDialog = true
            }
          }
        }

        // Launch permission request on start
        LaunchedEffect(Unit) {
          Log.d("MainActivity", "Requesting permissions: ${requiredPermissions.joinToString()}")
          permissionLauncher.launch(requiredPermissions)
        }

        // Show settings dialog
        if (showSettingsDialog) {
          AlertDialog(
            onDismissRequest = { /* Non-dismissable */ },
            title = {
              Text(
                text = "Permissions Required",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
              )
            },
            text = {
              Text(
                text = "This app requires the following permissions to function:\n" +
                  "- Camera: For video calls\n" +
                  "- Microphone: For audio in calls\n" +
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    "- Notifications: To receive alerts\n\n"
                  } else {
                    "\n"
                  } +
                  "Please grant all permissions to continue.",
                fontSize = 16.sp
              )
            },
            confirmButton = { },
            dismissButton = {
              TextButton(
                onClick = {
                  Log.d("MainActivity", "User chose to exit from settings dialog")
                  finish()
                }
              ) {
                Text("Exit", fontSize = 14.sp)
              }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(350.dp)
          )
        } else if (permissionsGranted) {
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
            var groupName by remember { mutableStateOf("") }
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
                onSuccess = { email, group, token, folder, config ->
                  identifier = email
                  groupName = group
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
                onSuccess = { email, group, token, folder, config ->
                  identifier = email
                  groupName = group
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
                groupName = groupName,
                token = accessToken,
                onVideosClick = { currentScreen = Screen.Videos },
                onSignallingClick = { currentScreen = Screen.Signalling },
                onDoorClick = { currentScreen = Screen.Door },
                onUserManagementClick = { currentScreen = Screen.UserManagement },
                onLogout = {
                  identifier = ""
                  groupName = ""
                  accessToken = ""
                  role = ""
                  currentScreen = Screen.RoleSelection
//                  (this.applicationContext as WebRTCApp).clearConfig() // Clearing this make MediaManager lost its config
                }
              )

              Screen.Videos -> VideoListScreen(
                role = role,
                viewModel = VideoListViewModel(token = accessToken),
                onBack = { currentScreen = Screen.Main }
              )

              Screen.Signalling -> SignallingScreen(
                role = role,
                identifier = identifier,
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
        } else if (!showSettingsDialog) {
          // Placeholder while requesting permissions
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator()
          }
        }
      }
    }
  }
}
