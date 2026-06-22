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

public class DeleteAccountDialog extends Dialog {
    
    private OnDeleteAccountListener listener;
    private String title = "Delete Account";
    private String message = "Are you sure you want to delete your account? This action cannot be undone and will permanently remove all your data including appointments and personal information.";
    
    public interface OnDeleteAccountListener {
        void onDeleteConfirmed();
        void onCancel();
    }

    public DeleteAccountDialog(@NonNull Context context) {
        super(context, android.R.style.Theme_DeviceDefault_Dialog);
    }

    public DeleteAccountDialog(@NonNull Context context, OnDeleteAccountListener listener) {
        super(context, android.R.style.Theme_DeviceDefault_Dialog);
        this.listener = listener;
    }

    public DeleteAccountDialog setTitle(String title) {
        this.title = title;
        return this;
    }

    public DeleteAccountDialog setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_delete_account);

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
                    listener.onDeleteConfirmed();
                }
                dismiss();
            });
        }
    }
}
