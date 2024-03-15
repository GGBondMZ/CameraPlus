package com.mz.mzocr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanActivity extends AppCompatActivity {

    private static final String TAG = "MZOCR_ScanActivity";

    private Context mContext;

    private ImageCapture imageCapture;
    private File outputDirectory;
    private ExecutorService cameraExecutor;

    private ImageButton img_exit;

    private Handler handler = new Handler();

    private BarcodeScanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        mContext = getApplicationContext();

        // 设置拍照按钮监听
        img_exit = findViewById(R.id.img_exit);
        img_exit.setOnClickListener(v -> takePhoto());

        // 设置照片等保存的位置
        outputDirectory = getOutputDirectory();
        cameraExecutor = Executors.newSingleThreadExecutor();

        initScanner();

    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private void initScanner() {
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_QR_CODE,
                                Barcode.FORMAT_AZTEC)
                        .build();

        scanner = BarcodeScanning.getClient();
    }

    private void startCamera() {
        // 将Camera的生命周期和Activity绑定在一起（设定生命周期所有者），这样就不用手动控制相机的启动和关闭。
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // 将你的相机和当前生命周期的所有者绑定所需的对象
                ProcessCameraProvider processCameraProvider = cameraProviderFuture.get();

                // 创建一个Preview 实例，并设置该实例的 surface 提供者（provider）。
                PreviewView viewFinder = (PreviewView) findViewById(R.id.viewFinder);
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // 选择后置摄像头作为默认摄像头
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // 创建拍照所需的实例
                imageCapture = new ImageCapture.Builder().build();

                // 设置预览帧分析
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
                imageAnalysis.setAnalyzer(cameraExecutor, new MyAnalyzer());

                // 重新绑定用例前先解绑
                processCameraProvider.unbindAll();

                // 绑定用例至相机
                processCameraProvider.bindToLifecycle(ScanActivity.this, cameraSelector, preview, imageCapture, imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "用例绑定失败！" + e);
            }
        }, ContextCompat.getMainExecutor(this));

    }

    private void takePhoto() {
        // 确保imageCapture 已经被实例化, 否则程序将可能崩溃
        if (imageCapture != null) {
            // 创建带时间戳的输出文件以保存图片，带时间戳是为了保证文件名唯一
            File photoFile = new File(outputDirectory, new SimpleDateFormat(MainActivity.Configuration.FILENAME_FORMAT, Locale.SIMPLIFIED_CHINESE).format(System.currentTimeMillis()) + ".png");

            // 创建 output option 对象，用以指定照片的输出方式
            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

            // 执行takePicture（拍照）方法
            imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {// 保存照片时的回调
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    Uri savedUri = Uri.fromFile(photoFile);
                    Log.d(TAG, "照片捕获成功! " + savedUri);
                    Log.d(TAG, "START SCAN ~");
                    scanBarcodes(savedUri);
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e(TAG, "Photo capture failed: " + exception.getMessage());
                }
            });
        }
    }

    private File getOutputDirectory() {
        File mediaDir = new File(getExternalMediaDirs()[0], getString(R.string.app_name));
        boolean isExist = mediaDir.exists() || mediaDir.mkdir();
        return isExist ? mediaDir : null;
    }

    private void scanBarcodes(Uri savedUri) {
        InputImage image = null;
        try {
            image = InputImage.fromFilePath(mContext, savedUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "START TASK...");
        Task<List<Barcode>> result = scanner.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {
                        // Task completed successfully
                        Log.d(TAG, "Task completed successfully!");
                        for (Barcode barcode : barcodes) {
                            Rect bounds = barcode.getBoundingBox();
                            Point[] corners = barcode.getCornerPoints();
                            String rawValue = barcode.getRawValue();
                            Log.d(TAG, "rawValue = " + rawValue);
                            Toast.makeText(mContext, "" + rawValue, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent();
                            intent.setClassName("com.mz.mzocr", "com.mz.mzocr.ScanTextActivity");
                            intent.putExtra("SCAN_TEXT", rawValue);
                            startActivity(intent);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        Log.d(TAG, "Task failed with an exception");
                    }
                });
    }

    private static class MyAnalyzer implements ImageAnalysis.Analyzer {
        @SuppressLint("UnsafeOptInUsageError")
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            // Log.d(TAG, "Image's stamp is " + Objects.requireNonNull(imageProxy.getImage()).getTimestamp());
            imageProxy.close();
        }

    }

}