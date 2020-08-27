package com.deepfine.camera

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point

interface CanvasDrawer {
    fun initPaints(): Array<Paint>
    fun draw(
        canvas: Canvas,
        point: Point?,
        paints: Array<Paint>?
    )

    class DefaultCanvasDrawer : CanvasDrawer {
        override fun initPaints(): Array<Paint> {
            val focusPaint =
                Paint(Paint.ANTI_ALIAS_FLAG)
            focusPaint.style = Paint.Style.STROKE
            focusPaint.strokeWidth = 2f
            focusPaint.color = Color.WHITE
            return arrayOf(focusPaint)
        }

        override fun draw(
            canvas: Canvas,
            point: Point?,
            paints: Array<Paint>?
        ) {
            if (paints == null || paints.size == 0) return
            canvas.drawCircle(point!!.x.toFloat(), point.y.toFloat(), 150f, paints[0]!!)
        }
    }
}