import android.graphics.Bitmap
import android.view.ViewGroup
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.media.ThumbnailUtils
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.runtime.saveable.rememberSaveable
import io.getstream.webrtc.sample.compose.ui.screens.list.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoItem(video: Video) {
  val context = LocalContext.current
  var isPlaying by rememberSaveable { mutableStateOf(false) } // Persists across config changes
  val exoPlayer = remember { // Lazily initialize ExoPlayer
    ExoPlayer.Builder(context).build().apply {
      setMediaItem(MediaItem.fromUri(video.file.toURI().toString()))
      prepare()
    }
  }

  // Thumbnail generation
  val thumbnail by produceState<Bitmap?>(null, video.file) {
    value = withContext(Dispatchers.IO) {
      ThumbnailUtils.createVideoThumbnail(video.file.path, MediaStore.Images.Thumbnails.MINI_KIND)
    }
  }

  // Cleanup ExoPlayer when composable is disposed
  DisposableEffect(Unit) {
    onDispose {
      exoPlayer.release()
    }
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable {
        isPlaying = !isPlaying
        if (isPlaying) {
          exoPlayer.play()
        } else {
          exoPlayer.pause()
        }
      },
    elevation = 4.dp
  ) {
    Row(
      modifier = Modifier.padding(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (isPlaying) {
        // Show video player
        AndroidView(
          factory = {
            PlayerView(context).apply {
              player = exoPlayer
              useController = true
              layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
              )
            }
          },
          modifier = Modifier.size(100.dp) // Match thumbnail size
        )
      } else {
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