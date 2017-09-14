package com.hcilab.nkbminh.smiledetection_opencv;

/**
 * Created by NKBMinh on 9/14/2017.
 */
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SmileDetector {
    private static final String TAG = "SmileDetector";
    private static final String MODEL_FILE = "file:///android_asset/optimized_tfdroid.pb";

    private static TensorFlowInferenceInterface mInferenceInterface;

    private static CascadeClassifier mFaceDetector;
    private static File mCascadeFile;
    private static float mRelativeFaceSize   = 0.2f;
    private static int mAbsoluteFaceSize   = 0;

    public static void initialize(Context context)
    {
        mInferenceInterface = new TensorFlowInferenceInterface();
        mInferenceInterface.initializeTensorFlow(context.getAssets(), MODEL_FILE);

        try {
            InputStream is = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
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
    }

    public static List<SmileObject> detectSmile(Mat matGray)
    {
        List<SmileObject> result = new ArrayList<SmileObject>();

        if (mAbsoluteFaceSize == 0) {
            int height = matGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        MatOfRect faces = new MatOfRect();
        if (mFaceDetector != null)
            mFaceDetector.detectMultiScale(matGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                    new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());

        Rect[] facesArray = faces.toArray();

        for (int i = 0; i < facesArray.length; i++) {
            Rect faceRect = facesArray[i];
            Mat faceMat = new Mat(matGray, faceRect);
            Mat resizedFaceMat = new Mat(64, 64, CvType.CV_8UC1);

            Imgproc.resize(faceMat, resizedFaceMat, resizedFaceMat.size(), 0, 0, Imgproc.INTER_CUBIC);

            byte[] faceBytes = new byte[resizedFaceMat.height() * resizedFaceMat.width()];
            resizedFaceMat.get(0, 0, faceBytes);


            float[] normalizeFace = new float[64 * 64];
            float mean = 0.5037291613f;
            float std = 0.17248591f;
            for (int j = 0; j < 64 * 64; j++) {
                float b = faceBytes[j];
                b = ((b + 128) / 255 - mean) / std;
                normalizeFace[j] = b;
            }
            String INPUT_NODE = "I";
            String OUTPUT_NODE = "O";
            int[] INPUT_SIZE = {1, 64, 64, 1};

            mInferenceInterface.fillNodeFloat(INPUT_NODE, INPUT_SIZE, normalizeFace);

            mInferenceInterface.runInference(new String[]{OUTPUT_NODE});
            float[] smilePrediction = {-1};
            mInferenceInterface.readNodeFloat(OUTPUT_NODE, smilePrediction);
            float predictScore = smilePrediction[0];

            SmileObject smileObject = new SmileObject(faceRect, predictScore);
            result.add(smileObject);
        }

        return result;
    }

}
