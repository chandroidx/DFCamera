package com.deepfine.camera

import android.app.Activity

object PhotographerFactory {
    @kotlin.jvm.JvmStatic
    fun createPhotographerWithCamera2(activity: Activity, preview: CameraView): Photographer {
        val photographer: InternalPhotographer =
            Camera2Photographer()
        photographer.initWithViewfinder(activity, preview)
        preview.assign(photographer)
        return photographer
    }
}