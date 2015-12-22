package com.dancedeets.android.util;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

/**
 * This is a Drawable that enforces a certain size, for layout purposes.
 * It's used as a no-op by PlaceholderNetworkImageView before we have actual image data.
 */
public class PendingBitmapDrawable extends Drawable {

    private int mWidth;
    private int mHeight;

    public PendingBitmapDrawable(int maxWidth, int maxHeight, int fullWidth, int fullHeight) {
        // Then compute the dimensions we would ideally like to decode to.
        int desiredWidth = getResizedDimension(maxWidth, maxHeight,
                fullWidth, fullHeight);
        int desiredHeight = getResizedDimension(maxHeight, maxWidth,
                fullHeight, fullWidth);
        if (fullWidth > desiredWidth) {
            mWidth = desiredWidth;
        } else {
            mWidth = fullWidth;
        }
        if (fullHeight > desiredHeight) {
            mHeight = desiredHeight;
        } else {
            mHeight = fullHeight;
        }
    }

    @Override
    public void draw(Canvas canvas) {

    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public int getIntrinsicWidth() {
        if (mWidth != 0) {
            return mWidth;
        } else {
            return super.getIntrinsicWidth();
        }
    }

    @Override
    public int getIntrinsicHeight() {
        if (mHeight != 0) {
            return mHeight;
        } else {
            return super.getIntrinsicHeight();
        }
    }


    /**
     * Scales one side of a rectangle to fit aspect ratio.
     *
     * @param maxPrimary Maximum size of the primary dimension (i.e. width for
     *        max width), or zero to maintain aspect ratio with secondary
     *        dimension
     * @param maxSecondary Maximum size of the secondary dimension, or zero to
     *        maintain aspect ratio with primary dimension
     * @param actualPrimary Actual size of the primary dimension
     * @param actualSecondary Actual size of the secondary dimension
     */
    private static int getResizedDimension(int maxPrimary, int maxSecondary, int actualPrimary,
                                           int actualSecondary) {
        // If no dominant value at all, just return the actual.
        if (maxPrimary == 0 && maxSecondary == 0) {
            return actualPrimary;
        }

        // If primary is unspecified, scale primary to match secondary's scaling ratio.
        if (maxPrimary == 0) {
            double ratio = (double) maxSecondary / (double) actualSecondary;
            return (int) (actualPrimary * ratio);
        }

        if (maxSecondary == 0) {
            return maxPrimary;
        }

        double ratio = (double) actualSecondary / (double) actualPrimary;
        int resized = maxPrimary;
        if (resized * ratio > maxSecondary) {
            resized = (int) (maxSecondary / ratio);
        }
        return resized;
    }

}
