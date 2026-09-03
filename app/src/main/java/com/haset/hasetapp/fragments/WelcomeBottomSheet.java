package com.haset.hasetapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.haset.hasetapp.R;

/**
 * Full-screen welcome bottom sheet shown after a successful registration /
 * email-verification send. It displays a brief welcome message, stays on screen
 * for a few seconds, then dismisses itself and navigates the user to login.
 */
public class WelcomeBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "WelcomeBottomSheet";

    public interface WelcomeCallback {
        /** Called after the welcome sheet has been shown and dismissed. */
        void onWelcomeDone();
    }

    private WelcomeCallback welcomeCallback;
    private static final long WELCOME_DURATION_MS = 3000L;

    private static final String ARG_TITLE = "welcome_title";
    private static final String ARG_MESSAGE = "welcome_message";

    public static WelcomeBottomSheet newInstance(@Nullable String title, @Nullable String message) {
        WelcomeBottomSheet fragment = new WelcomeBottomSheet();
        Bundle args = new Bundle();
        if (title != null) args.putString(ARG_TITLE, title);
        if (message != null) args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.FullScreenBottomSheetDialogTheme);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof WelcomeCallback) {
            welcomeCallback = (WelcomeCallback) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        welcomeCallback = null;
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
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            behavior.setDraggable(false);
        }

        // Auto-dismiss after a short welcome, then hand off to the callback.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                dismissAllowingStateLoss();
            }
        }, WELCOME_DURATION_MS);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_welcome, container, false);

        Bundle args = getArguments();
        String title = args != null ? args.getString(ARG_TITLE) : null;
        String message = args != null ? args.getString(ARG_MESSAGE) : null;

        android.widget.TextView tvWelcomeTitle = view.findViewById(R.id.tvWelcomeTitle);
        android.widget.TextView tvWelcomeDesc = view.findViewById(R.id.tvWelcomeDesc);
        if (title != null && tvWelcomeTitle != null) {
            tvWelcomeTitle.setText(title);
        }
        if (message != null && tvWelcomeDesc != null) {
            tvWelcomeDesc.setText(message);
        }

        return view;
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (welcomeCallback != null) {
            welcomeCallback.onWelcomeDone();
        }
    }
}
