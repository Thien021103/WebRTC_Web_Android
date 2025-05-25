package io.getstream.webrtc.sample.compose.ui.screens.list

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
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.layout.ContentScale
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

@Composable
fun VideoItem(
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
    Row(
      modifier = Modifier.padding(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Load Cloudinary thumbnail with GlideImage
      GlideImage(
        imageModel = { video.thumbnailUrl },
        modifier = Modifier.size(100.dp),
        imageOptions = ImageOptions(contentScale = ContentScale.Crop, alignment = Alignment.Center),
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
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = video.displayName,
        style = MaterialTheme.typography.subtitle1,
        modifier = Modifier.weight(1f)
      )
      // Download button
      IconButton(onClick = { onDownload() }) {
        Icon(
          imageVector = Icons.Filled.CloudDownload,
          contentDescription = "Download ${video.displayName}",
          tint = Color.Blue
        )
      }
      // Delete button
      IconButton(onClick = { showDeleteDialog = true }) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete ${video.displayName}",
          tint = Color.Red
        )
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