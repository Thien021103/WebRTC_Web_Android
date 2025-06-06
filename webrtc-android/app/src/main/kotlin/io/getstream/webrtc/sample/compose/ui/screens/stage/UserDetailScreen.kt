package io.getstream.webrtc.sample.compose.ui.screens.stage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.webrtc.sample.compose.R

@Composable
fun UserDetailScreen(
  role: String,
  identifier: String,
  groupName: String,
  onChangePassword: () -> Unit,
  onLogout: () -> Unit
) {
  var askLogout by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Card(
      modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
      elevation = 4.dp,
      shape = RoundedCornerShape(12.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.Start
      ) {
        Text(
          text = "Profile Details",
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colors.primary,
          modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
          text = "Role: $role",
          fontSize = 18.sp,
          modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
          text = "Identifier: $identifier",
          fontSize = 18.sp,
          modifier = Modifier.padding(bottom = 8.dp)
        )
        if (role == "Owner") {
          Text(
            text = "Group name: $groupName",
            fontSize = 18.sp
          )
        }
      }
    }
    Spacer(modifier = Modifier.weight(1f))
    Button(
      onClick = { askLogout = true },
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(12.dp),
      elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
      colors = ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.colors.secondaryVariant,
        contentColor = MaterialTheme.colors.onSecondary
      )
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.Redo,
        contentDescription = "Change password",
        modifier = Modifier.padding(end = 8.dp)
      )
      Text("Change password", fontSize = 20.sp)
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
      onClick = { askLogout = true },
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(12.dp),
      elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
      colors = ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.colors.error,
        contentColor = MaterialTheme.colors.onError
      )
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
        contentDescription = "Logout",
        modifier = Modifier.padding(end = 8.dp)
      )
      Text("Logout", fontSize = 20.sp)
    }
  }

  if (askLogout) {
    AlertDialog(
      onDismissRequest = { askLogout = false },
      title = {
        Text(
          text = stringResource(R.string.confirm_logout),
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Text(
          text = stringResource(R.string.logout_message),
          fontSize = 16.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onLogout()
            askLogout = false
          },
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.height(50.dp),
          colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.error,
            contentColor = MaterialTheme.colors.onError
          )
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 8.dp)
          ) {
            Text(
              text = stringResource(R.string.confirm),
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { askLogout = false }) {
          Text(stringResource(R.string.cancel), fontSize = 14.sp)
        }
      },
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.height(170.dp),
      backgroundColor = MaterialTheme.colors.surface
    )
  }
}