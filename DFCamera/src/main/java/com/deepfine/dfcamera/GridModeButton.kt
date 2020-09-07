package com.deepfine.camera

import android.R
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.TransitionDrawable
import android.view.View
import androidx.annotation.NonNull
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat


/**
 * @Description GridModeButton
 * @author jh.kim (DEEP.FINE)
 * @since 2020/08/21
 * @version 1.0.0
 */

final class GridModeButton @JvmOverloads constructor(
    @NonNull context: Context,
    private val text: String,
    private val lineColor: Int? = 0x000000,
    private val bgColor: Int = 0xCC000000.toInt()
) : AppCompatTextView(context) {
    private val showDrawable: GradientDrawable = GradientDrawable().apply {
        lineColor?.let {
            this.setStroke(1, it)
        }
    }
    private val hideDrawable: GradientDrawable = GradientDrawable().apply {
        setColor(bgColor)
    }

    init {
        showButtonAttrs(true)
    }

    /**
     *  촬영 시, 그리드 라인 및 선택되지 않은 영역 숨기기
     */
    fun hideButtonAttrs(selected: Boolean) {
        when (selected) {
            true -> {
                this.setText("")
                this.background = hideDrawable
            }
            false -> {
                visibility = View.INVISIBLE
            }

        }

    }

    /**
     *  촬영 후 이전 그리드 속성 그대로 적용
     */
    fun showButtonAttrs(selected: Boolean) {
        when (selected) {
            true -> {
                this.setText(text)
                this.background = showDrawable
            }
            false -> {
                visibility = View.VISIBLE
            }
        }
    }

    /**
     * 그리드 모드에서 선택 여부에 따른 drawable적용
     */
    fun selected(selected: Boolean) {
        showDrawable.apply {
            if (!selected) setColor(
                ResourcesCompat.getColor(
                    context.resources,
                    R.color.transparent,
                    null
                )
            ) else setColor(bgColor)
        }
    }

    /**
     * 포커싱 애니메이션 효과 적용
     */
    fun focusAnimation(lineColor: Int) {
        val width = (resources.displayMetrics.density).toInt()


        ValueAnimator.ofObject(ArgbEvaluator(), R.color.transparent, lineColor).apply {
            duration = 500
            repeatCount = 1
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animator ->
                showDrawable.setStroke(width, animator.animatedValue as Int)
            }
            start()
        }
    }
}

