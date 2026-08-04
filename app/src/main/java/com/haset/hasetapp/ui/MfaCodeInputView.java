package com.haset.hasetapp.ui;

import android.content.Context;
import android.graphics.Color;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

/** Reusable six-digit MFA input. Call setErrorState on a failed server response. */
public final class MfaCodeInputView extends LinearLayout {
    private final EditText[] boxes = new EditText[6];
    private int accent = Color.rgb(24, 150, 95);

    public MfaCodeInputView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setShowDividers(SHOW_DIVIDER_MIDDLE);
        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            EditText box = new EditText(context);
            boxes[i] = box;
            box.setGravity(Gravity.CENTER);
            box.setTextSize(22);
            box.setInputType(InputType.TYPE_CLASS_NUMBER);
            box.setSingleLine(true);
            box.setSelectAllOnFocus(true);
            box.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
            LayoutParams params = new LayoutParams(48, 56);
            params.setMargins(4, 0, 4, 0);
            addView(box, params);
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

    public String getCode() { StringBuilder out = new StringBuilder(6); for (EditText box : boxes) out.append(box.getText()); return out.toString(); }
    public boolean isComplete() { return getCode().length() == 6; }
    public void setErrorState(boolean error) { for (EditText box : boxes) box.setTextColor(error ? Color.rgb(190, 35, 45) : accent); }
    public void clearError() { setErrorState(false); }
    public void setSuccessState() { for (EditText box : boxes) box.setTextColor(accent); }
    public void clearCode() { for (EditText box : boxes) box.setText(""); boxes[0].requestFocus(); }
    public void focusFirst() { boxes[0].requestFocus(); ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(boxes[0], InputMethodManager.SHOW_IMPLICIT); }
}
