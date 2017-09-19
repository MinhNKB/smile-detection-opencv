package com.hcilab.nkbminh.smiledetection_opencv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class ResultActivity extends AppCompatActivity {

    private static final Scalar TEXT_COLOR = new Scalar(255, 255, 255, 255);

    ImageView mImageView;
    Bitmap mBitmap;
    int mCameraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        mImageView = (ImageView) this.findViewById(R.id.imageView);

        Bundle bundle = getIntent().getExtras();
        byte[] bitmapBytes = bundle.getByteArray("image");
        mCameraId = bundle.getInt("cameraId");



        Mat imageMat = Imgcodecs.imdecode(new MatOfByte(bitmapBytes), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
        Mat grayImageMat = new Mat(imageMat.width(), imageMat.height(), CvType.CV_8UC1);

        if(mCameraId == 1) {
            Core.rotate(imageMat, imageMat, 2);
            Core.flip(imageMat, imageMat, 1);

        }
        else
        {
            Core.rotate(imageMat, imageMat, 0);
        }

        Imgproc.cvtColor(imageMat, grayImageMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2RGB);


        List<SmileObject> smileObjects = SmileDetector.detectSmile(grayImageMat);

        if(smileObjects != null) {
            for (int i = 0; i < smileObjects.size(); i++) {
                Rect rect = smileObjects.get(i).getRect();
                Scalar color = smileObjects.get(i).getColor();

                String score = "P:" + String.valueOf(smileObjects.get(i).getScore());

                Imgproc.rectangle(imageMat, rect.tl(), rect.br(), color, 3);
                Imgproc.putText(imageMat, score, rect.tl(), Core.FONT_HERSHEY_SIMPLEX, 0.75f, TEXT_COLOR, 2);
            }
        }


        mBitmap = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageMat, mBitmap);
        mImageView.setImageBitmap(mBitmap);

        imageMat.release();
        grayImageMat.release();
    }
}
