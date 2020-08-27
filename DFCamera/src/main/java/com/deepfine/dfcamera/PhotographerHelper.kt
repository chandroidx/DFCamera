package com.deepfine.camera

class PhotographerHelper(private val photographer: Photographer) {
    fun switchMode() {
        val newMode: Int =
            if (photographer.mode == Values.MODE_IMAGE) Values.MODE_VIDEO else Values.MODE_IMAGE
        photographer.mode = newMode
    }

    fun flip() {
        val newFacing: Int =
            if (photographer.facing == Values.FACING_BACK) Values.FACING_FRONT else Values.FACING_BACK
        photographer.facing = newFacing
    }

    fun setFileDir(fileDir: String?) {
        Utils.setFileDir(fileDir)
    }

}