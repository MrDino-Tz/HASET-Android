package com.haset.hasetapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.haset.hasetapp.R;

public class ChatHeaderMoreOptionsBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ChatHeaderMoreOptionsBottomSheet";

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogAnimation; // Apply the custom animation style
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_chat_header_more_options, container, false);

        LinearLayout llDeleteMessages = view.findViewById(R.id.llDeleteMessages);
        LinearLayout llSearchMessages = view.findViewById(R.id.llSearchMessages);

        llDeleteMessages.setOnClickListener(v -> {
            Toast.makeText(getContext(), R.string.delete_messages_action, Toast.LENGTH_SHORT).show();
            dismiss();
        });

        llSearchMessages.setOnClickListener(v -> {
            Toast.makeText(getContext(), R.string.search_messages_action, Toast.LENGTH_SHORT).show();
            dismiss();
        });

        return view;
    }
}
