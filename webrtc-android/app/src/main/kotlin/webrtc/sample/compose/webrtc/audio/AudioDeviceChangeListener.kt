package webrtc.sample.compose.webrtc.audio

typealias AudioDeviceChangeListener = (
  audioDevices: List<AudioDevice>,
  selectedAudioDevice: AudioDevice?
) -> Unit
