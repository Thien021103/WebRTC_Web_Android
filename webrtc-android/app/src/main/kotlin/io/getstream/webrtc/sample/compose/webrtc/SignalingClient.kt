package io.getstream.webrtc.sample.compose.webrtc

import io.getstream.log.taggedLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class SignalingClient(
  private val accessToken: String,
  private val onWsClose: () -> Unit
) {
  private val logger by taggedLogger("Call:SignalingClient")
  private val signalingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val client = OkHttpClient()
  private val request = Request
    .Builder()
    .url("wss://thientranduc.id.vn:444")
    .build()

  // opening web socket with signaling server
  private val ws = client.newWebSocket(request, SignalingWebSocketListener())

  // session flow to send information about the session state to the subscribers
  private val _sessionStateFlow = MutableStateFlow(WebRTCSessionState.Offline)
  val sessionStateFlow: StateFlow<WebRTCSessionState> = _sessionStateFlow

  // signaling commands to send commands to value pairs to the subscribers
  private val _signalingCommandFlow = MutableSharedFlow<Pair<SignalingCommand, String>>()
  val signalingCommandFlow: SharedFlow<Pair<SignalingCommand, String>> = _signalingCommandFlow

  // Access token from login used for CONNECT, id used for other command
  fun sendCommand(signalingCommand: SignalingCommand, message: String) {
    if(signalingCommand === SignalingCommand.CONNECT) {
      logger.d { "[sendCommand] $signalingCommand $message" }
      ws.send("$signalingCommand user $accessToken")
    } else if (signalingCommand === SignalingCommand.PONG) {
      logger.d { "[sendCommand] $signalingCommand" }
      ws.send("$signalingCommand")
    } else {
      logger.d { "[sendCommand] $signalingCommand $message" }
      ws.send("$signalingCommand user $accessToken\n$message")
    }
  }

  private inner class SignalingWebSocketListener : WebSocketListener() {
    override fun onMessage(webSocket: WebSocket, text: String) {
      when {
        text.startsWith(SignalingCommand.STATE.toString(), true) ->
          handleStateMessage(text)
        text.startsWith(SignalingCommand.OFFER.toString(), true) ->
          handleSignalingCommand(SignalingCommand.OFFER, text)
        text.startsWith(SignalingCommand.ANSWER.toString(), true) ->
          handleSignalingCommand(SignalingCommand.ANSWER, text)
        text.startsWith(SignalingCommand.ICE.toString(), true) ->
          handleSignalingCommand(SignalingCommand.ICE, text)
        text.startsWith(SignalingCommand.PING.toString(), true) ->
          sendCommand(SignalingCommand.PONG, "")
      }
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
      super.onOpen(webSocket, response)
      sendCommand(SignalingCommand.CONNECT, "")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
      super.onClosed(webSocket, code, reason)
      logger.d { "[websocket] closed $code $reason" }
      _sessionStateFlow.value = WebRTCSessionState.Offline
      onWsClose.invoke()
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
      super.onClosing(webSocket, code, reason)
      logger.d { "[websocket] closing $code $reason" }
      onWsClose.invoke()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
      super.onFailure(webSocket, t, response)
      logger.d { "[websocket] failure" }
      onWsClose.invoke()
    }
  }

  private fun handleStateMessage(message: String) {
    val state = getSeparatedMessage(message)
    logger.d { "[stateMessage] $message" }
    _sessionStateFlow.value = WebRTCSessionState.valueOf(state)
  }

  private fun handleSignalingCommand(command: SignalingCommand, text: String) {
    val value = getNextLineMessage(text)
    logger.d { "received signaling: $command $value" }
    signalingScope.launch {
      _signalingCommandFlow.emit(command to value)
    }
  }

  private fun getSeparatedMessage(text: String) = text.substringAfter(' ')
  private fun getNextLineMessage(text: String) = text.substringAfter('\n')


  fun dispose() {
    _sessionStateFlow.value = WebRTCSessionState.Offline
    ws.close(1000,"")
    signalingScope.cancel()
    ws.cancel()
  }
}

enum class WebRTCSessionState {
  Active, // Offer and Answer messages has been sent
  Creating, // Creating session, offer has been sent
  Ready, // Both clients available and ready to initiate session
  Impossible, // We have less than two clients connected to the server
  Offline // unable to connect signaling server
}

enum class SignalingCommand {
  STATE, // Command for WebRTCSessionState
  OFFER, // to send or receive offer
  ANSWER, // to send or receive answer
  ICE, // to send and receive ice candidates
  CONNECT, // to connect to server
  PING,
  PONG
}
