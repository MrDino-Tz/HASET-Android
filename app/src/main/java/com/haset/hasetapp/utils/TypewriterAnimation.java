package com.haset.hasetapp.utils;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

public class TypewriterAnimation {
    
    private TextView textView;
    private String fullText;
    private int currentIndex = 0;
    private Handler handler;
    private Runnable runnable;
    private int delayBetweenChars = 100; // milliseconds
    
    public TypewriterAnimation(TextView textView, String text, int delayBetweenChars) {
        this.textView = textView;
        this.fullText = text;
        this.delayBetweenChars = delayBetweenChars;
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    public TypewriterAnimation(TextView textView, String text) {
        this(textView, text, 100);
    }
    
    public void start() {
        if (textView == null || fullText == null) return;
        
        textView.setText("");
        currentIndex = 0;
        
        runnable = new Runnable() {
            @Override
            public void run() {
                if (currentIndex <= fullText.length()) {
                    textView.setText(fullText.substring(0, currentIndex));
                    currentIndex++;
                    handler.postDelayed(this, delayBetweenChars);
                }
            }
        };
        
        handler.postDelayed(runnable, delayBetweenChars);
    }
    
    public void stop() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
    
    public static void animate(TextView textView, String text) {
        new TypewriterAnimation(textView, text).start();
    }
    
    public static void animate(TextView textView, String text, int delay) {
        new TypewriterAnimation(textView, text, delay).start();
    }
}
