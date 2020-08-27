package com.deepfine.camera

import java.util.*
import kotlin.Error

class Error : Error {
    private var code: Int
    override var cause: Throwable? = null

    internal constructor(code: Int) : super(
        messageFor(
            code
        )
    ) {
        this.code = code
    }

    internal constructor(code: Int, cause: Throwable) : super(cause.message) {
        this.code = code
        this.cause = cause
        cause.printStackTrace()
    }

    internal constructor(code: Int, message: String?) : super(message) {
        this.code = code
    }

    internal constructor(code: Int, message: String?, cause: Throwable) : super(
        message
    ) {
        this.code = code
        this.cause = cause
        cause.printStackTrace()
    }

    override fun toString(): String {
        return String.format(Locale.getDefault(), "%s(%d)", message, code)
    }

    companion object {
        const val ERROR_DEFAULT_CODE = -1
        const val ERROR_CAMERA = 1
        const val ERROR_UNSUPPORTED_OPERATION = 2
        const val ERROR_PERMISSION = 3
        const val ERROR_STORAGE = 4
        const val ERROR_INVALID_PARAM = 5
        private fun messageFor(code: Int): String {
            val message: String
            message = when (code) {
                ERROR_CAMERA -> "Camera error"
                ERROR_UNSUPPORTED_OPERATION -> "Unsupported operation"
                ERROR_PERMISSION -> "No enough permissions"
                ERROR_STORAGE -> "No enough storage"
                else -> "Undefined error"
            }
            return message
        }
    }
}