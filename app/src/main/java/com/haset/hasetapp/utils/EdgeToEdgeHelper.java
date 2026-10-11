package com.haset.hasetapp.utils;

import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.ComponentActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class EdgeToEdgeHelper {
    private EdgeToEdgeHelper() {}

    public static void enable(ComponentActivity activity) {
        EdgeToEdge.enable(activity);
        activity.getWindow().getDecorView().post(() -> {
            ViewGroup content = activity.findViewById(android.R.id.content);
            if (content == null || content.getChildCount() == 0) return;

            View root = content.getChildAt(0);
            int left = root.getPaddingLeft();
            int top = root.getPaddingTop();
            int right = root.getPaddingRight();
            int bottom = root.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
                Insets bars = windowInsets.getInsets(
                        WindowInsetsCompat.Type.systemBars()
                                | WindowInsetsCompat.Type.displayCutout());
                view.setPadding(left + bars.left, top + bars.top,
                        right + bars.right, bottom + bars.bottom);
                return windowInsets;
            });
            ViewCompat.requestApplyInsets(root);
        });
    }
}
