package com.deepfine.camera

import android.app.Activity

interface InternalPhotographer : Photographer {
    fun initWithViewfinder(activity: Activity, preview: CameraView)
}