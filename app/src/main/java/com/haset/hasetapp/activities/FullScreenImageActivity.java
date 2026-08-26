package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.haset.hasetapp.R;

public class FullScreenImageActivity extends LocalizedAppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ImageView imageView = findViewById(R.id.ivFullScreen);
        ImageView ivBack = findViewById(R.id.ivBack);
        
        String imageUrl = getIntent().getStringExtra("image_url");

        if (imageUrl != null) {
            Glide.with(this).load(imageUrl).into(imageView);
        }

        ivBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.scale_up_enter, R.anim.scale_down_exit);
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.scale_up_enter, R.anim.scale_down_exit);
    }
}
