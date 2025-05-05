package io.getstream.webrtc.sample.compose.ui.screens.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class Video(
  val id: Long, // Can use file.lastModified() or a counter for uniqueness
  val displayName: String,
  val file: File
)

class VideoListViewModel(context: Context) : ViewModel() {
  private val _videos = MutableStateFlow<List<Video>>(emptyList())
  val videos: StateFlow<List<Video>> = _videos.asStateFlow()

  private val externalDir = context.getExternalFilesDir(null)
  private val videoDir = File(externalDir, "id")

  init {
    viewModelScope.launch {
      _videos.value = loadVideos()
    }
  }

  private suspend fun loadVideos(): List<Video> = withContext(Dispatchers.IO) {
    val dir = videoDir ?: return@withContext emptyList()
    dir.listFiles { file -> file.isFile && file.extension in listOf("mp4", "mkv", "avi") }
      ?.mapIndexed { index, file ->
        Video(
          id = index.toLong(), // Simple ID; could use file.lastModified() for uniqueness
          displayName = file.name,
          file = file
        )
      } ?: emptyList()
  }
}