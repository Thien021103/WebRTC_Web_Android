import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.media.ThumbnailUtils
import android.provider.MediaStore
import androidx.compose.foundation.Image
import io.getstream.webrtc.sample.compose.ui.screens.list.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoItem(
  video: Video,
  onVideoSelected: (Video) -> Unit
) {
  // Thumbnail generation
  val thumbnail by produceState<Bitmap?>(null, video.file) {
    value = withContext(Dispatchers.IO) {
      ThumbnailUtils.createVideoThumbnail(video.file.path, MediaStore.Images.Thumbnails.MINI_KIND)
    }
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onVideoSelected(video) }, // Select video on click
    elevation = 4.dp
  ) {
    Row(
      modifier = Modifier.padding(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Show thumbnail or placeholder
      if (thumbnail != null) {
        Image(
          bitmap = thumbnail!!.asImageBitmap(),
          contentDescription = "Thumbnail for ${video.displayName}",
          modifier = Modifier.size(100.dp)
        )
      } else {
        Box(
          modifier = Modifier
            .size(100.dp)
            .background(Color.Gray),
          contentAlignment = Alignment.Center
        ) {
          Text("No Thumbnail", color = Color.White, fontSize = 12.sp)
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = video.displayName,
        style = MaterialTheme.typography.subtitle1,
        modifier = Modifier.weight(1f)
      )
    }
  }
}