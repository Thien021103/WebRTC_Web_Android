package io.getstream.webrtc.sample.compose.ui.screens.list

import VideoItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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