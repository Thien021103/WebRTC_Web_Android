package io.getstream.webrtc.sample.compose.ui.screens.video

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import io.getstream.webrtc.sample.compose.R

sealed class CallAction {
  data class ToggleMicroPhone(
    val isEnabled: Boolean
  ) : CallAction()

//  data class ToggleCamera(
//    val isEnabled: Boolean
//  ) : CallAction()

//  object FlipCamera : CallAction()

  object LeaveCall : CallAction()
}

data class VideoCallControlAction(
  val icon: Painter,
  val iconTint: Color,
  val background: Color,
  val callAction: CallAction
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

//  val cameraIcon = painterResource(
//    id = if (callMediaState.isCameraEnabled) {
//      R.drawable.ic_videocam_on
//    } else {
//      R.drawable.ic_videocam_off
//    }
//  )

  return listOf(
    VideoCallControlAction(
      icon = microphoneIcon,
      iconTint = Color.White,
      background = MaterialTheme.colors.secondary,
      callAction = CallAction.ToggleMicroPhone(callMediaState.isMicrophoneEnabled)
    ),
//    VideoCallControlAction(
//      icon = cameraIcon,
//      iconTint = Color.White,
//      background = Primary,
//      callAction = CallAction.ToggleCamera(callMediaState.isCameraEnabled)
//    ),
//    VideoCallControlAction(
//      icon = painterResource(id = R.drawable.ic_camera_flip),
//      iconTint = Color.White,
//      background = Primary,
//      callAction = CallAction.FlipCamera
//    ),
    VideoCallControlAction(
      icon = painterResource(id = R.drawable.ic_call_end),
      iconTint = Color.White,
      background = MaterialTheme.colors.error,
      callAction = CallAction.LeaveCall
    )
  )
}
