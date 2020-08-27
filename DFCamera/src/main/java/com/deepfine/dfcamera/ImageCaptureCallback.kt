package com.deepfine.camera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult

/**
 * A [CameraCaptureSession.CaptureCallback] for capturing a still picture.
 */
internal abstract class ImageCaptureCallback : CaptureCallback() {
    private var state = 0
    fun setState(state: Int) {
        this.state = state
    }

    override fun onCaptureProgressed(
        session: CameraCaptureSession,
        request: CaptureRequest, partialResult: CaptureResult
    ) {
        process(partialResult)
    }

    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest, result: TotalCaptureResult
    ) {
        process(result)
    }

    private fun process(result: CaptureResult) {
        when (state) {
            STATE_LOCKING -> {
                // 초점이 맞을 때, 정지 영상을 촬영.
                val af = result.get(CaptureResult.CONTROL_AF_STATE)
                af?.let {
                    if (it == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                        it == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
                    ) {
                        val ae = result.get(CaptureResult.CONTROL_AE_STATE)
                        if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            //초점이 맞고 촬영으로 상태 변경
                            setState(STATE_CAPTURING)
                            onReady()
                        } else {
                            setState(STATE_LOCKED)
                            onPrecaptureRequired()
                        }
                    }
                }
            }
            STATE_PRECAPTURE -> {
                val ae = result.get(CaptureResult.CONTROL_AE_STATE)
                if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || ae == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED || ae == CaptureResult.CONTROL_AE_STATE_CONVERGED
                ) {
                    setState(STATE_WAITING)
                }
            }
            STATE_WAITING -> {

                val ae = result.get(CaptureResult.CONTROL_AE_STATE)
                if (ae == null || ae != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    setState(STATE_CAPTURING)
                    onReady()
                }
            }
        }
    }

    /**
     * Called when it is ready to take a still picture.
     */
    abstract fun onReady()

    /**
     * Called when it is necessary to run the precapture sequence.
     */
    abstract fun onPrecaptureRequired()

    companion object {
        const val STATE_PREVIEW = 0
        const val STATE_LOCKING = 1
        const val STATE_LOCKED = 2
        const val STATE_PRECAPTURE = 3
        const val STATE_WAITING = 4
        const val STATE_CAPTURING = 5
    }
}