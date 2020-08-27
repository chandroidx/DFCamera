/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.deepfine.camera

import androidx.collection.ArrayMap
import java.util.*

/**
 * AspectRatio 및 Size를 묶어서 제공하는 클래스
 */
internal class SizeMap {
    private val mRatios =
        ArrayMap<AspectRatio, SortedSet<Size>>()

    /**
     * 해당 컬렉션에 새로운 [Size] 정보 add
     *
     * @param size The size to add
     * @return `true` if it is added / `false` if it already exists and is not added.
     */
    fun add(size: Size): Boolean {
        for (ratio in mRatios.keys) {
            if (ratio.matches(size)) {
                val sizes = mRatios[ratio]
                return if (sizes!!.contains(size)) {
                    false
                } else {
                    sizes.add(size)
                    true
                }
            }
        }

        // None of the existing ratio matches the provided size; add a new key
        val sizes: SortedSet<Size> =
            TreeSet()
        sizes.add(size)
        mRatios[AspectRatio.of(
            size.width,
            size.height
        )] = sizes
        return true
    }

    /**
     * 특정 ratio를 제거
     *
     * @param ratio The aspect ratio to be removed.
     */
    fun remove(ratio: AspectRatio?) {
        mRatios.remove(ratio)
    }

    fun ratios(): Set<AspectRatio> {
        return mRatios.keys
    }

    fun sizes(ratio: AspectRatio?): SortedSet<Size>? {
        return mRatios[ratio]
    }

    fun defaultSize(): Size {
        return mRatios[AspectRatio.parse("4:3")]!!.last()
    }

    fun clear() {
        mRatios.clear()
    }

    val isEmpty: Boolean
        get() = mRatios.isEmpty
}