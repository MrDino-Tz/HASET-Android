package com.haset.hasetapp.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.TransformationMethod;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
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
    private static final TransformationMethod MASK_TRANSFORMATION = new TransformationMethod() {
        @Override public CharSequence getTransformation(CharSequence source, View view) {
            return source != null && source.length() > 0 ? "•" : "";
        }

        @Override public void onFocusChanged(View view, CharSequence sourceText, boolean focused,
                                             int direction, Rect previouslyFocusedRect) {}
    };
    private int borderColor = BORDER_DEFAULT;

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
            box.setInputType(InputType.TYPE_CLASS_NUMBER);
            box.setTransformationMethod(MASK_TRANSFORMATION);
            box.setSingleLine(true);
            box.setSelectAllOnFocus(true);
            box.setPadding(0, 0, 0, 0);
            box.setBackground(boxBackground(BORDER_DEFAULT, false));
            box.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
            LayoutParams params = new LayoutParams(dp(40), dp(50));
            if (i > 0) params.setMarginStart(dp(8));
            addView(box, params);
            box.setOnFocusChangeListener((view, hasFocus) -> refreshBorders());
            box.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                public void onTextChanged(CharSequence s, int st, int before, int count) {
                    clearError();
                    if (s.length() > 1) {
                        String pasted = s.toString().replaceAll("[^0-9]", "");
                        for (int j = 0; j < pasted.length() && index + j < boxes.length; j++) {
                            boxes[index + j].setText(String.valueOf(pasted.charAt(j)));
                        }
                        boxes[Math.min(index + pasted.length(), boxes.length - 1)].requestFocus();
                        return;
                    }
                    if (s.length() == 1 && index < boxes.length - 1) boxes[index + 1].requestFocus();
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
