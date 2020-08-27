package com.deepfine.camera

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Environment
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import androidx.annotation.ColorInt
import java.io.File
import java.io.IOException
import java.util.*
import java.util.regex.Pattern


object Utils {
    private val pattern = Pattern.compile("^#(\\d+), (.+)")
    fun exceptionMessage(code: Int, message: String?): String {
        return String.format(Locale.getDefault(), "#%d, %s", code, message)
    }

    private fun codeFromThrowable(throwable: Throwable, fallback: Int): Int {
        var errorCode = fallback
        val message = throwable.message
        if (message != null) {
            val matcher =
                pattern.matcher(message)
            if (matcher.find()) {
                errorCode = matcher.group(1).toInt()
            }
        }
        return errorCode
    }

    private fun messageFromThrowable(throwable: Throwable): String? {
        var message = throwable.message
        if (message == null) {
            message = throwable.toString()
        } else {
            val matcher =
                pattern.matcher(message)
            if (matcher.find()) {
                message = matcher.group(2)
            }
        }
        return message
    }

    fun errorFromThrowable(throwable: Throwable): Error {
        return errorFromThrowable(
            throwable,
            Error.ERROR_DEFAULT_CODE
        )
    }

    private fun errorFromThrowable(
        throwable: Throwable,
        fallback: Int
    ): Error {
        return Error(
            codeFromThrowable(
                throwable,
                fallback
            ), messageFromThrowable(throwable)
        )
    }

    fun napInterrupted(): Boolean {
        return !sleep(1)
    }

    private fun sleep(millis: Long): Boolean {
        try {
            Thread.sleep(millis)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun getBoolean(
        params: Map<String?, Any?>?,
        key: String?,
        defaultValue: Boolean
    ): Boolean {
        var value = defaultValue
        if (params == null) {
            return value
        }
        val valueObject = params[key]
        if (valueObject is Boolean) {
            value = valueObject
        }
        return value
    }

    fun getInt(
        params: Map<String?, Any?>?,
        key: String?,
        defaultValue: Int
    ): Int {
        var value = defaultValue
        if (params == null) {
            return value
        }
        val valueObject = params[key]
        if (valueObject is Int) {
            value = valueObject
        }
        return value
    }

    fun getString(
        params: Map<String?, Any?>?,
        key: String?,
        defaultValue: String
    ): String {
        var value = defaultValue
        if (params == null) {
            return value
        }
        val valueObject = params[key]
        if (valueObject is String) {
            value = valueObject
            if (value.length == 0) {
                value = defaultValue
            }
        }
        return value
    }

    @kotlin.jvm.JvmStatic
    fun addMediaToGallery(
        context: Context,
        photoPath: String?
    ) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val photoFile = File(photoPath)
        val contentUri = Uri.fromFile(photoFile)
        mediaScanIntent.data = contentUri
        context.sendBroadcast(mediaScanIntent)
    }

    @get:Throws(IOException::class)
    val imageFilePath: String
        get() = getFilePath(".jpg")

    @get:Throws(IOException::class)
    val videoFilePath: String
        get() = getFilePath(".mp4")

    private var fileDir: String? =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            .toString() + "/TopDefaultsCamera/"

    fun setFileDir(fileDir: String?) {
        Utils.fileDir = fileDir
    }

    @Throws(IOException::class)
    private fun getFilePath(fileSuffix: String): String {
        val dir = File(fileDir)
        if (!dir.exists()) {
            val result = dir.mkdirs()
            if (!result) {
                throw IOException(
                    exceptionMessage(
                        Error.ERROR_STORAGE,
                        "Unable to create folder"
                    )
                )
            }
        }
        return dir.absolutePath + "/" + System.currentTimeMillis() + fileSuffix
    }

    fun checkFloatEqual(a: Float, b: Float): Boolean {
        return Math.abs(a - b) < 0.001
    }

    private const val FOCUS_AREA_SIZE = 150
    fun calculateFocusArea(
        sensorArraySize: Rect?,
        displayOrientation: Int,
        textureView: TextureView?,
        eventX: Int,
        eventY: Int
    ): Rect {
        val focusX: Int
        val focusY: Int
        when (displayOrientation) {
            0 -> {
                focusX =
                    (eventX / textureView!!.width.toFloat() * sensorArraySize!!.width()
                        .toFloat()).toInt()
                focusY =
                    (eventY / textureView.height.toFloat() * sensorArraySize.height()
                        .toFloat()).toInt()
            }
            180 -> {
                focusX =
                    ((1 - eventX / textureView!!.width.toFloat()) * sensorArraySize!!.width()
                        .toFloat()).toInt()
                focusY =
                    ((1 - eventY / textureView.height.toFloat()) * sensorArraySize.height()
                        .toFloat()).toInt()
            }
            270 -> {
                focusX =
                    ((1 - eventY / textureView!!.height.toFloat()) * sensorArraySize!!.width()
                        .toFloat()).toInt()
                focusY =
                    (eventX / textureView.width.toFloat() * sensorArraySize.height()
                        .toFloat()).toInt()
            }
            90 -> {
                focusX =
                    (eventY / textureView!!.height.toFloat() * sensorArraySize!!.width()
                        .toFloat()).toInt()
                focusY =
                    ((1 - eventX / textureView.width.toFloat()) * sensorArraySize.height()
                        .toFloat()).toInt()
            }
            else -> {
                focusX =
                    (eventY / textureView!!.height.toFloat() * sensorArraySize!!.width()
                        .toFloat()).toInt()
                focusY =
                    ((1 - eventX / textureView.width.toFloat()) * sensorArraySize.height()
                        .toFloat()).toInt()
            }
        }
        val left = Math.max(focusX - FOCUS_AREA_SIZE, 0)
        val top = Math.max(focusY - FOCUS_AREA_SIZE, 0)
        val right = Math.min(
            left + FOCUS_AREA_SIZE * 2,
            sensorArraySize.width()
        )
        val bottom = Math.min(
            top + FOCUS_AREA_SIZE * 2,
            sensorArraySize.width()
        )
        return Rect(left, top, right, bottom)
    }

    fun getDisplayOrientation(activity: Activity, sensorOrientation: Int): Int {
        val rotation = activity.windowManager.defaultDisplay.rotation
        return getOrientation(
            sensorOrientation,
            rotation
        )
    }

    private const val SENSOR_ORIENTATION_DEFAULT_DEGREES = 90
    private const val SENSOR_ORIENTATION_INVERSE_DEGREES = 270
    private val DEFAULT_ORIENTATIONS = SparseIntArray()
    private val INVERSE_ORIENTATIONS = SparseIntArray()
    fun getOrientation(sensorOrientation: Int, displayRotation: Int): Int {
        var degree = DEFAULT_ORIENTATIONS[displayRotation]
        when (sensorOrientation) {
            SENSOR_ORIENTATION_DEFAULT_DEGREES -> degree =
                DEFAULT_ORIENTATIONS[displayRotation]
            SENSOR_ORIENTATION_INVERSE_DEGREES -> degree =
                INVERSE_ORIENTATIONS[displayRotation]
        }
        return degree
    }

    fun clamp(value: Float, min: Float, max: Float): Float {
        return if (value < min) {
            min
        } else if (value > max) {
            max
        } else {
            value
        }
    }

    @ColorInt
    fun adjustAlpha(@ColorInt color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor).toInt()
        val red: Int = Color.red(color)
        val green: Int = Color.green(color)
        val blue: Int = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    fun dpToPixel(context: Context? = Application(), dp: Float): Float {
        return (dp * context!!.resources.displayMetrics.density)
    }


    init {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90)
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0)
        DEFAULT_ORIENTATIONS.append(
            Surface.ROTATION_180,
            270
        )
        DEFAULT_ORIENTATIONS.append(
            Surface.ROTATION_270,
            180
        )
    }

    init {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270)
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180)
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90)
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0)
    }
}