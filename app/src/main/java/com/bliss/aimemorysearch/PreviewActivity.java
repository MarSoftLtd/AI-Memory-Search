package com.bliss.aimemorysearch;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.File;

public class PreviewActivity extends AppCompatActivity {

    private ImageView previewImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preview);

        previewImage = findViewById(R.id.previewImage);

        String path =
                getIntent().getStringExtra("path");

        if (path != null) {

            Glide.with(this)
                    .load(new File(path))
                    .into(previewImage);
        }
    }
}