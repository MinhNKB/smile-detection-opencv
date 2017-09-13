package com.hcilab.nkbminh.smiledetection_opencv;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    private static final String TAG = "CameraActivity";

    JavaCameraView mCameraPreview;
    CascadeClassifier mFaceDetector;
    File mCascadeFile;
    Mat mRGBA;
    Mat mGray;

    private float mRelativeFaceSize   = 0.2f;
    private int mAbsoluteFaceSize   = 0;
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);

    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case BaseLoaderCallback.SUCCESS:
                {
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
        System.loadLibrary("OpenCVNativeLib");

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

        mCameraPreview = (JavaCameraView)findViewById(R.id.camera_preview);
        mCameraPreview.setVisibility(SurfaceView.VISIBLE);
        mCameraPreview.setCvCameraViewListener(this);

        // Example of a call to a native method
//        TextView tv = (TextView) findViewById(R.id.sample_text);
//        tv.setText(stringFromJNI());
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

        Core.rotate(mGray, tMatGray, 2);
        Core.rotate(mRGBA, tMatRGB, 2);

        if (mAbsoluteFaceSize == 0) {
            int height = tMatGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        MatOfRect faces = new MatOfRect();
        if (mFaceDetector != null)
            mFaceDetector.detectMultiScale(tMatGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                    new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());

        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++)
            Imgproc.rectangle(tMatRGB, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);

        Core.rotate(tMatRGB, mRGBA, 0);
        tMatGray.release();
        tMatRGB.release();

        return mRGBA;
    }
}

