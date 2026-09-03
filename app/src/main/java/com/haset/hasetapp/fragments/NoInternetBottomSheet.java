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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.FullScreenBottomSheetDialogTheme);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        View bottomSheet = getDialog() != null ? getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet) : null;
        if (bottomSheet != null) {
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            behavior.setDraggable(false);
        }
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

    public void showNetworkRestoredState(Runnable onComplete) {
        if (getView() != null) {
            android.widget.ImageView imgErrorState = getView().findViewById(R.id.imgErrorState);
            android.widget.TextView tvErrorTitle = getView().findViewById(R.id.tvErrorTitle);
            android.widget.TextView tvErrorDesc = getView().findViewById(R.id.tvErrorDesc);
            MaterialButton btnRetry = getView().findViewById(R.id.btnRetry);

            if (imgErrorState != null) {
                imgErrorState.setImageResource(R.drawable.internet_restored);
            }
            if (tvErrorTitle != null) {
                tvErrorTitle.setText(R.string.internet_restored);
            }
            if (tvErrorDesc != null) {
                tvErrorDesc.setText("");
            }
            if (btnRetry != null) {
                btnRetry.setVisibility(View.GONE);
            }

            getView().postDelayed(onComplete, 700);
        } else {
            if (onComplete != null) onComplete.run();
        }
    }

    public static final String ARG_IS_SESSION_EXPIRED = "is_session_expired";

    public static NoInternetBottomSheet newInstanceForSessionExpired() {
        NoInternetBottomSheet fragment = new NoInternetBottomSheet();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_SESSION_EXPIRED, true);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_no_internet, container, false);

        boolean isSessionExpired = getArguments() != null && getArguments().getBoolean(ARG_IS_SESSION_EXPIRED, false);

        android.widget.ImageView imgErrorState = view.findViewById(R.id.imgErrorState);
        android.widget.TextView tvErrorTitle = view.findViewById(R.id.tvErrorTitle);
        android.widget.TextView tvErrorDesc = view.findViewById(R.id.tvErrorDesc);
        MaterialButton btnRetry = view.findViewById(R.id.btnRetry);

        if (isSessionExpired) {
            if (imgErrorState != null) {
                imgErrorState.setImageResource(R.drawable.session_time);
            }
            if (tvErrorTitle != null) {
                tvErrorTitle.setText(R.string.session_expired_title);
            }
            if (tvErrorDesc != null) {
                tvErrorDesc.setText(R.string.session_expired_desc);
            }
            if (btnRetry != null) {
                btnRetry.setText(R.string.btn_ok);
                btnRetry.setOnClickListener(v -> {
                    if (isAdded()) {
                        dismissAllowingStateLoss();
                    }
                    if (getContext() != null) {
                        com.haset.hasetapp.utils.ErrorDisplay.navigateToLogin(getContext());
                    }
                });
            }
        } else {
            if (imgErrorState != null) {
                imgErrorState.setImageResource(R.drawable.no_internet1);
            }
            btnRetry.setOnClickListener(v -> {
                if (NetworkUtils.isNetworkAvailable(getContext())) {
                    showNetworkRestoredState(() -> {
                        Toast.makeText(getContext(), R.string.internet_restored, Toast.LENGTH_SHORT).show();
                        if (networkStateCallback != null) {
                            networkStateCallback.onNetworkAvailable();
                        }
                        if (isAdded()) {
                            dismissAllowingStateLoss();
                        }
                    });
                } else {
                    Toast.makeText(getContext(), R.string.still_no_internet, Toast.LENGTH_SHORT).show();
                    if (networkStateCallback != null) {
                        networkStateCallback.onNetworkUnavailable();
                    }
                }
            });
        }
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
