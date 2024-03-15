package com.mz.mzocr;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MZOCR_MainActivity";
    private Context mContext;
    private Button ocrBtn;
    private Button scanBtn;

    static class Configuration {
        public static final String TAG = "CameraxBasic";
        public static final String FILENAME_FORMAT = "yyyy-MM-dd_HH-mm-ss";
        public static final int REQUEST_CODE_PERMISSIONS = 10;
        public static final String[] REQUIRED_PERMISSIONS = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,};
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();

        // 请求相机权限
        if (allPermissionsGranted()) {
            Log.d(TAG, "Permissions Granted Success ~");
        } else {
            ActivityCompat.requestPermissions(this, MainActivity.Configuration.REQUIRED_PERMISSIONS, MainActivity.Configuration.REQUEST_CODE_PERMISSIONS);
        }

        initView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MainActivity.Configuration.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {// 申请权限通过
                // startCamera();
            } else {// 申请权限失败
                Log.d(TAG, "Permissions Granted Failed ~");
                Toast.makeText(this, "用户拒绝授予权限！", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : MainActivity.Configuration.REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    private void initView() {
        ocrBtn = findViewById(R.id.ocrBtn);
        ocrBtn.setOnClickListener(v -> startOcrActivity());

        scanBtn = findViewById(R.id.scanBtn);
        scanBtn.setOnClickListener(v -> startScanActivity());
    }

    private void startOcrActivity() {
        Log.d(TAG, "startOcrActivity...");
        Intent intent = new Intent();
        intent.setClassName("com.mz.mzocr", "com.mz.mzocr.OcrActivity");
        startActivity(intent);
    }

    private void startScanActivity() {
        Log.d(TAG, "startScanActivity...");
        Intent intent = new Intent();
        intent.setClassName("com.mz.mzocr", "com.mz.mzocr.ScanActivity");
        startActivity(intent);
    }

}