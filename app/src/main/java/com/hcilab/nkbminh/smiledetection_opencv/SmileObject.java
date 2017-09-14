package com.hcilab.nkbminh.smiledetection_opencv;

import org.opencv.core.Rect;
import org.opencv.core.Scalar;

/**
 * Created by NKBMinh on 9/14/2017.
 */

public class SmileObject {
    private static final Scalar SMILE_COLOR = new Scalar(0, 255, 0, 255);
    private static final Scalar NON_SMILE_COLOR = new Scalar(255, 0, 0, 255);
    private static final float SMILE_THRESHOLD = 0.55f;

    public boolean getIsSmile() {
        return mIsSmile;
    }

    private boolean mIsSmile;

    public Rect getRect() {
        return mRect;
    }

    private Rect mRect;

    public int getScore() {
        return mScore;
    }

    public void setScore(float score) {
        if(score <= SMILE_THRESHOLD)
        {
            this.mScore = Math.round((score - 0.5f)*100);
            mIsSmile = false;
        }
        else
        {
            this.mScore = Math.round(score*100);
            mIsSmile = true;
        }

    }

    private int mScore;

    public SmileObject(Rect rect, float score)
    {
        mRect = rect;
        setScore(score);
    }

    public Scalar getColor()
    {
        if(mIsSmile)
            return SMILE_COLOR;
        else
            return NON_SMILE_COLOR;
    }
}
