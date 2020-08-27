package com.deepfine.camera

open class SimpleOnEventListener :
    Photographer.OnEventListener {
    override fun onDeviceConfigured() {}
    override fun onPreviewStarted() {}
    override fun onZoomChanged(zoom: Float) {}
    override fun onPreviewStopped() {}
    override fun onStartRecording() {}
    override fun onFinishRecording(filePath: String?) {}
    override fun onShotFinished(filePath: String?) {}
    override fun onError(error: Error?) {}
}