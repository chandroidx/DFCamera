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

    private fun generateBitmap(view: LinearLayout): Bitmap? {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(
                0,
                View.MeasureSpec.UNSPECIFIED
            ),
            View.MeasureSpec.makeMeasureSpec(
                0,
                View.MeasureSpec.UNSPECIFIED
            )
        )

        //Assign a size and position to the view and all of its descendants
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        //Create the bitmap
        val bitmap = Bitmap.createBitmap(
            view.measuredWidth,
            view.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        //Create a canvas with the specified bitmap to draw into
        val c = Canvas(bitmap)

        //Render this view (and all of its children) to the given Canvas
        view.draw(c)
        return bitmap
    }

    //    private fun createBitmapFromView(view: View): Bitmap? {
//        val displayMetrics = DisplayMetrics()
//        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
//        view.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
//        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels)
//        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
//        view.buildDrawingCache()
//        val bitmap = Bitmap.createBitmap(
//            view.measuredWidth,
//            view.measuredHeight,
//            Bitmap.Config.ARGB_8888
//        )
//        val canvas = Canvas(bitmap)
//        view.draw(canvas)
//        return bitmap
//    }
    internal fun getResizedBitmap(bm: Bitmap, newWidth: Int, newHeight: Int): Bitmap? {
        val width = bm.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height

        // CREATE A MATRIX FOR THE MANIPULATION
        val matrix = Matrix()

        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight)

        // "RECREATE" THE NEW BITMAP
        val resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false)
        bm.recycle()
        return resizedBitmap
    }
}