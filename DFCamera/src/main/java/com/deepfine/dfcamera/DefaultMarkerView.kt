package com.deepfine.dfcamera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import androidx.core.content.ContextCompat
import org.jetbrains.annotations.NotNull

/**
 * @Description Class
 * @author jh.kim (DEEP.FINE)
 * @since 2020/09/16
 * @version 1.0.0
 */

final class DefaultMarkerView constructor(
    context: Context,
    @NotNull private val sizeWidth: Float,
    @NotNull private val sizeHeight: Float,
    @NotNull private val lineLength: Float,
    @NotNull private val strokeWidth: Float,
    @NotNull private val strokeColor: Int,
    private val value: Float = 0f
    ): View(context) {


    private val paint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).also{ paint ->
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth
            paint.color = strokeColor
        }
    }

    private val path: Path by lazy {
        val initPoint = strokeWidth / 2
        val left: Float = initPoint
        val top: Float = initPoint
        val right: Float = sizeWidth - initPoint
        val bottom: Float = sizeHeight - initPoint

        Path().apply {
            this.moveTo(left, top + lineLength)
            this.lineTo(left, top)
            this.lineTo(left + lineLength, top)

            this.moveTo(right - lineLength, top)
            this.lineTo(right, top - value)
            this.lineTo(right, top + lineLength)

            this.moveTo(right, bottom - lineLength)
            this.lineTo(right, bottom)
            this.lineTo(right - lineLength, bottom)

            this.moveTo(left + lineLength, bottom)
            this.lineTo(left, bottom)
            this.lineTo(left, bottom - lineLength)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawPath(path, paint)
    }
}