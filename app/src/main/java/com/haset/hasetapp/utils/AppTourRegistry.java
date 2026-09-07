package com.haset.hasetapp.utils;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.haset.hasetapp.R;

import java.util.ArrayList;
import java.util.List;

/** Screen-specific first-time tour definitions (Phase 2). */
public final class AppTourRegistry {

    private AppTourRegistry() {}

    public static void showBookAppointment(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.cardInstantAppointment),
                R.string.tour_book_instant_title, R.string.tour_book_instant_desc);
        add(steps, activity.findViewById(R.id.cardScheduleAppointment),
                R.string.tour_book_schedule_title, R.string.tour_book_schedule_desc);
        add(steps, activity.findViewById(R.id.btnConfirmBooking),
                R.string.tour_book_confirm_title, R.string.tour_book_confirm_desc);
        show(activity, pm, AppTourHelper.TOUR_BOOK_APPOINTMENT, steps);
    }

    public static void showDoctors(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.etSearch),
                R.string.tour_doctors_search_title, R.string.tour_doctors_search_desc);
        add(steps, activity.findViewById(R.id.rvDoctors),
                R.string.tour_doctors_list_title, R.string.tour_doctors_list_desc);
        show(activity, pm, AppTourHelper.TOUR_DOCTORS, steps);
    }

    public static void showSearch(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.etSearch),
                R.string.tour_search_bar_title, R.string.tour_search_bar_desc);
        add(steps, activity.findViewById(R.id.rvSearchResults),
                R.string.tour_search_results_title, R.string.tour_search_results_desc);
        show(activity, pm, AppTourHelper.TOUR_SEARCH, steps);
    }

    public static void showSettings(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.switchNotification),
                R.string.tour_settings_notifications_title, R.string.tour_settings_notifications_desc);
        add(steps, activity.findViewById(R.id.tvLanguage),
                R.string.tour_settings_language_title, R.string.tour_settings_language_desc);
        add(steps, activity.findViewById(R.id.tvTheme),
                R.string.tour_settings_theme_title, R.string.tour_settings_theme_desc);
        add(steps, activity.findViewById(R.id.switchMfa),
                R.string.tour_settings_mfa_title, R.string.tour_settings_mfa_desc);
        add(steps, activity.findViewById(R.id.btnChangePassword),
                R.string.tour_settings_password_title, R.string.tour_settings_password_desc);
        add(steps, activity.findViewById(R.id.btnSupport),
                R.string.tour_settings_support_title, R.string.tour_settings_support_desc);
        show(activity, pm, AppTourHelper.TOUR_SETTINGS, steps);
    }

    public static void showEditProfile(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.layoutProfileImage),
                R.string.tour_edit_photo_title, R.string.tour_edit_photo_desc);
        add(steps, activity.findViewById(R.id.etFullName),
                R.string.tour_edit_name_title, R.string.tour_edit_name_desc);
        add(steps, activity.findViewById(R.id.btnSave),
                R.string.tour_edit_save_title, R.string.tour_edit_save_desc);
        show(activity, pm, AppTourHelper.TOUR_EDIT_PROFILE, steps);
    }

    public static void showNotifications(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.tabs),
                R.string.tour_notifications_tabs_title, R.string.tour_notifications_tabs_desc);
        add(steps, activity.findViewById(R.id.rvAppointmentNotifications),
                R.string.tour_notifications_list_title, R.string.tour_notifications_list_desc);
        add(steps, activity.findViewById(R.id.btnClearNotifications),
                R.string.tour_notifications_clear_title, R.string.tour_notifications_clear_desc);
        show(activity, pm, AppTourHelper.TOUR_NOTIFICATIONS, steps);
    }

    public static void showPayment(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.tvAmount),
                R.string.tour_payment_amount_title, R.string.tour_payment_amount_desc);
        add(steps, activity.findViewById(R.id.tvPaymentMethod),
                R.string.tour_payment_method_title, R.string.tour_payment_method_desc);
        add(steps, activity.findViewById(R.id.btnPayNow),
                R.string.tour_payment_pay_title, R.string.tour_payment_pay_desc);
        show(activity, pm, AppTourHelper.TOUR_PAYMENT, steps);
    }

    public static void showDoctorWallet(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.tvBalance),
                R.string.tour_wallet_balance_title, R.string.tour_wallet_balance_desc);
        add(steps, activity.findViewById(R.id.btnWithdraw),
                R.string.tour_wallet_withdraw_title, R.string.tour_wallet_withdraw_desc);
        add(steps, activity.findViewById(R.id.rvTransactions),
                R.string.tour_wallet_history_title, R.string.tour_wallet_history_desc);
        show(activity, pm, AppTourHelper.TOUR_DOCTOR_WALLET, steps);
    }

    public static void showDoctorPatients(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.patientList),
                R.string.tour_patients_list_title, R.string.tour_patients_list_desc);
        show(activity, pm, AppTourHelper.TOUR_DOCTOR_PATIENTS, steps);
    }

    public static void showDoctorEdit(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.etSpecialty),
                R.string.tour_doctor_edit_specialty_title, R.string.tour_doctor_edit_specialty_desc);
        add(steps, activity.findViewById(R.id.etConsultationFee),
                R.string.tour_doctor_edit_fee_title, R.string.tour_doctor_edit_fee_desc);
        add(steps, activity.findViewById(R.id.switchOnlineStatus),
                R.string.tour_doctor_edit_online_title, R.string.tour_doctor_edit_online_desc);
        show(activity, pm, AppTourHelper.TOUR_DOCTOR_EDIT, steps);
    }

    public static void showPrescriptions(@NonNull Fragment fragment,
                                         @NonNull PreferenceManager pm,
                                         @NonNull View root) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.recyclerViewPrescriptions),
                R.string.tour_prescriptions_list_title, R.string.tour_prescriptions_list_desc);
        View fab = root.findViewById(R.id.fabAddPrescription);
        if (fab != null && fab.getVisibility() == View.VISIBLE) {
            add(steps, fab, R.string.tour_prescriptions_add_title, R.string.tour_prescriptions_add_desc);
        }
        show(fragment, pm, AppTourHelper.TOUR_PRESCRIPTIONS, steps);
    }

    public static void showArticles(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.searchBar),
                R.string.tour_articles_search_title, R.string.tour_articles_search_desc);
        add(steps, activity.findViewById(R.id.tabsArticles),
                R.string.tour_articles_tabs_title, R.string.tour_articles_tabs_desc);
        show(activity, pm, AppTourHelper.TOUR_ARTICLES, steps);
    }

    public static void showArticleDetail(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.tvArticleTitle),
                R.string.tour_article_detail_title, R.string.tour_article_detail_desc);
        add(steps, activity.findViewById(R.id.layoutLikes),
                R.string.tour_article_likes_title, R.string.tour_article_likes_desc);
        add(steps, activity.findViewById(R.id.layoutComments),
                R.string.tour_article_comments_title, R.string.tour_article_comments_desc);
        show(activity, pm, AppTourHelper.TOUR_ARTICLE_DETAIL, steps);
    }

    public static void showHospitals(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.rvHospitals),
                R.string.tour_hospitals_list_title, R.string.tour_hospitals_list_desc);
        show(activity, pm, AppTourHelper.TOUR_HOSPITALS, steps);
    }

    public static void showChatRoom(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.rvMessages),
                R.string.tour_chat_messages_title, R.string.tour_chat_messages_desc);
        add(steps, activity.findViewById(R.id.ivAttach),
                R.string.tour_chat_attach_title, R.string.tour_chat_attach_desc);
        add(steps, activity.findViewById(R.id.btnSend),
                R.string.tour_chat_send_title, R.string.tour_chat_send_desc);
        show(activity, pm, AppTourHelper.TOUR_CHAT_ROOM, steps);
    }

    public static void showPrescriptionDetail(@NonNull Fragment fragment,
                                            @NonNull PreferenceManager pm,
                                            @NonNull View root) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.recyclerViewMedicines),
                R.string.tour_prescription_meds_title, R.string.tour_prescription_meds_desc);
        add(steps, root.findViewById(R.id.tvInstructions),
                R.string.tour_prescription_instructions_title, R.string.tour_prescription_instructions_desc);
        show(fragment, pm, AppTourHelper.TOUR_PRESCRIPTION_DETAIL, steps);
    }

    public static void showDoctorDetails(@NonNull Fragment fragment,
                                         @NonNull PreferenceManager pm,
                                         @NonNull View root) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.btnBookAppointment),
                R.string.tour_doctor_book_title, R.string.tour_doctor_book_desc);
        add(steps, root.findViewById(R.id.tvConsultationFee),
                R.string.tour_doctor_fee_title, R.string.tour_doctor_fee_desc);
        add(steps, root.findViewById(R.id.tvBio),
                R.string.tour_doctor_bio_title, R.string.tour_doctor_bio_desc);
        show(fragment, pm, AppTourHelper.TOUR_DOCTOR_DETAILS, steps);
    }

    public static void showAboutUs(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.btnPrivacyPolicy),
                R.string.tour_about_privacy_title, R.string.tour_about_privacy_desc);
        add(steps, activity.findViewById(R.id.btnTermsOfService),
                R.string.tour_about_terms_title, R.string.tour_about_terms_desc);
        show(activity, pm, AppTourHelper.TOUR_ABOUT_US, steps);
    }

    public static void showLogin(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.etEmail),
                R.string.tour_login_email_title, R.string.tour_login_email_desc);
        add(steps, activity.findViewById(R.id.etPassword),
                R.string.tour_login_password_title, R.string.tour_login_password_desc);
        add(steps, activity.findViewById(R.id.btnLogin),
                R.string.tour_login_sign_in_title, R.string.tour_login_sign_in_desc);
        showDevice(activity, pm, AppTourHelper.TOUR_LOGIN, steps);
    }

    public static void showRegister(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.etFullName),
                R.string.tour_register_name_title, R.string.tour_register_name_desc);
        add(steps, activity.findViewById(R.id.etEmail),
                R.string.tour_register_email_title, R.string.tour_register_email_desc);
        add(steps, activity.findViewById(R.id.btnRegister),
                R.string.tour_register_submit_title, R.string.tour_register_submit_desc);
        showDevice(activity, pm, AppTourHelper.TOUR_REGISTER, steps);
    }

    public static void showForgotPassword(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.etEmail),
                R.string.tour_forgot_email_title, R.string.tour_forgot_email_desc);
        add(steps, activity.findViewById(R.id.btnSend),
                R.string.tour_forgot_send_title, R.string.tour_forgot_send_desc);
        showDevice(activity, pm, AppTourHelper.TOUR_FORGOT_PASSWORD, steps);
    }

    public static void showRoleSelection(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.cardPatient),
                R.string.tour_role_patient_title, R.string.tour_role_patient_desc);
        add(steps, activity.findViewById(R.id.cardDoctor),
                R.string.tour_role_doctor_title, R.string.tour_role_doctor_desc);
        showDevice(activity, pm, AppTourHelper.TOUR_ROLE_SELECTION, steps);
    }

    public static void showServiceAgreement(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.btnPrivacyPolicy),
                R.string.tour_service_privacy_title, R.string.tour_service_privacy_desc);
        add(steps, activity.findViewById(R.id.btnTermsConditions),
                R.string.tour_service_terms_title, R.string.tour_service_terms_desc);
        show(activity, pm, AppTourHelper.TOUR_SERVICE_AGREEMENT, steps);
    }

    public static void showMfaEnrollment(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.mfaQrCard),
                R.string.tour_mfa_qr_title, R.string.tour_mfa_qr_desc);
        add(steps, activity.findViewById(R.id.mfaCodeInput),
                R.string.tour_mfa_code_title, R.string.tour_mfa_code_desc);
        add(steps, activity.findViewById(R.id.mfaConfirm),
                R.string.tour_mfa_confirm_title, R.string.tour_mfa_confirm_desc);
        show(activity, pm, AppTourHelper.TOUR_MFA_ENROLLMENT, steps);
    }

    public static void showHostedCheckout(@NonNull Activity activity,
                                          @NonNull PreferenceManager pm,
                                          @NonNull View checkoutArea) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, checkoutArea,
                R.string.tour_checkout_web_title, R.string.tour_checkout_web_desc);
        show(activity, pm, AppTourHelper.TOUR_HOSTED_CHECKOUT, steps);
    }

    public static void showFullScreenImage(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.ivFullScreen),
                R.string.tour_image_view_title, R.string.tour_image_view_desc);
        show(activity, pm, AppTourHelper.TOUR_FULL_SCREEN_IMAGE, steps);
    }

    public static void showVideoPlayer(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.videoView),
                R.string.tour_video_play_title, R.string.tour_video_play_desc);
        show(activity, pm, AppTourHelper.TOUR_VIDEO_PLAYER, steps);
    }

    public static void showPrescriptionShell(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.tvToolbarTitle),
                R.string.tour_prescription_shell_title, R.string.tour_prescription_shell_desc);
        show(activity, pm, AppTourHelper.TOUR_PRESCRIPTION_SHELL, steps);
    }

    public static void showDashboard(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.bottomNavigation),
                R.string.tour_dashboard_nav_title, R.string.tour_dashboard_nav_desc);
        add(steps, activity.findViewById(R.id.fragmentContainer),
                R.string.tour_dashboard_content_title, R.string.tour_dashboard_content_desc);
        show(activity, pm, AppTourHelper.TOUR_DASHBOARD, steps);
    }

    public static void showOnboarding(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.onboarding_viewpager),
                R.string.tour_onboarding_slides_title, R.string.tour_onboarding_slides_desc);
        add(steps, activity.findViewById(R.id.btn_next),
                R.string.tour_onboarding_next_title, R.string.tour_onboarding_next_desc);
        add(steps, activity.findViewById(R.id.btn_skip),
                R.string.tour_onboarding_skip_title, R.string.tour_onboarding_skip_desc);
        showDevice(activity, pm, AppTourHelper.TOUR_ONBOARDING, steps);
    }

    public static void showMain(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.main),
                R.string.tour_main_welcome_title, R.string.tour_main_welcome_desc);
        showDevice(activity, pm, AppTourHelper.TOUR_MAIN, steps);
    }

    public static void showShimmerTest(@NonNull Activity activity, @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, activity.findViewById(R.id.btnShowShimmer),
                R.string.tour_shimmer_show_title, R.string.tour_shimmer_show_desc);
        add(steps, activity.findViewById(R.id.btnListShimmer),
                R.string.tour_shimmer_list_title, R.string.tour_shimmer_list_desc);
        showDevice(activity, pm, AppTourHelper.TOUR_SHIMMER_TEST, steps);
    }

    public static void showWelcomeSheet(@NonNull View root, @NonNull Fragment fragment,
                                        @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.tvWelcomeTitle),
                R.string.tour_welcome_title, R.string.tour_welcome_desc);
        showDeviceFromRoot(root, fragment, pm, AppTourHelper.TOUR_WELCOME_SHEET, steps);
    }

    public static void showNoInternet(@NonNull View root, @NonNull Fragment fragment,
                                      @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.btnRetry),
                R.string.tour_no_internet_retry_title, R.string.tour_no_internet_retry_desc);
        showDeviceFromRoot(root, fragment, pm, AppTourHelper.TOUR_NO_INTERNET, steps);
    }

    public static void showResetPassword(@NonNull View root, @NonNull Fragment fragment,
                                         @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.etPasteLink),
                R.string.tour_reset_link_title, R.string.tour_reset_link_desc);
        add(steps, root.findViewById(R.id.etNewPassword),
                R.string.tour_reset_password_title, R.string.tour_reset_password_desc);
        add(steps, root.findViewById(R.id.btnResetPassword),
                R.string.tour_reset_submit_title, R.string.tour_reset_submit_desc);
        showDeviceFromRoot(root, fragment, pm, AppTourHelper.TOUR_RESET_PASSWORD, steps);
    }

    public static void showAddPrescription(@NonNull View root, @NonNull Fragment fragment,
                                           @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.actvPatient),
                R.string.tour_add_rx_patient_title, R.string.tour_add_rx_patient_desc);
        add(steps, root.findViewById(R.id.btnAddMedicine),
                R.string.tour_add_rx_medicine_title, R.string.tour_add_rx_medicine_desc);
        add(steps, root.findViewById(R.id.btnSubmitPrescription),
                R.string.tour_add_rx_submit_title, R.string.tour_add_rx_submit_desc);
        showFromRoot(root, fragment, pm, AppTourHelper.TOUR_ADD_PRESCRIPTION, steps);
    }

    public static void showPrescriptionBottomSheet(@NonNull View root, @NonNull Fragment fragment,
                                                   @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.rvMedicines),
                R.string.tour_rx_sheet_meds_title, R.string.tour_rx_sheet_meds_desc);
        add(steps, root.findViewById(R.id.btnExportPdf),
                R.string.tour_rx_sheet_export_title, R.string.tour_rx_sheet_export_desc);
        showFromRoot(root, fragment, pm, AppTourHelper.TOUR_PRESCRIPTION_BOTTOM_SHEET, steps);
    }

    public static void showAddService(@NonNull View root, @NonNull Fragment fragment,
                                      @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.etServiceName),
                R.string.tour_add_service_name_title, R.string.tour_add_service_name_desc);
        add(steps, root.findViewById(R.id.etAppointmentFee),
                R.string.tour_add_service_fee_title, R.string.tour_add_service_fee_desc);
        add(steps, root.findViewById(R.id.btnSend),
                R.string.tour_add_service_send_title, R.string.tour_add_service_send_desc);
        showFromRoot(root, fragment, pm, AppTourHelper.TOUR_ADD_SERVICE, steps);
    }

    public static void showFileAttachment(@NonNull View root, @NonNull Fragment fragment,
                                          @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.llImageOption),
                R.string.tour_attach_image_title, R.string.tour_attach_image_desc);
        add(steps, root.findViewById(R.id.llDocumentOption),
                R.string.tour_attach_doc_title, R.string.tour_attach_doc_desc);
        add(steps, root.findViewById(R.id.btnPrescriptionOption),
                R.string.tour_attach_rx_title, R.string.tour_attach_rx_desc);
        showFromRoot(root, fragment, pm, AppTourHelper.TOUR_FILE_ATTACHMENT, steps);
    }

    public static void showChatMoreOptions(@NonNull View root, @NonNull Fragment fragment,
                                           @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.optionSearchMessages),
                R.string.tour_chat_opt_search_title, R.string.tour_chat_opt_search_desc);
        add(steps, root.findViewById(R.id.optionViewContact),
                R.string.tour_chat_opt_contact_title, R.string.tour_chat_opt_contact_desc);
        showFromRoot(root, fragment, pm, AppTourHelper.TOUR_CHAT_MORE_OPTIONS, steps);
    }

    public static void showChatHeaderOptions(@NonNull View root, @NonNull Fragment fragment,
                                             @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.llSearchMessages),
                R.string.tour_chat_opt_search_title, R.string.tour_chat_opt_search_desc);
        add(steps, root.findViewById(R.id.llDeleteMessages),
                R.string.tour_chat_opt_delete_title, R.string.tour_chat_opt_delete_desc);
        showFromRoot(root, fragment, pm, AppTourHelper.TOUR_CHAT_HEADER_OPTIONS, steps);
    }

    public static void showChatManagement(@NonNull View root, @NonNull Fragment fragment,
                                          @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.llSearchMessages),
                R.string.tour_chat_opt_search_title, R.string.tour_chat_opt_search_desc);
        add(steps, root.findViewById(R.id.llDeleteMessages),
                R.string.tour_chat_opt_delete_title, R.string.tour_chat_opt_delete_desc);
        showFromRoot(root, fragment, pm, AppTourHelper.TOUR_CHAT_MANAGEMENT, steps);
    }

    public static void showPostComments(@NonNull View root, @NonNull Fragment fragment,
                                        @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.rvComments),
                R.string.tour_comments_list_title, R.string.tour_comments_list_desc);
        add(steps, root.findViewById(R.id.etCommentInput),
                R.string.tour_comments_input_title, R.string.tour_comments_input_desc);
        add(steps, root.findViewById(R.id.ivSendComment),
                R.string.tour_comments_send_title, R.string.tour_comments_send_desc);
        showFromRoot(root, fragment, pm, AppTourHelper.TOUR_POST_COMMENTS, steps);
    }

    public static void showLearnMore(@NonNull View root, @NonNull Fragment fragment,
                                     @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.btnLearnMore),
                R.string.tour_learn_more_btn_title, R.string.tour_learn_more_btn_desc);
        showFromRoot(root, fragment, pm, AppTourHelper.TOUR_LEARN_MORE, steps);
    }

    public static void showHospitalLocation(@NonNull View root, @NonNull Fragment fragment,
                                            @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.webViewHospitalMap),
                R.string.tour_hospital_map_title, R.string.tour_hospital_map_desc);
        add(steps, root.findViewById(R.id.btnOpenInMaps),
                R.string.tour_hospital_open_maps_title, R.string.tour_hospital_open_maps_desc);
        showFromRoot(root, fragment, pm, AppTourHelper.TOUR_HOSPITAL_LOCATION, steps);
    }

    public static void showAddHospital(@NonNull View root, @NonNull Fragment fragment,
                                       @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.cardHospitalImage),
                R.string.tour_add_hospital_photo_title, R.string.tour_add_hospital_photo_desc);
        add(steps, root.findViewById(R.id.etHospitalName),
                R.string.tour_add_hospital_name_title, R.string.tour_add_hospital_name_desc);
        add(steps, root.findViewById(R.id.btnSave),
                R.string.tour_add_hospital_save_title, R.string.tour_add_hospital_save_desc);
        showFromRoot(root, fragment, pm, AppTourHelper.TOUR_ADD_HOSPITAL, steps);
    }

    public static void showExportAppointments(@NonNull View root, @NonNull Activity activity,
                                              @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.rvSelectAppointments),
                R.string.tour_export_select_title, R.string.tour_export_select_desc);
        add(steps, root.findViewById(R.id.btnExportSelected),
                R.string.tour_export_btn_title, R.string.tour_export_btn_desc);
        AppTourHelper.showIfFirstTime(root, activity, pm,
                AppTourHelper.tourKeyForUser(AppTourHelper.TOUR_EXPORT_APPOINTMENTS, pm), steps);
    }

    public static void showAppointmentsUpcomingTab(@NonNull View root, @NonNull Fragment fragment,
                                                   @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.rvAppointments),
                R.string.tour_appt_tab_upcoming_title, R.string.tour_appt_tab_upcoming_desc);
        showFromRoot(root, fragment, pm, AppTourHelper.TOUR_APPOINTMENTS_UPCOMING, steps);
    }

    public static void showAppointmentsCompletedTab(@NonNull View root, @NonNull Fragment fragment,
                                                    @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.rvAppointments),
                R.string.tour_appt_tab_completed_title, R.string.tour_appt_tab_completed_desc);
        showFromRoot(root, fragment, pm, AppTourHelper.TOUR_APPOINTMENTS_COMPLETED_TAB, steps);
    }

    public static void showAppointmentsCancelledTab(@NonNull View root, @NonNull Fragment fragment,
                                                    @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.rvAppointments),
                R.string.tour_appt_tab_cancelled_title, R.string.tour_appt_tab_cancelled_desc);
        showFromRoot(root, fragment, pm, AppTourHelper.TOUR_APPOINTMENTS_CANCELLED_TAB, steps);
    }

    public static void showArticleFeed(@NonNull View root, @NonNull Fragment fragment,
                                       @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.rvPosts),
                R.string.tour_article_feed_title, R.string.tour_article_feed_desc);
        add(steps, root.findViewById(R.id.swipeRefresh),
                R.string.tour_article_refresh_title, R.string.tour_article_refresh_desc);
        showFromRoot(root, fragment, pm, AppTourHelper.TOUR_ARTICLE_FEED, steps);
    }

    public static void showContactUs(@NonNull View root, @NonNull Activity activity,
                                     @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.llEmail),
                R.string.tour_contact_email_title, R.string.tour_contact_email_desc);
        add(steps, root.findViewById(R.id.llWhatsApp),
                R.string.tour_contact_whatsapp_title, R.string.tour_contact_whatsapp_desc);
        AppTourHelper.showIfFirstTime(root, activity, pm,
                AppTourHelper.tourKeyForUser(AppTourHelper.TOUR_CONTACT_US, pm), steps);
    }

    public static void showLanguage(@NonNull View root, @NonNull Activity activity,
                                    @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.llEnglish),
                R.string.tour_language_english_title, R.string.tour_language_english_desc);
        add(steps, root.findViewById(R.id.llSwahili),
                R.string.tour_language_swahili_title, R.string.tour_language_swahili_desc);
        AppTourHelper.showIfFirstTime(root, activity, pm,
                AppTourHelper.tourKeyForUser(AppTourHelper.TOUR_LANGUAGE, pm), steps);
    }

    public static void showPostComments(@NonNull View root, @NonNull Activity activity,
                                        @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.rvComments),
                R.string.tour_comments_list_title, R.string.tour_comments_list_desc);
        add(steps, root.findViewById(R.id.etCommentInput),
                R.string.tour_comments_input_title, R.string.tour_comments_input_desc);
        add(steps, root.findViewById(R.id.ivSendComment),
                R.string.tour_comments_send_title, R.string.tour_comments_send_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_POST_COMMENTS, steps);
    }

    public static void showSplash(@NonNull View root, @NonNull Activity activity,
                                  @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.tvAppName),
                R.string.tour_splash_title, R.string.tour_splash_desc);
        showDeviceFromRootActivity(root, activity, pm, AppTourHelper.TOUR_SPLASH, steps);
    }

    public static void showPaymentMethod(@NonNull View root, @NonNull Activity activity,
                                         @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.llMobileMoney),
                R.string.tour_payment_method_mobile_title, R.string.tour_payment_method_mobile_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_PAYMENT_METHOD, steps);
    }

    public static void showPaymentMobileProviders(@NonNull View root, @NonNull Activity activity,
                                                  @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.llMpesa),
                R.string.tour_payment_providers_title, R.string.tour_payment_providers_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_PAYMENT_MOBILE_PROVIDERS, steps);
    }

    public static void showPaymentMobileNumber(@NonNull View root, @NonNull Activity activity,
                                               @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.etMobileNumber),
                R.string.tour_payment_mobile_number_title, R.string.tour_payment_mobile_number_desc);
        add(steps, root.findViewById(R.id.btnConfirm),
                R.string.tour_payment_confirm_title, R.string.tour_payment_confirm_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_PAYMENT_MOBILE_NUMBER, steps);
    }

    public static void showPaymentSuccess(@NonNull View root, @NonNull Activity activity,
                                          @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.btnDone),
                R.string.tour_payment_success_title, R.string.tour_payment_success_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_PAYMENT_SUCCESS, steps);
    }

    public static void showPaymentError(@NonNull View root, @NonNull Activity activity,
                                      @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.btnRetry),
                R.string.tour_payment_error_retry_title, R.string.tour_payment_error_retry_desc);
        add(steps, root.findViewById(R.id.btnSecondary),
                R.string.tour_payment_error_status_title, R.string.tour_payment_error_status_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_PAYMENT_ERROR, steps);
    }

    public static void showPaymentAbort(@NonNull View root, @NonNull Activity activity,
                                        @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.btnRetry),
                R.string.tour_payment_abort_wait_title, R.string.tour_payment_abort_wait_desc);
        add(steps, root.findViewById(R.id.btnSecondary),
                R.string.tour_payment_abort_confirm_title, R.string.tour_payment_abort_confirm_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_PAYMENT_ABORT, steps);
    }

    public static void showWithdraw(@NonNull View root, @NonNull Activity activity,
                                    @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.etAmount),
                R.string.tour_withdraw_amount_title, R.string.tour_withdraw_amount_desc);
        add(steps, root.findViewById(R.id.llMobileMoney),
                R.string.tour_withdraw_destination_title, R.string.tour_withdraw_destination_desc);
        add(steps, root.findViewById(R.id.mfaCodeInput),
                R.string.tour_withdraw_mfa_title, R.string.tour_withdraw_mfa_desc);
        add(steps, root.findViewById(R.id.btnConfirmWithdraw),
                R.string.tour_withdraw_confirm_title, R.string.tour_withdraw_confirm_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_WITHDRAW, steps);
    }

    public static void showPayoutDestination(@NonNull View root, @NonNull Activity activity,
                                             @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.etPayoutProvider),
                R.string.tour_payout_provider_title, R.string.tour_payout_provider_desc);
        add(steps, root.findViewById(R.id.etPayoutPhone),
                R.string.tour_payout_phone_title, R.string.tour_payout_phone_desc);
        add(steps, root.findViewById(R.id.destinationMfaCodeInput),
                R.string.tour_payout_mfa_title, R.string.tour_payout_mfa_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_PAYOUT_DESTINATION, steps);
    }

    public static void showChangePassword(@NonNull View root, @NonNull Activity activity,
                                          @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.etOldPassword),
                R.string.tour_change_password_current_title, R.string.tour_change_password_current_desc);
        add(steps, root.findViewById(R.id.etNewPassword),
                R.string.tour_change_password_new_title, R.string.tour_change_password_new_desc);
        add(steps, root.findViewById(R.id.btnUpdatePassword),
                R.string.tour_change_password_save_title, R.string.tour_change_password_save_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_CHANGE_PASSWORD, steps);
    }

    public static void showThemeSelector(@NonNull View root, @NonNull Activity activity,
                                         @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.layoutLight),
                R.string.tour_theme_light_title, R.string.tour_theme_light_desc);
        add(steps, root.findViewById(R.id.layoutDark),
                R.string.tour_theme_dark_title, R.string.tour_theme_dark_desc);
        add(steps, root.findViewById(R.id.layoutSystem),
                R.string.tour_theme_system_title, R.string.tour_theme_system_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_THEME_SELECTOR, steps);
    }

    public static void showSupportOptions(@NonNull View root, @NonNull Activity activity,
                                          @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.btnWhatsApp),
                R.string.tour_support_whatsapp_title, R.string.tour_support_whatsapp_desc);
        add(steps, root.findViewById(R.id.btnCall),
                R.string.tour_support_call_title, R.string.tour_support_call_desc);
        add(steps, root.findViewById(R.id.btnBugReport),
                R.string.tour_support_bug_title, R.string.tour_support_bug_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_SUPPORT_OPTIONS, steps);
    }

    public static void showBugReport(@NonNull View root, @NonNull Activity activity,
                                     @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.etBugReport),
                R.string.tour_bug_report_input_title, R.string.tour_bug_report_input_desc);
        add(steps, root.findViewById(R.id.btnSubmit),
                R.string.tour_bug_report_submit_title, R.string.tour_bug_report_submit_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_BUG_REPORT, steps);
    }

    public static void showProfileQr(@NonNull View root, @NonNull Activity activity,
                                     @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.ivQRCode),
                R.string.tour_profile_qr_code_title, R.string.tour_profile_qr_code_desc);
        add(steps, root.findViewById(R.id.llCopyLink),
                R.string.tour_profile_qr_copy_title, R.string.tour_profile_qr_copy_desc);
        add(steps, root.findViewById(R.id.llShareQR),
                R.string.tour_profile_qr_share_title, R.string.tour_profile_qr_share_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_PROFILE_QR, steps);
    }

    public static void showDoctorPolicy(@NonNull View root, @NonNull Activity activity,
                                        @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.scrollDoctorPolicy),
                R.string.tour_doctor_policy_title, R.string.tour_doctor_policy_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_DOCTOR_POLICY, steps);
    }

    public static void showMfaChallenge(@NonNull View root, @NonNull Activity activity,
                                        @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.mfaCodeInput),
                R.string.tour_mfa_challenge_code_title, R.string.tour_mfa_challenge_code_desc);
        add(steps, root.findViewById(R.id.mfaVerify),
                R.string.tour_mfa_challenge_verify_title, R.string.tour_mfa_challenge_verify_desc);
        add(steps, root.findViewById(R.id.mfaUseRecovery),
                R.string.tour_mfa_challenge_recovery_title, R.string.tour_mfa_challenge_recovery_desc);
        showDeviceFromRootActivity(root, activity, pm, AppTourHelper.TOUR_MFA_CHALLENGE, steps);
    }

    public static void showExportResult(@NonNull View root, @NonNull Activity activity,
                                        @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.btnOpenLocation),
                R.string.tour_export_result_open_title, R.string.tour_export_result_open_desc);
        add(steps, root.findViewById(R.id.btnShare),
                R.string.tour_export_result_share_title, R.string.tour_export_result_share_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_EXPORT_RESULT, steps);
    }

    public static void showReschedule(@NonNull View root, @NonNull Activity activity,
                                      @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.btnSelectDate),
                R.string.tour_reschedule_date_title, R.string.tour_reschedule_date_desc);
        add(steps, root.findViewById(R.id.btnSelectTime),
                R.string.tour_reschedule_time_title, R.string.tour_reschedule_time_desc);
        add(steps, root.findViewById(R.id.btnConfirm),
                R.string.tour_reschedule_confirm_title, R.string.tour_reschedule_confirm_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_RESCHEDULE, steps);
    }

    public static void showChatStart(@NonNull View root, @NonNull Activity activity,
                                     @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.tvCountdown),
                R.string.tour_chat_start_countdown_title, R.string.tour_chat_start_countdown_desc);
        add(steps, root.findViewById(R.id.btnStartChat),
                R.string.tour_chat_start_btn_title, R.string.tour_chat_start_btn_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_CHAT_START, steps);
    }

    public static void showDoctorsMenu(@NonNull View root, @NonNull Activity activity,
                                       @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.tvSortName),
                R.string.tour_doctors_sort_title, R.string.tour_doctors_sort_desc);
        add(steps, root.findViewById(R.id.tvFilterSpecialty),
                R.string.tour_doctors_filter_title, R.string.tour_doctors_filter_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_DOCTORS_MENU, steps);
    }

    public static void showArticleOptions(@NonNull View root, @NonNull Activity activity,
                                          @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.optionSave),
                R.string.tour_article_options_save_title, R.string.tour_article_options_save_desc);
        add(steps, root.findViewById(R.id.optionAddToFavorite),
                R.string.tour_article_options_favorite_title, R.string.tour_article_options_favorite_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_ARTICLE_OPTIONS, steps);
    }

    public static void showAboutHArticle(@NonNull View root, @NonNull Activity activity,
                                         @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.btnClose),
                R.string.tour_about_h_article_title, R.string.tour_about_h_article_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_ABOUT_H_ARTICLE, steps);
    }

    public static void showArticleNotificationSettings(@NonNull View root, @NonNull Activity activity,
                                                         @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.switchArticles),
                R.string.tour_article_notif_articles_title, R.string.tour_article_notif_articles_desc);
        add(steps, root.findViewById(R.id.btnSaveSettings),
                R.string.tour_article_notif_save_title, R.string.tour_article_notif_save_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_ARTICLE_NOTIFICATION_SETTINGS, steps);
    }

    public static void showVoiceRecordingSheet(@NonNull View root, @NonNull Activity activity,
                                               @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.btnRecord),
                R.string.tour_voice_sheet_record_title, R.string.tour_voice_sheet_record_desc);
        add(steps, root.findViewById(R.id.btnClose),
                R.string.tour_voice_sheet_close_title, R.string.tour_voice_sheet_close_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_VOICE_RECORDING_SHEET, steps);
    }

    public static void showVoiceRecordingDialog(@NonNull View root, @NonNull Activity activity,
                                                @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.btnStop),
                R.string.tour_voice_dialog_stop_title, R.string.tour_voice_dialog_stop_desc);
        add(steps, root.findViewById(R.id.btnCancel),
                R.string.tour_voice_dialog_cancel_title, R.string.tour_voice_dialog_cancel_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_VOICE_RECORDING_DIALOG, steps);
    }

    public static void showResubmitDocuments(@NonNull View root, @NonNull Activity activity,
                                             @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.etResubmitNin),
                R.string.tour_resubmit_nin_title, R.string.tour_resubmit_nin_desc);
        add(steps, root.findViewById(R.id.btnChooseNinDocument),
                R.string.tour_resubmit_docs_title, R.string.tour_resubmit_docs_desc);
        add(steps, root.findViewById(R.id.btnSubmitResubmitDocuments),
                R.string.tour_resubmit_submit_title, R.string.tour_resubmit_submit_desc);
        showFromRootActivity(root, activity, pm, AppTourHelper.TOUR_RESUBMIT_DOCUMENTS, steps);
    }

    public static void showComingSoon(@NonNull View root, @NonNull Activity activity,
                                      @NonNull PreferenceManager pm) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.btnOk),
                R.string.tour_coming_soon_title, R.string.tour_coming_soon_desc);
        showDeviceFromRootActivity(root, activity, pm, AppTourHelper.TOUR_COMING_SOON, steps);
    }

    public static void showCustomDialog(@NonNull View root, @NonNull Activity activity,
                                        @NonNull PreferenceManager pm, @NonNull String baseKey,
                                        boolean includeNegative) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.tvDialogMessage),
                R.string.tour_custom_dialog_message_title, R.string.tour_custom_dialog_message_desc);
        add(steps, root.findViewById(R.id.btnPositive),
                R.string.tour_custom_dialog_confirm_title, R.string.tour_custom_dialog_confirm_desc);
        if (includeNegative) {
            add(steps, root.findViewById(R.id.btnNegative),
                    R.string.tour_custom_dialog_cancel_title, R.string.tour_custom_dialog_cancel_desc);
        }
        showFromRootActivity(root, activity, pm, baseKey, steps);
    }

    public static void showCustomDialogDevice(@NonNull View root, @NonNull Activity activity,
                                              @NonNull PreferenceManager pm, @NonNull String tourKey,
                                              boolean includeNegative) {
        List<AppTourHelper.Step> steps = new ArrayList<>();
        add(steps, root.findViewById(R.id.tvDialogMessage),
                R.string.tour_custom_dialog_message_title, R.string.tour_custom_dialog_message_desc);
        add(steps, root.findViewById(R.id.btnPositive),
                R.string.tour_custom_dialog_confirm_title, R.string.tour_custom_dialog_confirm_desc);
        if (includeNegative) {
            add(steps, root.findViewById(R.id.btnNegative),
                    R.string.tour_custom_dialog_cancel_title, R.string.tour_custom_dialog_cancel_desc);
        }
        showDeviceFromRootActivity(root, activity, pm, tourKey, steps);
    }

    private static void add(List<AppTourHelper.Step> steps, View target,
                            int titleRes, int descRes) {
        if (target != null) {
            steps.add(new AppTourHelper.Step(target, titleRes, descRes));
        }
    }

    private static void show(@NonNull Activity activity,
                             @NonNull PreferenceManager pm,
                             @NonNull String baseKey,
                             @NonNull List<AppTourHelper.Step> steps) {
        AppTourHelper.showIfFirstTime(activity, pm,
                AppTourHelper.tourKeyForUser(baseKey, pm), steps);
    }

    /** Once per device — login, register, role selection, etc. */
    private static void showDevice(@NonNull Activity activity,
                                   @NonNull PreferenceManager pm,
                                   @NonNull String tourKey,
                                   @NonNull List<AppTourHelper.Step> steps) {
        AppTourHelper.showIfFirstTime(activity, pm, tourKey, steps);
    }

    private static void show(@NonNull Fragment fragment,
                             @NonNull PreferenceManager pm,
                             @NonNull String baseKey,
                             @NonNull List<AppTourHelper.Step> steps) {
        AppTourHelper.showIfFirstTime(fragment, pm,
                AppTourHelper.tourKeyForUser(baseKey, pm), steps);
    }

    private static void showFromRoot(@NonNull View root,
                                     @NonNull Fragment fragment,
                                     @NonNull PreferenceManager pm,
                                     @NonNull String baseKey,
                                     @NonNull List<AppTourHelper.Step> steps) {
        if (!fragment.isAdded()) {
            return;
        }
        android.app.Activity activity = fragment.getActivity();
        if (activity == null) {
            return;
        }
        AppTourHelper.showIfFirstTime(root, activity, pm,
                AppTourHelper.tourKeyForUser(baseKey, pm), steps);
    }

    private static void showDeviceFromRoot(@NonNull View root,
                                           @NonNull Fragment fragment,
                                           @NonNull PreferenceManager pm,
                                           @NonNull String tourKey,
                                           @NonNull List<AppTourHelper.Step> steps) {
        if (!fragment.isAdded()) {
            return;
        }
        android.app.Activity activity = fragment.getActivity();
        if (activity == null) {
            return;
        }
        AppTourHelper.showIfFirstTime(root, activity, pm, tourKey, steps);
    }

    private static void showFromRootActivity(@NonNull View root,
                                             @NonNull Activity activity,
                                             @NonNull PreferenceManager pm,
                                             @NonNull String baseKey,
                                             @NonNull List<AppTourHelper.Step> steps) {
        AppTourHelper.showIfFirstTime(root, activity, pm,
                AppTourHelper.tourKeyForUser(baseKey, pm), steps);
    }

    private static void showDeviceFromRootActivity(@NonNull View root,
                                                   @NonNull Activity activity,
                                                   @NonNull PreferenceManager pm,
                                                   @NonNull String tourKey,
                                                   @NonNull List<AppTourHelper.Step> steps) {
        AppTourHelper.showIfFirstTime(root, activity, pm, tourKey, steps);
    }
}
