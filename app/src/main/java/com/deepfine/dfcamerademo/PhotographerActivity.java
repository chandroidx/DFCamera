package com.deepfine.dfcamerademo;

import android.Manifest;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import com.deepfine.camera.CameraView;
import com.deepfine.camera.CanvasDrawer;
import com.deepfine.camera.Error;
import com.deepfine.camera.Grid;
import com.deepfine.camera.Photographer;
import com.deepfine.camera.PhotographerFactory;
import com.deepfine.camera.PhotographerHelper;
import com.deepfine.camera.SimpleOnEventListener;
import com.deepfine.camera.Size;
import com.deepfine.camera.Utils;
import com.deepfine.camera.Values;
import com.deepfine.dfcamerademo.dialog.PickerDialog;
import com.deepfine.dfcamerademo.dialog.SimplePickerDialog;
import com.deepfine.dfcamerademo.options.AspectRatioItem;
import com.deepfine.dfcamerademo.options.Commons;
import com.deepfine.dfcamerademo.options.SizeItem;
import com.tbruyelle.rxpermissions2.RxPermissions;

import top.defaults.view.TextButton;

public class PhotographerActivity extends AppCompatActivity {

    Photographer photographer;
    PhotographerHelper photographerHelper;
    private boolean isRecordingVideo;

    @BindView(R.id.preview) CameraView preview;
    @BindView(R.id.status) TextView statusTextView;

    @BindView(R.id.chooseSize)
    TextButton chooseSizeButton;
    @BindView(R.id.flash) TextButton flashTextButton;
    @BindView(R.id.exposure) TextButton exposrue;
    @BindView(R.id.flash_torch) ImageButton flashTorch;

    @BindView(R.id.switch_mode) TextButton switchButton;
    @BindView(R.id.action) ImageButton actionButton;
    @BindView(R.id.flip) ImageButton flipButton;

    @BindView(R.id.zoomValue) TextView zoomValueTextView;

    private int currentFlash = Values.FLASH_AUTO;

    private static final int[] FLASH_OPTIONS = {
            Values.FLASH_AUTO,
            Values.FLASH_OFF,
            Values.FLASH_ON,
    };

    private static final int[] FLASH_ICONS = {
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_on,
    };

    private static final int[] FLASH_TITLES = {
            R.string.flash_auto,
            R.string.flash_off,
            R.string.flash_on,
    };

    @OnClick(R.id.chooseRatio)
    void chooseRatio() {
        List<AspectRatioItem> supportedAspectRatios = Commons.wrapItems(photographer.getSupportedAspectRatios(), AspectRatioItem::new);
        if (supportedAspectRatios != null) {
            SimplePickerDialog<AspectRatioItem> dialog = SimplePickerDialog.create(new PickerDialog.ActionListener<AspectRatioItem>() {
                @Override
                public void onCancelClick(PickerDialog<AspectRatioItem> dialog) { }

                @Override
                public void onDoneClick(PickerDialog<AspectRatioItem> dialog) {
                    AspectRatioItem item = dialog.getSelectedItem(AspectRatioItem.class);
                    photographer.setAspectRatio(item.get());
                }
            });
            dialog.setItems(supportedAspectRatios);
            dialog.setInitialItem(Commons.findEqual(supportedAspectRatios, photographer.getAspectRatio()));
            dialog.show(getFragmentManager(), "aspectRatio");
        }
    }

    @OnClick(R.id.exposure)
    void chooseExposure() { photographer.setExposure(-1f); }

    @OnClick(R.id.exposure1)
    void chooseExposure1() {
        photographer.setExposure(-0.5f);
    }

    @OnClick(R.id.exposure2)
    void chooseExposure2() {
        photographer.setExposure(0f);
    }

    @OnClick(R.id.exposure3)
    void chooseExposure3() {
        photographer.setExposure(0.5f);
    }

    @OnClick(R.id.exposure4)
    void chooseExposure4() {
        preview.setEnableInterceptTouch(!preview.getEnableInterceptTouch());
    }

    @OnClick(R.id.zoom1)
    void chooseZoom1() {
        photographer.setZoom(1.0f);
    }

    @OnClick(R.id.zoom2)
    void chooseZoom2() {
        photographer.setZoom(2.0f);
    }

    @OnClick(R.id.zoom3)
    void chooseZoom3() {
        photographer.setZoom(3.0f);
    }

    @OnClick(R.id.zoom4)
    void chooseZoom4() {
        photographer.setZoom(4.0f);
    }


    @OnClick(R.id.chooseSize)
    void chooseSize() {
        Size selectedSize = null;
        List<SizeItem> supportedSizes = null;
        int mode = photographer.getMode();
        if (mode == Values.MODE_VIDEO) {
            Set<Size> videoSizes = photographer.getSupportedVideoSizes();
            selectedSize = photographer.getVideoSize();
            if (videoSizes != null && videoSizes.size() > 0) {
                supportedSizes = Commons.wrapItems(videoSizes, SizeItem::new);
            }
        } else if (mode == Values.MODE_IMAGE || mode == Values.MODE_GRID) {
            Set<Size> imageSizes = photographer.getSupportedImageSizes();
            selectedSize = photographer.getImageSize();
            if (imageSizes != null && imageSizes.size() > 0) {
                supportedSizes = Commons.wrapItems(imageSizes, SizeItem::new);
            }
        }

        if (supportedSizes != null) {
            SimplePickerDialog<SizeItem> dialog = SimplePickerDialog.create(new PickerDialog.ActionListener<SizeItem>() {
                @Override
                public void onCancelClick(PickerDialog<SizeItem> dialog) { }

                @Override
                public void onDoneClick(PickerDialog<SizeItem> dialog) {
                    SizeItem sizeItem = dialog.getSelectedItem(SizeItem.class);
                    if (mode == Values.MODE_VIDEO) {
                        photographer.setVideoSize(sizeItem.get());
                    } else {
                        photographer.setImageSize(sizeItem.get());
                    }
                }
            });
            dialog.setItems(supportedSizes);
            dialog.setInitialItem(Commons.findEqual(supportedSizes, selectedSize));
            dialog.show(getFragmentManager(), "cameraOutputSize");
        }
    }

    @OnCheckedChanged(R.id.fillSpace)
    void onFillSpaceChecked(boolean checked) {
        preview.setFocusGrid(Grid.DRAW_3X3);
    }

    @OnCheckedChanged(R.id.enableZoom)
    void onEnableZoomChecked(boolean checked) {
//        preview.setPinchToZoom(checked);
        photographerHelper.switchMode(Values.MODE_GRID);
    }

    @OnClick(R.id.flash)
    void flash() {
        currentFlash = (currentFlash + 1) % FLASH_OPTIONS.length;
        flashTextButton.setText(FLASH_TITLES[currentFlash]);
        flashTextButton.setCompoundDrawablesWithIntrinsicBounds(FLASH_ICONS[currentFlash], 0, 0, 0);
        photographer.setFlash(FLASH_OPTIONS[currentFlash]);
    }

    @OnClick(R.id.action)
    void action() {
        int mode = photographer.getMode();
        if (mode == Values.MODE_VIDEO) {
            if (isRecordingVideo) {
                finishRecordingIfNeeded();
            } else {
                isRecordingVideo = true;
                /*
                new Photographer.MediaRecorderConfigurator() {
                    @Override
                    public void configure(@org.jetbrains.annotations.Nullable MediaRecorder mediaRecorder) {
                        if (null != mediaRecorder) {
                            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//                            CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
//                            mediaRecorder.setVideoFrameRate(profile.videoFrameRate);
//                            mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
//                            mediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
//                            mediaRecorder.setAudioSamplingRate(profile.audioSampleRate);

                            mediaRecorder.setVideoFrameRate(30);
//                            mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
                            mediaRecorder.setAudioEncodingBitRate(384000);
                            mediaRecorder.setAudioSamplingRate(44100);
                            //                    mRecorder.setAudioSamplingRate(48000);
                        }
                    }

                    @Override
                    public boolean useDefaultConfigs() {
                        return false;
                    }
                }
                 */
                photographer.startRecording(null);
                actionButton.setEnabled(false);
            }
        } else if (mode == Values.MODE_IMAGE || mode == Values.MODE_GRID) {
            photographer.takePicture();
        }
    }

    @OnClick(R.id.flash_torch)
    void toggleFlashTorch() {
        int flash = photographer.getFlash();
        if (flash == Values.FLASH_TORCH) {
            photographer.setFlash(currentFlash);
            flashTextButton.setEnabled(true);
            flashTorch.setImageResource(R.drawable.light_off);
        } else {
            photographer.setFlash(Values.FLASH_TORCH);
            flashTextButton.setEnabled(false);
            flashTorch.setImageResource(R.drawable.light_on);
        }
    }

    @OnClick(R.id.switch_mode)
    void switchMode() {
        if (photographer.getMode() == Values.MODE_IMAGE) {
            photographerHelper.switchMode(Values.MODE_VIDEO);
        } else {
            photographerHelper.switchMode(Values.MODE_IMAGE);
        }

    }

    @OnClick(R.id.flip)
    void flip() {
        photographerHelper.flip();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_record);
        ButterKnife.bind(this);

        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions
                .request(Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe( granted -> {
                    initView(granted);
                });
    }

    private void initView(Boolean isGranted) {
        if (!isGranted) { return; }

        preview.setFocusIndicatorDrawer(new CanvasDrawer() {
            private static final int SIZE = 300;
            private static final int LINE_LENGTH = 50;

            @Override
            public Paint[] initPaints() {
                Paint focusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                focusPaint.setStyle(Paint.Style.STROKE);
                focusPaint.setStrokeWidth(2);
                focusPaint.setColor(Color.WHITE);
                return new Paint[]{ focusPaint };
            }

            @Override
            public void draw(Canvas canvas, Point point, Paint[] paints) {
                if (paints == null || paints.length == 0) return;

                int left = point.x - (SIZE / 2);
                int top = point.y - (SIZE / 2);
                int right = point.x + (SIZE / 2);
                int bottom = point.y + (SIZE / 2);

                Paint paint = paints[0];

                canvas.drawLine(left, top + LINE_LENGTH, left, top, paint);
                canvas.drawLine(left, top, left + LINE_LENGTH, top, paint);

                canvas.drawLine(right - LINE_LENGTH, top, right, top, paint);
                canvas.drawLine(right, top, right, top + LINE_LENGTH, paint);

                canvas.drawLine(right, bottom - LINE_LENGTH, right, bottom, paint);
                canvas.drawLine(right, bottom, right - LINE_LENGTH, bottom, paint);

                canvas.drawLine(left + LINE_LENGTH, bottom, left, bottom, paint);
                canvas.drawLine(left, bottom, left, bottom - LINE_LENGTH, paint);
            }
        });
        photographer = PhotographerFactory.createPhotographerWithCamera2(this, preview);
        photographerHelper = new PhotographerHelper(photographer);
        photographerHelper.setFileDir(Commons.MEDIA_DIR);
        photographer.setOnEventListener(new SimpleOnEventListener() {
            @Override
            public void onDeviceConfigured() {
                if (photographer.getMode() == Values.MODE_VIDEO) {
                    actionButton.setImageResource(R.drawable.record);
                    chooseSizeButton.setText(R.string.video_size);
                    switchButton.setText(R.string.video_mode);
                } else {
                    actionButton.setImageResource(R.drawable.ic_camera);
                    chooseSizeButton.setText(R.string.image_size);
                    switchButton.setText(R.string.image_mode);
                }
            }

            @Override
            public void onZoomChanged(float zoom) {
                zoomValueTextView.setText(String.format(Locale.getDefault(), "%.1fX", zoom));
            }

            @Override
            public void onStartRecording() {
                switchButton.setVisibility(View.INVISIBLE);
                flipButton.setVisibility(View.INVISIBLE);
                actionButton.setEnabled(true);
                actionButton.setImageResource(R.drawable.stop);
                statusTextView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFinishRecording(String filePath) {
                announcingNewFile(filePath);
            }

            @Override
            public void onShotFinished(String filePath) {
                announcingNewFile(filePath);
            }

            @Override
            public void onSelectedGridCount(int count) {

            }

            @Override
            public void onError(Error error) {
                Log.e("Error happens: %s", error.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterFullscreen();
        photographer.startPreview();
    }

    @Override
    protected void onPause() {
        finishRecordingIfNeeded();
        photographer.stopPreview();
        super.onPause();
    }

    private void enterFullscreen() {
        View decorView = getWindow().getDecorView();
        decorView.setBackgroundColor(Color.BLACK);
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void finishRecordingIfNeeded() {
        if (isRecordingVideo) {
            isRecordingVideo = false;
            photographer.finishRecording();
            statusTextView.setVisibility(View.INVISIBLE);
            switchButton.setVisibility(View.VISIBLE);
            flipButton.setVisibility(View.VISIBLE);
            actionButton.setEnabled(true);
            actionButton.setImageResource(R.drawable.record);
        }
    }

    private void announcingNewFile(String filePath) {
        Toast.makeText(PhotographerActivity.this, "File: " + filePath, Toast.LENGTH_SHORT).show();
        Utils.addMediaToGallery(PhotographerActivity.this, filePath);
    }
}
