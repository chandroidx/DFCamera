package com.deepfine.camera

import android.R
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.annotation.NonNull
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat

/**
 * @Description GridModeButton
 * @author jh.kim (DEEP.FINE)
 * @since 2020/08/21
 * @version 1.0.0
 */

final class GridModeButton @JvmOverloads constructor(
    @NonNull context: Context,
    private val text: String,
    private val lineColor: Int? = 0x000000
) : AppCompatTextView(context) {
    private val drawable: GradientDrawable = GradientDrawable()

    init {
        showButtonAttrs(true)
    }


    fun hideButtonAttrs(selected: Boolean) {
        when (selected) {
            true -> {
                drawable.apply {
                    lineColor?.let {
                        this.setStroke(0, it)
                    }
                }
                this.setText("")
                this.background = drawable
            }
            false -> {
                visibility = View.INVISIBLE
            }

        }

    }

    fun showButtonAttrs(selected: Boolean) {
        when (selected) {
            true -> {
                drawable.apply {
                    lineColor?.let {
                        this.setStroke(1, it)
                    }
                }
                this.setText(text)
                this.background = drawable
            }
            false -> {
                visibility = View.VISIBLE
            }
        }
    }
///0xCC000000
    fun selected(selected: Boolean, bgColor: Int? = 0xCC000000.toInt()) {
        drawable.apply {
            lineColor?.let {
                setStroke(1, it)
            }
            if (!selected) setColor(ResourcesCompat.getColor(context.resources, R.color.transparent, null)) else  setColor(bgColor!!)
        }
    }
}

