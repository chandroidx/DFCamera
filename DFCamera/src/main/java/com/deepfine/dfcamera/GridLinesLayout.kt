package com.deepfine.camera

import android.R
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting

/**
 * @Description Grid
 * @author jh.kim (DEEP.FINE)
 * @since 2020/08/18
 * @version 1.0.0
 */
enum class Grid {
    OFF {
        override val index: Int = 0
    }, DRAW_3X3 {
        override val index: Int = 1
    }, DRAW_4X4 {
        override val index: Int = 2
    }, DRAW_5X3 {
        override val index: Int = 3
    };

    abstract val index: Int

    companion object {
        fun indexToCase(index: Int) = when (index) {
            1 -> DRAW_3X3
            2 -> DRAW_4X4
            3 -> DRAW_5X3
            else -> OFF
        }
    }
}

/**
 * @Description GridLinesLayout
 * @author jh.kim (DEEP.FINE)
 * @since 2020/08/18
 * @version 1.0.0
 */
class GridLinesLayout @JvmOverloads constructor(
    @NonNull context: Context,
    @Nullable attrs: AttributeSet? = null,
    private val gridLineColor: Int? = 0xCC000000.toInt()
) : View(context, attrs) {
    private var _gridMode: Grid =
        Grid.OFF
    var gridMode: Grid
    get() = _gridMode
    set(value) {
        _gridMode = value
        postInvalidate()
    }
    private var _gridColor =
        gridLineColor!!
    var gridColor: Int
    get() = _gridColor
    set(color) {
        _gridColor = color
    }
    lateinit var horiz: ColorDrawable
    lateinit var vert: ColorDrawable
    var width: Float = 0f

    interface DrawCallback {
        fun onDraw(lines: Int)
    }

    @VisibleForTesting
    var callback: DrawCallback? = null
    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        super.onLayout(changed, left, top, right, bottom)
        horiz.setBounds(left, 0, right, width.toInt())
        vert.setBounds(0, top, width.toInt(), bottom)
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

    private fun getLinePosition(lineNumber: Int): Float {
        return 1f / (lineCount + 1) * (lineNumber + 1f)
    }

    private fun getColumnPosition(lineNumber: Int): Float {
        return 1f / (columnsCount + 1) * (lineNumber + 1f)
    }

    override fun onDraw(@NonNull canvas: Canvas) {
        super.onDraw(canvas)
        val count = lineCount
        for (n in 0 until count) {
            val pos = getLinePosition(n)

            // Draw horizontal line
            canvas.translate(0f, pos * height)
            horiz.draw(canvas)
            canvas.translate(0f, -pos * height)

            val column = getColumnPosition(n)
            // Draw vertical line
            canvas.translate(column * width, 0f)
            vert.draw(canvas)
            canvas.translate(-column * width, 0f)
        }
        if (callback != null) {
            callback!!.onDraw(count)
        }
    }

    init {
        horiz = ColorDrawable(gridColor)
        vert = ColorDrawable(gridColor)
        width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 0.9f,
            context.resources.displayMetrics
        )
    }
}