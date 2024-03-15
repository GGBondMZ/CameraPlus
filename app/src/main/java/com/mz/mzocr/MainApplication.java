package com.mz.mzocr;

import android.app.Application;
import android.util.Log;

import com.youdao.sdk.app.YouDaoApplication;

public class MainApplication extends Application {

    private static final String TAG = "MZOCR_MainApplication";
    private static MainApplication swYouAppction;

    @Override
    public void onCreate() {
        super.onCreate();
        YouDaoApplication.init(this, "0a1241e617e06d5b");
        swYouAppction = this;
        Log.d(TAG, "init OCR Application Success ~");
    }

    public static MainApplication getInstance() {
        return swYouAppction;
    }
}
