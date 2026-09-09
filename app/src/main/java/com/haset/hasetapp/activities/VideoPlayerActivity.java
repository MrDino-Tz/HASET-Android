package com.haset.hasetapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.CloudinaryUploadHelper;

public class VideoPlayerActivity extends LocalizedAppCompatActivity {
    private static final String TAG = "VideoPlayer";

    private VideoView videoView;
    private ProgressBar progressBar;
    private ImageView ivBack;
    private String videoUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        videoView = findViewById(R.id.videoView);
        progressBar = findViewById(R.id.progressBar);
        ivBack = findViewById(R.id.ivBack);

        videoUrl = CloudinaryUploadHelper.toPublicDeliveryUrl(
                getIntent().getStringExtra("video_url"));
        videoUrl = ensurePlayableVideoUrl(videoUrl);

        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            Toast.makeText(this, R.string.unable_to_play_video, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        playWithVideoView(videoUrl);

        ivBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.scale_up_enter, R.anim.scale_down_exit);
        });
    }

    private void playWithVideoView(String url) {
        progressBar.setVisibility(View.VISIBLE);
        try {
            Uri uri = Uri.parse(url);
            videoView.setVideoURI(uri);

            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);

            videoView.setOnPreparedListener(mp -> {
                progressBar.setVisibility(View.GONE);
                mp.setLooping(false);
                videoView.start();
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "VideoView error what=" + what + " extra=" + extra + " url=" + url);
                progressBar.setVisibility(View.GONE);
                openInExternalPlayer(url);
                return true;
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to start VideoView", e);
            progressBar.setVisibility(View.GONE);
            openInExternalPlayer(url);
        }
    }

    /** Prefer progressive mp4 delivery for Android VideoView. */
    private static String ensurePlayableVideoUrl(String url) {
        if (url == null || url.isEmpty()) return url;
        if (!url.contains("res.cloudinary.com")) return url;
        // Force public upload path
        String normalized = url.replace("/authenticated/", "/upload/").replace("/private/", "/upload/");
        // If it's a Cloudinary video without an explicit format transform, request mp4.
        if (normalized.contains("/video/upload/")
                && !normalized.contains("/video/upload/f_")
                && !normalized.contains("/video/upload/sp_")) {
            normalized = normalized.replace("/video/upload/", "/video/upload/f_mp4,q_auto/");
        }
        return normalized;
    }

    private void openInExternalPlayer(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), "video/*");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent, getString(R.string.open_video)));
        } catch (Exception e) {
            Log.e(TAG, "No external player for url=" + url, e);
            Toast.makeText(this, R.string.unable_to_play_video, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        if (videoView != null) {
            videoView.stopPlayback();
        }
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.scale_up_enter, R.anim.scale_down_exit);
    }
}
