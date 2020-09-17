package com.deepfine.camera

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.animation.DecelerateInterpolator


internal class CameraViewOverlay @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr) {
//    private var focusPoint: Point? = null
//    private var canvasDrawer: CanvasDrawer? = null
//    private var paints: Array<Paint>? = null
//    fun setCanvasDrawer(drawer: CanvasDrawer) {
//        canvasDrawer = drawer
//        paints = drawer.initPaints()
//    }

    // 포커싱 되는 영역에 Overlay로 drawIndicator하는 부분
//    fun focusRequestAt(x: Int, y: Int) {
//        if (x >= 0 && x <= measuredWidth && y >= 0 && y <= measuredHeight) {
//            focusPoint = Point(x, y)
//        }
//        drawIndicator()
//    }
//
//    fun focusFinished() {
//        focusPoint = null
//        postDelayed({ this.clear() }, 300)
//    }
//
//    private fun drawIndicator() {
//        invalidate()
//        if (holder.surface.isValid) {
//            val canvas = holder.lockCanvas()
//            if (canvas != null) {
//                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
//                canvas.drawColor(Color.TRANSPARENT)
//                if (canvasDrawer != null) {
//                    canvasDrawer!!.draw(canvas, focusPoint, paints)
//                }
//                holder.unlockCanvasAndPost(canvas)
//            }
//        }
//    }

//    private fun clear() {
//        val canvas = holder.lockCanvas()
//        if (canvas != null) {
//            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
//            holder.unlockCanvasAndPost(canvas)
//        }
//    }

    fun shot() {
        val colorFrom = Color.TRANSPARENT
        val colorTo = -0x51000000
        val colorAnimation =
            ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.interpolator = DecelerateInterpolator()
        colorAnimation.duration = SHUTTER_ONE_WAY_TIME.toLong()
        colorAnimation.addUpdateListener { animator: ValueAnimator ->
            setBackgroundColor(
                animator.animatedValue as Int
            )
        }
        colorAnimation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator) {
                setBackgroundColor(colorFrom)
            }
        })
        colorAnimation.start()
        postDelayed(
            {
                colorAnimation.reverse()
            },
            SHUTTER_ONE_WAY_TIME.toLong()
        )
    }

    companion object {
        private const val SHUTTER_ONE_WAY_TIME = 150
    }

    init {
        setZOrderOnTop(true)
//        holder.setFormat(PixelFormat.TRANSPARENT)
//        holder.addCallback(object : SurfaceHolder.Callback {
//            override fun surfaceCreated(holder: SurfaceHolder) {
//                clear()
//            }
//
//            override fun surfaceChanged(
//                holder: SurfaceHolder,
//                format: Int,
//                width: Int,
//                height: Int
//            ) {
//                clear()
//            }
//
//            override fun surfaceDestroyed(holder: SurfaceHolder) {}
//        })
    }
}