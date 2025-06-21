package webrtc.sample.compose.ui.screens.stage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoleSelectionScreen(
  onRoleSelected: (String) -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "Select Your Role",
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 32.dp)
    )
    Button(
      onClick = { onRoleSelected("Owner") },
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(12.dp),
      elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
      colors = ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary
      )
    ) {
      Text("Owner", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(18.dp))
    Button(
      onClick = { onRoleSelected("User") },
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(12.dp),
      elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
      colors = ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.colors.secondary,
        contentColor = MaterialTheme.colors.onSecondary
      )
    ) {
      Text("User", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
  }
}