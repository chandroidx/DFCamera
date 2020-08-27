/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.deepfine.camera

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import java.util.*

/**
 * A [TextureView] that can be adjusted to a specified aspect ratio.
 */
class AutoFitTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextureView(context, attrs, defStyle) {
    private var ratioWidth = 0
    private var ratioHeight = 0
    var _fillSpace: Boolean = false
    var fillSpace: Boolean
        get() = _fillSpace
        set(value) {
            _fillSpace = value
            requestLayout()
        }

    var _displayOrientation: Int = 180
    var displayOrientation: Int
        get() = _displayOrientation
    set(value) {
        _displayOrientation = value
        configureTransform()
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    fun setAspectRatio(width: Int, height: Int) {
        require(!(width < 0 || height < 0)) { "Size cannot be negative." }
        ratioWidth = width
        ratioHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (0 == ratioWidth || 0 == ratioHeight) {
            setMeasuredDimension(width, height)
        } else {
            // is filling space by default
            val isFillSpaceWithoutScale = width == height * ratioWidth / ratioHeight
            if (isFillSpaceWithoutScale) {
                setMeasuredDimension(width, height)
                return
            }
            if (fillSpace!!) {
                if (width < height * ratioWidth / ratioHeight) {
                    setMeasuredDimension(height * ratioWidth / ratioHeight, height)
                } else {
                    setMeasuredDimension(width, width * ratioHeight / ratioWidth)
                }
            } else {
                if (width < height * ratioWidth / ratioHeight) {
                    setMeasuredDimension(width, width * ratioHeight / ratioWidth)
                } else {
                    setMeasuredDimension(height * ratioWidth / ratioHeight, height)
                }
            }
        }
    }

    val surface: Surface
        get() = Surface(surfaceTexture)

    fun setBufferSize(width: Int, height: Int) {
        surfaceTexture?.setDefaultBufferSize(width, height)
    }

    private fun configureTransform() {
        val matrix = Matrix()
        if (displayOrientation % 180 == 90) {
            val width = width
            val height = height
            // Rotate the camera preview when the screen is landscape.
            matrix.setPolyToPoly(
                floatArrayOf(
                    0f, 0f,  // top left
                    width.toFloat(), 0f,  // top right
                    0f, height.toFloat(),  // bottom left
                    width.toFloat(), height.toFloat()
                ), 0,
                if (displayOrientation == 90) floatArrayOf(
                    0f, height.toFloat(),  // top left
                    0f, 0f,  // top right
                    width.toFloat(), height.toFloat(),  // bottom left
                    width.toFloat(), 0f
                ) else floatArrayOf(
                    width.toFloat(), 0f,  // top left
                    width.toFloat(), height.toFloat(),  // top right
                    0f, 0f,  // bottom left
                    0f, height.toFloat()
                ), 0,
                4
            )
        } else if (displayOrientation == 180) {
            matrix.postRotate(180f, width / 2.toFloat(), height / 2.toFloat())
        }
        setTransform(matrix)
    }

    interface Callback {
        fun onSurfaceChanged()
    }

    private val callbacks: MutableList<Callback> =
        LinkedList()

    fun addCallback(callback: Callback?) {
        if (callback != null) {
            callbacks.add(callback)
        }
    }

    private fun dispatchSurfaceChanged() {
        for (callback in callbacks) {
            callback.onSurfaceChanged()
        }
    }

    init {
        surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                configureTransform()
                dispatchSurfaceChanged()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                configureTransform()
                dispatchSurfaceChanged()
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }
}