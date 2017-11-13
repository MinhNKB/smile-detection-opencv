package com.hcilab.nkbminh.smiledetection_opencv;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private int mCameraId = 0;
    CustomJavaCameraView mCameraPreview;
    CascadeClassifier mFaceDetector;
    File mCascadeFile;
    Mat mRGBA;
    Mat mGray;
    Mat mGrayT;
    private static final Scalar TEXT_COLOR = new Scalar(255, 255, 255, 255);

    List<SmileObject> mSmileObjects = null;
    boolean mIsRunning = false;
    boolean mAutoCapture = false;

    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case BaseLoaderCallback.SUCCESS:
                {
                    // Add permission for camera and let user grant the permission
                    if (ActivityCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                        return;
                    }

                    try {
                        InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mFaceDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mFaceDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mFaceDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        cascadeDir.delete();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mCameraPreview.enableView();
                    break;
                }
                default:{
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
//        System.loadLibrary("OpenCVNativeLib");
        System.loadLibrary("tensorflow_inference");

        if(OpenCVLoader.initDebug())
        {
            Log.d(TAG, "OpenCV loaded");
        }
        else
        {
            Log.d(TAG, "OpenCV load failed");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mCameraPreview = (CustomJavaCameraView)findViewById(R.id.camera_preview);
        mCameraPreview.setVisibility(SurfaceView.VISIBLE);
        mCameraPreview.setCvCameraViewListener(this);

        SmileDetector.initialize(this);

        mCameraPreview.setCameraIndex(mCameraId);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(CameraActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause(){
        super.onPause();

        if(mCameraPreview != null)
        {
            mCameraPreview.disableView();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(mCameraPreview != null)
        {
            mCameraPreview.disableView();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mSmileObjects = null;

        if(OpenCVLoader.initDebug())
        {
            Log.i(TAG, "OpenCV loaded");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else
        {
            Log.i(TAG, "OpenCV load failed");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mLoaderCallback);
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
        mGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();
        mGray = inputFrame.gray();
        Mat tMatGray = new Mat(mGray.width(), mGray.height(), CvType.CV_8UC1);
        Mat tMatRGB = new Mat(mRGBA.width(), mRGBA.height(), CvType.CV_8UC3);

        if(mCameraId == 1) {
            Core.rotate(mRGBA, tMatRGB, 2);
            Core.flip(tMatRGB, tMatRGB, 1);
        }
        else
        {
            Core.rotate(mRGBA, tMatRGB, 0);
        }


        if (!mIsRunning) {
            if(mCameraId == 1) {
                Core.rotate(mGray, tMatGray, 2);
                Core.flip(tMatGray, tMatGray, 1);
            }
            else
            {
                Core.rotate(mGray, tMatGray, 0);
            }

            mIsRunning = true;
            mGrayT = tMatGray;
            new Thread(new Runnable() {
                public void run() {
                    mSmileObjects = SmileDetector.detectSmile(mGrayT);

                    if(mAutoCapture) {
                        boolean allSmile = false;
                        if (mSmileObjects != null) {
                            if (mSmileObjects.size() > 0) {
                                allSmile = true;
                            }
                            for (int i = 0; i < mSmileObjects.size(); i++) {
                                if (!mSmileObjects.get(i).getIsSmile())
                                    allSmile = false;
                            }
                        }
                        if (allSmile) {
                            mAutoCapture = false;
                            mCameraPreview.takePicture();
                        }
                    }
                    mGrayT.release();
                    mIsRunning = false;
                }
            }).start();
        }
        else
        {
            tMatGray.release();
        }

        if(mSmileObjects != null) {
            for (int i = 0; i < mSmileObjects.size(); i++) {
                Rect rect = mSmileObjects.get(i).getRect();
                Scalar color = mSmileObjects.get(i).getColor();

                String score = "P:" + String.valueOf(mSmileObjects.get(i).getScore());

                if(mCameraId == 1) {
                    Imgproc.rectangle(tMatRGB, rect.tl(), rect.br(), color, 3);
                    Imgproc.putText(tMatRGB, score, rect.tl(), Core.FONT_HERSHEY_SIMPLEX, 0.75f, TEXT_COLOR, 2);
                }
                else {
                    Imgproc.rectangle(tMatRGB, rect.tl(), rect.br(), color, 3);
                    Imgproc.putText(tMatRGB, score, rect.tl(), Core.FONT_HERSHEY_SIMPLEX, 0.75f, TEXT_COLOR, 2);
                }
            }
        }

        if(mCameraId == 1) {
            Core.flip(tMatRGB, tMatRGB, 1);
            Core.rotate(tMatRGB, mRGBA, 0);
        }
        else
        {
            Core.rotate(tMatRGB, mRGBA, 2);
        }

        tMatRGB.release();

        return mRGBA;
    }

    private void swapCamera()
    {
        mCameraPreview.disableView();
        mCameraId = mCameraId^1; //bitwise not operation to flip 1 to 0 and vice versa
        mCameraPreview.setCameraIndex(mCameraId);
        mCameraPreview.enableView();
    }

    public void onChangeCamera(View view) {
        swapCamera();
    }

    public void onCapture(View view) {
        mCameraPreview.takePicture();
    }

    public void onAutoCapture(View view) {
        if(!mAutoCapture) {
            Toast.makeText(CameraActivity.this, "Auto detect smile and capture images is ON", Toast.LENGTH_SHORT).show();
            mAutoCapture = true;
        }
        else
        {
            Toast.makeText(CameraActivity.this, "Auto detect smile and capture images is OFF", Toast.LENGTH_SHORT).show();
            mAutoCapture = false;
        }
    }
}

