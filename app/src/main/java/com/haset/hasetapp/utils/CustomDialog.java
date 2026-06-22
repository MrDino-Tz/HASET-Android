package com.haset.hasetapp.utils;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.os.Build;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;

import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;

/**
 * CustomDialog - Reusable dialog with custom background, rounded corners, and icons
 * Can be used for all popups, confirmations, and load logs throughout the app
 */
public class CustomDialog {

    public static void showInfo(Context context, String appointmentDetails, String s, String ok, Object o) {
    }

    public enum DialogType {
        WARNING(R.drawable.ic_warning, R.drawable.circle_background_warning, R.color.warning_color),
        ERROR(R.drawable.ic_error, R.drawable.circle_background_error, R.color.colorError),
        SUCCESS(R.drawable.ic_success, R.drawable.circle_background_success, R.color.success_color),
        INFO(R.drawable.ic_info, R.drawable.circle_background_info, R.color.info_color);
        
        private final int iconRes;
        private final int backgroundRes;
        private final int colorRes;
        
        DialogType(int iconRes, int backgroundRes, int colorRes) {
            this.iconRes = iconRes;
            this.backgroundRes = backgroundRes;
            this.colorRes = colorRes;
        }
        
        public int getIconRes() { return iconRes; }
        public int getBackgroundRes() { return backgroundRes; }
        public int getColorRes() { return colorRes; }
    }
    
    private final Context context;
    private AlertDialog.Builder builder;
    private AlertDialog dialog;
    private View customView;
    
    private ImageView ivIcon;
    private TextView tvTitle;
    private TextView tvMessage;
    private MaterialButton btnPositive;
    private MaterialButton btnNegative;
    
    public CustomDialog(Context context) {
        this.context = context;
        initializeDialog();
    }
    
    private void initializeDialog() {
        builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        customView = inflater.inflate(R.layout.custom_dialog_layout, null);
        
        // Initialize views
        ivIcon = customView.findViewById(R.id.ivDialogIcon);
        tvTitle = customView.findViewById(R.id.tvDialogTitle);
        tvMessage = customView.findViewById(R.id.tvDialogMessage);
        btnPositive = customView.findViewById(R.id.btnPositive);
        btnNegative = customView.findViewById(R.id.btnNegative);
        
        builder.setView(customView);
    }
    
    /**
     * Set dialog type (WARNING, ERROR, SUCCESS, INFO)
     */
    public CustomDialog setDialogType(DialogType type) {
        ivIcon.setImageResource(type.getIconRes());
        ivIcon.setBackgroundResource(type.getBackgroundRes());
        ivIcon.setColorFilter(context.getColor(type.getColorRes()));
        return this;
    }
    
    /**
     * Set dialog title
     */
    public CustomDialog setTitle(String title) {
        tvTitle.setText(title);
        return this;
    }
    
    /**
     * Set dialog message
     */
    public CustomDialog setMessage(String message) {
        tvMessage.setText(message);
        return this;
    }
    
    /**
     * Set positive button text and click listener
     */
    public CustomDialog setPositiveButton(String text, View.OnClickListener listener) {
        btnPositive.setText(text);
        btnPositive.setOnClickListener(listener);
        return this;
    }
    
    /**
     * Set negative button text and click listener
     */
    public CustomDialog setNegativeButton(String text, View.OnClickListener listener) {
        btnNegative.setText(text);
        btnNegative.setOnClickListener(listener);
        return this;
    }
    
    /**
     * Hide negative button
     */
    public CustomDialog hideNegativeButton() {
        btnNegative.setVisibility(View.GONE);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnPositive.setLayoutParams(params);
        return this;
    }

    /**
     * Hide positive button
     */
    public CustomDialog hidePositiveButton() {
        btnPositive.setVisibility(View.GONE);
        return this;
    }
    
    /**
     * Set positive button color
     */
    public CustomDialog setPositiveButtonColor(int colorRes) {
        btnPositive.setBackgroundTintList(context.getResources().getColorStateList(colorRes));
        return this;
    }
    
    /**
     * Set negative button color
     */
    public CustomDialog setNegativeButtonColor(int colorRes) {
        btnNegative.setBackgroundTintList(context.getResources().getColorStateList(colorRes));
        return this;
    }
    
    /**
     * Show the dialog
     */
    public CustomDialog show() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = builder.create();
        dialog.show();
        
        // Make dialog background transparent to show custom background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        // Add ripple/pulse animation to the icon
        View contentView = dialog.findViewById(R.id.ivDialogIcon);
        if (contentView != null) {
            animateIcon(contentView);
        }
        
        return this;
    }
    
    /**
     * Animate the dialog icon with pulse effect
     */
    private void animateIcon(View iconView) {
        // Scale animation - pop in effect
        ScaleAnimation scaleAnim = new ScaleAnimation(
            0.5f, 1.0f, // fromX, toX
            0.5f, 1.0f, // fromY, toY
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnim.setDuration(400);
        scaleAnim.setInterpolator(new OvershootInterpolator(1.5f));
        
        // Fade in animation
        AlphaAnimation fadeAnim = new AlphaAnimation(0.0f, 1.0f);
        fadeAnim.setDuration(300);
        
        // Combine animations
        AnimationSet animSet = new AnimationSet(true);
        animSet.addAnimation(scaleAnim);
        animSet.addAnimation(fadeAnim);
        
        iconView.startAnimation(animSet);
    }
    
    /**
     * Dismiss the dialog
     */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
    
    /**
     * Check if dialog is showing
     */
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
    
    /**
     * Get the underlying AlertDialog for advanced customization
     */
    public AlertDialog getDialog() {
        return dialog;
    }
    
    /**
     * Show a single choice dialog
     */
    public static void showSingleChoiceDialog(Context context, String title, String[] choices, int selectedIndex, 
                                             OnChoiceSelectedListener listener, String positiveText, String negativeText) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View customView = inflater.inflate(R.layout.custom_choice_dialog, null);
        
        // Initialize views
        ImageView ivIcon = customView.findViewById(R.id.ivDialogIcon);
        TextView tvTitle = customView.findViewById(R.id.tvDialogTitle);
        RadioGroup radioGroup = customView.findViewById(R.id.radioGroupChoices);
        MaterialButton btnPositive = customView.findViewById(R.id.btnPositive);
        MaterialButton btnNegative = customView.findViewById(R.id.btnNegative);
        
        // Set title
        tvTitle.setText(title);
        
        // Create radio buttons
        for (int i = 0; i < choices.length; i++) {
            android.widget.RadioButton radioButton = new android.widget.RadioButton(context);
            radioButton.setText(choices[i]);
            radioButton.setId(i);
            radioGroup.addView(radioButton);
            
            if (i == selectedIndex) {
                radioButton.setChecked(true);
            }
        }
        
        // Build dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(customView);
        
        AlertDialog dialog = builder.create();
        
        // Set button listeners
        btnPositive.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId != -1 && listener != null) {
                listener.onChoiceSelected(selectedId, choices[selectedId]);
            }
            dialog.dismiss();
        });
        
        btnNegative.setOnClickListener(v -> dialog.dismiss());
        
        // Set button texts
        if (positiveText != null) btnPositive.setText(positiveText);
        if (negativeText != null) btnNegative.setText(negativeText);
        
        // Make dialog background transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        dialog.show();
    }
    
    /**
     * Interface for choice selection callback
     */
    public interface OnChoiceSelectedListener {
        void onChoiceSelected(int index, String choice);
    }
    
    // Convenience methods for common dialog types
    
    /**
     * Show warning dialog
     */
    public static CustomDialog showWarning(Context context, String title, String message, 
                                         String positiveText, View.OnClickListener positiveListener,
                                         String negativeText, View.OnClickListener negativeListener) {
        return new CustomDialog(context)
                .setDialogType(DialogType.WARNING)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, positiveListener)
                .setNegativeButton(negativeText, negativeListener)
                .setPositiveButtonColor(R.color.warning_color)
                .show();
    }
    
    /**
     * Show error dialog
     */
    public static CustomDialog showError(Context context, String title, String message, 
                                       String positiveText, View.OnClickListener positiveListener) {
        return new CustomDialog(context)
                .setDialogType(DialogType.ERROR)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, positiveListener)
                .hideNegativeButton()
                .setPositiveButtonColor(R.color.colorError)
                .show();
    }
    
    /**
     * Show success dialog
     */
    public static CustomDialog showSuccess(Context context, String title, String message, 
                                         String positiveText, View.OnClickListener positiveListener) {
        return new CustomDialog(context)
                .setDialogType(DialogType.SUCCESS)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, positiveListener)
                .hideNegativeButton()
                .setPositiveButtonColor(R.color.success_color)
                .show();
    }
    
    /**
     * Show info dialog
     */
    public static CustomDialog showInfo(Context context, String title, String message, 
                                       String positiveText, View.OnClickListener positiveListener,
                                       String negativeText, View.OnClickListener negativeListener) {
        return new CustomDialog(context)
                .setDialogType(DialogType.INFO)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, positiveListener)
                .setNegativeButton(negativeText, negativeListener)
                .setPositiveButtonColor(R.color.info_color)
                .show();
    }
    private static AlertDialog loadingDialog;
    private static android.app.Dialog fullScreenLoadingDialog;

    /**
     * Show full screen loading dialog
     */
    public static void showFullScreenLoading(Context context, String message) {
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (activity.isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) {
                return;
            }
        }

        hideFullScreenLoading();
        
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setCancelable(false);
        
        View view = LayoutInflater.from(context).inflate(R.layout.layout_loading_dialog, null);
        
        TextView tvMessage = view.findViewById(R.id.tvLoadingMessage);
        if (tvMessage != null) {
            tvMessage.setText(message);
        }
        
        dialog.setContentView(view);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, 
                                         android.view.WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        try {
            fullScreenLoadingDialog = dialog;
            dialog.show();
        } catch (Exception e) {
            android.util.Log.e("CustomDialog", "Error showing full screen loading: " + e.getMessage());
            fullScreenLoadingDialog = null;
        }
    }
    
    /**
     * Hide full screen loading dialog
     */
    public static void hideFullScreenLoading() {
        try {
            if (fullScreenLoadingDialog != null && fullScreenLoadingDialog.isShowing()) {
                fullScreenLoadingDialog.dismiss();
            }
        } catch (Exception e) {
            android.util.Log.e("CustomDialog", "Error hiding full screen loading: " + e.getMessage());
        } finally {
            fullScreenLoadingDialog = null;
        }
    }
    
    /**
     * Show loading dialog
     */
    public static void showLoading(Context context, String message) {
        showFullScreenLoading(context, message);
    }
    
    /**
     * Hide loading dialog
     */
    public static void hideLoading() {
        hideFullScreenLoading();
    }
}
