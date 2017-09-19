package com.hcilab.nkbminh.smiledetection_opencv;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;

import java.io.FileOutputStream;
import java.util.List;

/**
 * Created by NKBMinh on 9/14/2017.
 */

public class CustomJavaCameraView extends JavaCameraView implements Camera.PictureCallback {

    private static final String TAG = "CustomJavaCameraView";

    public CustomJavaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public List<String> getEffectList() {
        return mCamera.getParameters().getSupportedColorEffects();
    }

    public boolean isEffectSupported() {
        return (mCamera.getParameters().getColorEffect() != null);
    }

    public String getEffect() {
        return mCamera.getParameters().getColorEffect();
    }

    public void setEffect(String effect) {
        Camera.Parameters params = mCamera.getParameters();
        params.setColorEffect(effect);
        mCamera.setParameters(params);
    }

    public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void takePicture() {
        Log.i(TAG, "Taking picture");

        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        mFrameWidth = (int) sizes.get(0).width;
        mFrameHeight = (int) sizes.get(0).height;

        params.setPictureSize(mFrameWidth, mFrameHeight);
        mCamera.setParameters(params);

        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);


        Intent intent = new Intent(getContext(), ResultActivity.class);
        intent.putExtra("image", data);
        intent.putExtra("cameraId", mCameraIndex);
        getContext().startActivity(intent);
    }
}
