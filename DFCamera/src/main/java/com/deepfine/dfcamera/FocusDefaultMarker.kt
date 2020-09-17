package com.deepfine.dfcamera

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.VisibleForTesting

/**
 * @Description FocusDefaultMarker
 * @author jh.kim (DEEP.FINE)
 * @since 2020/09/16
 * @version 1.0.0
 */

public class FocusDefaultMarker: FocusMarker {
    private var markerView: View? = null
    private val MAX_SCALE: Float = 1.36f

    override fun onAttach(context: Context, container: ViewGroup?): View? {
        /*
        // inflate 사용 시.
        var view: View = LayoutInflater.from(context).inflate(
            R.layout.layout_focus_default_marker,
            container, false
        )

        mContainer = view.findViewById(R.id.focusMarkerContainer)
        markerView = view.findViewById(R.id.focusMarkerFill)
        */

        val density = context.resources.displayMetrics.density
        val viewWidth = 80 * density
        val viewHeight = 80 * density
        markerView = DefaultMarkerView(context, viewWidth, viewHeight, 30 * density, 6 * density, (0x80d8d8d8).toInt()).apply {
            layoutParams = LinearLayout.LayoutParams(
                viewWidth.toInt(), viewHeight.toInt()
            )
        }
        return  markerView
    }

    /**
     * 애니메이션 시작
     */
    override fun onAutoFocusStart() {
        (markerView?.parent as? FrameLayout)?.apply {
            clearAnimation()
            scaleX = 1f
            scaleY = 1f
            alpha = 0f
            animate(this, MAX_SCALE, 1f, 300, 0, null)
        }

    }

    /**
     * 애니메이션 종료
     */
    override fun onAutoFocusEnd(
        successful: Boolean
    ) {
            if (successful) {
                (markerView?.parent as? FrameLayout)?.apply {
                    animate(this, 1f, 1f, 300, 0, null)
                }
            } else {

                (markerView?.parent as? FrameLayout)?.apply {
                    animate(
                        this, MAX_SCALE, 1f, 500, 0,
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                animate(
                                    this@apply, MAX_SCALE, 0f, 200, 1000,
                                    null
                                )
                            }
                        })
                }

            }
    }

    override fun clear() {
        (markerView?.parent as? FrameLayout)?.apply {
            this.alpha = 0f
            this.scaleX = 1f
            this.scaleY = 1f
        }
    }

    private fun animate(
        view: View, scale: Float, alpha: Float, duration: Long,
        delay: Long, listener: Animator.AnimatorListener?
    ) {
        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .alpha(alpha)
            .setDuration(duration)
            .setStartDelay(delay)
            .setListener(listener)
            .start()
    }
}