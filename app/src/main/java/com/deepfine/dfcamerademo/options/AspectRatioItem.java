package com.deepfine.dfcamerademo.options;

import com.deepfine.camera.AspectRatio;

public class AspectRatioItem implements PickerItemWrapper<AspectRatio> {

    private AspectRatio aspectRatio;

    public AspectRatioItem(AspectRatio ratio) {
        aspectRatio = ratio;
    }

    @Override
    public String getText() {
        return aspectRatio.toString();
    }

    @Override
    public AspectRatio get() {
        return aspectRatio;
    }
}
