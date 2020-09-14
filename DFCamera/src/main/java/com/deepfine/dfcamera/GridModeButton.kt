package com.deepfine.camera

import android.R
import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
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
    private val density = (resources.displayMetrics.density).toInt()
    private final val ANIMATION_DURATION: Long = 500
    private var isAnimating: Boolean = false

    private val showDrawable: GradientDrawable = GradientDrawable().apply {
        lineColor?.let {
            this.setStroke(1 * density / 2, it)
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
    fun focusAnimation(toLineColor: Int, completion: ()-> Unit) {
        if(!isAnimating) {
            isAnimating = true
            var widthAnimator: Int = 1 * density
            var colorAnimator: Int = lineColor!!
            ValueAnimator.ofInt(1 * density / 2, 4 * density).apply {
                duration = ANIMATION_DURATION / 2
                repeatCount = 1
                repeatMode = ValueAnimator.REVERSE
                addUpdateListener { animator ->
                    widthAnimator = animator?.animatedValue as Int
                    showDrawable.setStroke(widthAnimator, colorAnimator)
                }
                start()

                ValueAnimator.ofObject(ArgbEvaluator(), lineColor, toLineColor).apply {
                    duration = ANIMATION_DURATION / 2
                    repeatCount = 1
                    repeatMode = ValueAnimator.REVERSE
                    addUpdateListener { animator ->
                        colorAnimator = animator?.animatedValue as Int
                        showDrawable.setStroke(widthAnimator, colorAnimator)
                    }
                    addListener(object: Animator.AnimatorListener {
                        override fun onAnimationEnd(p0: Animator?) {
                            isAnimating = false
                        }

                        override fun onAnimationRepeat(p0: Animator?) {
                        }

                        override fun onAnimationCancel(p0: Animator?) {
                        }

                        override fun onAnimationStart(p0: Animator?) {
                        }
                    })
                    start()
                }


//            ValueAnimator.ofInt(1 * density, 4 * density).apply {
//                duration = ANIMATION_DURATION / 5
//                addUpdateListener { animator ->
//                    showDrawable.setStroke(animator?.animatedValue as Int, lineColor!!)
//                }
//                addListener(object: Animator.AnimatorListener {
//                    override fun onAnimationEnd(p0: Animator?) {
//                        showDrawable.setStroke( 1 * density, lineColor ?: R.color.transparent)
//                        ValueAnimator.ofObject(ArgbEvaluator(), lineColor ?: R.color.transparent, toLineColor).apply {
//                            duration = ANIMATION_DURATION / 2
//                            repeatCount = 1
//                            repeatMode = ValueAnimator.REVERSE
//                            addUpdateListener { animator ->
//                                showDrawable.setStroke(4 * density, animator?.animatedValue as Int)
//                            }
//                            addListener(object: Animator.AnimatorListener {
//                                override fun onAnimationEnd(p0: Animator?) {
//                                    ValueAnimator.ofInt(4 * density, 1 * density).apply {
//                                        duration = ANIMATION_DURATION / 4
//                                        addUpdateListener { animator ->
//                                            showDrawable.setStroke(animator?.animatedValue as Int, lineColor!!)
//                                        }
//                                        addListener(object: Animator.AnimatorListener{
//                                            override fun onAnimationCancel(p0: Animator?) {
//                                            }
//
//                                            override fun onAnimationStart(p0: Animator?) {
//                                            }
//
//                                            override fun onAnimationRepeat(p0: Animator?) {
//                                            }
//
//                                            override fun onAnimationEnd(p0: Animator?) {
//                                                isAnimating = false
//                                                completion()
//                                            }
//                                        })
//                                        start()
//                                    }
//                                }
//
//                                override fun onAnimationRepeat(p0: Animator?) {
//                                }
//
//                                override fun onAnimationCancel(p0: Animator?) {
//                                }
//
//                                override fun onAnimationStart(p0: Animator?) {
//                                }
//                            })
//                            start()
//                        }
//                    }
//
//                    override fun onAnimationRepeat(p0: Animator?) {
//                    }
//
//                    override fun onAnimationCancel(p0: Animator?) {
//                    }
//
//                    override fun onAnimationStart(p0: Animator?) {
//                    }
//                })
//                start()
            }
        }
    }

//    private fun focusAnimationEnd(toLineColor: Int) {
//        ValueAnimator.ofObject(ArgbEvaluator(), toLineColor, lineColor ?: R.color.transparent).apply {
//            duration = ANIMATION_DURATION / 2
//            repeatCount = 1
//            repeatMode = ValueAnimator.REVERSE
//            addUpdateListener { animator ->
//                showDrawable.setStroke(4 * density, animator?.animatedValue as Int)
//            }
//            addListener(object: Animator.AnimatorListener {
//                override fun onAnimationEnd(p0: Animator?) {
//                    showDrawable.setStroke( 1 * density, lineColor ?: R.color.transparent)
//                }
//
//                override fun onAnimationRepeat(p0: Animator?) {
//                }
//
//                override fun onAnimationCancel(p0: Animator?) {
//                }
//
//                override fun onAnimationStart(p0: Animator?) {
////                    showDrawable.setStroke(4 * density, toLineColor)
//                    showDrawable.setStroke( 4 * density, lineColor ?: R.color.transparent)
//                    Log.d("t","t")
//                }
//            })
//            start()
//        }
//        ValueAnimator.ofInt(1 * density, 4 * density).apply {
//            duration = ANIMATION_DURATION / 2
//            repeatCount = 1
//            repeatMode = ValueAnimator.REVERSE
//            addUpdateListener { animator ->
//                showDrawable.setStroke(4 * density, animator?.animatedValue as Int)
//            }
//            addListener(object: Animator.AnimatorListener {
//                override fun onAnimationEnd(p0: Animator?) {
//                    showDrawable.setStroke( 1 * density, lineColor ?: R.color.transparent)
//                }
//
//                override fun onAnimationRepeat(p0: Animator?) {
//                }
//
//                override fun onAnimationCancel(p0: Animator?) {
//                }
//
//                override fun onAnimationStart(p0: Animator?) {
////                    showDrawable.setStroke(4 * density, toLineColor)
//                    showDrawable.setStroke( 4 * density, lineColor ?: R.color.transparent)
//                    Log.d("t","t")
//                }
//            })
//            start()
//        }
//    }


}

