package webrtc.sample.compose.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import webrtc.sample.compose.webrtc.sessions.LocalWebRtcSessionManager
import org.webrtc.RendererCommon
import org.webrtc.VideoTrack

/**
 * Renders a single video track based on the call state and always records it to a file.
 *
 * @param videoTrack The track containing the video stream for a given participant.
 * @param modifier Modifier for styling.
 */
@Composable
fun VideoRenderer(
  videoTrack: VideoTrack,
  recordingManager: RecordingManager,
  modifier: Modifier = Modifier
) {
  val trackState: MutableState<VideoTrack?> = remember { mutableStateOf(null) }
  var view: VideoTextureViewRenderer? by remember { mutableStateOf(null) }

  DisposableEffect(videoTrack) {
    onDispose {
      // Stop recording and clean up when the Composable is disposed
//      recordingManager.stopRecording()
      cleanTrack(view, trackState)
      videoTrack.removeSink(recordingManager)
    }
  }

  val sessionManager = LocalWebRtcSessionManager.current
  AndroidView(
    factory = { context ->
      VideoTextureViewRenderer(context).apply {
        init(
          sessionManager.peerConnectionFactory.eglBaseContext,
          object : RendererCommon.RendererEvents {
            override fun onFirstFrameRendered() = Unit
            override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) = Unit
          }
        )
        setupVideo(trackState, videoTrack, this, recordingManager)
        view = this
      }
    },
    update = { v -> setupVideo(trackState, videoTrack, v, recordingManager) },
    modifier = modifier
  )
}

private fun cleanTrack(
  view: VideoTextureViewRenderer?,
  trackState: MutableState<VideoTrack?>
) {
  view?.let { trackState.value?.removeSink(it) }
  trackState.value = null
}

private fun setupVideo(
  trackState: MutableState<VideoTrack?>,
  track: VideoTrack,
  renderer: VideoTextureViewRenderer,
  recordingManager: RecordingManager
) {
  if (trackState.value == track) {
    return
  }

  cleanTrack(renderer, trackState)

  trackState.value = track
  track.addSink(renderer)
  track.addSink(recordingManager)
}
