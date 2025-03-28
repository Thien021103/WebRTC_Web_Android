/*
 * Copyright 2023 Stream.IO, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.getstream.webrtc.sample.compose.ui.components

import android.content.Context
import android.content.res.Resources
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import org.webrtc.EglBase
import org.webrtc.EglRenderer
import org.webrtc.GlRectDrawer
import org.webrtc.RendererCommon.RendererEvents
import org.webrtc.ThreadUtils
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch

open class VideoTextureViewRenderer @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : TextureView(context, attrs), VideoSink, SurfaceTextureListener {

  private val resourceName: String = getResourceName()
  private val eglRenderer: EglRenderer = EglRenderer(resourceName)
  private var rendererEvents: RendererEvents? = null
  private val uiThreadHandler = Handler(Looper.getMainLooper())
  private var isFirstFrameRendered = false
  private var rotatedFrameWidth = 0
  private var rotatedFrameHeight = 0
  private var frameRotation = 0

  // Recording components
  private var mediaCodec: MediaCodec? = null
  private var mediaMuxer: MediaMuxer? = null
  private var trackIndex: Int = -1
  private var isRecording = false
  private var isMuxerStarted = false
  private var outputFile: File? = null

  init {
    surfaceTextureListener = this
  }

  override fun onFrame(videoFrame: VideoFrame) {
    eglRenderer.onFrame(videoFrame)
    updateFrameData(videoFrame)
    if (isRecording) {
      try {
        recordFrame(videoFrame)
      } catch (e: Exception) {
        Log.e("VideoRenderer", "Error recording frame", e)
      }
    }
  }

  private fun updateFrameData(videoFrame: VideoFrame) {
    if (!isFirstFrameRendered) {
      rendererEvents?.onFirstFrameRendered()
      isFirstFrameRendered = true
      val outputFilePath = "${context.getExternalFilesDir(null)}/recorded_video_${System.currentTimeMillis()}.mp4"
      Log.e("VideoRenderer", "${context.filesDir}")

      rotatedFrameWidth = videoFrame.rotatedWidth
      rotatedFrameHeight = videoFrame.rotatedHeight
      startRecording(outputFilePath)
    }

    if (videoFrame.rotatedWidth != rotatedFrameWidth ||
      videoFrame.rotatedHeight != rotatedFrameHeight ||
      videoFrame.rotation != frameRotation
    ) {
      rotatedFrameWidth = videoFrame.rotatedWidth
      rotatedFrameHeight = videoFrame.rotatedHeight
      frameRotation = videoFrame.rotation

      uiThreadHandler.post {
        rendererEvents?.onFrameResolutionChanged(
          rotatedFrameWidth,
          rotatedFrameHeight,
          frameRotation
        )
      }
    }
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    eglRenderer.setLayoutAspectRatio((right - left) / (bottom.toFloat() - top))
  }

  /**
   * Initialise the renderer. Should be called from the main thread.
   *
   * @param sharedContext [EglBase.Context]
   * @param rendererEvents Sets the render event listener.
   */
  fun init(
    sharedContext: EglBase.Context,
    rendererEvents: RendererEvents
  ) {
    ThreadUtils.checkIsOnMainThread()
    this.rendererEvents = rendererEvents
    eglRenderer.init(sharedContext, EglBase.CONFIG_PLAIN, GlRectDrawer())
  }

  private fun startRecording(outputFilePath: String) {
    if (isRecording || rotatedFrameWidth <= 0 || rotatedFrameHeight <= 0) return
    outputFile = File(outputFilePath)

    val format = MediaFormat.createVideoFormat("video/avc", rotatedFrameWidth, rotatedFrameHeight).apply {
      setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)
      setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000)
      setInteger(MediaFormat.KEY_FRAME_RATE, 30)
      setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }

    mediaCodec = MediaCodec.createEncoderByType("video/avc").apply {
      configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      start()
    }
    mediaMuxer = MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    isRecording = true
  }

  fun stopRecording() {
    if (!isRecording) return
    isRecording = false

    mediaCodec?.let { codec ->
      try {
        codec.stop()
      } catch (e: IllegalStateException) {
        Log.e("VideoRenderer", "Error stopping codec", e)
      }
      codec.release()
    }
    mediaMuxer?.let { muxer ->
      if (isMuxerStarted) {
        try {
          muxer.stop()
        } catch (e: IllegalStateException) {
          Log.e("VideoRenderer", "Error stopping muxer", e)
        }
      }
      muxer.release()
    }

    mediaCodec = null
    mediaMuxer = null
    trackIndex = -1
    isMuxerStarted = false
    outputFile = null
  }

  private fun recordFrame(videoFrame: VideoFrame) {
    val buffer = videoFrame.buffer.toI420()
    if (buffer != null) {
      // Allocate for unpadded YUV420 size
      val ySize = buffer.width * buffer.height
      val uvSize = (buffer.width / 2) * (buffer.height / 2)
      val totalSize = ySize + uvSize + uvSize // width * height * 3/2
      val yuvData = ByteBuffer.allocateDirect(totalSize)

      Log.d("FrameRecorder", "Frame: ${buffer.width}x${buffer.height}, Strides: Y=${buffer.strideY}, U=${buffer.strideU}, V=${buffer.strideV}, TotalSize=$totalSize")

      copyPlane(buffer.dataY, buffer.strideY, buffer.width, buffer.height, yuvData)
      copyPlane(buffer.dataU, buffer.strideU, buffer.width / 2, buffer.height / 2, yuvData)
      copyPlane(buffer.dataV, buffer.strideV, buffer.width / 2, buffer.height / 2, yuvData)
      yuvData.flip()

      val inputBufferIndex = mediaCodec?.dequeueInputBuffer(10000) ?: return
      if (inputBufferIndex >= 0) {
        val inputBuffer = mediaCodec?.getInputBuffer(inputBufferIndex)
        inputBuffer?.clear()
        inputBuffer?.put(yuvData)
//        if (inputBuffer != null) {
//          Log.d("VideoRenderer", "Queuing buffer: size=${yuvData.capacity()}, capacity=${inputBuffer.capacity()}")
//        }
        mediaCodec?.queueInputBuffer(
          inputBufferIndex,
          0,
          yuvData.capacity(),
          videoFrame.timestampNs / 1000,
          0
        )
      }

      val bufferInfo = MediaCodec.BufferInfo()
      var outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: return
      while (outputBufferIndex >= 0) {
        val outputBuffer = mediaCodec?.getOutputBuffer(outputBufferIndex) ?: break
        if (trackIndex < 0) {
          trackIndex = mediaMuxer?.addTrack(mediaCodec!!.outputFormat) ?: -1
          mediaMuxer?.start()
          isMuxerStarted = true
        }
        mediaMuxer?.writeSampleData(trackIndex, outputBuffer, bufferInfo)
        mediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
        outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1
      }
      buffer.release()
    }
  }

  private fun copyPlane(source: ByteBuffer, stride: Int, width: Int, height: Int, dest: ByteBuffer) {
    source.position(0)
    val capacity = source.capacity()
    val limit = source.limit()
    val maxPos = minOf(capacity, limit) // Respect the buffer's limit
//    Log.d("CopyPlane", "CopyPlane: stride=$stride, width=$width, height=$height, capacity=$capacity, limit=$limit")
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

  override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
    eglRenderer.createEglSurface(surfaceTexture)
  }

  override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
    stopRecording();
    val completionLatch = CountDownLatch(1)
    eglRenderer.releaseEglSurface { completionLatch.countDown() }
    ThreadUtils.awaitUninterruptibly(completionLatch)
    return true
  }

  override fun onSurfaceTextureSizeChanged(
    surfaceTexture: SurfaceTexture,
    width: Int,
    height: Int
  ) {
  }

  override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}

  override fun onDetachedFromWindow() {
    stopRecording()
    eglRenderer.release()
    super.onDetachedFromWindow()
  }

  private fun getResourceName(): String {
    return try {
      resources.getResourceEntryName(id) + ": "
    } catch (e: Resources.NotFoundException) {
      ""
    }
  }
}