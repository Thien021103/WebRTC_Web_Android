package webrtc.sample.compose.ui.components

import android.content.Context
import org.webrtc.AudioTrackSink
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RecordingAudioSink(private val context: Context) : AudioTrackSink {
  private val audioChunks = mutableListOf<ByteArray>()
  private var isRecording = false
  private var sampleRate = 0
  private var channels = 0
  private var bitsPerSample = 0

  /**
   * Starts recording audio data.
   */
  fun startRecording() {
    audioChunks.clear()
    isRecording = true
  }

  /**
   * Stops recording and saves the audio data to a WAV file.
   */
  fun stopRecording() {
    isRecording = false
    saveAudio()
  }

  /**
   * Receives audio data from the audio track.
   * @param audioData The buffer containing audio samples.
   * @param bitsPerSample Bits per audio sample (e.g., 16).
   * @param sampleRate Sample rate in Hz (e.g., 48000).
   * @param channels Number of audio channels (e.g., 1 for mono).
   * @param frames Number of frames in the buffer.
   * @param timestamp Timestamp of the audio data.
   */
  override fun onData(
    audioData: ByteBuffer,
    bitsPerSample: Int,
    sampleRate: Int,
    channels: Int,
    frames: Int,
    timestamp: Long
  ) {
    if (isRecording) {
      val data = ByteArray(audioData.remaining())
      audioData.get(data)
      audioChunks.add(data)
      this.sampleRate = sampleRate
      this.channels = channels
      this.bitsPerSample = bitsPerSample
    }
  }

  /**
   * Saves the recorded audio data to a WAV file.
   */
  /**
   * Saves the recorded audio data to a WAV file in external storage.
   */
  private fun saveAudio() {
    if (audioChunks.isEmpty() || sampleRate == 0 || channels == 0 || bitsPerSample == 0) {
      return // No data to save
    }

    val totalSize = audioChunks.sumOf { it.size }
    // Use app-specific external files directory (e.g., /storage/emulated/0/Android/data/<package>/files)
    val outputDir = context.getExternalFilesDir(null) // null for default "files" directory
    val outputFile = File(outputDir, "recorded_audio_${System.currentTimeMillis()}.wav")

    FileOutputStream(outputFile).use { outputStream ->
      // Write WAV header
      val byteRate = sampleRate * channels * bitsPerSample / 8
      val blockAlign = channels * bitsPerSample / 8

      outputStream.write("RIFF".toByteArray())
      outputStream.write(intToByteArray(36 + totalSize)) // Total file size - 8
      outputStream.write("WAVE".toByteArray())
      outputStream.write("fmt ".toByteArray())
      outputStream.write(intToByteArray(16)) // Subchunk1 size (16 for PCM)
      outputStream.write(shortToByteArray(1)) // Audio format (1 = PCM)
      outputStream.write(shortToByteArray(channels.toShort()))
      outputStream.write(intToByteArray(sampleRate))
      outputStream.write(intToByteArray(byteRate))
      outputStream.write(shortToByteArray(blockAlign.toShort()))
      outputStream.write(shortToByteArray(bitsPerSample.toShort()))
      outputStream.write("data".toByteArray())
      outputStream.write(intToByteArray(totalSize)) // Data size

      // Write audio data
      for (chunk in audioChunks) {
        outputStream.write(chunk)
      }
    }
  }

  /**
   * Converts an integer to a little-endian byte array.
   */
  private fun intToByteArray(value: Int): ByteArray =
    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

  /**
   * Converts a short to a little-endian byte array.
   */
  private fun shortToByteArray(value: Short): ByteArray =
    ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
}