package com.deepfine.camera

interface Values {
    companion object {
        const val DEBUG = false
        val DEFAULT_ASPECT_RATIO: AspectRatio =
            AspectRatio.of(4, 3)
        const val MODE_IMAGE = 0
        const val MODE_VIDEO = 1
        const val FLASH_OFF = 0
        const val FLASH_ON = 1
        const val FLASH_TORCH = 2
        const val FLASH_AUTO = 3
        const val FLASH_RED_EYE = 4
        const val FACING_BACK = 0
        const val FACING_FRONT = 1
        const val GRID_OFF = 0
        const val GRID_3X3 = 1
        const val GRID_4X4 = 2
        const val GRID_PHI = 3
    }
}