package com.deepfine.dfcamerademo;

import android.app.Application;

import top.defaults.view.TextButton;
import top.defaults.view.TextButtonEffect;

public class CameraApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        TextButton.Defaults defaults = TextButton.Defaults.get();
        defaults.set(R.styleable.TextButton_backgroundEffect, TextButtonEffect.BACKGROUND_EFFECT_RIPPLE);
        defaults.set(R.styleable.TextButton_rippleColor, 0xffff0000);
    }
}
