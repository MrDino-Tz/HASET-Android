package com.haset.hasetapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.NetworkUtils;

public class NoInternetBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "NoInternetBottomSheet";

    private NetworkStateCallback networkStateCallback;

    public interface NetworkStateCallback {
        void onRetryConnection();
        void onNetworkAvailable();
        void onNetworkUnavailable();
        void onBottomSheetDismissed(); // New method to signal dismissal
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NetworkStateCallback) {
            networkStateCallback = (NetworkStateCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        networkStateCallback = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_no_internet, container, false);

        MaterialButton btnRetry = view.findViewById(R.id.btnRetry);
        btnRetry.setOnClickListener(v -> {
            if (NetworkUtils.isNetworkAvailable(getContext())) {
                Toast.makeText(getContext(), R.string.internet_restored, Toast.LENGTH_SHORT).show();
                if (networkStateCallback != null) {
                    networkStateCallback.onNetworkAvailable(); // Signal network is available
                }
                dismiss(); // Dismiss the bottom sheet
            } else {
                Toast.makeText(getContext(), R.string.still_no_internet, Toast.LENGTH_SHORT).show();
                if (networkStateCallback != null) {
                    networkStateCallback.onNetworkUnavailable(); // Signal network is still unavailable
                }
                // Do NOT dismiss here, let it stay open if retry fails
            }
        });
        return view;
    }

    @Override
    public void onCancel(@NonNull android.content.DialogInterface dialog) {
        super.onCancel(dialog);
        if (networkStateCallback != null) {
            networkStateCallback.onBottomSheetDismissed(); // Signal that the bottom sheet was dismissed
        }
    }
}
