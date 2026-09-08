package com.haset.hasetapp.utils;

import android.app.Activity;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.haset.hasetapp.R;

import java.util.ArrayList;
import java.util.List;

/**
 * One-time spotlight tips shown the first time a user opens a screen.
 */
public final class AppTourHelper {

    // Phase 1 — main tabs
    public static final String TOUR_HOME_PATIENT = "tour_home_patient_v2";
    public static final String TOUR_HOME_DOCTOR = "tour_home_doctor_v2";

    // Phase 2 — feature screens
    // public static final String TOUR_BOOK_APPOINTMENT = "tour_book_appointment";
    // public static final String TOUR_SETTINGS = "tour_settings";
    // public static final String TOUR_EDIT_PROFILE = "tour_edit_profile";
    // public static final String TOUR_NOTIFICATIONS = "tour_notifications";
    // public static final String TOUR_DOCTOR_WALLET = "tour_doctor_wallet";
    // public static final String TOUR_DOCTOR_PATIENTS = "tour_doctor_patients";
    // public static final String TOUR_DOCTOR_EDIT = "tour_doctor_edit";
    // public static final String TOUR_HOSPITALS = "tour_hospitals";
    // public static final String TOUR_ABOUT_US = "tour_about_us";

    // Utility screens
    // public static final String TOUR_FULL_SCREEN_IMAGE = "tour_full_screen_image";
    // public static final String TOUR_VIDEO_PLAYER = "tour_video_player";
    // public static final String TOUR_PRESCRIPTION_SHELL = "tour_prescription_shell";

    // App shell
    // public static final String TOUR_ONBOARDING = "tour_onboarding";
    // public static final String TOUR_MAIN = "tour_main";
    // public static final String TOUR_SHIMMER_TEST = "tour_shimmer_test";

    // Bottom sheets & sub-views
    // public static final String TOUR_RESET_PASSWORD = "tour_reset_password";
    // public static final String TOUR_ADD_PRESCRIPTION = "tour_add_prescription";
    // public static final String TOUR_PRESCRIPTION_BOTTOM_SHEET = "tour_prescription_bottom_sheet";
    // public static final String TOUR_ADD_SERVICE = "tour_add_service";
    // public static final String TOUR_POST_COMMENTS = "tour_post_comments";
    // public static final String TOUR_CONTACT_US = "tour_contact_us";
    // public static final String TOUR_LANGUAGE = "tour_language";

    // Popups & dialogs
    // public static final String TOUR_WITHDRAW = "tour_withdraw";
    // public static final String TOUR_MFA_REQUIRED_WITHDRAW = "tour_mfa_required_withdraw";
    // public static final String TOUR_PAYOUT_DESTINATION = "tour_payout_destination";
    // public static final String TOUR_CHANGE_PASSWORD = "tour_change_password";
    // public static final String TOUR_THEME_SELECTOR = "tour_theme_selector";
    // public static final String TOUR_SUPPORT_OPTIONS = "tour_support_options";
    // public static final String TOUR_BUG_REPORT = "tour_bug_report";
    // public static final String TOUR_MFA_CHALLENGE = "tour_mfa_challenge";
    // public static final String TOUR_COMING_SOON = "tour_coming_soon";
    // public static final String TOUR_DELETE_ACCOUNT = "tour_delete_account";
    // public static final String TOUR_DELETE_ACCOUNT_FINAL = "tour_delete_account_final";
    // public static final String TOUR_EXIT_APP = "tour_exit_app";
    // public static final String TOUR_HEALTH_TIP = "tour_health_tip";

    private AppTourHelper() {}

    /** Temporary kill switch — set true to re-enable TapTarget tours. */
    private static final boolean TOURS_ENABLED = false;

    public static final class Step {
        private final View target;
        @StringRes private final int titleRes;
        @StringRes private final int descRes;

        public Step(View target, @StringRes int titleRes, @StringRes int descRes) {
            this.target = target;
            this.titleRes = titleRes;
            this.descRes = descRes;
        }
    }

    public static void showIfFirstTime(@NonNull Fragment fragment,
                                       @NonNull PreferenceManager preferenceManager,
                                       @NonNull String tourKey,
                                       @NonNull List<Step> steps) {
        if (!fragment.isAdded()) {
            return;
        }
        View root = fragment.getView();
        if (root == null) {
            return;
        }
        root.post(() -> {
            Activity activity = fragment.getActivity();
            if (activity == null || activity.isFinishing() || !fragment.isAdded()) {
                return;
            }
            startSequence(activity, preferenceManager, tourKey, steps);
        });
    }

    public static void showIfFirstTime(@NonNull Activity activity,
                                       @NonNull PreferenceManager preferenceManager,
                                       @NonNull String tourKey,
                                       @NonNull List<Step> steps) {
        View root = activity.findViewById(android.R.id.content);
        if (root == null) {
            return;
        }
        showIfFirstTime(root, activity, preferenceManager, tourKey, steps);
    }

    /** Use when tour targets live inside a dialog, bottom sheet, or custom root. */
    public static void showIfFirstTime(@NonNull View anchor,
                                       @NonNull Activity activity,
                                       @NonNull PreferenceManager preferenceManager,
                                       @NonNull String tourKey,
                                       @NonNull List<Step> steps) {
        anchor.post(() -> {
            if (activity.isFinishing()) {
                return;
            }
            startSequence(activity, preferenceManager, tourKey, steps);
        });
    }

    private static void startSequence(@NonNull Activity activity,
                                      @NonNull PreferenceManager preferenceManager,
                                      @NonNull String tourKey,
                                      @NonNull List<Step> steps) {
        // Tours temporarily disabled (splash + all screen/dialog tours).
        if (!TOURS_ENABLED) {
            return;
        }
        if (preferenceManager.isTourSeen(tourKey)) {
            return;
        }

        List<TapTarget> targets = new ArrayList<>();
        for (Step step : steps) {
            if (step.target == null || step.target.getVisibility() != View.VISIBLE) {
                continue;
            }
            targets.add(buildTarget(activity, step));
        }

        if (targets.isEmpty()) {
            preferenceManager.setTourSeen(tourKey, true);
            return;
        }

        new TapTargetSequence(activity)
                .targets(targets)
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        preferenceManager.setTourSeen(tourKey, true);
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {}

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {
                        preferenceManager.setTourSeen(tourKey, true);
                    }
                })
                .continueOnCancel(true)
                .start();
    }

    private static TapTarget buildTarget(@NonNull Activity activity, @NonNull Step step) {
        int titleSize = spToPx(activity, 18);
        int descSize = spToPx(activity, 14);
        return TapTarget.forView(step.target,
                        activity.getString(step.titleRes),
                        activity.getString(step.descRes))
                .outerCircleColor(R.color.green_primary)
                .outerCircleAlpha(0.92f)
                .targetCircleColor(R.color.white)
                .titleTextColor(android.R.color.white)
                .descriptionTextColor(android.R.color.white)
                .textColor(android.R.color.white)
                .titleTextSize(titleSize)
                .descriptionTextSize(descSize)
                .dimColor(R.color.tour_dim_overlay)
                .drawShadow(false)
                .cancelable(true)
                .tintTarget(false)
                .transparentTarget(true)
                .targetRadius(52);
    }

    private static int spToPx(@NonNull Activity activity, int sp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, sp, activity.getResources().getDisplayMetrics());
    }

    /** Per logged-in user (main app screens). */
    public static String tourKeyForUser(@NonNull String baseKey, @NonNull PreferenceManager preferenceManager) {
        String userId = preferenceManager.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return baseKey;
        }
        return baseKey + "_" + userId;
    }
}
