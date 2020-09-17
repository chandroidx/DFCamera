package com.deepfine.dfcamera

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Point
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * @Description FocusMarkerLayout
 * @author jh.kim (DEEP.FINE)
 * @since 2020/09/16
 * @version 1.0.0
 */

final class FocusMarkerLayout constructor(
    context: Context
): FrameLayout(context) {
    private var mViews: View? = null

    /**
     * FocusMarker 적용
     */
    fun onMarker(marker: FocusMarker?) {
        mViews?.let { removeView(it) }

        if (marker == null) return

        marker.onAttach(context, this)?.let {
            mViews = it
            addView(it)
        }
        this.alpha  = 0f
    }

    /**
     * 포커싱된 위치로 좌표 이동
     */
    fun onEvent(x: Int, y: Int) {
        val focusView = mViews ?: return
        focusView.clearAnimation()

        val pointX: Float = (x - (focusView.width / 2)).toFloat()
        val pointY: Float = (y - (focusView.height / 2)).toFloat()

        this.translationX = pointX
        this.translationY = pointY
    }
}