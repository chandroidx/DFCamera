package com.deepfine.camera

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.NonNull
import androidx.annotation.Nullable

import android.R
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

/**
 * @Description FocusGridView
 * @author jh.kim (DEEP.FINE)
 * @since 2020/08/19
 * @version 1.0.0
 */
class FocusGridView @JvmOverloads constructor(
    @NonNull context: Context,
    @Nullable attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private var _gridMode: Grid =
        Grid.OFF
    var gridMode: Grid
        get() = _gridMode
        set(value) {
            _gridMode = value
            updateView()
            postInvalidate()
        }
    var callback: TouchCallback? = null

    interface TouchCallback {
        fun onTouch(x: Int, y: Int)
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
        this.removeAllViewsInLayout()

        if (lineCount > 1) {
            for (i in 0..lineCount) {
                val row = LinearLayout(context)
                row.layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT,
                    1f
                )
                row.orientation = HORIZONTAL
                row.gravity = View.TEXT_ALIGNMENT_CENTER
                for (j in 0..columnsCount) {
                    val textView = TextView(context)
                    textView.layoutParams =  LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.MATCH_PARENT,
                        1f
                    )

                    textView.text = "포커스 " + (j + 1 + columnsCount * lineCount)
                    textView.setTextColor(ResourcesCompat.getColor(context.resources, R.color.black, null))
                    textView.setOnClickListener { view ->
                        callback?.let {
                            val location = IntArray(2)
                            view.getLocationOnScreen(location)
                            val x = location[0] + (view.width / 2)
                            val y = location[1] + (view.height / 2)

                            it.onTouch(x, y)
                        }
                    }
                    row.addView(textView)
                }
                addView(row)
            }
        }
    }

    init {
        this.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        this.orientation = VERTICAL
    }
}