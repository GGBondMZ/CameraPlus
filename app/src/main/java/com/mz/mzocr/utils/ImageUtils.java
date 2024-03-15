package com.mz.mzocr.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageUtils {

    private static final String TAG = "MZOCR_ImageUtils";

    public static Bitmap readBitmapFromFile(String filePath, int size) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        float srcWidth = options.outWidth;
        float srcHeight = options.outHeight;
        int inSampleSize = 1;

        if (srcHeight > size || srcWidth > size) {
            if (srcWidth < srcHeight) {
                inSampleSize = Math.round(srcHeight / size);
            } else {
                inSampleSize = Math.round(srcWidth / size);
            }
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;
        return BitmapFactory.decodeFile(filePath, options);
    }

}
