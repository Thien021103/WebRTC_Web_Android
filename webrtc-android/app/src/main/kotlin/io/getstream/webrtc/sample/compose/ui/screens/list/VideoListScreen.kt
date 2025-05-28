package io.getstream.webrtc.sample.compose.ui.screens.list

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import com.cloudinary.android.cldvideoplayer.CldVideoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun VideoListScreen(
  role: String,
  viewModel: VideoListViewModel,
  onBack: () -> Unit
) {
  val videos by viewModel.videos.collectAsState()
  val context = LocalContext.current
  var selectedVideo by remember { mutableStateOf<Video?>(null) }
  var downloadVideo by remember { mutableStateOf<Video?>(null) }
  var isLoading by remember { mutableStateOf(false) }

  val coroutineScope = rememberCoroutineScope()

  val cldVideoPlayer = remember {
    CldVideoPlayer(context, "") // Empty URL initially
  }

  DisposableEffect(Unit) {
    onDispose {
      cldVideoPlayer.player?.release()
      Log.d("VideoListScreen", "CldVideoPlayer released")
    }
  }

  val downloadLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult(),
    onResult = { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        result.data?.data?.let { uri ->
          coroutineScope.launch(Dispatchers.IO) {
            try {
              context.contentResolver.openOutputStream(uri)?.use { output ->
                URL(downloadVideo!!.url).openStream().use { input ->
                  input.copyTo(output)
                }
              }
              withContext(Dispatchers.Main) {
                Toast.makeText(context, "Downloaded to $uri", Toast.LENGTH_SHORT).show()
              }
            } catch (e: Exception) {
              withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
              }
            }
          }
        }
      }
    }
  )
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Video List", fontSize = 20.sp) },
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary
      )
    },
    bottomBar = {
      Button(
        onClick = onBack,
        modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 32.dp).height(56.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
        colors = ButtonDefaults.buttonColors(
          backgroundColor = MaterialTheme.colors.primary,
          contentColor = MaterialTheme.colors.onPrimary
        )
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          modifier = Modifier.padding(end = 8.dp)
        )
        Text(text = "Back", fontSize = 18.sp, fontWeight = FontWeight.Bold)
      }
    },
    content = { padding ->
      Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = selectedVideo?.displayName ?: "Select a video to play",
          color = Color.Black,
          fontSize = 24.sp,
          textAlign = TextAlign.Center
        )
        Box(
          modifier = Modifier.fillMaxWidth().height(280.dp).background(Color.Black),
          contentAlignment = Alignment.Center
        ) {
          if (selectedVideo != null) {
            AndroidView(
              factory = { ctx ->
                PlayerView(ctx).apply {
                  player = cldVideoPlayer.player
                  useController = true
                  layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                  )
                  Log.d("VideoListScreen", "PlayerView created")
                }
              },
              update = {
                cldVideoPlayer.player?.apply {
                  setMediaItem(MediaItem.fromUri(selectedVideo!!.url))
                  prepare()
                  play()
                }
                Log.d("VideoListScreen", "Playing video: ${selectedVideo!!.id}")
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
        if (videos.isEmpty() || isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(50.dp).align(Alignment.CenterHorizontally),
            color = MaterialTheme.colors.onSecondary,
            strokeWidth = 2.dp
          )
        }
        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(videos) { video ->
            VideoItem(
              role = role,
              video = video,
              onVideoSelected = { selectedVideo = video },
              onDownload = {
                downloadVideo = video
                viewModel.downloadVideo(video = video, launcher = downloadLauncher)
              },
              onDelete = {
                isLoading = true
                viewModel.deleteVideo(
                  video = video,
                  onDone = {
                    isLoading = false;
                  }
                )
              }
            )
          }
        }
      }
    }
  )
}