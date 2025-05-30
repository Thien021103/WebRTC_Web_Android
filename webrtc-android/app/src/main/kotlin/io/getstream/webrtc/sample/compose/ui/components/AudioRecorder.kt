package io.getstream.webrtc.sample.compose.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.webrtc.AudioTrack

@Composable
fun AudioRecorder(
  audioTrack: AudioTrack,
  recordingManager: RecordingManager
) {
  DisposableEffect(audioTrack) {
    audioTrack.addSink(recordingManager)

    onDispose {
      audioTrack.removeSink(recordingManager)
//      recordingManager.stopRecording()
    }
  }
}