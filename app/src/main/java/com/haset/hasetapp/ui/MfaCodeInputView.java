package com.haset.hasetapp.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

/** Reusable six-digit MFA input. Call setErrorState on a failed server response. */
public final class MfaCodeInputView extends LinearLayout {
    private final EditText[] boxes = new EditText[6];
    private static final int BORDER_DEFAULT = Color.rgb(203, 206, 210);
    private static final int BORDER_ERROR = Color.rgb(211, 47, 47);
    private static final int BORDER_SUCCESS = Color.rgb(0, 136, 0);
    private static final int TEXT_COLOR = Color.rgb(31, 41, 55);
    /** Keep digits only; strip spaces/dashes so paste of "123 456" still works. */
    private static final InputFilter DIGITS_ONLY_KEEP = (source, start, end, dest, dstart, dend) -> {
        boolean changed = false;
        StringBuilder kept = new StringBuilder(end - start);
        for (int i = start; i < end; i++) {
            char c = source.charAt(i);
            if (Character.isDigit(c)) kept.append(c);
            else changed = true;
        }
        return changed ? kept : null;
    };
    private int borderColor = BORDER_DEFAULT;
    private boolean distributing;

    public MfaCodeInputView(Context context) {
        super(context);
        initialize(context);
    }

    public MfaCodeInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public MfaCodeInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            EditText box = new EditText(context);
            boxes[i] = box;
            box.setGravity(Gravity.CENTER);
            box.setTextSize(22);
            box.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
            box.setTextColor(TEXT_COLOR);
            box.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            box.setTransformationMethod(PasswordTransformationMethod.getInstance());
            box.setSingleLine(true);
            box.setSelectAllOnFocus(true);
            box.setPadding(0, 0, 0, 0);
            box.setBackground(boxBackground(BORDER_DEFAULT, false));
            // Length 6 so a full OTP paste reaches the watcher; distribute() then fills each box.
            box.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6), DIGITS_ONLY_KEEP});
            LayoutParams params = new LayoutParams(dp(40), dp(50));
            if (i > 0) params.setMarginStart(dp(8));
            addView(box, params);
            box.setOnFocusChangeListener((view, hasFocus) -> refreshBorders());
            box.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                public void onTextChanged(CharSequence s, int st, int before, int count) {
                    if (distributing) return;
                    clearError();
                    String digits = s.toString().replaceAll("\\D", "");
                    if (digits.length() > 1) {
                        distribute(digits);
                        return;
                    }
                    if (digits.length() == 1 && index < boxes.length - 1) boxes[index + 1].requestFocus();
                }
                public void afterTextChanged(Editable e) {}
            });
            box.setOnKeyListener((v, key, event) -> {
                if (key == android.view.KeyEvent.KEYCODE_DEL && event.getAction() == android.view.KeyEvent.ACTION_DOWN
                        && box.getText().length() == 0 && index > 0) { boxes[index - 1].requestFocus(); return true; }
                return false;
            });
        }
    }

    /** Spread a pasted multi-digit OTP across the six boxes (always from the start). */
    private void distribute(String digits) {
        distributing = true;
        try {
            for (int j = 0; j < boxes.length; j++) {
                boxes[j].setText(j < digits.length() ? String.valueOf(digits.charAt(j)) : "");
            }
            int focus = Math.min(digits.length(), boxes.length) - 1;
            if (focus < 0) focus = 0;
            boxes[focus].requestFocus();
            boxes[focus].setSelection(boxes[focus].getText().length());
        } finally {
            distributing = false;
        }
    }

    private GradientDrawable boxBackground(int color, boolean active) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.TRANSPARENT);
        background.setCornerRadius(dp(6));
        background.setStroke(dp(active ? 2 : 1), color);
        return background;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public String getCode() { StringBuilder out = new StringBuilder(6); for (EditText box : boxes) out.append(box.getText()); return out.toString(); }
    public boolean isComplete() { return getCode().length() == 6; }
    public void setErrorState(boolean error) { setBorderColor(error ? BORDER_ERROR : BORDER_DEFAULT); }
    public void clearError() { setErrorState(false); }
    public void setSuccessState() { setBorderColor(BORDER_SUCCESS); }
    private void setBorderColor(int color) { borderColor = color; refreshBorders(); }
    private void refreshBorders() {
        for (EditText box : boxes) {
            if (box == null) continue;
            boolean active = box.hasFocus() && borderColor == BORDER_DEFAULT;
            box.setBackground(boxBackground(active ? BORDER_SUCCESS : borderColor, active));
        }
    }
    public void clearCode() { for (EditText box : boxes) box.setText(""); boxes[0].requestFocus(); }
    public void focusFirst() { boxes[0].requestFocus(); ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(boxes[0], InputMethodManager.SHOW_IMPLICIT); }
}
