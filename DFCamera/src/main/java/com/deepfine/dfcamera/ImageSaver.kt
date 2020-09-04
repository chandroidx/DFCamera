package com.deepfine.camera

import android.graphics.*
import android.media.Image
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import java.io.FileOutputStream
import java.io.IOException


internal class ImageSaver(
    private val image: Image,
    private val filePath: String?,
    private val gridModeView: GridModeView?,
    private val width: Int,
    private val height: Int,
    private val degree: Int
) :
    Runnable {
    override fun run() {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer[bytes]
        var output: FileOutputStream? = null

        try {
            output = FileOutputStream(filePath)

            gridModeView?.let { gridModeView ->
                val gridModeBitmap = Bitmap.createBitmap(
                    gridModeView.measuredWidth,
                    gridModeView.measuredHeight,
                    Bitmap.Config.ARGB_8888
                )

                val canvas = Canvas(gridModeBitmap)
                gridModeView.draw(canvas)

                val captureImage: Bitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                captureImage?.let { captureImage ->
                    var resultImage: Bitmap? = null
                    val resizingImage: Bitmap? =
                        Bitmap.createScaledBitmap(captureImage, width, height, true).rotate(degree)

                    resizingImage?.let {
                        resultImage = Bitmap.createBitmap(it.width, it.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas()
                        canvas.setBitmap(resultImage)
                        canvas.drawBitmap(it, 0f, 0f, null)

                        val resizingGridImage: Bitmap? =
                            Bitmap.createScaledBitmap(gridModeBitmap, it.width, it.height, true)

                        resizingGridImage?.run {
                            canvas.drawBitmap(this, 0f, 0f, null)
                        }
                    }

                    resultImage!!.compress(Bitmap.CompressFormat.JPEG, 100, output)
                }
            }
            output.write(bytes)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            image.close()
            if (null != output) {
                try {
                    output.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    infix fun Bitmap.rotate(degrees: Number): Bitmap? {
        return Bitmap.createBitmap(
            this,
            0,
            0,
            width,
            height,
            Matrix().apply { postRotate(degrees.toFloat()) },
            true
        )
    }
}