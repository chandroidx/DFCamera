package com.deepfine.camera

import android.graphics.Rect
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.params.MeteringRectangle

internal class FocusHandler {
    private var isFocusProcessing = false
    fun focus(
        captureSession: CameraCaptureSession?, captureRequestBuilder: CaptureRequest.Builder?,
        focusArea: Rect?, callback: Callback
    ) {
        if (isFocusProcessing) {
            return
        }
        try {
            captureSession?.stopRepeating()
        } catch (e: CameraAccessException) {
            callback.onFinish(
                Error(
                    Error.ERROR_CAMERA,
                    e
                )
            )
            return
        }
        captureRequestBuilder?.let {
            // cancel any existing AF trigger (repeated touches, etc.)
            it.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
            )
            it.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF
            )

            // add a new AF trigger with focus region
            if (focusArea != null) {
                val rectangle =
                    MeteringRectangle(focusArea, MeteringRectangle.METERING_WEIGHT_MAX - 1)
                it.set(
                    CaptureRequest.CONTROL_AF_REGIONS,
                    arrayOf(rectangle)
                )
            }
            it.set(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO
            )
            it.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_AUTO
            )
            it.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START
            )
            it.setTag("FOCUS_TAG") // we'll capture this later for resuming the preview
            try {
                val focusCallbackHandler: CaptureCallback = object : CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        isFocusProcessing = false
                        if (request.tag === "FOCUS_TAG") {
                            // the focus trigger is complete, clear AF trigger
                            it.set(CaptureRequest.CONTROL_AF_TRIGGER, null)
                            callback.onFinish(null)
                        }
                    }

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {
                        isFocusProcessing = false
                        callback.onFinish(
                            Error(
                                Error.ERROR_CAMERA,
                                "focus failed"
                            )
                        )
                    }
                }
                captureSession?.capture(it.build(), focusCallbackHandler, null)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
                callback.onFinish(
                    Error(
                        Error.ERROR_CAMERA,
                        e
                    )
                )
                return
            }
            isFocusProcessing = true
        }
    }

    internal interface Callback {
        fun onFinish(error: Error?)
    }
}