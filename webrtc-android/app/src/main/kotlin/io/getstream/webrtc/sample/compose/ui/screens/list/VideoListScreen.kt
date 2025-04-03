package io.getstream.webrtc.sample.compose.ui.screens.list

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoListScreen(viewModel: VideoListViewModel, onBack: () -> Unit) {
  val videos by viewModel.videos.collectAsState()

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(videos) { video ->
        VideoItem(video)
      }
    }
    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
      Text("Back", fontSize = 20.sp)
    }
  }
}

@Composable
fun VideoItem(video: Video) {
  val thumbnail by produceState<Bitmap?>(null, video.file) {
    value = withContext(Dispatchers.IO) {
      ThumbnailUtils.createVideoThumbnail(video.file.path, MediaStore.Images.Thumbnails.MINI_KIND)
    }
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = 4.dp
  ) {
    Row(
      modifier = Modifier.padding(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (thumbnail != null) {
        Image(
          bitmap = thumbnail!!.asImageBitmap(),
          contentDescription = "Thumbnail for ${video.displayName}",
          modifier = Modifier.size(100.dp)
        )
      } else {
        Box(
          modifier = Modifier.size(100.dp).background(Color.Gray),
          contentAlignment = Alignment.Center
        ) {
          Text("No Thumbnail", color = Color.White)
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