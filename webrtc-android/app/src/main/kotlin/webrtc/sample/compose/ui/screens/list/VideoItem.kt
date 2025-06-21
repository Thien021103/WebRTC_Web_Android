package webrtc.sample.compose.ui.screens.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.layout.ContentScale
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

@Composable
fun VideoItem(
  role: String,
  video: Video,
  onVideoSelected: () -> Unit,
  onDownload: () -> Unit,
  onDelete: () -> Unit
) {
  var showDeleteDialog by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier.fillMaxWidth().clickable { onVideoSelected() },
    elevation = 4.dp
  ) {
    Column(
      modifier = Modifier.padding(8.dp)
    ) {
      // Video name at the top
      Text(
        text = video.displayName,
        style = MaterialTheme.typography.subtitle1,
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp)
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Thumbnail on the left
        GlideImage(
          imageModel = { video.thumbnailUrl },
          modifier = Modifier.size(100.dp),
          imageOptions = ImageOptions(
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
          ),
          loading = {
            Box(
              modifier = Modifier.size(100.dp).background(Color.Gray),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator()
            }
          },
          failure = {
            Box(
              modifier = Modifier.size(100.dp).background(Color.Gray),
              contentAlignment = Alignment.Center
            ) {
              Text("No Thumbnail", color = Color.White, fontSize = 12.sp)
            }
          },
          success = { _, painter ->
            Log.d("VideoItem", "Loaded thumbnail for ${video.displayName}")
            Image(
              painter = painter,
              contentDescription = "Thumbnail for ${video.displayName}",
              modifier = Modifier.size(100.dp),
              contentScale = ContentScale.Crop
            )
          }
        )
        Spacer(modifier = Modifier.width(20.dp))
        // Spacer to push buttons to the right
        Spacer(modifier = Modifier.weight(1f))
        // Column for buttons on the right
        Column(
          horizontalAlignment = Alignment.End,
          modifier = Modifier.padding(8.dp)
        ) {
          // Download button
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colors.primaryVariant,
            modifier = Modifier.clickable { onDownload() }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Filled.CloudDownload,
                contentDescription = "Download ${video.displayName}",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Download",
                color = Color.White,
                fontSize = 16.sp
              )
            }
          }
          if (role == "Owner") {
            Spacer(modifier = Modifier.height(16.dp))
            // Delete button
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colors.error,
              modifier = Modifier.clickable { showDeleteDialog = true }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Delete ${video.displayName}",
                  tint = Color.White,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Delete",
                  color = Color.White,
                  fontSize = 16.sp
                )
              }
            }
          }
        }
      }
    }
  }

  // Delete Confirmation Dialog
  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("Confirm Delete") },
      text = { Text("Are you sure you want to delete this user?") },
      confirmButton = {
        TextButton(
          onClick = {
            onDelete()
            showDeleteDialog = false
          }
        ) {
          Text("Delete", color = MaterialTheme.colors.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}