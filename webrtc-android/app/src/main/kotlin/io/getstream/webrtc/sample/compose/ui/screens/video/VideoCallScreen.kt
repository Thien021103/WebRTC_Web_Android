package io.getstream.webrtc.sample.compose.ui.screens.video

import android.content.Context
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.getstream.webrtc.sample.compose.ui.components.VideoRenderer
import io.getstream.webrtc.sample.compose.webrtc.sessions.LocalWebRtcSessionManager
import io.getstream.webrtc.sample.compose.ui.components.AudioRecorder
import io.getstream.webrtc.sample.compose.ui.components.RecordingManager
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
fun getOutputFile(
  context: Context,
  identifier: String,
  cloudFolder: String
): File {
  // Format: |day-month-year|hour:min:sec|recorded.mp4
  val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH-mm-ss")
  val timestamp = LocalDateTime.now().format(formatter)
  val fileName = "Record $timestamp (Caller: $identifier).mp4" // e.g., |24-04-2025|17:48:42|recorded.mp4
  val baseDir = context.getExternalFilesDir(null)
  try {
    // Try Downloads directory first
    val idDir = File(baseDir, cloudFolder) // /storage/emulated/0/Android/data/<package_name>/files/cloudFolder/
    // Ensure /id directory exists
    if (!idDir.exists()) {
      idDir.mkdirs()
    }
    return File(idDir, fileName)
  } catch (_: Exception) {
    return File(baseDir, fileName)
  }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun VideoCallScreen(
  role: String,
  identifier: String,
  accessToken: String,
  cloudFolder: String,
  onCancelCall: () -> Unit
) {
  val sessionManager = LocalWebRtcSessionManager.current
  val context = LocalContext.current
  val outputFile = getOutputFile(cloudFolder = cloudFolder, context = context, identifier = identifier)
  val mediaMuxer = remember { MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4) }
  val recordingManager = remember { RecordingManager(cloudFolder = cloudFolder, context = context, mediaMuxer = mediaMuxer, outputFile = outputFile) }

  val remoteVideoTrackState by sessionManager.remoteVideoTrackFlow.collectAsState(null)
  val remoteVideoTrack = remoteVideoTrackState

//  val localVideoTrackState by sessionManager.localVideoTrackFlow.collectAsState(null)
//  val localVideoTrack = localVideoTrackState

//  val remoteAudioTrackState by sessionManager.remoteAudioTrackFlow.collectAsState(initial = null)
//  val remoteAudioTrack = remoteAudioTrackState

  var callMediaState by remember { mutableStateOf(CallMediaState()) }
  var showUploadDialog by remember { mutableStateOf(false) }
  var uploadProgress by remember { mutableFloatStateOf(0f) } // 0 to 1
  var uploadCompleted by remember { mutableStateOf(false) }
  var uploadFailed by remember { mutableStateOf(false) }

  LaunchedEffect(key1 = Unit) {
    sessionManager.onSessionScreenReady()
  }

  Box(
    modifier = Modifier.fillMaxSize()
  ) {
    var parentSize: IntSize by remember { mutableStateOf(IntSize(0, 0)) }
    if (remoteVideoTrack != null) {
      VideoRenderer(
        videoTrack = remoteVideoTrack,
        recordingManager = recordingManager,
        modifier = Modifier
          .fillMaxSize()
          .absolutePadding(0.dp, 0.dp, 0.dp, 100.dp)
          .clip(RoundedCornerShape(5.dp))
          .onSizeChanged { parentSize = it }
      )
    }
//    if(remoteAudioTrack != null) {
//      AudioRecorder(
//        audioTrack = remoteAudioTrack,
//        recordingManager = recordingManager
//      )
//    }

// Enhanced logging for local video condition
//    if (localVideoTrack != null) {
////      if (callMediaState.isCameraEnabled) {
//        Log.d("VideoCallScreen", "Local video track available and camera enabled")
//        FloatingVideoRenderer(
//          modifier = Modifier
//            .size(width = 150.dp, height = 210.dp)
//            .clip(RoundedCornerShape(16.dp))
//            .align(Alignment.TopEnd),
//          videoTrack = localVideoTrack,
//          parentBounds = parentSize,
//          paddingValues = PaddingValues(0.dp)
//        )
////      } else {
////        Log.d ( "VideoCallScreen", "Local video track exists but camera is disabled: isCameraEnabled=${callMediaState.isCameraEnabled}" )
////      }
//    } else {
//      Log.d ( "VideoCallScreen", "Local video track null" )
//    }

//    val activity = (LocalContext.current as? Activity)

    VideoCallControls(
      context = context,
      identifier = identifier,
      role = role,
      accessToken = accessToken,
      modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
      callMediaState = callMediaState,
      onChangeDevice = { device -> sessionManager.changeDevice(device) },
      onCallAction = {
        when (it) {
          is CallAction.ToggleMicroPhone -> {
            val enabled = callMediaState.isMicrophoneEnabled.not()
            callMediaState = callMediaState.copy(isMicrophoneEnabled = enabled)
            sessionManager.enableMicrophone(enabled)
          }
          CallAction.LeaveCall -> {
            Log.d("Upload Dialog", "Show Upload dialog")
            showUploadDialog = true
            sessionManager.disconnect()
            recordingManager.stopRecording (
              onUploadUpdate = { url, progress, isComplete ->
                uploadProgress = progress
                uploadCompleted = isComplete && url != null
                uploadFailed = isComplete && url == null
              }
            )
          }
        }
      }
    )
  }
  // Upload Dialog
  if (showUploadDialog) {
    AlertDialog(
      onDismissRequest = { /* Prevent dismiss until complete */ },
      title = { Text("Uploading Video") },
      text = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          if (uploadCompleted) {
            Text("Upload Complete!")
            Log.d("Upload Dialog", "Close dialog on success")
          } else if (uploadFailed) {
            Text("Upload Failed.")
            Log.d("Upload Dialog", "Close dialog on failed")
          } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
              CircularProgressIndicator()
              Spacer(modifier = Modifier.height(12.dp))
              Text("Progress: ${(uploadProgress * 100).toInt()}%")
            }
          }
        }
      },
      confirmButton = {
        if (uploadCompleted || uploadFailed) {
          TextButton(onClick = {
            Log.d("Upload Dialog", "Close dialog")
            showUploadDialog = false
            onCancelCall.invoke() // Proceed to parent screen
          }) {
            Text("OK")
          }
        }
      },
      dismissButton = {
        if (!uploadCompleted && !uploadFailed) {
          TextButton(onClick = {
            Log.d("Upload Dialog", "Close upload")
            showUploadDialog = false
            onCancelCall.invoke() // Proceed to parent screen
          }) {
            Text("Close", color = MaterialTheme.colors.error)
          }
        }
      }
    )
  }
}
