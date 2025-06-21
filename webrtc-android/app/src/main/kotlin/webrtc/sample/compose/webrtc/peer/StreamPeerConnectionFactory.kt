package webrtc.sample.compose.webrtc.peer

import android.content.Context
import android.os.Build
import io.getstream.log.taggedLogger
import kotlinx.coroutines.CoroutineScope
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.EglBase
import org.webrtc.HardwareVideoEncoderFactory
import org.webrtc.IceCandidate
import org.webrtc.Logging
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SimulcastVideoEncoderFactory
import org.webrtc.SoftwareVideoEncoderFactory
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * The type of peer connections, either a [PUBLISHER] that sends data to the call or a [SUBSCRIBER]
 * that receives and decodes the data from the server.
 */
enum class StreamPeerType {
  PUBLISHER,
  SUBSCRIBER
}

class StreamPeerConnectionFactory constructor(
  private val context: Context
) {
  private val webRtcLogger by taggedLogger("Call:WebRTC")
  private val audioLogger by taggedLogger("Call:AudioTrackCallback")

  val eglBaseContext: EglBase.Context by lazy {
    EglBase.create().eglBaseContext
  }

  /**
   * Default video decoder factory used to unpack video from the remote tracks.
   */
  private val videoDecoderFactory by lazy {
    DefaultVideoDecoderFactory(
      eglBaseContext
    )
  }

  // rtcConfig contains STUN and TURN servers list
  val rtcConfig = PeerConnection.RTCConfiguration(
    arrayListOf(
//       Adding google's standard server, adding turn
//       PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
      PeerConnection.IceServer.builder("turn:103.149.28.136:3478")
        .setUsername("user1")
        .setPassword("password2")
        .createIceServer()
    )
  ).apply {
    // Use new unified sdp semantics
    sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
  }

  /**
   * Default encoder factory that supports Simulcast, used to send video tracks to the server.
   */
  private val videoEncoderFactory by lazy {
    val hardwareEncoder = HardwareVideoEncoderFactory(eglBaseContext, true, true)
    SimulcastVideoEncoderFactory(hardwareEncoder, SoftwareVideoEncoderFactory())
  }

  /**
   * Factory that builds all the connections based on the extensive configuration provided under
   * the hood.
   */
  private val factory by lazy {
    PeerConnectionFactory.initialize(
      PeerConnectionFactory.InitializationOptions.builder(context)
        .setInjectableLogger({ message, severity, label ->
          when (severity) {
            Logging.Severity.LS_VERBOSE -> {
              webRtcLogger.v { "[onLogMessage] label: $label, message: $message" }
            }
            Logging.Severity.LS_INFO -> {
              webRtcLogger.i { "[onLogMessage] label: $label, message: $message" }
            }
            Logging.Severity.LS_WARNING -> {
              webRtcLogger.w { "[onLogMessage] label: $label, message: $message" }
            }
            Logging.Severity.LS_ERROR -> {
              webRtcLogger.e { "[onLogMessage] label: $label, message: $message" }
            }
            Logging.Severity.LS_NONE -> {
              webRtcLogger.d { "[onLogMessage] label: $label, message: $message" }
            }
            else -> {}
          }
        }, Logging.Severity.LS_VERBOSE)
        .createInitializationOptions()
    )

    PeerConnectionFactory.builder()
      .setVideoDecoderFactory(videoDecoderFactory)
      .setVideoEncoderFactory(videoEncoderFactory)
      .setAudioDeviceModule(
        JavaAudioDeviceModule
          .builder(context)
          .setUseHardwareAcousticEchoCanceler(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
          .setUseHardwareNoiseSuppressor(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
          .setAudioRecordErrorCallback(object :
              JavaAudioDeviceModule.AudioRecordErrorCallback {
              override fun onWebRtcAudioRecordInitError(p0: String?) {
                audioLogger.w { "[onWebRtcAudioRecordInitError] $p0" }
              }

              override fun onWebRtcAudioRecordStartError(
                p0: JavaAudioDeviceModule.AudioRecordStartErrorCode?,
                p1: String?
              ) {
                audioLogger.w { "[onWebRtcAudioRecordInitError] $p1" }
              }

              override fun onWebRtcAudioRecordError(p0: String?) {
                audioLogger.w { "[onWebRtcAudioRecordError] $p0" }
              }
            })
          .setAudioTrackErrorCallback(object :
              JavaAudioDeviceModule.AudioTrackErrorCallback {
              override fun onWebRtcAudioTrackInitError(p0: String?) {
                audioLogger.w { "[onWebRtcAudioTrackInitError] $p0" }
              }

              override fun onWebRtcAudioTrackStartError(
                p0: JavaAudioDeviceModule.AudioTrackStartErrorCode?,
                p1: String?
              ) {
                audioLogger.w { "[onWebRtcAudioTrackStartError] $p0" }
              }

              override fun onWebRtcAudioTrackError(p0: String?) {
                audioLogger.w { "[onWebRtcAudioTrackError] $p0" }
              }
            })
          .setAudioRecordStateCallback(object :
              JavaAudioDeviceModule.AudioRecordStateCallback {
              override fun onWebRtcAudioRecordStart() {
                audioLogger.d { "[onWebRtcAudioRecordStart] no args" }
              }

              override fun onWebRtcAudioRecordStop() {
                audioLogger.d { "[onWebRtcAudioRecordStop] no args" }
              }
            })
          .setAudioTrackStateCallback(object :
              JavaAudioDeviceModule.AudioTrackStateCallback {
              override fun onWebRtcAudioTrackStart() {
                audioLogger.d { "[onWebRtcAudioTrackStart] no args" }
              }

              override fun onWebRtcAudioTrackStop() {
                audioLogger.d { "[onWebRtcAudioTrackStop] no args" }
              }
            })
          .createAudioDeviceModule().also {
            it.setMicrophoneMute(false)
            it.setSpeakerMute(false)
          }
      )
      .createPeerConnectionFactory()
  }

  /**
   * Builds a [StreamPeerConnection] that wraps the WebRTC [PeerConnection] and exposes several
   * helpful handlers.
   *
   * @param coroutineScope Scope used for asynchronous operations.
   * @param configuration The [PeerConnection.RTCConfiguration] used to set up the connection.
   * @param type The type of connection, either a subscriber of a publisher.
   * @param mediaConstraints Constraints used for audio and video tracks in the connection.
   * @param onStreamAdded Handler when a new [MediaStream] gets added.
   * @param onNegotiationNeeded Handler when there's a new negotiation.
   * @param onIceCandidateRequest Handler whenever we receive [IceCandidate]s.
   * @return [StreamPeerConnection] That's fully set up and can be observed and used to send and
   * receive tracks.
   */
  fun makePeerConnection(
    coroutineScope: CoroutineScope,
    configuration: PeerConnection.RTCConfiguration,
    type: StreamPeerType,
    mediaConstraints: MediaConstraints,
    onStreamAdded: ((MediaStream) -> Unit)? = null,
    onIceCandidateRequest: ((IceCandidate, StreamPeerType) -> Unit)? = null,
    onTrack: ((RtpTransceiver?) -> Unit)? = null
  ): StreamPeerConnection {
    val peerConnection = StreamPeerConnection(
      coroutineScope = coroutineScope,
      type = type,
      mediaConstraints = mediaConstraints,
      onStreamAdded = onStreamAdded,
      onIceCandidate = onIceCandidateRequest,
      onTrack = onTrack
    )
    val connection = makePeerConnectionInternal(
      configuration = configuration,
      observer = peerConnection
    )
    return peerConnection.apply { initialize(connection) }
  }

  /**
   * Builds a [PeerConnection] internally that connects to the server and is able to send and
   * receive tracks.
   *
   * @param configuration The [PeerConnection.RTCConfiguration] used to set up the connection.
   * @param observer Handler used to observe different states of the connection.
   * @return [PeerConnection] that's fully set up.
   */
  private fun makePeerConnectionInternal(
    configuration: PeerConnection.RTCConfiguration,
    observer: PeerConnection.Observer?
  ): PeerConnection {
    return requireNotNull(
      factory.createPeerConnection(
        configuration,
        observer
      )
    )
  }

  /**
   * Builds an [AudioSource] from the [factory]
   *
   * @param constraints The constraints used to change the way the audio behaves.
   * @return [AudioSource] that can be used to build tracks.
   */
  fun makeAudioSource(constraints: MediaConstraints = MediaConstraints()): AudioSource =
    factory.createAudioSource(constraints)

  /**
   * Builds an [AudioTrack] from the [factory]
   *
   * @param source The [AudioSource] used for the track.
   * @param trackId The unique ID for this track.
   * @return [AudioTrack] That represents an audio feed.
   */
  fun makeAudioTrack(
    source: AudioSource,
    trackId: String
  ): AudioTrack = factory.createAudioTrack(trackId, source)
}
