package webrtc.sample.compose.ui.screens.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MainScreen(
  role: String,
  identifier: String,
  groupName: String,
  token: String,
  onVideosClick: () -> Unit,
  onSignallingClick: () -> Unit,
  onDoorClick: () -> Unit,
  onUserManagementClick: () -> Unit,
  onLogout: () -> Unit
) {
  val navController = rememberNavController()
  var currentRoute by remember { mutableStateOf("main") }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text =
              if (currentRoute == "main") {"Welcome!"}
              else if (currentRoute == "group") {"Your Group"}
              else if (currentRoute == "notifications") {"Notifications history"}
              else {"Profile"},
            fontSize = 20.sp
          )
        },
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary
      )
    },
    bottomBar = {
      BottomNavigation(
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface
      ) {
        BottomNavigationItem(
          icon = { Icon(Icons.Filled.Home, contentDescription = "Main") },
          label = { Text("Main") },
          selected = currentRoute == "main",
          selectedContentColor = MaterialTheme.colors.primary,
          unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
          onClick = {
            if (currentRoute != "main") {
              navController.navigate("main")
              currentRoute = "main"
            }
          }
        )
        BottomNavigationItem(
          icon = { Icon(Icons.Filled.Group, contentDescription = "Group") },
          label = { Text("Group") },
          selected = currentRoute == "group",
          selectedContentColor = MaterialTheme.colors.primary,
          unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
          onClick = {
            if (currentRoute != "group") {
              navController.navigate("group")
              currentRoute = "group"
            }
          }
        )
        BottomNavigationItem(
          icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
          label = { Text("Profile") },
          selected = currentRoute == "profile",
          selectedContentColor = MaterialTheme.colors.primary,
          unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
          onClick = {
            if (currentRoute != "profile") {
              navController.navigate("profile")
              currentRoute = "profile"
            }
          }
        )
        BottomNavigationItem(
          icon = { Icon(Icons.Filled.Notifications, contentDescription = "Notifications") },
          label = { Text("Notifications") },
          selected = currentRoute == "notifications",
          selectedContentColor = MaterialTheme.colors.primary,
          unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
          onClick = {
            if (currentRoute != "notifications") {
              navController.navigate("notifications")
              currentRoute = "notifications"
            }
          }
        )
      }
    },
    content = { padding ->
      NavHost(
        navController = navController,
        startDestination = "main",
        modifier = Modifier.fillMaxSize().padding(padding)
      ) {
        composable("main") {
          OptionScreen(
            role = role,
            identifier = identifier,
            onVideosClick = onVideosClick,
            onSignallingClick = onSignallingClick,
            onDoorClick = onDoorClick,
            onUserManagementClick = onUserManagementClick
          )
        }
        composable("profile") {
          UserDetailScreen(
            role = role,
            identifier = identifier,
            groupName = groupName,
            accessToken = token,
            onLogout = { onLogout() }
          )
        }
        composable("group") {
          GroupDetailScreen(
            accessToken = token,
          )
        }
        composable("notifications") {
          NotificationScreen (
            accessToken = token,
          )
        }
      }
    }
  )
}