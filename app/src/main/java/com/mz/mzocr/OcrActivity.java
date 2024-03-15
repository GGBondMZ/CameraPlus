package com.mz.mzocr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
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

import com.google.common.util.concurrent.ListenableFuture;
import com.mz.mzocr.utils.ImageUtils;
import com.youdao.ocr.online.ImageOCRecognizer;
import com.youdao.ocr.online.Line;
import com.youdao.ocr.online.OCRListener;
import com.youdao.ocr.online.OCRParameters;
import com.youdao.ocr.online.OCRResult;
import com.youdao.ocr.online.OcrErrorCode;
import com.youdao.ocr.online.RecognizeLanguage;
import com.youdao.ocr.online.Region;
import com.youdao.ocr.online.Word;
import com.youdao.sdk.app.EncryptHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OcrActivity extends AppCompatActivity {

    private static final String TAG = "MZOCR_MainActivity";
    private Context mContext;
    private OCRParameters tps;
    private ImageCapture imageCapture;
    private File outputDirectory;
    private ExecutorService cameraExecutor;

    private Button camera_capture_button;

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        mContext = getApplicationContext();

        initOcrParameter();

        // 设置拍照按钮监听
        camera_capture_button = findViewById(R.id.camera_capture_button);
        camera_capture_button.setOnClickListener(v -> takePhoto());

        // 设置照片等保存的位置
        outputDirectory = getOutputDirectory();
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
    }

    private void initOcrParameter() {
        //OCR识别
        tps = new OCRParameters.Builder()
                .source("youdaoocr")
                .timeout(100000)
                .lanType(RecognizeLanguage.AUTO.getCode())
                .build();
        //默认按行识别，支持自动、中英繁、日韩、拉丁、印地语，其中自动识别不支持印地语识别，其他都可以
        // 当采用按字识别时，识别语言支持中英和英文识别，其中"zh-en"为中英识别，"en"参数表示只识别英文。若为纯英文识别，"zh-en"的识别效果不如"en"，请妥善选择
    }

    private void startRecognize(String filePath) {

        final Bitmap bitmap = ImageUtils.readBitmapFromFile(filePath, 768);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 100;
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        byte[] datas = baos.toByteArray();
        String bases64 = EncryptHelper.getBase64(datas);
        int count = bases64.length();
        while (count > 1.4 * 1024 * 1024) {
            quality = quality - 10;
            baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            datas = baos.toByteArray();
            bases64 = EncryptHelper.getBase64(datas);
        }
        final String base64 = bases64;
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "开始识别...");
                Toast.makeText(mContext, "开始识别...", Toast.LENGTH_SHORT).show();
            }
        });
        //OCR识别
        ImageOCRecognizer.getInstance(tps).recognize(base64, new OCRListener() {
                    @Override
                    public void onResult(OCRResult result, String input) {
                        //识别成功
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                String text = getResult(result);
                                Log.d(TAG, "TEXT = " + text);
                                Intent intent = new Intent();
                                intent.setClassName("com.mz.mzocr", "com.mz.mzocr.TextActivity");
                                intent.putExtra("OCR_TEXT", text);
                                startActivity(intent);
                            }
                        });
                    }

                    @Override
                    public void onError(OcrErrorCode error) {
                        //识别失败
                    }
                });
    }

    private String getResult(OCRResult result) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        //按文本识别
        List<com.youdao.ocr.online.Region> regions = result.getRegions();
        for (Region region : regions) {
            List<Line> lines = region.getLines();
            for (Line line : lines) {
                sb.append("文本" + i++ + "： ");
                List<Word> words = line.getWords();
                for (Word word : words) {
                    sb.append(word.getText()).append(" ");
                }
                sb.append("\n");
            }
        }

        String text = sb.toString();
        if (!TextUtils.isEmpty(text)) {
            text = text.substring(0, text.length() - 2);
        }
        return text;
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
                    startRecognize(photoFile.toString());
                    String msg = "照片捕获成功! " + savedUri;
                    // Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, msg);
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e(TAG, "Photo capture failed: " + exception.getMessage());
                }
            });
        }
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
                processCameraProvider.bindToLifecycle(OcrActivity.this, cameraSelector, preview, imageCapture, imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "用例绑定失败！" + e);
            }
        }, ContextCompat.getMainExecutor(this));

    }

    private File getOutputDirectory() {
        File mediaDir = new File(getExternalMediaDirs()[0], getString(R.string.app_name));
        boolean isExist = mediaDir.exists() || mediaDir.mkdir();
        return isExist ? mediaDir : null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }


    private static class MyAnalyzer implements ImageAnalysis.Analyzer {
        @SuppressLint("UnsafeOptInUsageError")
        @Override
        public void analyze(@NonNull ImageProxy image) {
            //Log.d(TAG, "Image's stamp is " + Objects.requireNonNull(image.getImage()).getTimestamp());
            image.close();
        }
    }
}