package com.hcilab.nkbminh.smiledetection_opencv;

/**
 * Created by NKBMinh on 9/12/2017.
 */

public class OpenCVNative {
    public native static String getTestMessage();

    public native static String getTestMessageNew();

    public native static int convertToGray(long addrMatRgba, long addrMatGray);

    public native static void faceDetection(long addrMatRgba);
}
