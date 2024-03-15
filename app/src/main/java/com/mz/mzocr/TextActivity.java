package com.mz.mzocr;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

public class TextActivity extends AppCompatActivity {

    private static final String TAG = "MZOCR_TextActivity";
    private Context mContext;
    private EditText editText;

    private String ocrText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);
        mContext = getApplicationContext();
        editText = findViewById(R.id.edit_txt);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        ocrText = intent.getStringExtra("OCR_TEXT");
        editText.setText(ocrText);

    }
}