package webrtc.sample.compose.ui.screens.main

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp


@Composable
fun MainScreen(
  onNavigateToMain: () -> Unit,
  onNavigateToGroup: () -> Unit,
  onNavigateToNotifications: () -> Unit,
  onNavigateToProfile: () -> Unit,
  currentRoute: String,
  contentScreen: @Composable () -> Unit
) {

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = when (currentRoute) {
              "main/main" -> "Welcome!"
              "main/group" -> "Your Group"
              "main/notifications" -> "Notifications history"
              "main/profile" -> "Profile"
              else -> "Welcome!"
            },
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
          selected = currentRoute == "main/main",
          selectedContentColor = MaterialTheme.colors.primary,
          unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
          onClick = { onNavigateToMain() }
        )
        BottomNavigationItem(
          icon = { Icon(Icons.Filled.Group, contentDescription = "Group") },
          label = { Text("Group") },
          selected = currentRoute == "main/group",
          selectedContentColor = MaterialTheme.colors.primary,
          unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
          onClick = { onNavigateToGroup() }
        )
        BottomNavigationItem(
          icon = { Icon(Icons.Filled.Notifications, contentDescription = "Notifications") },
          label = { Text("Notifications") },
          selected = currentRoute == "main/notifications",
          selectedContentColor = MaterialTheme.colors.primary,
          unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
          onClick = { onNavigateToNotifications() }
        )
        BottomNavigationItem(
          icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
          label = { Text("Profile") },
          selected = currentRoute == "main/profile",
          selectedContentColor = MaterialTheme.colors.primary,
          unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
          onClick = { onNavigateToProfile() }
        )
      }
    },
    content = { padding ->
      Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        contentScreen()
      }
    }
  )
}