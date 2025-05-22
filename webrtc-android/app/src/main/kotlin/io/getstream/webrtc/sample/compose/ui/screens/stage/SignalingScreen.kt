package io.getstream.webrtc.sample.compose.ui.screens.stage

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.webrtc.sample.compose.R
import io.getstream.webrtc.sample.compose.ui.screens.video.VideoCallScreen
import io.getstream.webrtc.sample.compose.webrtc.SignalingClient
import io.getstream.webrtc.sample.compose.webrtc.peer.StreamPeerConnectionFactory
import io.getstream.webrtc.sample.compose.webrtc.sessions.LocalWebRtcSessionManager
import io.getstream.webrtc.sample.compose.webrtc.sessions.WebRtcSessionManager
import io.getstream.webrtc.sample.compose.webrtc.sessions.WebRtcSessionManagerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SignallingScreen(
  role: String,
  email: String,
  accessToken: String,
  cloudFolder: String,
  onBack: () -> Unit
) {
  var startedSignalling by remember { mutableStateOf(false) }
  var sessionManager by remember { mutableStateOf<WebRtcSessionManager?>(null) }
  var onCallScreen by remember { mutableStateOf(false) }

  val context = LocalContext.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(id = R.string.signalling),
            fontSize = 20.sp
          )
        },
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary
      )
    },
    content = { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        if (!startedSignalling) {
          Text(
            text = "Ready to start signalling",
            fontSize = 24.sp,
            color = MaterialTheme.colors.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
          )
          Button(
            onClick = {
              sessionManager = WebRtcSessionManagerImpl(
                context = context,
                signalingClient = SignalingClient(
                  accessToken = accessToken,
                  onWsClose = {
//                    onCallScreen = false
//                    sessionManager = null
                  }
                ),
                peerConnectionFactory = StreamPeerConnectionFactory(context)
              )
              startedSignalling = true
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevation(defaultElevation = 4.dp),
            colors = ButtonDefaults.buttonColors(
              backgroundColor = MaterialTheme.colors.secondary,
              contentColor = MaterialTheme.colors.onSecondary
            )
          ) {
            Icon(
              imageVector = Icons.Filled.PlayArrow,
              contentDescription = "Start Signalling",
              modifier = Modifier.padding(end = 8.dp)
            )
            Text(
              text = "Start Signalling",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.height(24.dp))
          Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
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
            Text(
              text = "Back",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
        if (startedSignalling && sessionManager != null) {
          CompositionLocalProvider(LocalWebRtcSessionManager provides sessionManager!!) {
            val state by sessionManager!!.signalingClient.sessionStateFlow.collectAsState()
            if (!onCallScreen) {
              StageScreen(
                state = state,
                onJoinCall = { onCallScreen = true },
                onBack = {
                  startedSignalling = false
                  sessionManager!!.disconnect()
                  sessionManager = null
                }
              )
            } else {
              VideoCallScreen (
                role = role,
                email = email,
                accessToken = accessToken,
                cloudFolder = cloudFolder,
                onCancelCall = {
                  Log.d("Upload Dialog", "Cancel call, startedSignalling = false")
                  onCallScreen = false
                  startedSignalling = false
                  sessionManager = null
                }
              )
            }
          }
        }
      }
    }
  )
}