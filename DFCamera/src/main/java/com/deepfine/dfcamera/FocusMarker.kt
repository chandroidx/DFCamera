package com.deepfine.dfcamera

import android.content.Context
import android.view.View
import android.view.ViewGroup

/**
 * @Description Interface
 * @author jh.kim (DEEP.FINE)
 * @since 2020/09/16
 * @version 1.0.0
 */

public interface FocusMarker {
    /**
     * 마커에 붙일 뷰.
     * @param context
     * @param container layoutInflate로 생성할 뷰 그룹
     */
    fun onAttach(context: Context, container: ViewGroup?): View?

    /**
     * 포커싱 시작
     */
    fun onAutoFocusStart()

    /**
     * 포커싱\ 종료
     */
    fun onAutoFocusEnd(successful: Boolean)

    /**
     * 초기화
     */
    fun clear()
}