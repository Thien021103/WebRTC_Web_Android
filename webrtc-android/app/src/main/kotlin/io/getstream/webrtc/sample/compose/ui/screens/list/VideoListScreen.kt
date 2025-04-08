package io.getstream.webrtc.sample.compose.ui.screens.list

import VideoItem
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun VideoListScreen(
  viewModel: VideoListViewModel,
  onBack: () -> Unit
) {
  val videos by viewModel.videos.collectAsState()
  val context = LocalContext.current

  // Single ExoPlayer instance for the screen
  val exoPlayer = remember {
    ExoPlayer.Builder(context).build()
  }

  // Track the currently selected video
  var selectedVideo by remember { mutableStateOf<Video?>(null) }

  // Cleanup ExoPlayer when screen is disposed
  DisposableEffect(Unit) {
    onDispose {
      exoPlayer.release()
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      text = if (selectedVideo != null) selectedVideo!!.displayName else "Select a video to play",
      color = Color.Black,
      fontSize = 16.sp,
      textAlign = TextAlign.End
    )
    // Dedicated Player View at the top
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(280.dp) // Fixed height for player
        .background(Color.Black),
      contentAlignment = Alignment.Center
    ) {
      if (selectedVideo != null) {
        AndroidView(
          factory = {
            PlayerView(context).apply {
              player = exoPlayer
              useController = true
              layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
              )
            }
          },
          update = { playerView ->
            // Update media when selectedVideo changes
            if (selectedVideo != null) {
              exoPlayer.setMediaItem(MediaItem.fromUri(selectedVideo!!.file.toURI().toString()))
              exoPlayer.prepare()
              exoPlayer.play()
            }
          },
          modifier = Modifier.fillMaxSize()
        )
      } else {
        Text(
          text = "Select a video to play",
          color = Color.White,
          fontSize = 16.sp
        )
      }
    }
    // List of videos below the player
    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(videos) { video ->
        VideoItem(
          video = video,
          onVideoSelected = { selected ->
            selectedVideo = selected
          }
        )
      }
    }
    // Back button
    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
      Text("Back", fontSize = 20.sp)
    }
  }
}