package com.deepfine.camera

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.widget.RelativeLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import com.deepfine.camera.CanvasDrawer.DefaultCanvasDrawer
import com.deepfine.dfcamera.R
import java.util.*
import kotlin.collections.ArrayList

class CameraView @SuppressLint("ClickableViewAccessibility") constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : RelativeLayout(context, attrs, defStyleAttr) {
    val textureView: AutoFitTextureView
    private var overlay: CameraViewOverlay? = null
    private val displayOrientationDetector: DisplayOrientationDetector
    private val aspectRatio: String?
    private val autoFocus: Boolean
    private val facing: Int
    private val flash: Int
    private val mode: Int
    private var pinchToZoom: Boolean
    private val gestureDetector: GestureDetector
    private val scaleGestureDetector: ScaleGestureDetector
    private val isSmartGlasses: Boolean
    private val exposure: Float
    private var grid: Int
    private var gridLayout: GridLinesLayout? = null
    private var focusGridView: FocusGridView? = null
    private var gridModeView: GridModeView? = null
    private var gridLineColor: Int? = null
    private var gridBgColor: Int? = null
    private var gridTextColor: Int? = null
    private var gridText: String? = null
    private var gridTextSize: Float? = null
    private var gridModeMarginTopBottom: Float? = null
    private var gridModeDimColor: Int? = null

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : this(
        context,
        attrs,
        0
    ) {
    }

    fun assign(photographer: InternalPhotographer) {
        photographer.mode = mode
        photographer.aspectRatio =
            AspectRatio.parse(aspectRatio)
        photographer.autoFocus = autoFocus
        photographer.facing = facing
        photographer.flash = flash
        photographer.isSmartGlasses = isSmartGlasses
        photographer.exposure = exposure
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            displayOrientationDetector.enable(ViewCompat.getDisplay(this))
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            displayOrientationDetector.disable()
        }
        super.onDetachedFromWindow()
    }

    var isFillSpace: Boolean
        get() = textureView.fillSpace
        set(fillSpace) {
            textureView.fillSpace = fillSpace
        }

    // 스마트글래스에서 포커스 줄 때 그려지는 Grid
    var focusGrid: Grid
    get() = Grid.indexToCase(grid)
    set (value){
        grid = value.index
        gridLayout?.apply {
            this.gridMode = Grid.indexToCase(grid)
        }
        focusGridView?.apply {
            this.gridMode = Grid.indexToCase(grid)
        }
    }

    // 그리드모드를 선택했을 경우의 뷰의 유무
    private var _gridModeLine: Boolean = false
    internal var gridModeLine: Boolean
    get() = _gridModeLine
    set(value) {
        _gridModeLine = value
        gridModeView?.apply {
            this.gridMode = if (value) Grid.DRAW_5X3 else Grid.OFF
        }
    }
    var selectedGrid: MutableList<Int> = ArrayList()

    var gridModeViewOrNull: GridModeView? = null
        get() {
            return if (gridModeLine) gridModeView!! else null
        }


    fun setPinchToZoom(pinchToZoom: Boolean) {
        this.pinchToZoom = pinchToZoom
    }

    private fun addOverlay() {
        overlay = CameraViewOverlay(context)
        val overlayParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        overlayParams.addRule(ALIGN_LEFT, R.id.textureView)
        overlayParams.addRule(ALIGN_TOP, R.id.textureView)
        overlayParams.addRule(ALIGN_RIGHT, R.id.textureView)
        overlayParams.addRule(ALIGN_BOTTOM, R.id.textureView)
        addView(overlay, overlayParams)
    }



    fun setFocusIndicatorDrawer(drawer: CanvasDrawer) {
        overlay!!.setCanvasDrawer(drawer)
    }

    fun focusRequestAt(x: Int, y: Int) {
        overlay!!.focusRequestAt(x, y)
    }

    fun focusFinished() {
        overlay!!.focusFinished()
    }

    fun shot(closure: () -> Unit) {
        return overlay!!.shot {
            closure()
        }
    }

    interface Callback : AutoFitTextureView.Callback {
        fun onSingleTap(x: Int, y: Int)
        fun onScale(scaleFactor: Float)
    }

    private val callbacks: MutableList<Callback> =
        LinkedList()

    fun addCallback(callback: Callback?) {
        if (callback != null) {
            callbacks.add(callback)
            textureView.addCallback(callback)
        }
    }

    private fun dispatchOnSingleTap(e: MotionEvent) {
        for (callback in callbacks) {
            callback.onSingleTap(e.x.toInt(), e.y.toInt())
        }
    }

    private fun dispatchOnScale(scaleFactor: Float) {
        for (callback in callbacks) {
            callback.onScale(scaleFactor)
        }
    }

    init {
        textureView = AutoFitTextureView(context)
        textureView.id = R.id.textureView
        val textureViewParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textureViewParams.addRule(CENTER_IN_PARENT)
        addView(textureView, textureViewParams)
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraView)
        aspectRatio = typedArray.getString(R.styleable.CameraView_aspectRatio)
        autoFocus = typedArray.getBoolean(R.styleable.CameraView_autoFocus, true)
        facing = typedArray.getInt(
            R.styleable.CameraView_facing,
            Values.FACING_BACK
        )
        flash = typedArray.getInt(
            R.styleable.CameraView_flash,
            Values.FLASH_OFF
        )
        mode = typedArray.getInt(
            R.styleable.CameraView_mode,
            Values.MODE_IMAGE
        )

        grid = typedArray.getInt(
            R.styleable.CameraView_grid,
            Values.GRID_OFF
        )

        exposure = typedArray.getFloat(
            R.styleable.CameraView_exposure, 0.0.toFloat()
        )

        isSmartGlasses = typedArray.getBoolean(
            R.styleable.CameraView_isSmartGlasses, true
        )

        val blackColor = ResourcesCompat.getColor(context.resources, android.R.color.black, null)
        gridLineColor = typedArray.getColor(
            R.styleable.CameraView_gridModeLineColor,
            blackColor
        )

        val blackAlphaColor = 0x80ffc800.toInt()
        gridBgColor = typedArray.getColor(
            R.styleable.CameraView_gridModeBgColor,
            Utils.adjustAlpha(blackAlphaColor, 0.3f)
        )

        gridTextColor = typedArray.getColor(
            R.styleable.CameraView_gridModeTextColor,
            blackColor
        )

        gridText = typedArray.getString(
            R.styleable.CameraView_gridModeText
        )

        gridTextSize = typedArray.getFloat(
            R.styleable.CameraView_gridModeTextSize,
            Utils.dpToPixel(context, 13f)
        )

        gridModeMarginTopBottom = typedArray.getFloat(
            R.styleable.CameraView_gridModeMarginTopBottom,
            40f
        )

        gridModeDimColor = typedArray.getColor(
            R.styleable.CameraView_gridModeDimColor,
            0xAB000000.toInt()
        )

        val fillSpace =
            typedArray.getBoolean(R.styleable.CameraView_fillSpace, false)
        textureView.fillSpace = fillSpace

        pinchToZoom = typedArray.getBoolean(R.styleable.CameraView_pinchToZoom, true)
        val showFocusIndicator =
            typedArray.getBoolean(R.styleable.CameraView_showFocusIndicator, true)
        typedArray.recycle()

        when (isSmartGlasses) {
            true -> {
                // 포커
                focusGridView = FocusGridView(context, null)
                val gridViewParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                focusGridView?.let {
                    addView(it, gridViewParams)
                    it.gridMode =
                        Grid.indexToCase(grid)
                }

                focusGridView?.callback = object :
                    FocusGridView.TouchCallback {
                    override fun onTouch(x: Int, y: Int) {
                        for (callback in callbacks) {
                            callback.onSingleTap(x, y)
                        }
                    }
                }
            }
            false -> {
                gridLayout = GridLinesLayout(context, null, gridLineColor)
                gridLayout?.let {
                    addView(it)
                    it.gridMode =
                        Grid.indexToCase(grid)
                }
            }
        }

        gridModeView = GridModeView(
            context,
            null,
            gridLineColor,
            gridBgColor,
            gridTextColor,
            gridText,
            gridTextSize,
            gridModeMarginTopBottom,
            gridModeDimColor
        )
        gridModeView?.let {
            addView(it)
            it.gridMode = Grid.indexToCase(grid)
        }

        gridModeView?.callback = object :
            GridModeView.TouchCallback {
            override fun onSelected(selected: MutableList<Int>) {
                selectedGrid = selected
            }
        }

        addOverlay()
        if (showFocusIndicator) {
            setFocusIndicatorDrawer(DefaultCanvasDrawer())
        }
        displayOrientationDetector = object : DisplayOrientationDetector(context) {
            public override fun onDisplayOrientationChanged(displayOrientation: Int) {
                textureView.displayOrientation = displayOrientation
            }
        }
        gestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                dispatchOnSingleTap(e)
                return true
            }
        })
        scaleGestureDetector =
            ScaleGestureDetector(context, object : SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    if (pinchToZoom) {
                        dispatchOnScale(detector.scaleFactor)
                    }
                    return true
                }
            })
        textureView.setOnTouchListener { v: View?, event: MotionEvent? ->
            if (gestureDetector.onTouchEvent(event)) {
                return@setOnTouchListener true
            }
            if (scaleGestureDetector.onTouchEvent(event)) {
                return@setOnTouchListener true
            }
            true
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return !isSmartGlasses
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isSmartGlasses) return false
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }
        if (scaleGestureDetector.onTouchEvent(event)) {
            return true
        }
        return false
    }


}