package webrtc.sample.compose

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import webrtc.sample.compose.ui.screens.users.RegisterUserScreen
import webrtc.sample.compose.ui.screens.users.UserManagementScreen
import com.google.firebase.messaging.FirebaseMessaging
import webrtc.sample.compose.ui.screens.door.DoorHistoryScreen
import webrtc.sample.compose.ui.screens.door.DoorScreen
import webrtc.sample.compose.ui.screens.list.VideoListScreen
import webrtc.sample.compose.ui.screens.list.VideoListViewModel
import webrtc.sample.compose.ui.screens.stage.LoginScreen
import webrtc.sample.compose.ui.screens.main.MainScreen
import webrtc.sample.compose.ui.screens.stage.RegisterScreen
import webrtc.sample.compose.ui.screens.stage.RoleSelectionScreen
import webrtc.sample.compose.ui.screens.stage.SignallingScreen
import webrtc.sample.compose.ui.screens.users.UserListScreen
import webrtc.sample.compose.ui.theme.WebrtcSampleComposeTheme
import kotlinx.coroutines.tasks.await
import webrtc.sample.compose.ui.screens.main.GroupDetailScreen
import webrtc.sample.compose.ui.screens.main.NotificationScreen
import webrtc.sample.compose.ui.screens.main.OptionScreen
import webrtc.sample.compose.ui.screens.main.UserDetailScreen

class MainActivity : ComponentActivity() {
  @RequiresApi(Build.VERSION_CODES.R)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      WebrtcSampleComposeTheme {
        var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
        var permissionsGranted by rememberSaveable { mutableStateOf(false) }
        val navController = rememberNavController()

        // Save and restore state
        var currentScreen by rememberSaveable { mutableStateOf("RoleSelection") }
        var identifier by rememberSaveable { mutableStateOf("") }
        var groupName by rememberSaveable { mutableStateOf("") }
        var accessToken by rememberSaveable { mutableStateOf("") }
        var role by rememberSaveable { mutableStateOf("") }
        var cloudFolder by rememberSaveable { mutableStateOf("") }

        // Save Cloudinary config components
        var cloudName by rememberSaveable { mutableStateOf("") }

        // Restore Cloudinary config
        LaunchedEffect(identifier, accessToken, cloudName) {
          if (identifier.isNotEmpty() && accessToken.isNotEmpty() && cloudName.isNotEmpty()) {
            val config = mapOf("cloud_name" to cloudName)
            try {
              (applicationContext as WebRTCApp).initializeMediaManager(config)
            } catch (e: Exception) {
              Log.e("MainActivity", "Failed to initialize MediaManager: ${e.message}", e)
              if (navController.currentDestination?.route !in listOf("roleSelection", "login", "register")) {
                identifier = ""
                accessToken = ""
                groupName = ""
                role = ""
                try {
                  navController.navigate("roleSelection")
                } catch (err: Exception) {
                  Log.e("MainActivity", "Error: ${err.message}", err)
                }
              }
            }
          } else if (navController.currentDestination?.route !in listOf("roleSelection", "login", "register")) {
            Log.w("MainActivity", "Cannot restore MediaManager")
            identifier = ""
            accessToken = ""
            groupName = ""
            role = ""
            try {
              navController.navigate("roleSelection")
            } catch (err: Exception) {
              Log.e("MainActivity", "Error: ${err.message}", err)
            }
          }
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

        LaunchedEffect(navController) {
          navController.addOnDestinationChangedListener { _, destination, _ ->
            requestedOrientation = if (destination.route == "videos") {
              ActivityInfo.SCREEN_ORIENTATION_SENSOR
            } else {
              ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
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
            title = { Text(text = "Permissions Required", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
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
            confirmButton = {},
            dismissButton = {
              TextButton(onClick = { finish() }) {
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
            NavHost(
              navController = navController,
              startDestination = "roleSelection",
              modifier = Modifier.fillMaxSize()
            ) {
              composable("roleSelection") {
                RoleSelectionScreen(
                  onRoleSelected = { selectedRole ->
                    role = selectedRole
                    navController.navigate("login")
                  }
                )
              }
              composable("login") {
                LoginScreen(
                  fcmToken = fcmToken,
                  role = role,
                  onSuccess = { email, group, token, folder, config ->
                    identifier = email
                    groupName = group
                    accessToken = token
                    cloudFolder = folder
                    cloudName = config["cloud_name"].toString()
                    (applicationContext as WebRTCApp).initializeMediaManager(config)
                    navController.navigate("main/main") { popUpTo("roleSelection") { inclusive = true } }
                    Log.d("MainActivity", "Login success: identifier=$email")
                  },
                  onRegister = { navController.navigate("register") },
                  onBack = { navController.popBackStack() }
                )
              }
              composable("register") {
                RegisterScreen(
                  fcmToken = fcmToken,
                  onSuccess = { email, group, token, folder, config ->
                    identifier = email
                    groupName = group
                    accessToken = token
                    cloudFolder = folder
                    cloudName = config["cloud_name"].toString()
                    (applicationContext as WebRTCApp).initializeMediaManager(config)
                    role = "Owner"
                    navController.navigate("main/main") { popUpTo("roleSelection") { inclusive = true } }
                    Log.d("MainActivity", "Register success: identifier=$email")
                  },
                  onLogin = { navController.navigate("login") },
                  onBack = { navController.popBackStack() }
                )
              }
              composable("main/main") {
                MainScreen(
                  onNavigateToMain = { navController.navigate("main/main") },
                  onNavigateToGroup = { navController.navigate("main/group") },
                  onNavigateToNotifications = { navController.navigate("main/notifications") },
                  onNavigateToProfile = { navController.navigate("main/profile") },
                  currentRoute = "main/main",
                  contentScreen = {
                  OptionScreen(
                    role = role,
                    identifier = identifier,
                    onVideosClick = { navController.navigate("videos") },
                    onSignallingClick = { navController.navigate("signalling") { popUpTo("main/main") { inclusive = true } } },
                    onDoorClick = { navController.navigate("door") },
                    onUserManagementClick = { navController.navigate("user") },
                  )
                }
                )
              }
              composable("main/group") {
                MainScreen(
                  onNavigateToMain = { navController.navigate("main/main") },
                  onNavigateToGroup = { navController.navigate("main/group") },
                  onNavigateToNotifications = { navController.navigate("main/notifications") },
                  onNavigateToProfile = { navController.navigate("main/profile") },
                  currentRoute = "main/group",
                  contentScreen = { GroupDetailScreen(accessToken = accessToken) }
                )
              }
              composable("main/notifications") {
                MainScreen(
                  onNavigateToMain = { navController.navigate("main/main") },
                  onNavigateToGroup = { navController.navigate("main/group") },
                  onNavigateToNotifications = { navController.navigate("main/notifications") },
                  onNavigateToProfile = { navController.navigate("main/profile") },
                  currentRoute = "main/notifications",
                  contentScreen = { NotificationScreen(accessToken = accessToken) }
                )
              }
              composable("main/profile") {
                MainScreen(
                  onNavigateToMain = { navController.navigate("main/main") },
                  onNavigateToGroup = { navController.navigate("main/group") },
                  onNavigateToNotifications = { navController.navigate("main/notifications") },
                  onNavigateToProfile = { navController.navigate("main/profile") },
                  currentRoute = "main/profile",
                  contentScreen = {
                    UserDetailScreen(
                      role = role,
                      identifier = identifier,
                      groupName = groupName,
                      accessToken = accessToken,
                      onLogout = {
                        identifier = ""
                        groupName = ""
                        accessToken = ""
                        role = ""
//                        cloudFolder = ""
//                        cloudName = ""
//                        (applicationContext as WebRTCApp).clearConfig()
                        navController.navigate("roleSelection") { popUpTo("roleSelection") { inclusive = true } }
                      }
                    )
                  }
                )
              }
              composable("videos") {
                VideoListScreen(
                  role = role,
                  viewModel = VideoListViewModel(accessToken),
                  onBack = { navController.popBackStack() }
                )
              }
              composable("signalling") {
                SignallingScreen(
                  role = role,
                  identifier = identifier,
                  accessToken = accessToken,
                  cloudFolder = cloudFolder,
                  onBack = { navController.navigate("main/main") { popUpTo("signalling") { inclusive = true } } }
                )
              }
              composable("user") {
                UserManagementScreen(
                  onRegisterUserClick = { navController.navigate("user/register") },
                  onUserListClick = { navController.navigate("user/list") },
                  onBack = { navController.popBackStack() }
                )
              }
              composable("user/register") {
                RegisterUserScreen(
                  accessToken = accessToken,
                  onBack = { navController.popBackStack() }
                )
              }
              composable("user/list") {
                UserListScreen(
                  accessToken = accessToken,
                  onBack = { navController.popBackStack() }
                )
              }
              composable("door") {
                DoorScreen(
                  accessToken = accessToken,
                  identifier = identifier,
                  role = role,
                  onHistoryClick = { navController.navigate("door/history") },
                  onBack = { navController.popBackStack() }
                )
              }
              composable("door/history") {
                DoorHistoryScreen(
                  accessToken = accessToken,
                  onBack = { navController.popBackStack() }
                )
              }
            }
          }
        } else {
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