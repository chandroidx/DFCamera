package com.deepfine.camera

import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.Log

internal class CallbackHandler(context: Context?): Handler(context!!.mainLooper) {
    private var onEventListener: Photographer.OnEventListener? = null
    fun setOnEventListener(listener: Photographer.OnEventListener?) {
        onEventListener = listener
    }

    override fun handleMessage(msg: Message) {
        if (onEventListener == null) {
            return
        }
        if (Values.DEBUG) {
            Log.d("Message","handleMessage: " + msg.what)
        }
        when (msg.what) {
            CALLBACK_ON_DEVICE_CONFIGURED -> onEventListener!!.onDeviceConfigured()
            CALLBACK_ON_PREVIEW_STARTED -> onEventListener!!.onPreviewStarted()
            CALLBACK_ON_ZOOM_CHANGED -> onEventListener!!.onZoomChanged(
                msg.obj as Float
            )
            CALLBACK_ON_PREVIEW_STOPPED -> onEventListener!!.onPreviewStopped()
            CALLBACK_ON_START_RECORDING -> onEventListener!!.onStartRecording()
            CALLBACK_ON_FINISH_RECORDING -> onEventListener!!.onFinishRecording(
                msg.obj as String
            )
            CALLBACK_ON_SHOT_FINISHED -> onEventListener!!.onShotFinished(
                msg.obj as String
            )
            CALLBACK_ON_ERROR -> onEventListener!!.onError(
                msg.obj as Error
            )
            else -> {
            }
        }
    }

    /**
     * 디바이스 구성 완료 callbaCk
     */
    fun onDeviceConfigured() {
        Message.obtain(
            this,
            CALLBACK_ON_DEVICE_CONFIGURED
        ).sendToTarget()
    }

    /**
     * 프리뷰 시작 callbaCk
     */
    fun onPreviewStarted() {
        Message.obtain(
            this,
            CALLBACK_ON_PREVIEW_STARTED
        ).sendToTarget()
    }

    /**
     * 줌 변경 callback
     */
    fun onZoomChanged(zoom: Float) {
        Message.obtain(
            this,
            CALLBACK_ON_ZOOM_CHANGED,
            zoom
        ).sendToTarget()
    }

    /**
     * 프리뷰 정지 callbaCk
     */
    fun onPreviewStopped() {
        Message.obtain(
            this,
            CALLBACK_ON_PREVIEW_STOPPED
        ).sendToTarget()
    }

    /**
     * 비디오 촬영 시작 callbaCk
     */
    fun onStartRecording() {
        Message.obtain(
            this,
            CALLBACK_ON_START_RECORDING
        ).sendToTarget()
    }

    /**
     * 비디오 촬영 완료 callbaCk
     * @param time
     */
    fun onFinishRecording(filePath: String?) {
        Message.obtain(
            this,
            CALLBACK_ON_FINISH_RECORDING,
            filePath
        ).sendToTarget()
    }

    /**
     * 촬영 완 callbaCk
     * @param filePath
     */
    fun onShotFinished(filePath: String?) {
        Message.obtain(
            this,
            CALLBACK_ON_SHOT_FINISHED,
            filePath
        ).sendToTarget()
    }

    /**
     * 에러 전달 callbaCk
     * @param error
     */
    fun onError(error: Error?) {
        Message.obtain(
            this,
            CALLBACK_ON_ERROR,
            error
        ).sendToTarget()
    }

    companion object {
        private const val CALLBACK_ON_DEVICE_CONFIGURED = 1
        private const val CALLBACK_ON_PREVIEW_STARTED = 2
        private const val CALLBACK_ON_ZOOM_CHANGED = 3
        private const val CALLBACK_ON_PREVIEW_STOPPED = 4
        private const val CALLBACK_ON_START_RECORDING = 5
        private const val CALLBACK_ON_FINISH_RECORDING = 6
        private const val CALLBACK_ON_SHOT_FINISHED = 7
        private const val CALLBACK_ON_ERROR = 9

    }
}