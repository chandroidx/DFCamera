package com.deepfine.camera

class PhotographerHelper(private val photographer: Photographer) {
    fun switchMode(mode: Int? = Values.MODE_IMAGE) {
        if (photographer.mode == mode) { return }
        photographer.mode = mode!!
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