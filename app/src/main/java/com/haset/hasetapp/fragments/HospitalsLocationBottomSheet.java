package com.haset.hasetapp.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;

public class HospitalsLocationBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "HospitalsLocationBottomSheet";

    private static final String MAP_EMBED_URL = "https://maps.google.com/maps?q=HASET+Hospital,+Dar+es+Salaam,+Tanzania&z=15&output=embed";

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_hospital_location, container, false);

        WebView webViewMap = view.findViewById(R.id.webViewHospitalMap);
        ProgressBar progressBar = view.findViewById(R.id.progressBarMapLoading);

        WebSettings settings = webViewMap.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        // Google Maps embed refuses to render with the default WebView user agent
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.5735.196 Mobile Safari/537.36");

        webViewMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView webView, String url) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView webView, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webViewMap.loadUrl(MAP_EMBED_URL);

        MaterialButton btnOpenInMaps = view.findViewById(R.id.btnOpenInMaps);
        btnOpenInMaps.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=HASET+Hospital+Dar+es+Salaam"));
            startActivity(intent);
        });

        MaterialButton btnClose = view.findViewById(R.id.btnCloseHospitalLocation);
        btnClose.setOnClickListener(v -> dismiss());

        return view;
    }
}
