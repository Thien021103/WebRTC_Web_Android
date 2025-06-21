package webrtc.sample.compose.ui.screens.video

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import webrtc.sample.compose.R

sealed class CallAction {
  data class ToggleMicroPhone(
    val isEnabled: Boolean
  ) : CallAction()

  object LeaveCall : CallAction()
}

data class VideoCallControlAction(
  val icon: Painter,
  val iconTint: Color,
  val background: Color,
  val callAction: CallAction
)

data class CallMediaState(
  val isMicrophoneEnabled: Boolean = true,
  val isCameraEnabled: Boolean = true
)

@Composable
fun buildDefaultCallControlActions(
  callMediaState: CallMediaState
): List<VideoCallControlAction> {
  val microphoneIcon =
    painterResource(
      id = if (callMediaState.isMicrophoneEnabled) {
        R.drawable.ic_mic_on
      } else {
        R.drawable.ic_mic_off
      }
    )

  return listOf(
    VideoCallControlAction(
      icon = microphoneIcon,
      iconTint = Color.White,
      background = MaterialTheme.colors.secondary,
      callAction = CallAction.ToggleMicroPhone(callMediaState.isMicrophoneEnabled)
    ),
    VideoCallControlAction(
      icon = painterResource(id = R.drawable.ic_call_end),
      iconTint = Color.White,
      background = MaterialTheme.colors.error,
      callAction = CallAction.LeaveCall
    )
  )
}
