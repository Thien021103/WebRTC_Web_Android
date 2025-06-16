package io.getstream.webrtc.sample.compose

import android.Manifest
import android.content.pm.ActivityInfo
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
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

//sealed class Screen {
//  object Login : Screen()
//  object Register : Screen()
//  object Main : Screen()
//  object Videos : Screen()
//  object Signalling : Screen()
//  object RoleSelection : Screen()
//  object RegisterUser : Screen()
//  object UserManagement : Screen()
//  object UserList : Screen()
//  object Door : Screen()
//  object DoorHistory : Screen()
//}

// Helper functions to serialize/deserialize Screen
//private fun screenToString(screen: Screen): String = screen.javaClass.simpleName
//private fun stringToScreen(name: String): Screen = when (name) {
//  "Login" -> Screen.Login
//  "Register" -> Screen.Register
//  "Main" -> Screen.Main
//  "Videos" -> Screen.Videos
//  "Signalling" -> Screen.Signalling
//  "RoleSelection" -> Screen.RoleSelection
//  "RegisterUser" -> Screen.RegisterUser
//  "UserManagement" -> Screen.UserManagement
//  "UserList" -> Screen.UserList
//  "Door" -> Screen.Door
//  "DoorHistory" -> Screen.DoorHistory
//  else -> Screen.RoleSelection // Fallback
//}

class MainActivity : ComponentActivity() {
  @RequiresApi(Build.VERSION_CODES.R)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      WebrtcSampleComposeTheme {
        var showSettingsDialog by remember { mutableStateOf(false) }
        var permissionsGranted by remember { mutableStateOf(false) }

        // Save and restore state
        val viewModel: MainViewModel = viewModel()
        var currentScreen by rememberSaveable {
          mutableStateOf(viewModel.savedStateHandle.get<String>("currentScreen") ?: "RoleSelection")
        }
        var identifier by rememberSaveable {
          mutableStateOf(viewModel.savedStateHandle.get<String>("identifier") ?: "")
        }
        var groupName by rememberSaveable {
          mutableStateOf(viewModel.savedStateHandle.get<String>("groupName") ?: "")
        }
        var accessToken by rememberSaveable {
          mutableStateOf(viewModel.savedStateHandle.get<String>("accessToken") ?: "")
        }
        var role by rememberSaveable {
          mutableStateOf(viewModel.savedStateHandle.get<String>("role") ?: "")
        }
        var cloudFolder by rememberSaveable {
          mutableStateOf(viewModel.savedStateHandle.get<String>("cloudFolder") ?: "")
        }

        // Update saved state
        LaunchedEffect(currentScreen, identifier, groupName, accessToken, role, cloudFolder) {
          viewModel.savedStateHandle["currentScreen"] = currentScreen
          viewModel.savedStateHandle["identifier"] = identifier
          viewModel.savedStateHandle["groupName"] = groupName
          viewModel.savedStateHandle["accessToken"] = accessToken
          viewModel.savedStateHandle["role"] = role
          viewModel.savedStateHandle["cloudFolder"] = cloudFolder
        }

        // Permissions
        val requiredPermissions = mutableListOf(
          Manifest.permission.CAMERA,
          Manifest.permission.RECORD_AUDIO
        ).apply {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
          }
        }.toTypedArray()

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

        // Manage orientation based on screen
        LaunchedEffect(currentScreen) {
          requestedOrientation = when (currentScreen) {
//            Screen.Videos -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            "Videos" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
          }
        }

        // FCM Token
        var fcmToken by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
          try {
            fcmToken = FirebaseMessaging.getInstance().token.await()
            Log.d("FCM Token", fcmToken)
          } catch (e: Exception) {
            Log.e("MainActivity", "Failed to get FCM token: $e")
          }
        }

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
            when (currentScreen) {
              "RoleSelection" -> RoleSelectionScreen(
                onRoleSelected = { selectedRole ->
                  role = selectedRole
                  currentScreen = "Login"
                }
              )

              "Login" -> LoginScreen(
                fcmToken = fcmToken,
                role = role,
                onSuccess = { email, group, token, folder, config ->
                  identifier = email
                  groupName = group
                  accessToken = token
                  cloudFolder = folder
                  currentScreen = "Main"
                  (this.applicationContext as WebRTCApp).initializeMediaManager(config)
                },
                onRegister = { currentScreen = "Register" },
                onBack = { currentScreen = "RoleSelection" }
              )

              "Register" -> RegisterScreen(
                fcmToken = fcmToken,
                onSuccess = { email, group, token, folder, config ->
                  identifier = email
                  groupName = group
                  accessToken = token
                  cloudFolder = folder
                  currentScreen = "Main"
                  role = "Owner"
                  (this.applicationContext as WebRTCApp).initializeMediaManager(config)
                },
                onLogin = { currentScreen = "Login" },
                onBack = { currentScreen = "RoleSelection" }
              )

              "Main" -> MainScreen(
                role = role,
                identifier = identifier,
                groupName = groupName,
                token = accessToken,
                onVideosClick = { currentScreen = "Videos" },
                onSignallingClick = { currentScreen = "Signalling" },
                onDoorClick = { currentScreen = "Door" },
                onUserManagementClick = { currentScreen = "UserManagement" },
                onLogout = {
                  identifier = ""
                  groupName = ""
                  accessToken = ""
                  role = ""
                  currentScreen = "RoleSelection"
//                  (this.applicationContext as WebRTCApp).clearConfig() // Clearing this make MediaManager lost its config
                }
              )

              "Videos" -> VideoListScreen(
                role = role,
                viewModel = viewModel(factory = VideoListViewModel.Factory(accessToken)),
                onBack = { currentScreen = "Main" }
              )

              "Signalling" -> SignallingScreen(
                role = role,
                identifier = identifier,
                accessToken = accessToken,
                cloudFolder = cloudFolder,
                onBack = { currentScreen = "Main" }
              )

              "UserManagement" -> UserManagementScreen(
                onRegisterUserClick = { currentScreen = "RegisterUser" },
                onUserListClick = { currentScreen = "UserList" },
                onBack = { currentScreen = "Main" }
              )

              "RegisterUser" -> RegisterUserScreen(
                accessToken = accessToken,
                onBack = { currentScreen = "UserManagement" }
              )

              "UserList" -> UserListScreen(
                accessToken = accessToken,
                onBack = { currentScreen = "UserManagement" }
              )

              "Door" -> DoorScreen(
                accessToken = accessToken,
                identifier = identifier,
                role = role,
                onHistoryClick = { currentScreen = "DoorHistory" },
                onBack = { currentScreen = "Main" }
              )

              "DoorHistory" -> DoorHistoryScreen(
                accessToken = accessToken,
                onBack = { currentScreen = "Door" }
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

// ViewModel to hold saved state
class MainViewModel : ViewModel() {
  val savedStateHandle = SavedStateHandle()
}
