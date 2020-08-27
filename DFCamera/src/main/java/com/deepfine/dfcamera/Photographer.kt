package com.deepfine.camera

import android.media.MediaRecorder

interface Photographer {
    val supportedImageSizes: Set<Size>
    val supportedVideoSizes: Set<Size>
    fun startPreview()
    fun restartPreview()
    fun stopPreview()
    val previewSize: Size?
    var imageSize: Size?
    var videoSize: Size?
    val supportedAspectRatios: Set<AspectRatio?>?
    var aspectRatio: AspectRatio?
    var autoFocus: Boolean
    var facing: Int
    var flash: Int
    var zoom: Float
    var mode: Int
    var grid: Int
    var exposure: Float
    var isSmartGlasses: Boolean
    fun takePicture()
    fun startRecording(configurator: MediaRecorderConfigurator?)

    /**
     * Only works when API level >= 24 (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N).
     */
    fun pauseRecording()

    /**
     * Only works when API level >= 24 (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N).
     */
    fun resumeRecording()
    fun finishRecording()
    interface MediaRecorderConfigurator {
        /**
         * Our Photographer's MediaRecorder use the default configs below:
         * <pre>
         * mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
         * mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
         * mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
         * if (nextVideoAbsolutePath == null || nextVideoAbsolutePath.isEmpty()) {
         * nextVideoAbsolutePath = getVideoFilePath(activityContext); // random file
         * }
         * mediaRecorder.setOutputFile(nextVideoAbsolutePath);
         * mediaRecorder.setVideoEncodingBitRate(10000000);
         * mediaRecorder.setVideoFrameRate(30);
         * mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
         * mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
         * mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        </pre> *
         *
         * If you need to configure the MediaRecorder by yourself, please be carefully otherwise
         * [IllegalStateException] may be thrown. See javadoc for [MediaRecorder]
         *
         * @return A boolean value which indicates if we use the default configs for MediaRecorder.
         */
        fun useDefaultConfigs(): Boolean

        /**
         * Your configurations here may override the default configs when possible.
         *
         * If you need to configure the MediaRecorder by yourself, please be carefully otherwise
         * [IllegalStateException] may be thrown. See javadoc for [MediaRecorder]
         *
         * @param recorder The recorder to be configured.
         */
        fun configure(recorder: MediaRecorder?)
    }

    fun setOnEventListener(listener: OnEventListener?)
    interface OnEventListener {
        fun onDeviceConfigured()
        fun onPreviewStarted()
        fun onZoomChanged(zoom: Float)
        fun onPreviewStopped()
        fun onStartRecording()
        fun onFinishRecording(filePath: String?)
        fun onShotFinished(filePath: String?)
        fun onError(error: Error?)
    }
}