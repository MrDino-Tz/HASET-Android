package com.haset.hasetapp.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;

public class LearnMoreBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "LearnMoreBottomSheet";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_learn_more, container, false);

        MaterialButton btnLearnMore = view.findViewById(R.id.btnLearnMore);
        btnLearnMore.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://hasethospital.or.tz/security/afyaplus"));
            startActivity(intent);
        });

        return view;
    }
}
