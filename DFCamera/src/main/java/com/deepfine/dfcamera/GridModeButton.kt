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
    private val lineColor: Int? = 0x000000,
    private val bgColor: Int = 0xCC000000.toInt()
) : AppCompatTextView(context) {
    private val showDrawable: GradientDrawable = GradientDrawable().apply {
        lineColor?.let {
            this.setStroke(1, it)
        }
    }
    private val hideDrawable: GradientDrawable = GradientDrawable().apply{
        setColor(bgColor)
    }

    init {
        showButtonAttrs(true)
    }


    fun hideButtonAttrs(selected: Boolean) {
        when (selected) {
            true -> {
                this.setText("")
                this.background = hideDrawable
            }
            false -> {
                visibility = View.INVISIBLE
            }

        }

    }

    fun showButtonAttrs(selected: Boolean) {
        when (selected) {
            true -> {
                this.setText(text)
                this.background = showDrawable
            }
            false -> {
                visibility = View.VISIBLE
            }
        }
    }
///0xCC000000
    fun selected(selected: Boolean) {
        showDrawable.apply {
            if (!selected) setColor(ResourcesCompat.getColor(context.resources, R.color.transparent, null)) else setColor(bgColor)
        }
    }
}

