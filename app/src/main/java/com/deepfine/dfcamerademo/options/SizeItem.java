package com.deepfine.dfcamerademo.options;

import com.deepfine.camera.Size;

public class SizeItem implements PickerItemWrapper<Size> {

    private Size size;

    public SizeItem(Size size) {
        this.size = size;
    }

    @Override
    public String getText() {
        return size.getWidth() + " * " + size.getHeight();
    }

    @Override
    public Size get() {
        return size;
    }
}
