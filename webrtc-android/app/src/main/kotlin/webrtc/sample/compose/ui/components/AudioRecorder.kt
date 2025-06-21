package webrtc.sample.compose.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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