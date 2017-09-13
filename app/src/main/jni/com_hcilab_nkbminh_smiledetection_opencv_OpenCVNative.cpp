#include "com_hcilab_nkbminh_smiledetection_opencv_OpenCVNative.h"

/*
 * Class:     com_hcilab_nkbminh_smiledetection_opencv_OpenCVNative
 * Method:    getTestMessage
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_hcilab_nkbminh_smiledetection_1opencv_OpenCVNative_getTestMessage
  (JNIEnv *env, jclass){
    return env->NewStringUTF("NEWWWW Hello world fron JNI!! MinhNKB");
}

JNIEXPORT jstring JNICALL Java_com_hcilab_nkbminh_smiledetection_1opencv_OpenCVNative_getTestMessageNew
        (JNIEnv *env, jclass){
    return env->NewStringUTF("SUPERRR NEWWWW Hello world fron JNI!! MinhNKB");
}


JNIEXPORT jint JNICALL Java_com_hcilab_nkbminh_smiledetection_1opencv_OpenCVNative_convertToGray
        (JNIEnv *env, jclass obj, jlong addrRgba, jlong addrGray){
    Mat& mRgb = *(Mat*)addrRgba;
    Mat& mGray = *(Mat*)addrGray;

    int conv;
    jint retVal;
    conv = toGray(mRgb, mGray);

    retVal = (jint) conv;
    return retVal;
}


int toGray(Mat img, Mat &gray)
{
    cvtColor(img, gray, CV_RGBA2GRAY);
    if(gray.rows == img.rows && gray.cols == img.cols)
        return 1;

    return 0;
}

