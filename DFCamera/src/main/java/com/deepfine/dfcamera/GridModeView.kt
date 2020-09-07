package com.deepfine.camera

import android.R
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.res.ResourcesCompat
import java.util.*


class GridModeView @JvmOverloads constructor(
    @NonNull context: Context,
    @Nullable attrs: AttributeSet? = null,
    val lineColor: Int? = null,
    val bgColor: Int? = null,
    val textColor: Int? = null,
    val text: String? = null,
    val textSize: Int? = null,
    val marginTopBottom: Int? = 40,
    val dimColor: Int? = 0xAB000000.toInt()
) : LinearLayout(context, attrs) {
    private var _gridMode: Grid = Grid.OFF
    var gridMode: Grid
        get() = _gridMode
        set(value) {
//            if (_gridMode == value) { return }
            _gridMode = value
            updateView()
            postInvalidate()
        }

    var callback: TouchCallback? = null
    val selected: MutableList<Int> = ArrayList()
    val hiddenViewList: MutableList<Int> = ArrayList()

    interface TouchCallback {
        fun onSelected(selected: MutableList<Int>)
    }

    private val lineCount: Int
        private get() {
            when (gridMode) {
                Grid.DRAW_3X3, Grid.DRAW_5X3 -> return 2
                Grid.DRAW_4X4 -> return 3
                else -> return 0
            }
        }
    private val columnsCount: Int
        private get() {
            when (gridMode) {
                Grid.DRAW_3X3 -> return 2
                Grid.DRAW_4X4 -> return 3
                Grid.DRAW_5X3 -> return 4
                else -> return 0
            }
        }

    fun updateView() {
        // 초기화
        removeAllViewsInLayout()
        selected.clear()
        hiddenViewList.clear()

        if (lineCount > 1) {
            // 배경 상단 뷰
            val blankTop: View = View(context).also {
                it.id = View.generateViewId()
                hiddenViewList.add(it.id)

                it.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,0,1f
                )

                it.setBackgroundColor(dimColor!!)
                addView(it)
            }


            addView(LinearLayout(context).also {
                it.layoutParams = LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT,0f
                )
                it.orientation = LinearLayout.HORIZONTAL

                val blankLeft: View = View(context).also { view ->
                    view.id = View.generateViewId()
                    hiddenViewList.add(view.id)

                    view.layoutParams = LinearLayout.LayoutParams(
                        0,FrameLayout.LayoutParams.MATCH_PARENT,1f
                    )

                    view.setBackgroundColor(dimColor!!)
                    it.addView(view)
                }

                // 중앙 그리드 뷰
                val metrics = resources.displayMetrics

                val contentsHeight: Int = if (metrics.heightPixels > metrics.widthPixels) {
                    (metrics.widthPixels - metrics.density * marginTopBottom!!) / (columnsCount + 1)
                } else {
                    (metrics.heightPixels - metrics.density * marginTopBottom!!) / (lineCount + 1)
                }.toInt()

                val container: LinearLayout = LinearLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        (contentsHeight * (columnsCount + 1)),
                        (contentsHeight * (lineCount + 1)),
                        0f
                    )

                    orientation = LinearLayout.VERTICAL
                }

                for (i in 0..lineCount) {
                    val row = LinearLayout(context)
                    row.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    row.orientation = LinearLayout.HORIZONTAL

                    for (j in 0..columnsCount) {
                        val textView = GridModeButton(
                            context,
                            text ?: "" + (j + 1 + i * (columnsCount + 1)).toString(),
                            lineColor,
                            bgColor!!
                        ).apply {
                            this.layoutParams = LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                1f
                            )
                            this.gravity = Gravity.CENTER
                            this.setTextColor(
                                textColor ?: ResourcesCompat.getColor(
                                    context.resources,
                                    R.color.black,
                                    null
                                )
                            )
                            this.id = j + 1 + i * 4
                            this.setOnClickListener { view ->
                                var isExist = false
                                for (i in selected) {
                                    if (i == view.id) {
                                        isExist = true
                                        break
                                    }
                                }

                                if (isExist) {
                                    selected.remove(view.id)
                                    (view as? GridModeButton)?.selected(false)
                                } else {
                                    selected.add(view.id)
                                    (view as? GridModeButton)?.selected(true)
                                }

                                callback?.let {
                                    it.onSelected(selected)
                                }
                            }
                        }
                        textView.setTextSize(
                            TypedValue.COMPLEX_UNIT_PX, textSize?.toFloat() ?: Utils.dpToPixel(
                            context,
                            30f
                        ))
                        textView.id = View.generateViewId()
                        hiddenViewList.add(textView.id)

                        row.addView(textView)
                    }
                    container.addView(row)
                }
                it.addView(container)

                val blankRight: View = View(context).also { view ->
                    view.id = View.generateViewId()
                    hiddenViewList.add(view.id)

                    view.layoutParams = LinearLayout.LayoutParams(
                        0,LinearLayout.LayoutParams.MATCH_PARENT,1f
                    )

                    view.setBackgroundColor(dimColor!!)
                    it.addView(view)
                }
            })

            val blankBottom: View = View(context).also {
                it.id = View.generateViewId()
                hiddenViewList.add(it.id)

                it.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,0,1f
                )

                it.setBackgroundColor(dimColor!!)
                addView(it)
            }
        }
    }

    fun hideButtonAttrs() {
       hiddenViewList.forEach { viewId ->
           var isApply = false
           (this.findViewById(viewId) as? GridModeButton)?.apply {
               isApply = true
               hideButtonAttrs(selected.filter { it == this.id }.isNotEmpty())
           }

           if (!isApply) (this.findViewById<View>(viewId))?.apply {
               visibility = View.INVISIBLE
           }
       }
    }

    fun showButtonAttrs() {
        hiddenViewList.forEach { viewId ->
            var isApply = false
            (this.findViewById(viewId) as? GridModeButton)?.apply {
                isApply = true
                showButtonAttrs(selected.filter { it == this.id }.isNotEmpty())
            }

            if (!isApply) (this.findViewById<View>(viewId))?.apply {
                visibility = View.VISIBLE
            }
        }
    }

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        orientation = LinearLayout.VERTICAL
    }
}