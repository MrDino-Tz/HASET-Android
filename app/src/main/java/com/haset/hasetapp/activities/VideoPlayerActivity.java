package com.haset.hasetapp.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.haset.hasetapp.R;

public class VideoPlayerActivity extends AppCompatActivity {
    private VideoView videoView;
    private ProgressBar progressBar;
    private ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        videoView = findViewById(R.id.videoView);
        progressBar = findViewById(R.id.progressBar);
        ivBack = findViewById(R.id.ivBack);

        String videoUrl = getIntent().getStringExtra("video_url");

        if (videoUrl != null) {
            progressBar.setVisibility(View.VISIBLE);
            Uri uri = Uri.parse(videoUrl);
            videoView.setVideoURI(uri);

            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);

            videoView.setOnPreparedListener(mp -> {
                progressBar.setVisibility(View.GONE);
                videoView.start();
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                progressBar.setVisibility(View.GONE);
                return false;
            });

            videoView.setOnCompletionListener(mp -> {
                // Done
            });
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
