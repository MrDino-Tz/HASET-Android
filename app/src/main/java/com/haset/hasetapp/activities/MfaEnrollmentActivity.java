package com.haset.hasetapp.activities;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.haset.hasetapp.R;
import com.haset.hasetapp.api.RetrofitClient;
import com.haset.hasetapp.ui.MfaCodeInputView;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.CrashMonitor;
import com.haset.hasetapp.utils.SensitiveActivityHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MfaEnrollmentActivity extends BaseActivity {
    private static final long CLIPBOARD_CLEAR_DELAY_MS = 60_000L;
    private TextView manualKey, recoveryCodes, setupTitle, setupSubtitle;
    private ImageView qrCode;
    private ProgressBar qrProgress;
    private View manualContainer, qrCard, recoveryContainer;
    private MfaCodeInputView codeInput;
    private Button confirm, continueButton, manualToggle, copyKey, copyRecovery;
    private CheckBox saved;
    private String secret;
    private final List<String> recoveryCodeList = new ArrayList<>();
    private boolean busy;

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        SensitiveActivityHelper.blockScreenshots(this);
        setContentView(R.layout.activity_mfa_enrollment);
        manualKey = findViewById(R.id.mfaManualKey);
        recoveryCodes = findViewById(R.id.mfaRecoveryCodes);
        recoveryContainer = findViewById(R.id.mfaRecoveryContainer);
        setupTitle = findViewById(R.id.mfaSetupTitle);
        setupSubtitle = findViewById(R.id.mfaSetupSubtitle);
        codeInput = findViewById(R.id.mfaCodeInput);
        confirm = findViewById(R.id.mfaConfirm);
        continueButton = findViewById(R.id.mfaContinue);
        saved = findViewById(R.id.mfaSaved);
        qrCode = findViewById(R.id.mfaQrCode);
        qrProgress = findViewById(R.id.mfaQrProgress);
        qrCard = findViewById(R.id.mfaQrCard);
        manualContainer = findViewById(R.id.mfaManualContainer);
        manualToggle = findViewById(R.id.mfaManualToggle);
        copyKey = findViewById(R.id.mfaCopyKey);
        copyRecovery = findViewById(R.id.mfaCopyRecovery);
        continueButton.setVisibility(View.GONE);
        saved.setVisibility(View.GONE);
        recoveryContainer.setVisibility(View.GONE);
        manualToggle.setEnabled(false);
        manualToggle.setOnClickListener(v -> {
            boolean show = manualContainer.getVisibility() != View.VISIBLE;
            manualContainer.setVisibility(show ? View.VISIBLE : View.GONE);
            manualToggle.setText(show ? "Hide setup key" : getString(R.string.cant_scan_use_a_setup_key));
        });
        copyKey.setOnClickListener(v -> copySetupKey());
        copyRecovery.setOnClickListener(v -> copyRecoveryCodes());
        confirm.setOnClickListener(v -> confirmCode());
        continueButton.setOnClickListener(v -> {
            if (saved.isChecked()) {
                clearSecrets();
                setResult(RESULT_OK);
                finish();
            }
        });
        codeInput.postDelayed(() -> codeInput.focusFirst(), 250);
        requestSetup();
    }

    private void requestSetup() {
        FirebaseUser u = FirebaseHelper.getFirebaseAuth().getCurrentUser();
        if (u == null) { fail("Authentication expired."); return; }
        u.getIdToken(true).addOnSuccessListener(t ->
            RetrofitClient.getInstance().getMobileMfaApiService().setup("Bearer " + t.getToken())
                .enqueue(new Callback<JsonObject>() {
                    public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                        if (!r.isSuccessful() || r.body() == null) {
                            fail(r.code() == 429 ? "Too many requests. Try later." : "Unable to start MFA setup.");
                            return;
                        }
                        String uri = r.body().has("otpauth_uri") ? r.body().get("otpauth_uri").getAsString() : "";
                        String fromBody = r.body().has("secret") ? r.body().get("secret").getAsString() : "";
                        secret = resolveSecret(fromBody, uri);
                        parseRecoveryCodes(r.body().get("recovery_codes"));
                        manualKey.setText(formatSecret(secret));
                        manualToggle.setEnabled(!secret.isEmpty());
                        renderQrCode(uri);
                    }
                    public void onFailure(Call<JsonObject> c, Throwable x) {
                        CrashMonitor.report("auth", "MfaEnrollment.requestSetup", "MFA setup network failure", x);
                        fail("Network error. Retry setup.");
                    }
                })
        ).addOnFailureListener(e -> {
            CrashMonitor.report("auth", "MfaEnrollment.requestSetup", "MFA setup token refresh failed", e);
            fail("Authentication expired.");
        });
    }

    private void parseRecoveryCodes(JsonElement element) {
        recoveryCodeList.clear();
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                if (item == null || item.isJsonNull()) continue;
                String code = item.getAsString().trim().toUpperCase(Locale.US);
                if (!code.isEmpty()) recoveryCodeList.add(code);
            }
            return;
        }
        // Fallback if API returns a single string / CSV.
        String raw = element.getAsString().replace("[", "").replace("]", "").replace("\"", "");
        for (String part : raw.split("[,\\s]+")) {
            String code = part.trim().toUpperCase(Locale.US);
            if (!code.isEmpty()) recoveryCodeList.add(code);
        }
    }

    private String formatRecoveryCodesForDisplay() {
        if (recoveryCodeList.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < recoveryCodeList.size(); i++) {
            if (i > 0) out.append('\n');
            out.append(String.format(Locale.US, "%2d.  %s", i + 1, recoveryCodeList.get(i)));
        }
        return out.toString();
    }

    private String formatRecoveryCodesForCopy() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < recoveryCodeList.size(); i++) {
            if (i > 0) out.append('\n');
            out.append(recoveryCodeList.get(i));
        }
        return out.toString();
    }

    private void showRecoveryCodes() {
        setupTitle.setText(R.string.recovery_codes_title);
        setupSubtitle.setVisibility(View.GONE);
        recoveryCodes.setText(formatRecoveryCodesForDisplay());
        recoveryContainer.setVisibility(View.VISIBLE);
        saved.setVisibility(View.VISIBLE);
        continueButton.setVisibility(View.VISIBLE);
        confirm.setVisibility(View.GONE);
        codeInput.setVisibility(View.GONE);
        qrCard.setVisibility(View.GONE);
        manualToggle.setVisibility(View.GONE);
        manualContainer.setVisibility(View.GONE);
        codeInput.clearCode();
    }

    private void renderQrCode(String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            qrProgress.setVisibility(View.GONE);
            fail("The server did not return an authenticator QR code.");
            return;
        }
        new Thread(() -> {
            try {
                Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
                hints.put(EncodeHintType.MARGIN, 3);
                hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                BitMatrix matrix = new QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE, 720, 720, hints);
                int[] pixels = new int[720 * 720];
                for (int y = 0; y < 720; y++)
                    for (int x = 0; x < 720; x++)
                        pixels[y * 720 + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
                Bitmap bitmap = Bitmap.createBitmap(720, 720, Bitmap.Config.ARGB_8888);
                bitmap.setPixels(pixels, 0, 720, 0, 0, 720, 720);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    qrCode.setImageBitmap(bitmap);
                    qrCode.setVisibility(View.VISIBLE);
                    qrProgress.setVisibility(View.GONE);
                });
            } catch (Exception error) {
                CrashMonitor.report("auth", "MfaEnrollment.requestSetup", "MFA QR generation failed", error);
                runOnUiThread(() -> {
                    qrProgress.setVisibility(View.GONE);
                    fail("Unable to generate the authenticator QR code.");
                });
            }
        }).start();
    }

    /** Prefer secret from otpauth URI (what QR encodes) so manual entry matches QR. */
    private String resolveSecret(String fromBody, String otpauthUri) {
        String fromUri = null;
        if (otpauthUri != null && !otpauthUri.trim().isEmpty()) {
            try {
                fromUri = Uri.parse(otpauthUri).getQueryParameter("secret");
            } catch (Exception ignored) {}
        }
        String raw = (fromUri != null && !fromUri.isEmpty()) ? fromUri : fromBody;
        return normalizeBase32Secret(raw);
    }

    /** Strip spaces/dashes and uppercase — authenticators expect compact base32. */
    private String normalizeBase32Secret(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ' ' || c == '-' || c == '\n' || c == '\r' || c == '\t') continue;
            out.append(Character.toUpperCase(c));
        }
        return out.toString();
    }

    private String formatSecret(String value) {
        if (value == null) return "";
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            if (i > 0 && i % 4 == 0) formatted.append(' ');
            formatted.append(value.charAt(i));
        }
        return formatted.toString();
    }

    private void copyToClipboard(String label, String value, int toastRes) {
        if (value == null || value.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value));
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ClipData currentClip = clipboard.getPrimaryClip();
            if (currentClip != null
                    && currentClip.getItemCount() > 0
                    && value.contentEquals(currentClip.getItemAt(0).coerceToText(this))) {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""));
            }
        }, CLIPBOARD_CLEAR_DELAY_MS);
        Toast.makeText(this, toastRes, Toast.LENGTH_SHORT).show();
    }

    private void copySetupKey() {
        copyToClipboard("Authenticator setup key", secret, R.string.setup_key_copied);
    }

    private void copyRecoveryCodes() {
        copyToClipboard("MFA recovery codes", formatRecoveryCodesForCopy(), R.string.recovery_codes_copied);
    }

    private void confirmCode() {
        if (busy) return;
        if (!codeInput.isComplete()) { codeInput.setErrorState(true); return; }
        FirebaseUser u = FirebaseHelper.getFirebaseAuth().getCurrentUser();
        if (u == null) { fail("Authentication expired."); return; }
        busy = true;
        confirm.setEnabled(false);
        u.getIdToken(true).addOnSuccessListener(t -> {
            JsonObject b = new JsonObject();
            b.addProperty("code", codeInput.getCode());
            RetrofitClient.getInstance().getMobileMfaApiService()
                .confirm("Bearer " + t.getToken(), b)
                .enqueue(new Callback<JsonObject>() {
                    public void onResponse(Call<JsonObject> c, Response<JsonObject> r) {
                        busy = false;
                        if (!r.isSuccessful()) {
                            confirm.setEnabled(true);
                            codeInput.setErrorState(true);
                            fail(r.code() == 429 ? "Too many attempts. Try later." : "Invalid or expired code.");
                            return;
                        }
                        showRecoveryCodes();
                    }
                    public void onFailure(Call<JsonObject> c, Throwable x) {
                        busy = false;
                        confirm.setEnabled(true);
                        CrashMonitor.report("auth", "MfaEnrollment.confirmCode", "MFA confirm network failure", x);
                        fail("Network error. Retry.");
                    }
                });
        }).addOnFailureListener(e -> {
            busy = false;
            confirm.setEnabled(true);
            CrashMonitor.report("auth", "MfaEnrollment.confirmCode", "MFA confirm token refresh failed", e);
            fail("Authentication expired.");
        });
    }

    private void fail(String message) {
        String detail = com.haset.hasetapp.utils.ErrorDisplay.localizeMessage(MfaEnrollmentActivity.this, message);
        com.haset.hasetapp.utils.ErrorLogger.log(detail, message);
        Toast.makeText(this, detail, Toast.LENGTH_LONG).show();
    }

    private void clearSecrets() {
        secret = null;
        recoveryCodeList.clear();
        if (codeInput != null) codeInput.clearCode();
        if (manualKey != null) manualKey.setText("");
        if (qrCode != null) {
            qrCode.setImageDrawable(null);
            qrCode.setVisibility(View.INVISIBLE);
        }
        if (recoveryCodes != null) recoveryCodes.setText("");
        if (recoveryContainer != null) recoveryContainer.setVisibility(View.GONE);
    }

    @Override protected void onDestroy() {
        clearSecrets();
        super.onDestroy();
    }
}
