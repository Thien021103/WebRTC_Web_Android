package webrtc.sample.compose.webrtc.sessions

import webrtc.sample.compose.webrtc.SignalingClient
import webrtc.sample.compose.webrtc.peer.StreamPeerConnectionFactory
import kotlinx.coroutines.flow.SharedFlow
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack

interface WebRtcSessionManager {

  val signalingClient: SignalingClient

  val peerConnectionFactory: StreamPeerConnectionFactory

  val remoteVideoTrackFlow: SharedFlow<VideoTrack>

  val remoteAudioTrackFlow: SharedFlow<AudioTrack>

  fun onSessionScreenReady()

  fun enableMicrophone(enabled: Boolean)

  fun changeDevice(device: String)

  fun disconnect()
}
