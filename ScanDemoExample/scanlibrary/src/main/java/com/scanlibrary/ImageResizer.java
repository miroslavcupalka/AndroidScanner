package com.scanlibrary;

import android.graphics.Bitmap;

import com.scanlibrary.ScalingUtilities.ScalingLogic;

import java.io.IOException;

public class ImageResizer {

    // ===========================================================
    // Constants
    // ===========================================================

    // ===========================================================
    // Fields
    // ===========================================================

    // ===========================================================
    // Constructors
    // ===========================================================

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    // ===========================================================
    // Methods
    // ===========================================================

    /**
     * @return full image path
     */
    public static Bitmap resizeImage(Bitmap unscaledBitmap, int desiredWidth, int desiredHeight) throws IOException {

        try {
            // Part 1: Decode image
            if (!(unscaledBitmap.getWidth() <= desiredWidth && unscaledBitmap.getHeight() <= desiredHeight)) {
                // Part 2: Scale image
                Bitmap scaledBitmap = ScalingUtilities.createScaledBitmap(unscaledBitmap, desiredWidth, desiredHeight, ScalingLogic.FIT);
                unscaledBitmap.recycle();
                return scaledBitmap;
            } else {

                return unscaledBitmap;
            }

        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

}
