package com.haset.hasetapp.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.haset.hasetapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VoiceWaveView extends View {
    private Paint barPaint;
    private List<Bar> bars;
    private Random random;
    private int barCount = 30;
    private int baseAmplitude = 8;
    private int maxAmplitude = 80;
    private float barWidth = 6f;
    private float barSpacing = 2f;
    private int waveColor;
    private long lastUpdateTime = 0;
    private float phase = 0f;
    
    public VoiceWaveView(Context context) {
        super(context);
        init(context);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }
    
    public VoiceWaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }
    
    public VoiceWaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }
    
    private void init(Context context) {
        waveColor = ContextCompat.getColor(context, R.color.green_primary);
        
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setColor(waveColor);
        
        bars = new ArrayList<>();
        random = new Random();
        
        for (int i = 0; i < barCount; i++) {
            Bar bar = new Bar();
            bar.height = baseAmplitude + random.nextInt(10);
            bar.targetHeight = bar.height;
            bar.phaseOffset = random.nextFloat() * (float) Math.PI;
            bars.add(bar);
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        
        if (width <= 0 || height <= 0) {
            return;
        }
        
        int centerY = height / 2;
        
        // Calculate spacing to fill the width evenly
        float availableWidth = width - (barWidth * 2);
        float totalSpacing = barCount > 1 ? availableWidth / (barCount - 1) : 0;
        float actualBarSpacing = Math.max(barSpacing, totalSpacing);
        float totalBarWidth = barCount * barWidth + (barCount - 1) * actualBarSpacing;
        float startX = (width - totalBarWidth) / 2;
        
        // Update phase for faster wave animation
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime > 30) {  // Faster updates (was 50ms)
            phase += 0.25f;  // Larger phase increment (was 0.15f)
            lastUpdateTime = currentTime;
        }
        
        for (int i = 0; i < bars.size(); i++) {
            Bar bar = bars.get(i);
            
            // Smooth animation towards target
            float diff = bar.targetHeight - bar.height;
            bar.height += diff * 0.15f;
            
            float x = startX + i * (barWidth + actualBarSpacing);
            float barHeight = Math.max(4, bar.height);
            
            // Draw rounded rectangle
            RectF rect = new RectF(
                x,
                centerY - barHeight / 2,
                x + barWidth,
                centerY + barHeight / 2
            );
            
            barPaint.setColor(waveColor);
            canvas.drawRoundRect(rect, barWidth / 2, barWidth / 2, barPaint);
        }
        
        postInvalidateOnAnimation();
    }
    
    public void updateAmplitude(int amplitude) {
        if (amplitude <= 0) {
            // Reset to idle state
            for (Bar bar : bars) {
                bar.targetHeight = baseAmplitude + random.nextInt(6);
            }
            return;
        }
        
        // Ultra-sensitive amplitude mapping
        int normalizedAmplitude = Math.min(amplitude, maxAmplitude);
        
        // Maximum sensitivity - use full range
        int targetBase = (int) (baseAmplitude + (normalizedAmplitude * 0.95f));
        
        for (int i = 0; i < bars.size(); i++) {
            Bar bar = bars.get(i);
            
            // Create dynamic wave pattern
            float waveFactor = (float) Math.sin(phase + bar.phaseOffset + i * 0.5);
            float waveFactor2 = (float) Math.cos(phase * 1.3 + bar.phaseOffset + i * 0.3);
            
            // Combined wave for more movement
            float variation = (waveFactor * 30) + (waveFactor2 * 15);
            bar.targetHeight = targetBase + variation;
            
            // Wide range for maximum reactivity
            bar.targetHeight = Math.max(3, Math.min(bar.targetHeight, maxAmplitude + 30));
        }
    }
    
    public void reset() {
        phase = 0;
        for (Bar bar : bars) {
            bar.height = baseAmplitude;
            bar.targetHeight = baseAmplitude;
        }
        invalidate();
    }
    
    private static class Bar {
        float height;
        float targetHeight;
        float phaseOffset;
    }
}

