/*
 * Copyright 2023 Stream.IO, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.getstream.webrtc.sample.compose.ui.components

import android.content.Context
import android.content.res.Resources
import android.graphics.SurfaceTexture
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

  init {
    surfaceTextureListener = this
  }

  override fun onFrame(videoFrame: VideoFrame) {
    eglRenderer.onFrame(videoFrame)
    updateFrameData(videoFrame)
  }

  private fun updateFrameData(videoFrame: VideoFrame) {
    if (!isFirstFrameRendered) {
      rendererEvents?.onFirstFrameRendered()
      isFirstFrameRendered = true
      rotatedFrameWidth = videoFrame.rotatedWidth
      rotatedFrameHeight = videoFrame.rotatedHeight
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

  override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
    eglRenderer.createEglSurface(surfaceTexture)
  }

  override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
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