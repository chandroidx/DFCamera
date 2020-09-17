package com.deepfine.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.util.SparseIntArray
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import androidx.core.content.ContextCompat
import com.deepfine.camera.Photographer.MediaRecorderConfigurator
import java.io.IOException
import java.util.*

class Camera2Photographer : InternalPhotographer {
    companion object {
        // MediaReocder부터 2610p 이상의 사이즈는 사용할 수 없음.
        private const val MAX_VIDEO_SIZE = 3840 * 2160

        // 렌즈 앞 / 뒤
        private val INTERNAL_FACINGS = SparseIntArray()

        // 권한 관련
        private val RECORD_VIDEO_PERMISSIONS = ArrayList<String>(3)

        // 비디오 사이즈
        private fun chooseVideoSize(choices: SortedSet<Size>): Size {
            var chosen: Size? = null
            for (size in choices) {
                if (size.width == size.height * 4 / 3 && size.height <= MAX_VIDEO_SIZE) {
                    chosen = size
                }
            }
            return chosen ?: choices.last()
        }

        init {
            INTERNAL_FACINGS.put(
                Values.FACING_BACK,
                CameraCharacteristics.LENS_FACING_BACK
            )
            INTERNAL_FACINGS.put(
                Values.FACING_FRONT,
                CameraCharacteristics.LENS_FACING_FRONT
            )
        }

        init {
            RECORD_VIDEO_PERMISSIONS.add(Manifest.permission.CAMERA)
            RECORD_VIDEO_PERMISSIONS.add(Manifest.permission.RECORD_AUDIO)
            RECORD_VIDEO_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    //////////////////// Get / Set  ////////////////////

    var _mode: Int = Values.MODE_IMAGE
    override var mode: Int
        get() = _mode
    set(mode) {
        if (_mode == mode) return

        preview?.focusGrid = Grid.OFF
        if (_mode != Values.MODE_VIDEO && mode != Values.MODE_VIDEO) {
            preview?.gridModeLine = mode == Values.MODE_GRID
            _mode = mode
            callbackHandler?.onDeviceConfigured()
            return
        }

        _mode = mode
        preview?.focusFinished()
        restartPreview()
    }

    var _aspectRatio: AspectRatio? =
        Values.DEFAULT_ASPECT_RATIO
    override var aspectRatio: AspectRatio?
        get() = _aspectRatio
    set(ratio) {
        if (!isPreviewStarted) {
            return
        }
        if (ratio == null || !previewSizeMap.ratios().contains(ratio)) {
            callbackHandler?.onError(
                Error(
                    Error.ERROR_INVALID_PARAM,
                    ratio.toString() + " not supported."
                )
            )
            return
        }
        if (ratio == _aspectRatio) {
            return
        }
        resetSizes()
        _aspectRatio = ratio
        restartPreview()
    }

    var _autoFocus: Boolean = true
    override var autoFocus: Boolean
        get() = _autoFocus
    set(autoFocus) {
        if (_autoFocus == autoFocus) {
            return
        }
        _autoFocus = autoFocus
        preview?.focusFinished()

        if (previewRequestBuilder != null) {
            updateAutoFocus()
            updatePreview(Runnable { _autoFocus = !_autoFocus })
        }
    }

    var _facing: Int = Values.FACING_BACK
    override var facing: Int
        get() = _facing
        set(facing) {
            _facing = facing
            restartPreview()
        }

    var _flash: Int = Values.FLASH_OFF
    override var flash: Int
        get() = _flash
        set(flash) {
            if (_flash == flash) {
                return
            }
            val saved = this.flash
            _flash = flash
            if (previewRequestBuilder != null) {
                updateFlash()
                updatePreview(Runnable { this.flash = saved })
            }
        }

    var _exposure: Float = 50.0f
    override var exposure: Float
    get() = _exposure
    set(value) {
        _exposure = value
        updateExposure(value)
    }

    // 스마트 글래스 사용 시 true / 모바일 앱에서 사용 시 false
    var _isSmartGlasses: Boolean = true
    override  var isSmartGlasses: Boolean
    get() = _isSmartGlasses
    set(value) {
       _isSmartGlasses = value
    }

    var _imageSize: Size? = null
    override var imageSize: Size?
        get() = _imageSize
        set(size) {
            if (size == null || !supportedImageSizes.contains(size)) {
                callbackHandler?.onError(
                    Error(
                        Error.ERROR_INVALID_PARAM,
                        size.toString() + " not supported."
                    )
                )
                return
            }
            if (_imageSize == size) {
                return
            }
            resetSizes()
            _imageSize = size
            restartPreview()
        }

    var _videoSize: Size? = null
    override var videoSize: Size?
        get() = _videoSize
    set(size) {
        if (size == null || !supportedVideoSizes.contains(size)) {
            callbackHandler?.onError(
                Error(
                    Error.ERROR_INVALID_PARAM,
                    size.toString() + " not supported."
                )
            )
            return
        }
        if (_videoSize == size) {
            return
        }
        resetSizes()
        _videoSize = size
        restartPreview()
    }

    var _zoom: Float = 1.0f
    override var zoom: Float
        get() = _zoom
    set(zoom) {
        if (_zoom == zoom) {
            return
        }

        val newZoom = clampZoom(zoom)
        if (Utils.checkFloatEqual(_zoom, newZoom)) return
        _zoom = newZoom
        updateZoom(newZoom)
        updatePreview(null)
    }

    private val displayOrientation: Int
        private get() {
            val rotation = activityContext!!.windowManager.defaultDisplay.rotation
            return Utils.getOrientation(
                sensorOrientation,
                rotation
            )
        }

    override val supportedAspectRatios: Set<AspectRatio?>?
        get() = previewSizeMap.ratios()

    //////////////////// val ////////////////////
    private final val TAG = "Camera2PhotoGrapher"

    private val videoSizeMap = SizeMap()
    override val supportedVideoSizes: SortedSet<Size> =
        TreeSet()

    private val focusHandler = FocusHandler()
    private val previewSizeMap = SizeMap()
    private val supportedPreviewSizes: SortedSet<Size> =
        TreeSet()
    private val imageSizeMap = SizeMap()
    override val supportedImageSizes: SortedSet<Size> =
        TreeSet()

    //////////////////// var ////////////////////

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    override var previewSize: Size? = null
        private set

    private var activityContext: Activity? = null
    private var preview: CameraView? = null
    private var textureView: AutoFitTextureView? = null
    private var isInitialized = false
    private var isPreviewStarted = false
    private var cameraManager: CameraManager? = null
    private var camera: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var cameraId: String? = null
    private var characteristics: CameraCharacteristics? = null
    private var sensorOrientation = if (isSmartGlasses) 0 else 90
    // last determined degree, it is either Surface.Rotation_0, _90, _180, _270, or -1 (undetermined)
    private var currentDeviceRotation = -1
    private var maxZoom = 2f
    private var imageReader: ImageReader? = null
    private var mediaRecorder: MediaRecorder? = null
    private var nextImageAbsolutePath: String? = null
    private var nextVideoAbsolutePath: String? = null
    private var isRecordingVideo = false


    //////////////////// Callback Handler ////////////////////
    private var callbackHandler: CallbackHandler? = null
    private var orientationEventListener: OrientationEventListener? = null

    // 카메라 State Callback
    private val cameraDeviceCallback: CameraDevice.StateCallback =
        object : CameraDevice.StateCallback() {
            // 카메라 디바이스 준비
            override fun onOpened(camera: CameraDevice) {
                this@Camera2Photographer.camera = camera
                startCaptureSession()
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                this@Camera2Photographer.camera = null
                callbackHandler?.onPreviewStopped()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                stopPreview()
                callbackHandler?.onError(
                    Error(
                        Error.ERROR_CAMERA
                    )
                )
            }
        }

    // 캡쳐 세션 State Callback
    private val sessionCallback: CameraCaptureSession.StateCallback =
        object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                updateAutoFocus()
                updateFlash()
//                updateGrid()
                updateExposure(exposure)
                applyZoom()
                updatePreview(null)
                callbackHandler?.onPreviewStarted()
                callbackHandler?.onZoomChanged(zoom)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                stopPreview()
                callbackHandler?.onError(
                    Error(
                        Error.ERROR_CAMERA
                    )
                )
            }

            override fun onClosed(session: CameraCaptureSession) {
                if (captureSession != null && captureSession == session) {
                    captureSession = null
                }
            }
        }

    // 이미지 캡쳐 Callback
    private val imageCaptureCallback: ImageCaptureCallback = object : ImageCaptureCallback() {
        public override fun onPrecaptureRequired() {
            previewRequestBuilder?.set(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            )
            setState(STATE_PRECAPTURE)
            try {
                previewRequestBuilder?.let {
                    captureSession?.capture(it.build(), this, null)
                    it.set(
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE
                    )
                }

            } catch (e: CameraAccessException) {
                callbackHandler?.onError(
                    Error(
                        Error.ERROR_CAMERA,
                        e
                    )
                )
            }
        }

        public override fun onReady() {
            captureStillPicture()
        }
    }

    // 이미지 캡쳐 완료 후의 리스너
    private val onImageAvailableListener = OnImageAvailableListener { reader ->
            backgroundHandler?.post(
                ImageSaver(
                    reader.acquireLatestImage(),
                    nextImageAbsolutePath,
                    // 그리드 모드인 경우, 그리드 라인 숨기기
                    captureShowHideLineGridMode(false),
                    previewSize!!.width,
                    previewSize!!.height,
                    Utils.getOrientation(
                        sensorOrientation,
                        currentDeviceRotation
                    )
                ) {
                    callbackHandler?.onShotFinished(nextImageAbsolutePath)
                    captureShowHideLineGridMode(true)
                }
            )
            preview?.gridModeViewOrNull?.alpha = 0.0f
        }

    //////////////////// Function ////////////////////

    /**
     * 해당 모듈을 사용하는 액티비티, 뷰에서 호출하는 함수로, 초기화 작업 진행
     * @param activity
     * @param preview
     */
    override fun initWithViewfinder(activity: Activity, preview: CameraView) {
        activityContext = activity
        this.preview = preview
        textureView = preview.textureView
        cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // preview 관련 callback 리스너
        callbackHandler = CallbackHandler(activityContext)
        preview.addCallback(object : CameraView.Callback {
            override fun onSingleTap(x: Int, y: Int) {
                focusAt(x, y)
            }

            override fun onScale(scaleFactor: Float) {
                zoom *= scaleFactor
            }

            // 텍스쳐뷰 준비 완료 시,
            override fun onSurfaceChanged() {
                startCaptureSession()
            }

            override fun onSelected(selected: MutableList<Int>) {
                callbackHandler?.onSelectedGridCount(selected.count())
            }
        })

        // 회전 전환 리스너
        if (!isSmartGlasses) {
            orientationEventListener = object : OrientationEventListener(activityContext) {
                // a slop before change the device orientation
                private val changeSlop = 10
                override fun onOrientationChanged(orientation: Int) {
                    if (shouldChange(orientation)) {
                        var rotation = Surface.ROTATION_0
                        if (orientation >= 0 && orientation < 45 || orientation >= 315 && orientation < 360) {
                            rotation = Surface.ROTATION_0
                        } else if (orientation >= 45 && orientation < 135) {
                            rotation = Surface.ROTATION_270
                        } else if (orientation >= 135 && orientation < 225) {
                            rotation = Surface.ROTATION_180
                        } else if (orientation >= 225 && orientation < 315) {
                            rotation = Surface.ROTATION_90
                        }
                        currentDeviceRotation = rotation
                    }
                }

                private fun shouldChange(orientation: Int): Boolean {
                    if (currentDeviceRotation == -1) return true
                    if (currentDeviceRotation == 0) return orientation >= 45 + changeSlop && orientation < 315 - changeSlop
                    val upLimit = currentDeviceRotation + 45 + changeSlop
                    val downLimit = currentDeviceRotation - 45 - changeSlop
                    return !(orientation >= downLimit && orientation < upLimit)
                }
            }
        }

        isInitialized = true
    }

    /**
     * 해당 클래스 초기화 확인
     */
    private fun throwIfNotInitialized() {
        if (!isInitialized) {
            throw RuntimeException("Camera2Photographer is not initialized")
        }
    }

    /**
     * MediaRecorder 초기화 확인
     */
    private fun throwIfNoMediaRecorder() {
        if (mediaRecorder == null) {
            throw RuntimeException("MediaRecorder is not initialized")
        }
    }

    /**
     * Activity LifeCycle 중 onResume에서 실행되는 함수.
     */
    override fun startPreview() {
        throwIfNotInitialized()     // 초기화 전에, 해당 함수가 실행되는 것 방지.

        // 프리뷰 시작 전, 권한 체크 한번 더 하기
        for (permission in RECORD_VIDEO_PERMISSIONS) {
            val permissionCheck =
                ContextCompat.checkSelfPermission(activityContext!!, permission)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                callbackHandler?.onError(
                    Error(
                        Error.ERROR_PERMISSION,
                        "Unsatisfied permission: $permission"
                    )
                )
                return
            }
        }

        startBackgroundThread()
        // 카메라 ID - > Front / Back / 광각 / 망원 등 핸드폰에 있는 사용할 수 있는 카메라 ID
        if (!chooseCameraIdByFacing()) {
            callbackHandler?.onError(
                Error(
                    Error.ERROR_CAMERA
                )
            )
            return
        }

        if (!collectCameraInfo()) {
            return
        }
        prepareWorkers()

        callbackHandler?.onDeviceConfigured()
        startOpeningCamera()
//        if (orientationEventListener != null) {
//            orientationEventListener?.enable()
//        }
        isPreviewStarted = true
    }

    /**
     * 선택된 CameraID
     * @return Boolean
     */
    private fun chooseCameraIdByFacing(): Boolean {
        return try {
            val internalFacing = INTERNAL_FACINGS[facing]
            val ids = cameraManager?.cameraIdList

            // 디바이스 카메라 존재 X
            if (ids?.isEmpty() ?: true) {
                callbackHandler?.onError(
                    Error(
                        Error.ERROR_CAMERA,
                        "No camera available."
                    )
                )
                return false
            }


            for (id in ids!!) {
                val characteristics = cameraManager?.getCameraCharacteristics(id)
                val level = characteristics?.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    continue
                }
                // LENS_FACING -> 렌즈 방향을 얻는 함수.
                // LENS_FACING_FRONT -> 전면(0) / LENS_FACING_BACK -> 후면(1) / LENS_FACING_EXTERNAL -> 기타(2)
                val internal = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (internal == null) {
                    callbackHandler?.onError(
                        Error(
                            Error.ERROR_CAMERA,
                            "Unexpected state: LENS_FACING null."
                        )
                    )
                    return false
                }
                if (internal == internalFacing) {
                    updateCameraInfo(id, characteristics)
                    return true
                }
            }

            // INTERNAL_FACINGS 에 맞는 카메라가 없는 경우 0번째 렌즈로 카메라 정보 업데이트
            updateCameraInfo(ids[0], cameraManager!!.getCameraCharacteristics(ids[0]))

            val internal = characteristics?.get(CameraCharacteristics.LENS_FACING)
            if (internal == null) {
                callbackHandler?.onError(
                    Error(
                        Error.ERROR_CAMERA,
                        "Unexpected state: LENS_FACING null."
                    )
                )
                return false
            }
            var i = 0
            val count = INTERNAL_FACINGS.size()
            while (i < count) {
                if (INTERNAL_FACINGS.valueAt(i) == internal) {
                    facing = INTERNAL_FACINGS.keyAt(i)
                    return true
                }
                i++
            }
            // 카메라 렌즈가 오직 외부밖에 존재하지 않는 경우 해당 부분을 탐.
            // 이 경우에 FACING_BACK 으로 설정
            facing = Values.FACING_BACK
            true
        } catch (e: CameraAccessException) {
            callbackHandler?.onError(
                Error(
                    Error.ERROR_CAMERA,
                    e
                )
            )
            false
        }
    }

    /**
     * 선택된 렌즈로 카메라 정보 변경
     */
    private fun updateCameraInfo(
        cameraId: String,
        characteristics: CameraCharacteristics
    ) {
        this.cameraId = cameraId
        this.characteristics = characteristics
        val orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
        if (orientation != null) {
            sensorOrientation = orientation
        }
        val maxZoomObject =
            characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
        if (maxZoomObject != null) {
            maxZoom = maxZoomObject
        }
    }


    /**
     * 사이즈 모두 리셋
     */
    private fun resetSizes() {
        // clear the image/video size & aspect ratio
        aspectRatio = null
        imageSize = null
        videoSize = null
    }

    /**
     * 아용가능한 사이즈 정보 가져오기
     * @return Boolean
     */
    private fun collectCameraInfo(): Boolean {
        val map = characteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        if (map == null) {
            callbackHandler?.onError(
                Error(
                    Error.ERROR_CAMERA,
                    "Cannot get available preview/video sizes"
                )
            )
            return false
        }
        collectPreviewSizes(map)
        collectImageSizes(map)
        collectVideoSizes(map)
        refineSizes()
        return true
    }

    /**
     * 아용가능한 사이즈 정보 가져오기 등 사전 작업
     */
    private fun prepareWorkers() {
        val size: Size?
        when(mode) {
            Values.MODE_IMAGE, Values.MODE_GRID, Values.MODE_SCAN -> {
                if (imageSize == null) {
                    // determine image size
                    val sizesWithAspectRatio: SortedSet<Size>? =
                        imageSizeMap.sizes(aspectRatio)

                    imageSize = if (sizesWithAspectRatio != null && sizesWithAspectRatio.size > 0) {
                        sizesWithAspectRatio.last()
                    } else {
                        imageSizeMap.defaultSize()
                    }
                }
                size = imageSize
                // 구한 이미지 사이즈로 imageReader 생성 및 리스너 설정. 리스너는 이미지 촬영 완료 시 실행됨.
                imageSize?.let {
                    imageReader = ImageReader.newInstance(
                        it.width, it.height,
                        ImageFormat.JPEG, 2
                    )
                    imageReader?.setOnImageAvailableListener(onImageAvailableListener, null)
                }
            }
            Values.MODE_VIDEO -> {
                if (videoSize == null) {
                    // determine video size
                    val sizesWithAspectRatio =
                        videoSizeMap.sizes(aspectRatio)
                    videoSize = if (sizesWithAspectRatio != null && sizesWithAspectRatio.size > 0) {
                        sizesWithAspectRatio.last()
                    } else {
                        chooseVideoSize(
                            supportedVideoSizes
                        )
                    }
                }
                // 구해진 사이즈 저장 및 MediaRecorder 생성
                size = videoSize
                mediaRecorder = MediaRecorder()
            }
            else -> throw RuntimeException("Wrong mode value: $mode")
        }

        // 구한 사이즈로 프리뷰 최적 사이즈 설정
        previewSize = chooseOptimalPreviewSize(size!!)

        // Orientation
        val orientation = activityContext!!.resources.configuration.orientation

        previewSize?.let {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView?.setAspectRatio(it.width, it.height)
            } else {
                textureView?.setAspectRatio(it.height, it.width)
            }
        }

        preview?.gridModeLine = mode == Values.MODE_GRID
    }


    /**
     * 카메라 사용 시작
     */
    @SuppressLint("MissingPermission")
    private fun startOpeningCamera() {
        try {
            cameraId?.run {
                cameraManager?.openCamera(this, cameraDeviceCallback, null)
            }
        } catch (e: CameraAccessException) {
            callbackHandler?.onError(
                Error(
                    Error.ERROR_CAMERA,
                    "Failed to open camera: $cameraId",
                    e
                )
            )
        }
    }

    /**
     * 프리뷰 다시 시작
     */
    override fun restartPreview() {
        if (isPreviewStarted) {
            stopPreview()
            startPreview()
        }
    }

    /**
     * 프리뷰 정지
     */
    override fun stopPreview() {
        isPreviewStarted = false
        if (orientationEventListener != null) {
            orientationEventListener?.disable()
        }
        throwIfNotInitialized()
        closeCamera()
        stopBackgroundThread()
    }

    /**
     * 아용가능한 프리뷰 사이즈 정보 설정
     * @param map
     */
    private fun collectPreviewSizes(map: StreamConfigurationMap) {
        supportedPreviewSizes.clear()
        for (size in map.getOutputSizes(SurfaceTexture::class.java)) {
            val s =
                Size(size.width, size.height)
            supportedPreviewSizes.add(s)
            previewSizeMap.add(s)
        }
    }

    /**
     *아용가능한 이미지 사이즈 정보 설정
     * @param map
     */
    private fun collectImageSizes(map: StreamConfigurationMap) {
        supportedImageSizes.clear()
        for (size in map.getOutputSizes(ImageFormat.JPEG)) {
            val s =
                Size(size.width, size.height)
            supportedImageSizes.add(s)
            imageSizeMap.add(s)
        }
    }

    /**
     * 아용가능한 비디오 사이즈 정보 설정
     * @param map
     */
    private fun collectVideoSizes(map: StreamConfigurationMap) {
        supportedVideoSizes.clear()
        for (size in map.getOutputSizes(MediaRecorder::class.java)) {
            val s =
                Size(size.width, size.height)
            if (s.areaSize > MAX_VIDEO_SIZE) continue
            supportedVideoSizes.add(s)
            videoSizeMap.add(s)
        }
    }

    /**
     * 가져온 사이즈 정보들 중 사용 가능한 항목으 정리
     */
    private fun refineSizes() {
        for (ratio in previewSizeMap.ratios()) {
            if (mode == Values.MODE_VIDEO && !videoSizeMap.ratios()
                    .contains(ratio)
                || mode == Values.MODE_IMAGE && !imageSizeMap.ratios()
                    .contains(ratio)
                || mode == Values.MODE_GRID && !imageSizeMap.ratios()
                    .contains(ratio)
                || mode == Values.MODE_SCAN && !imageSizeMap.ratios().contains(ratio)
            ) {
                if (previewSizeMap.sizes(ratio) != null) {
                    supportedPreviewSizes.removeAll(previewSizeMap.sizes(ratio)!!)
                }
                previewSizeMap.remove(ratio)
            }
        }

        // fix the aspectRatio if set
        if (aspectRatio != null && !previewSizeMap.ratios().contains(aspectRatio!!)) {
            aspectRatio = previewSizeMap.ratios().iterator().next()
        }
    }

    /**
     * 프리뷰 최적 사이즈 구하기
     */
    private fun chooseOptimalPreviewSize(preferred: Size): Size? {
        val surfaceLonger: Int
        val surfaceShorter: Int
        val surfaceWidth = preferred.width
        val surfaceHeight = preferred.height
        if (surfaceWidth < surfaceHeight) {
            surfaceLonger = surfaceHeight
            surfaceShorter = surfaceWidth
        } else {
            surfaceLonger = surfaceWidth
            surfaceShorter = surfaceHeight
        }
        val preferredAspectRatio: AspectRatio =
            AspectRatio.of(
                surfaceLonger,
                surfaceShorter
            )
        // Pick the smallest of those big enough
        for (size in supportedPreviewSizes) {
            if (preferredAspectRatio.matches(size)
                && size.width >= surfaceLonger && size.height >= surfaceShorter
            ) {
                return size
            }
        }

        // If no size is big enough, pick the largest one which matches the ratio.
        val matchedSizes =
            previewSizeMap.sizes(preferredAspectRatio)
        return if (matchedSizes != null && matchedSizes.size > 0) {
            matchedSizes.last()
        } else supportedPreviewSizes.last()

        // If no size is big enough or ratio cannot be matched, pick the largest one.
    }

    /**
     * 카메라 종료
     */
    private fun closeCamera() {
        closePreviewSession()
        if (camera != null) {
            camera?.close()
            camera = null
        }
        if (mediaRecorder != null) {
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }

    /**
     * 카메라 세션 종료
     */
    private fun closePreviewSession() {
        if (captureSession != null) {
            captureSession?.close()
            captureSession = null
        }
    }

    /**
     * 프리뷰 및 CaptureSession 생성
     */
    private fun startCaptureSession() {
        if (camera == null || textureView?.surfaceTexture == null || mode == Values.MODE_IMAGE && imageReader == null || mode == Values.MODE_GRID && imageReader == null || mode == Values.MODE_SCAN && imageReader == null) {
            return
        }
        try {
            // 카메라 프리뷰 설정
            previewSize?.run {
                textureView?.setBufferSize(this.width, this.height)
                previewRequestBuilder = camera?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                val previewSurface = textureView?.surface
                previewRequestBuilder?.addTarget(previewSurface!!)

                // 프리뷰 업데이트
                val surfaces: MutableList<Surface?> =
                    ArrayList()
                surfaces.add(previewSurface)
                if (mode == Values.MODE_IMAGE || mode == Values.MODE_GRID || mode == Values.MODE_SCAN) {
                    surfaces.add(imageReader?.surface)
                }
                camera?.createCaptureSession(surfaces, sessionCallback, null)
            }

        } catch (e: CameraAccessException) {
            callbackHandler?.onError(
                Error(
                    Error.ERROR_CAMERA,
                    e
                )
            )
        }
    }

    /**
     * 오토포커스 업데이트
     */
    private fun updateAutoFocus() {
        // 오토포커스가 아니면 Mode_OFF 후 return
        if (!(autoFocus ?: false)) {
            previewRequestBuilder?.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF
            )
            return
        }

        val modes =
            characteristics?.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)

        // 조리개 조절
        previewRequestBuilder?.set(
            CaptureRequest.LENS_FOCUS_DISTANCE,
            5f  /*0.0f means infinity focus  10f는 가까운 초점  0f에 가까울 수록 먼 곳에 초점을 잡는다.*/
        );

        // 오토포커스 지원 여부
        if (modes == null ||
            modes.size == 0 ||
            modes.size == 1 && modes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF
        ) {
            autoFocus = false
            previewRequestBuilder?.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF
            )
        } else {
            // 이미지 / 비디오 오토 포커스
            if (mode == Values.MODE_IMAGE || mode == Values.MODE_GRID || mode == Values.MODE_SCAN) {
                previewRequestBuilder?.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            } else {
                previewRequestBuilder?.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                )
            }
        }
    }

    /**
     * 플래시 업데이트
     */
    private fun updateFlash() {
        when (flash) {
            // 플래시 꺼져있음
            Values.FLASH_OFF -> {
                previewRequestBuilder?.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
                previewRequestBuilder?.set(
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_OFF
                )
            }

            // 촬영할 때만 플래시 켜기
            Values.FLASH_ON -> {
                previewRequestBuilder?.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                )
                previewRequestBuilder?.set(
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_OFF
                )
            }

            // 플래시 항상 켜기
            Values.FLASH_TORCH -> {
                previewRequestBuilder?.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
                previewRequestBuilder?.set(
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_TORCH
                )
            }

            // 촬영 시, 플래시 자동으로 조정
            Values.FLASH_AUTO -> {
                previewRequestBuilder?.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                )
                previewRequestBuilder?.set(
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_OFF
                )
            }

            // Auto와 비슷하지만, 적목 현상 감소 기능이 있는 모드.
            Values.FLASH_RED_EYE -> {
                previewRequestBuilder?.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE
                )
                previewRequestBuilder?.set(
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_OFF
                )
            }
        }
    }


    /**
     * 노출도 업데이트
     */
    private fun updateExposure(exposureAdjustment: Float) {
        characteristics?.let {
            val range1: Range<Int> = it.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)!!
            val minExposure: Int = range1.lower
            val maxExposure: Int = range1.upper

//            previewRequestBuilder?.apply {
//                try {
////                    val captureRequest: CaptureRequest = mCaptureRequest.build()
//                    this.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
////                    this.set(CaptureRequest.CONTROL_AE_LOCK, true)
//
//                    captureSession?.let { session ->
//                        session.setRepeatingRequest(this.build(), null, null)
//                        this.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, iso.toInt())
//                        session.capture(this.build(), null, null)
//                    }
//
//                } catch (e: CameraAccessException) {
//                }
//            }

            if (minExposure != 0 || maxExposure != 0) {
                var newCalculatedValue = 0f
                newCalculatedValue = if (exposureAdjustment >= 0) {
                    (minExposure * exposureAdjustment)
                } else {
                    (maxExposure * -1 * exposureAdjustment)
                }

                previewRequestBuilder?.apply {
                    try {
//                    val captureRequest: CaptureRequest = mCaptureRequest.build()
//                        this.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
//                        this.set(CaptureRequest.CONTROL_AE_LOCK, true)

                        captureSession?.let { session ->
//                            session.setRepeatingRequest(this.build(), imageCaptureCallback, callbackHandler)
                            this.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, newCalculatedValue.toInt())
                            updatePreview(null)
//                            session.capture(this.build(), imageCaptureCallback, callbackHandler)
                        }

                    } catch (e: CameraAccessException) {
                    }
                }
            }


        }
    }

    /**
     * 사진 촬영
     */
    override fun takePicture() {
        if (mode == Values.MODE_VIDEO) {
            callbackHandler?.onError(
                Error(
                    Error.ERROR_INVALID_PARAM,
                    "Cannot takePicture() in non-IMAGE mode"
                )
            )
            return
        }
        nextImageAbsolutePath = try {
            Utils.imageFilePath
        } catch (e: IOException) {
            callbackHandler?.onError(
                Utils.errorFromThrowable(
                    e
                )
            )
            return
        }

        // Capture Flow 1
        // 오토포커스를 사용할 경우, preview에 설정된 포커스를 잡아두기 위해서 lockFocus 과정 필요.
        if (autoFocus) {
            lockFocus()
        } else {
            captureStillPicture()
        }
        preview?.shot()
    }

    /**
     * 비디오 촬영
     */
    override fun startRecording(configurator: MediaRecorderConfigurator?) {
        throwIfNoMediaRecorder()
        if (camera == null || !(textureView?.isAvailable ?: false) || previewSize == null) {
            callbackHandler?.onError(
                Error(
                    Error.ERROR_CAMERA
                )
            )
            return
        }
        nextVideoAbsolutePath = try {
            Utils.videoFilePath
        } catch (e: IOException) {
            callbackHandler?.onError(
                Utils.errorFromThrowable(
                    e
                )
            )
            return
        }
        try {
            closePreviewSession()
            setUpMediaRecorder(configurator)
            previewRequestBuilder = camera?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            val surfaces: MutableList<Surface?> =
                ArrayList()
            val previewSurface = textureView?.surface?.run {
                surfaces.add(this)
                previewRequestBuilder?.addTarget(this)
            }


            // Set up Surface for the MediaRecorder
            val recorderSurface = mediaRecorder?.surface?.run{
                surfaces.add(this)
                previewRequestBuilder?.addTarget(this)
            }


            // Start a capture session
            // Once the session starts, we can update the UI and start recording

            camera?.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    captureSession = cameraCaptureSession
                    applyZoom()
                    updatePreview(null)
                    isRecordingVideo = true
                    mediaRecorder?.start()
                    callbackHandler?.onStartRecording()
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    callbackHandler?.onError(
                        Error(
                            Error.ERROR_CAMERA
                        )
                    )
                }
            }, null)
        } catch (e: CameraAccessException) {
            callbackHandler?.onError(
                Error(
                    Error.ERROR_CAMERA,
                    e
                )
            )
        } catch (e: IOException) {
            callbackHandler?.onError(
                Utils.errorFromThrowable(
                    e
                )
            )
        }
    }

    /**
     * MediaRecorder 설정 및 준비
     */
    @Throws(IOException::class)
    private fun setUpMediaRecorder(configurator: MediaRecorderConfigurator?) {
        if (configurator == null || configurator.useDefaultConfigs()) {
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncodingBitRate(10000000)
                setVideoFrameRate(30)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            }
        }
        videoSize?.run {
            mediaRecorder?.setVideoSize(this.width, this.height)
        }

        mediaRecorder?.setOutputFile(nextVideoAbsolutePath)
        configurator?.configure(mediaRecorder)
        mediaRecorder?.setOrientationHint(
            Utils.getOrientation(
                sensorOrientation,
                currentDeviceRotation
            )
        )
        mediaRecorder?.prepare()
    }

    /**
     * 비디오 일시정지 Nougat 이상 사용 가능
     */
    override fun pauseRecording() {
        throwIfNoMediaRecorder()
        if (!isRecordingVideo) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.pause()
        } else {
            callbackHandler?.onError(
                Error(
                    Error.ERROR_UNSUPPORTED_OPERATION
                )
            )
        }
    }

    /**
     * 비디오 일시정지
     */
    override fun resumeRecording() {
        throwIfNoMediaRecorder()
        if (!isRecordingVideo) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.resume()
        } else {
            callbackHandler?.onError(
                Error(
                    Error.ERROR_UNSUPPORTED_OPERATION
                )
            )
        }
    }

    /**
     * 비디오 촬영 완료
     */
    override fun finishRecording() {
        if (!isRecordingVideo) return
        throwIfNoMediaRecorder()
        isRecordingVideo = false
        mediaRecorder?.stop()
        mediaRecorder?.reset()
        callbackHandler?.onFinishRecording(nextVideoAbsolutePath)
        startCaptureSession()
    }

    /**
     * callback 리스너
     */
    override fun setOnEventListener(listener: Photographer.OnEventListener?) {
        throwIfNotInitialized()
        callbackHandler?.setOnEventListener(listener)
    }

    /**
     * 촬영 시, 초점을 잡기 위해 포커스 잠금
     */
    private fun lockFocus() {
        // 초점을 잡기위한 Request 설정. - 오토포커스에서만 사용 가능
        previewRequestBuilder?.set(
            CaptureRequest.CONTROL_AF_TRIGGER,
            CaptureRequest.CONTROL_AF_TRIGGER_START
        )
        // Capture Flow 2 - 1
        try {
            imageCaptureCallback.setState(ImageCaptureCallback.STATE_LOCKING)
            previewRequestBuilder?.run {
                captureSession?.capture(this.build(), imageCaptureCallback, null)
            }
        } catch (e: CameraAccessException) {
            callbackHandler?.onError(
                Error(
                    Error.ERROR_CAMERA,
                    "Failed to lock focus.",
                    e
                )
            )
        }
    }

    // Capture Flow 2 - 2
    /**
     * 초점을 맞춘 상태에서 정적인 이미지 촬영을 위한 함수
     */
    private fun captureStillPicture() {
        // 촬영 시, 수동 포커싱으로 전환 후 정적인 이미지 촬영.
        try {
            val captureRequestBuilder = camera?.createCaptureRequest(
                CameraDevice.TEMPLATE_STILL_CAPTURE
            )
            imageReader?.surface?.run {
                captureRequestBuilder?.addTarget(this)
//            captureRequestBuilder.addTarget()
                captureRequestBuilder?.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    previewRequestBuilder?.get(CaptureRequest.CONTROL_AF_MODE)
                )
            }

            when (flash) {
                Values.FLASH_OFF -> {
                    captureRequestBuilder?.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON
                    )
                    captureRequestBuilder?.set(
                        CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF
                    )
                }
                Values.FLASH_ON -> captureRequestBuilder?.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                )
                Values.FLASH_TORCH -> {
                    captureRequestBuilder?.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON
                    )
                    captureRequestBuilder?.set(
                        CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_TORCH
                    )
                }
                Values.FLASH_AUTO -> captureRequestBuilder?.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                )
                Values.FLASH_RED_EYE -> captureRequestBuilder?.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                )
            }

            // 그리드 모드에서는 비트맵 변환이 필요하기에, ImageSaver에서 로테이션 처리 진행.
            if (mode != Values.MODE_GRID) {
                captureRequestBuilder?.set(
                    CaptureRequest.JPEG_ORIENTATION,
                    Utils.getOrientation(
                        sensorOrientation,
                        currentDeviceRotation
                    )
                )
            }

            captureRequestBuilder?.set(
                CaptureRequest.SCALER_CROP_REGION,
                calculateZoomRect()
            )

            captureSession?.stopRepeating()
            captureRequestBuilder?.run {
                captureSession?.capture(
                    this.build(),
                    object : CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult
                        ) {
                            unlockFocus()
                        }

                        override fun onCaptureFailed(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            failure: CaptureFailure
                        ) {
                            unlockFocus()
                            callbackHandler?.onError(
                                Error(
                                    Error.ERROR_CAMERA
                                )
                            )
                        }
                    }, null
                )
            }
        } catch (e: CameraAccessException) {
            callbackHandler?.onError(
                Error(
                    Error.ERROR_CAMERA,
                    "Cannot capture a still picture.",
                    e
                )
            )
        }
    }

    /**
     * 촬영 완료 후, AE Lock 해제
     */
    private fun unlockFocus() {
        // AF Lock 해제. (트리거 취소) request
        previewRequestBuilder?.set(
            CaptureRequest.CONTROL_AF_TRIGGER,
            CaptureRequest.CONTROL_AF_TRIGGER_CANCEL
        )
        try {
            previewRequestBuilder?.run {
                captureSession?.capture(this.build(), imageCaptureCallback, null)
                updateAutoFocus()
                updateFlash()
                previewRequestBuilder?.set(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_IDLE
                )
                updatePreview(null)
                imageCaptureCallback.setState(ImageCaptureCallback.STATE_PREVIEW)
            }

        } catch (e: CameraAccessException) {
            callbackHandler?.onError(
                Error(
                    Error.ERROR_CAMERA,
                    e
                )
            )
        }
    }

    /**
     * Background 쓰레드 생성
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").apply {
            start()
        }
        backgroundThread?.run {
            backgroundHandler = Handler(this.looper)
        }
    }

    /**
     * Background 쓰레드 멈추기
     */
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            callbackHandler?.onError(
                Error(
                    Error.ERROR_DEFAULT_CODE,
                    e
                )
            )
        }
    }

    /**
     * 해당 좌표에 초점 맞추기
     * @param x
     * @param y
     */
    private fun focusAt(x: Int, y: Int) {
        var focusRect: Rect? = null
        val maxRegionsAf =
            characteristics?.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)
        if (maxRegionsAf != null && maxRegionsAf >= 1) {
            val sensorArraySize =
                characteristics?.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            focusRect = Utils.calculateFocusArea(
                sensorArraySize,
                displayOrientation,
                textureView,
                x,
                y
            )
        }

        val callBack = object : FocusHandler.Callback {
            override fun onFinish(error: Error?) {
                updatePreview(null)

                // 포커싱 완료되면 사라지기 -> 계속 고정으로 변경됨.
//                preview?.focusFinished()
                preview?.focusCompleted()

                if (error != null) {
                    preview?.focusFinished()
                    callbackHandler?.onError(error)
                }
            }
        }



        focusHandler.focus(
            captureSession, previewRequestBuilder,
            focusRect, callBack)

        // 포커싱 되는 영역에 Overlay로 drawIndicator하는 부분 주석 - 스마트 글래스는 다르게 표시
        preview?.focusRequestAt(x, y)
    }

    /**
     * 해당 값으로 줌 업데이트
     * @param newZoom
     */
    private fun updateZoom(newZoom: Float) {
        callbackHandler?.onZoomChanged(newZoom)
        applyZoom()
    }

    /**
     * 변경하려는 줌 값 Validation - 최대, 최소 값을 벗어나는 경우 근접 값으로 적용
     * @param newZoom
     */
    private fun clampZoom(zoom: Float): Float {
        return Utils.clamp(zoom, 1f, maxZoom)
    }

    /**
     * 줌 적용하
     */
    private fun applyZoom() {
        val zoomRect = calculateZoomRect()
        previewRequestBuilder?.set(
            CaptureRequest.SCALER_CROP_REGION,
            zoomRect
        )
    }

    /**
     * 줌 사각형 계산
     * @return Rect?
     */
    private fun calculateZoomRect(): Rect? {
        val origin =
            characteristics?.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                ?: return null
        if (zoom < 1f) return origin
        val xOffset = ((1 - 1 / zoom) / 2 * (origin.right - origin.left)).toInt()
        val yOffset = ((1 - 1 / zoom) / 2 * (origin.bottom - origin.top)).toInt()
        return Rect(
            xOffset,
            yOffset,
            origin.right - xOffset,
            origin.bottom - yOffset
        )
    }

    /**
     * 프리뷰 업데이트
     * @param exceptionCallback
     */
    private fun updatePreview(exceptionCallback: Runnable?) {
        if (camera == null) {
            return
        }
        try {
            previewRequestBuilder?.run {
                if (mode == Values.MODE_IMAGE || mode == Values.MODE_GRID || mode == Values.MODE_SCAN) {
                    captureSession?.setRepeatingRequest(
                        this.build(),
                        imageCaptureCallback,
                        null
                    )
                } else {
                    previewRequestBuilder?.set(
                        CaptureRequest.CONTROL_MODE,
                        CameraMetadata.CONTROL_MODE_AUTO
                    )
                    captureSession?.setRepeatingRequest(this.build(), null, null)
                }
            }

        } catch (e: CameraAccessException) {
            exceptionCallback?.run()
            callbackHandler?.onError(
                Error(
                    Error.ERROR_CAMERA,
                    e
                )
            )
        }
    }

    //TODO: - Mephrine. 해당 부분 작업하고 capture 및 capture 완료 시 함수 호출해줘야 함.
    /**
     * 그리드 모드에서 캡쳐 / 캡쳐완료 시 Grid Line을 숨기고 보이는 함수
     * @return Rect?
     */
    private fun captureShowHideLineGridMode(show: Boolean) : GridModeView? {
        if (mode != Values.MODE_GRID) return null

        preview?.gridModeViewOrNull?.let {
            when(show) {
                true -> {
                    activityContext?.runOnUiThread {
                        it.showButtonAttrs()

                        preview?.gridModeViewOrNull?.apply {
                            animate()
                                .alpha(1.0f)
                                .setDuration(300)
                        }
                    }
                    return null

                }
                false -> {
                    it.hideButtonAttrs()
                    return it
                }
            }
        }
        return null
    }

    override fun clearAllGrid() {
        preview?.gridModeViewOrNull?.apply {
            clearAllGrid()
        }
    }

    override fun showGridInGridMode(isShowing: Boolean) {
        preview?.gridModeLine = isShowing
    }
}