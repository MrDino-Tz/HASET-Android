package com.haset.hasetapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.haset.hasetapp.R;

public class ChatMoreOptionsBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ChatMoreOptionsBottomSheet";

    public interface OnOptionSelectedListener {
        void onSearchMessagesSelected();
        void onViewContactSelected();
    }

    private OnOptionSelectedListener listener;

    public void setOnOptionSelectedListener(OnOptionSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_chat_more_options, container, false);

        LinearLayout optionSearchMessages = view.findViewById(R.id.optionSearchMessages);
        LinearLayout optionViewContact = view.findViewById(R.id.optionViewContact);

        optionSearchMessages.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSearchMessagesSelected();
            }
            dismiss();
        });

        optionViewContact.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewContactSelected();
            }
            dismiss();
        });

        return view;
    }
}
