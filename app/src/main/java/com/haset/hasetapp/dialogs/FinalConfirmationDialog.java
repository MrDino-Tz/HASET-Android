package com.haset.hasetapp.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;

public class FinalConfirmationDialog extends Dialog {
    
    private OnFinalConfirmationListener listener;
    private String title = "Final Confirmation";
    private String message = "This is your last chance! All your data will be permanently deleted. Are you absolutely sure?";
    
    public interface OnFinalConfirmationListener {
        void onFinalConfirmed();
        void onCancel();
    }

    public FinalConfirmationDialog(@NonNull Context context) {
        super(context, android.R.style.Theme_DeviceDefault_Dialog);
    }

    public FinalConfirmationDialog(@NonNull Context context, OnFinalConfirmationListener listener) {
        super(context, android.R.style.Theme_DeviceDefault_Dialog);
        this.listener = listener;
    }

    public FinalConfirmationDialog setTitle(String title) {
        this.title = title;
        return this;
    }

    public FinalConfirmationDialog setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_final_confirmation);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        TextView tvTitle = findViewById(R.id.tvDialogTitle);
        TextView tvMessage = findViewById(R.id.tvDialogMessage);
        
        if (tvTitle != null) {
            tvTitle.setText(title);
        }
        if (tvMessage != null) {
            tvMessage.setText(message);
        }
    }

    private void setupClickListeners() {
        MaterialButton btnCancel = findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = findViewById(R.id.btnConfirm);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCancel();
                }
                dismiss();
            });
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFinalConfirmed();
                }
                dismiss();
            });
        }
    }
}
