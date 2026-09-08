package com.haset.hasetapp.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.SensitiveActivityHelper;

/** Full-screen HASET container for Snippe's PCI-hosted checkout. */
public class HostedCheckoutActivity extends LocalizedAppCompatActivity {
    public static final String EXTRA_CHECKOUT_URL = "checkout_url";

    private WebView checkoutView;
    private LinearProgressIndicator progress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Payment screen: block OS screenshots/screen recording (see SCREENSHOT_BLOCKING_POLICY.md)
        SensitiveActivityHelper.blockScreenshots(this);

        String checkoutUrl = getIntent().getStringExtra(EXTRA_CHECKOUT_URL);
        if (!isTrustedCheckoutUrl(checkoutUrl)) {
            finish();
            return;
        }

        /*
        // Passive tamper warning (does not block): rooted/debug environments
        if (com.haset.hasetapp.utils.RootIntegrityHelper.isPotentiallyCompromised(this)) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(com.haset.hasetapp.R.string.security_warning_title)
                    .setMessage(com.haset.hasetapp.R.string.rooted_device_warning)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
        */

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle(R.string.secure_payment);
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel);
        toolbar.setNavigationOnClickListener(view -> finish());
        root.addView(toolbar, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        progress = new LinearProgressIndicator(this);
        progress.setIndeterminate(true);
        root.addView(progress, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        checkoutView = new WebView(this);
        configureCheckoutView(checkoutView);
        root.addView(checkoutView, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        overridePendingTransition(R.anim.anim_slide_up, 0);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (checkoutView != null && checkoutView.canGoBack()) checkoutView.goBack();
                else finish();
            }
        });

        checkoutView.loadUrl(checkoutUrl);
    }

    private void configureCheckoutView(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSaveFormData(false);
        settings.setUserAgentString(settings.getUserAgentString() + " HASET-App");

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (isHasetPaymentCallback(uri)) {
                    finish();
                    return true;
                }
                String scheme = uri.getScheme();
                if ("https".equalsIgnoreCase(scheme)) {
                    return false; // PCI checkout flows through trusted https redirects
                }
                if ("http".equalsIgnoreCase(scheme)) {
                    return true; // never load cleartext content in the payment WebView
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override public void onPageFinished(WebView view, String url) {
                progress.setVisibility(android.view.View.GONE);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) progress.setVisibility(android.view.View.VISIBLE);
                else progress.setVisibility(android.view.View.GONE);
            }
        });
    }

    private static boolean isTrustedCheckoutUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) return false;
        Uri uri = Uri.parse(rawUrl);
        String host = uri.getHost();
        return "https".equalsIgnoreCase(uri.getScheme()) && host != null
            && (host.equalsIgnoreCase("snippe.me") || host.toLowerCase().endsWith(".snippe.me"));
    }

    private static boolean isHasetPaymentCallback(Uri uri) {
        String host = uri.getHost();
        String path = uri.getPath();
        return "https".equalsIgnoreCase(uri.getScheme())
            && "hasethospital.or.tz".equalsIgnoreCase(host)
            && path != null
            && (path.startsWith("/payment/success") || path.startsWith("/payment/cancel"));
    }

    @Override protected void onDestroy() {
        if (checkoutView != null) {
            checkoutView.stopLoading();
            checkoutView.setWebChromeClient(null);
            checkoutView.setWebViewClient(null);
            checkoutView.destroy();
            checkoutView = null;
        }
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.anim_slide_down);
    }
}
