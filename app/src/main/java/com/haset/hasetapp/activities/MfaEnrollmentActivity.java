package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;
import com.haset.hasetapp.R;
import com.haset.hasetapp.api.RetrofitClient;
import com.haset.hasetapp.ui.MfaCodeInputView;
import com.haset.hasetapp.utils.FirebaseHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MfaEnrollmentActivity extends BaseActivity {
    private TextView manualKey, recoveryCodes;
    private MfaCodeInputView codeInput;
    private Button confirm, continueButton;
    private CheckBox saved;
    private String secret;
    private String recoveryCodesRaw;
    private boolean busy;
    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state); getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_mfa_enrollment);
        manualKey=findViewById(R.id.mfaManualKey); recoveryCodes=findViewById(R.id.mfaRecoveryCodes); codeInput=findViewById(R.id.mfaCodeInput); confirm=findViewById(R.id.mfaConfirm); continueButton=findViewById(R.id.mfaContinue); saved=findViewById(R.id.mfaSaved);
        continueButton.setVisibility(View.GONE); saved.setVisibility(View.GONE); recoveryCodes.setVisibility(View.GONE);
        confirm.setOnClickListener(v -> confirmCode()); continueButton.setOnClickListener(v -> { if(saved.isChecked()){clearSecrets();setResult(RESULT_OK);finish();} });
        codeInput.postDelayed(() -> codeInput.focusFirst(), 250);
        requestSetup();
    }
    private void requestSetup(){ FirebaseUser u=FirebaseHelper.getFirebaseAuth().getCurrentUser(); if(u==null){fail("Authentication expired.");return;} u.getIdToken(true).addOnSuccessListener(t -> RetrofitClient.getInstance().getMobileMfaApiService().setup("Bearer "+t.getToken()).enqueue(new Callback<JsonObject>(){ public void onResponse(Call<JsonObject> c,Response<JsonObject> r){ if(!r.isSuccessful()||r.body()==null){fail(r.code()==429?"Too many requests. Try later.":"Unable to start MFA setup.");return;} secret=r.body().has("secret")?r.body().get("secret").getAsString():""; recoveryCodesRaw=r.body().has("recovery_codes")?r.body().get("recovery_codes").toString():""; manualKey.setText("Manual setup key:\n"+secret); } public void onFailure(Call<JsonObject> c,Throwable x){fail("Network error. Retry setup.");} })).addOnFailureListener(e->fail("Authentication expired.")); }
    private void confirmCode(){ if(busy)return; if(!codeInput.isComplete()){codeInput.setErrorState(true);return;} FirebaseUser u=FirebaseHelper.getFirebaseAuth().getCurrentUser(); if(u==null){fail("Authentication expired.");return;} busy=true;confirm.setEnabled(false);u.getIdToken(true).addOnSuccessListener(t->{JsonObject b=new JsonObject();b.addProperty("code",codeInput.getCode());RetrofitClient.getInstance().getMobileMfaApiService().confirm("Bearer "+t.getToken(),b).enqueue(new Callback<JsonObject>(){public void onResponse(Call<JsonObject> c,Response<JsonObject> r){busy=false;if(!r.isSuccessful()){confirm.setEnabled(true);codeInput.setErrorState(true);fail(r.code()==429?"Too many attempts. Try later.":"Invalid or expired code.");return;} recoveryCodes.setText(recoveryCodesRaw);recoveryCodes.setVisibility(View.VISIBLE);saved.setVisibility(View.VISIBLE);continueButton.setVisibility(View.VISIBLE);confirm.setVisibility(View.GONE);codeInput.clearCode();}public void onFailure(Call<JsonObject> c,Throwable x){busy=false;confirm.setEnabled(true);fail("Network error. Retry.");}});}).addOnFailureListener(e->{busy=false;confirm.setEnabled(true);fail("Authentication expired.");}); }
    private void fail(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    private void clearSecrets(){secret=null;recoveryCodesRaw=null;if(codeInput!=null)codeInput.clearCode();if(recoveryCodes!=null)recoveryCodes.setText("");}
    @Override protected void onDestroy(){clearSecrets();super.onDestroy();}
}
