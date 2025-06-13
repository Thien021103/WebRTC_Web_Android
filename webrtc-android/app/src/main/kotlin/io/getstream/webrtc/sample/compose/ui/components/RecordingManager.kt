package io.getstream.webrtc.sample.compose.ui.components

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import io.getstream.webrtc.sample.compose.WebRTCApp
import org.webrtc.AudioTrackSink
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import java.io.File
import java.nio.ByteBuffer

class RecordingManager (
  private var cloudFolder: String,
  private var context: Context,
  private var mediaMuxer: MediaMuxer,
  private val outputFile: File,
) : AudioTrackSink, VideoSink {

  private val muxerLock = Any()
  private var isMuxerStarted = false

  // Video recording components
  private var isVideoRecording = false
  private var videoMediaCodec: MediaCodec? = null
  private var videoTrackIndex: Int = -1
  private var rotatedFrameWidth = 0
  private var rotatedFrameHeight = 0
  private var offsetTimestampUs = 0L

  // Audio recording components
  private var isAudioRecording = false
  private var audioMediaCodec: MediaCodec? = null
  private var audioTrackIndex: Int = -1
  private val audioBuffer = ByteArray(2048) // Buffer for 1024 samples (16-bit mono)
  private var bufferOffset = 0 // Current position in the buffer
  private var currentTimestampUs = 0L // Store the timestamp of the latest packet

  /**
   * Receives audio data from the audio track.
   * @param audioData The buffer containing audio samples.
   * @param bitsPerSample Bits per audio sample (e.g., 16).
   * @param sampleRate Sample rate in Hz (e.g., 48000).
   * @param channels Number of audio channels (e.g., 1 for mono).
   * @param frames Number of frames in the buffer.
   * @param timestamp Timestamp of the audio data. (seem corrupted)
   */
  override fun onData(
    audioData: ByteBuffer,
    bitsPerSample: Int,
    sampleRate: Int,
    channels: Int,
    frames: Int,
    timestamp: Long
  ) {
    synchronized(muxerLock) {
      if (!isAudioRecording) {
        // Setup audio MediaCodec on first audio data if not already initialized
        if (audioMediaCodec == null) {
          val audioFormat =
            MediaFormat.createAudioFormat("audio/mp4a-latm", sampleRate, channels).apply {
              setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
              setInteger(MediaFormat.KEY_BIT_RATE, 32_000) // 128 kbps
            }
          audioMediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm").apply {
            configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()
          }
          Log.d("AudioCodec", "Audio codec started, bitsPerSample:$bitsPerSample, timestamp: $timestamp, $bitsPerSample, $sampleRate, $frames")
        }
        currentTimestampUs = 0L
        isAudioRecording = true
      } else {
        val paddedBuffer = ByteArray(2048)
        // Copy audio data into buffer
        val dataSize = audioData.remaining()
//        if (bufferOffset + dataSize <= audioBuffer.size) {
        audioData.get(paddedBuffer, bufferOffset, dataSize)
        bufferOffset += dataSize
        for (i in dataSize until 2048) {
          paddedBuffer[i] = 0
        }
//        }

        // Queue when buffer has 2048 bytes (1024 samples)
//        Log.d("AudioRecordingManager", "bufferOffset: $bufferOffset")
//        if (bufferOffset >= 1900) {
          try {
            val normalizedTimestampUs = currentTimestampUs // Already in µs
//            Log.d("AudioRecordingManager", "timestamp: $normalizedTimestampUs")
            recordAudioData(ByteBuffer.wrap(paddedBuffer, 0, 2048), normalizedTimestampUs)
            currentTimestampUs += (bufferOffset * 1_000_000L / 48000)
            bufferOffset = 0 // Reset buffer
          } catch (e: Exception) {
            Log.e("AudioRecordingManager", "Error recording audio data", e)
          }
//        } else {
//          val normalizedTimestampUs = currentTimestampUs + (bufferOffset * 3 * 1_000_000L / 48000) // Already in µs
//          val paddedBuffer = ByteArray(2048)
////          System.arraycopy(audioBuffer, 0, paddedBuffer, 0, 160)
//          recordAudioData(ByteBuffer.wrap(paddedBuffer, 0, bufferOffset), normalizedTimestampUs)
//        }
      }
    }
  }

  override fun onFrame(
    videoFrame: VideoFrame
  ) {
    synchronized(muxerLock) {
      rotatedFrameWidth = videoFrame.rotatedWidth
      rotatedFrameHeight = videoFrame.rotatedHeight

      if (!isVideoRecording) {
        // Setup audio MediaCodec on first audio data if not already initialized
        if (videoMediaCodec == null) {
          val videoFormat =
            MediaFormat.createVideoFormat("video/avc", rotatedFrameWidth, rotatedFrameHeight)
              .apply {
                setInteger(
                  MediaFormat.KEY_COLOR_FORMAT,
                  MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
                )
                setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
              }

          videoMediaCodec = MediaCodec.createEncoderByType("video/avc").apply {
            configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()
          }
          isVideoRecording = true
          offsetTimestampUs = videoFrame.timestampNs/1000 - currentTimestampUs
        }
        Log.d("VideoCodec", "Video codec started")
      } else {
        try {
          val normalizedTimestampUs = videoFrame.timestampNs/1000 - offsetTimestampUs
//          Log.d("VideoRecordingManager", "timestamp: $normalizedTimestampUs")
          recordVideoFrame(videoFrame, normalizedTimestampUs)
        } catch (e: Exception) {
          Log.e("VideoRecordingManager", "Error recording video frame", e)
        }
      }
    }
  }

  private fun recordVideoFrame(
    videoFrame: VideoFrame,
    timestamp: Long
  ) {
    // Process video frame
    val buffer = videoFrame.buffer.toI420()
    if (buffer != null) {
      // Allocate for unpadded YUV420 size
      val ySize = buffer.width * buffer.height
      val uvSize = (buffer.width / 2) * (buffer.height / 2)
      val totalSize = ySize + uvSize + uvSize // width * height * 3/2
      val yuvData = ByteBuffer.allocateDirect(totalSize)

//      Log.d("VideoRecordingManager", "Video frame: ${buffer.width}x${buffer.height}, TotalSize=$totalSize")

      copyPlane(buffer.dataY, buffer.strideY, buffer.width, buffer.height, yuvData)
      copyPlane(buffer.dataU, buffer.strideU, buffer.width / 2, buffer.height / 2, yuvData)
      copyPlane(buffer.dataV, buffer.strideV, buffer.width / 2, buffer.height / 2, yuvData)
      yuvData.flip()

      val inputBufferIndex = videoMediaCodec?.dequeueInputBuffer(10000) ?: return
      if (inputBufferIndex >= 0) {
        val inputBuffer = videoMediaCodec?.getInputBuffer(inputBufferIndex)
        inputBuffer?.clear()
        inputBuffer?.put(yuvData)
        videoMediaCodec?.queueInputBuffer(
          inputBufferIndex,
          0,
          yuvData.capacity(),
          timestamp,
          0
        )
      }

      val bufferInfo = MediaCodec.BufferInfo()
      var outputBufferIndex = videoMediaCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: return
      while (outputBufferIndex >= 0) {
        val outputBuffer = videoMediaCodec?.getOutputBuffer(outputBufferIndex) ?: break
        if (videoTrackIndex < 0) {
          videoTrackIndex = mediaMuxer.addTrack(videoMediaCodec!!.outputFormat) ?: -1
          isVideoRecording = true
          if (!isMuxerStarted /*&& audioTrackIndex >= 0 */&& videoTrackIndex >= 0 ) {
            mediaMuxer.start()
            Log.d("RecordingManager", "Muxer started for video")
            isMuxerStarted = true
          }
        }
        if(isMuxerStarted) {
          Log.d("VideoRecordingManager", "Writing sample video, input_ts=${timestamp}, output_ts=${bufferInfo.presentationTimeUs}, size=${bufferInfo.size}")
          mediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
        }
        videoMediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
        outputBufferIndex = videoMediaCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1
      }
      buffer.release()
    }
  }

  private fun copyPlane(source: ByteBuffer, stride: Int, width: Int, height: Int, dest: ByteBuffer) {
    source.position(0)
    val capacity = source.capacity()
    val limit = source.limit()
    val maxPos = minOf(capacity, limit) // Respect the buffer's limit
    for (i in 0 until height) {
      val rowStart = i * stride
      val rowEnd = rowStart + width
      if (rowEnd <= maxPos) {
        source.limit(rowEnd)
        source.position(rowStart)
        dest.put(source)
      } else {
        Log.w("CopyPlane", "Row $i exceeds limit: $rowEnd > $maxPos")
        break
      }
    }
  }

  private fun recordAudioData(
    audioData: ByteBuffer,
    timestamp: Long)
  {
    // Process audio data
    val inputBufferIndex = audioMediaCodec?.dequeueInputBuffer(10000) ?: return
    if (inputBufferIndex >= 0) {
      val inputBuffer = audioMediaCodec?.getInputBuffer(inputBufferIndex)
      inputBuffer?.clear()
      inputBuffer?.put(audioData)
//      Log.d("AudioRecordingManager", "Queued audio input, index=$inputBufferIndex, timestamp=${timestamp / 1000} us")
//      Log.d("AudioRecordingManager", "Audio data, timestamp=$timestamp")
      audioMediaCodec?.queueInputBuffer(
        inputBufferIndex,
        0,
        audioData.capacity(),
        timestamp,
        0
      )
    }
    val bufferInfo = MediaCodec.BufferInfo()
    var outputBufferIndex = audioMediaCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: return
    while (outputBufferIndex >= 0) {
      val outputBuffer = audioMediaCodec?.getOutputBuffer(outputBufferIndex) ?: break
      if (audioTrackIndex < 0) {
        audioTrackIndex = mediaMuxer.addTrack(audioMediaCodec!!.outputFormat) ?: -1
        isAudioRecording = true
        if (!isMuxerStarted && audioTrackIndex >= 0 && videoTrackIndex >= 0) {
          Log.d("RecordingManager", "Muxer started for audio")
          mediaMuxer.start()
          isMuxerStarted = true
        }
      }
      if (isMuxerStarted) {
        Log.d("AudioRecordingManager", "Writing sample audio, input_ts=${timestamp}, output_ts=${bufferInfo.presentationTimeUs}, size=${bufferInfo.size}")
        mediaMuxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
      }
      audioMediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
      outputBufferIndex = audioMediaCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1
    }
  }

  fun stopRecording(onUploadUpdate: (String?, Float, Boolean) -> Unit = { _, _, _ -> }) {
    Log.d("Recording Manager", "Stopping recording")
    if (!isVideoRecording && !isAudioRecording) return
    isVideoRecording = false
    isAudioRecording = false

    videoMediaCodec?.let { codec ->
      try {
        codec.stop()
      } catch (e: IllegalStateException) {
        Log.e("RecordingManager", "Error stopping video codec", e)
      }
      codec.release()
    }
    audioMediaCodec?.let { codec ->
      try {
        codec.stop()
      } catch (e: IllegalStateException) {
        Log.e("RecordingManager", "Error stopping audio codec", e)
      }
      codec.release()
    }

    mediaMuxer.let { muxer ->
      if (isMuxerStarted) {
        try {
          muxer.stop()
          uploadToCloudinary(file = outputFile, onUploadUpdate = onUploadUpdate)
        } catch (e: IllegalStateException) {
          Log.e("RecordingManager", "Error stopping muxer", e)
        }
      }
      else {
        onUploadUpdate(null, 0f, false)
      }
      muxer.release()
    }

    videoMediaCodec = null
    videoTrackIndex = -1
    audioTrackIndex = -1
    isMuxerStarted = false
  }

  private fun uploadToCloudinary(file: File, onUploadUpdate: (String?, Float, Boolean) -> Unit) {
    val config = (context.applicationContext as WebRTCApp).getCloudinaryConfig()
    if (config["cloud_name"].isNullOrEmpty()) {
      onUploadUpdate(null, 0f, true)
      Log.e("RecordingManager", "Cannot upload: MediaManager not initialized")
      return
    }

    if (file.length() == 0L) {
      onUploadUpdate(null, 0f, true)
      Log.e("RecordingManager", "Cannot upload: File ${file.name} is empty (0 bytes)")
      return
    }

    val publicId = "${file.nameWithoutExtension}}"
    Log.d("Recording Manager", "Uploading file $cloudFolder/${file.nameWithoutExtension}" )
    MediaManager.get().upload(file.absolutePath)
      .option("resource_type", "video")
      .option("folder", cloudFolder) // Explicitly create folder
      .option("public_id", publicId)
      .callback(object : UploadCallback {
        override fun onStart(requestId: String) {
          onUploadUpdate(null, 0f, false)
        }

        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
          val progress = (bytes * 100 / totalBytes).toFloat() / 100
          onUploadUpdate(null, progress, false)
        }

        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
          val publicUrl = resultData["url"].toString()
          onUploadUpdate(publicUrl, 1f, true)
        }

        override fun onError(requestId: String, error: ErrorInfo) {
          onUploadUpdate(null, 0f, true)
        }

        override fun onReschedule(requestId: String, error: ErrorInfo) {
          // Retry logic if needed
        }
      })
      .dispatch()
  }
}