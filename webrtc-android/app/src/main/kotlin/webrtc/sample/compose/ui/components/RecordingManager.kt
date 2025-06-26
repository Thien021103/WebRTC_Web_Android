package webrtc.sample.compose.ui.components

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
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
  private var isUsingFlexibleFormat = false

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
              setInteger(MediaFormat.KEY_BIT_RATE, 128_000) // 128 kbps
            }
          audioMediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm").apply {
            configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()
          }
          Log.d("AudioCodec", "Audio codec started, bitsPerSample:$bitsPerSample, timestamp: $timestamp, channel: $channels, sample rate: $sampleRate, frames: $frames")
        }
        currentTimestampUs = 0L
        isAudioRecording = true
      } else {
        // Copy only the actual data, no padding
        val dataSize = audioData.remaining()
        val audioBytes = ByteArray(dataSize)
        audioData.get(audioBytes)
        System.arraycopy(audioBytes, 0, audioBuffer, bufferOffset, dataSize)
        bufferOffset += dataSize
        // Process full 960-byte packets
        while (bufferOffset >= 960) {
          try {
            Log.d("AudioRecording", "Processing data size: 960, timestamp: $currentTimestampUs")
            recordAudioData(ByteBuffer.wrap(audioBuffer, 0, 960), currentTimestampUs)
            currentTimestampUs += 10_000L // 10 ms for 480 samples

            // Shift remaining data
            val remaining = bufferOffset - 960
            if (remaining > 0) {
              System.arraycopy(audioBuffer, 960, audioBuffer, 0, remaining)
            }
            bufferOffset = remaining
          } catch (e: Exception) {
            Log.e("AudioRecordingManager", "Error recording audio data", e)
          }
        }
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

          try {
            videoMediaCodec = MediaCodec.createEncoderByType("video/avc").apply {
              configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
              start()
            }
            Log.d("CodecInfo", "Codec: ${videoMediaCodec?.name}")
            Log.d("InputFormat", videoMediaCodec?.inputFormat.toString())
            isUsingFlexibleFormat = false
          } catch (e: Exception) {
            Log.d("VideoCodec", "Video codec error ${e.message}")
            val videoFormat2 =
              MediaFormat.createVideoFormat("video/avc", rotatedFrameWidth, rotatedFrameHeight)
                .apply {
                  setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                  )
                  setInteger(MediaFormat.KEY_BIT_RATE, 1_000_000)
                  setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                  setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }
            videoMediaCodec = MediaCodec.createEncoderByType("video/avc").apply {
              configure(videoFormat2, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
              start()
            }
            isUsingFlexibleFormat = true
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

      val yuvData =
        if (isUsingFlexibleFormat) {
        convertI420ToNV12(buffer)
      } else {
        // I420 planar
        val data = ByteBuffer.allocateDirect(totalSize)
        copyPlane(buffer.dataY, buffer.strideY, buffer.width, buffer.height, data)
        copyPlane(buffer.dataU, buffer.strideU, buffer.width / 2, buffer.height / 2, data)
        copyPlane(buffer.dataV, buffer.strideV, buffer.width / 2, buffer.height / 2, data)
        data.flip()
        data
      }

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

  private fun convertI420ToNV12(i420: VideoFrame.I420Buffer): ByteBuffer {
    val width = i420.width
    val height = i420.height
    val strideY = i420.strideY
    val strideU = i420.strideU
    val strideV = i420.strideV

    val ySize = width * height
    val uvSize = ySize / 2
    val output = ByteBuffer.allocateDirect(ySize + uvSize)

    // 1. Copy Y plane
    copyPlane(i420.dataY, strideY, width, height, output)

    // 2. Interleave U + V into UV plane (NV12)
    val halfHeight = height / 2
    val halfWidth = width / 2

    for (y in 0 until halfHeight) {
      val srcUPos = y * strideU
      val srcVPos = y * strideV
      for (x in 0 until halfWidth) {
        val u = i420.dataU.get(srcUPos + x)
        val v = i420.dataV.get(srcVPos + x)
        output.put(u) // U
        output.put(v) // V
      }
    }
    output.flip()
    Log.d("Convert", output.capacity().toString())

    return output
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
    val publicId = "${file.nameWithoutExtension}}"
    Log.d("Recording Manager", "Uploading file $cloudFolder/${file.nameWithoutExtension}" )
    MediaManager.get().upload(file.absolutePath)
      .option("resource_type", "video")
      .option("folder", cloudFolder) // Explicitly create folder
      .option("public_id", publicId)
      .unsigned("thien_upload")
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