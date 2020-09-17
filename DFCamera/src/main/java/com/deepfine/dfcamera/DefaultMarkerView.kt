package com.deepfine.dfcamera

import android.content.Context
import android.graphics.Canvas
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
        val left: Float = 0f
        val top: Float = 0f
        val right: Float = sizeWidth
        val bottom: Float = sizeHeight

        Path().apply {
            this.moveTo(left - value, top + lineLength)
            this.lineTo(left - value, top - value)
            this.lineTo(left + lineLength, top - value)

            this.moveTo(right - lineLength, top - value)
            this.lineTo(right + value, top - value)
            this.lineTo(right + value, top + lineLength)

            this.moveTo(right + value, bottom - lineLength)
            this.lineTo(right + value, bottom + value)
            this.lineTo(right - lineLength, bottom + value)

            this.moveTo(left + lineLength, bottom + value)
            this.lineTo(left - value, bottom + value)
            this.lineTo(left - value, bottom - lineLength)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawPath(path, paint)
    }
}