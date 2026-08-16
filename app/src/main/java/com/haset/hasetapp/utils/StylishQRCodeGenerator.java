package com.haset.hasetapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.Log;

import androidx.annotation.DrawableRes;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

public class StylishQRCodeGenerator {

    private static final String TAG = "StylishQRGenerator";

    private static final int COLOR_PRIMARY = 0xFF008800;
    private static final int COLOR_PRIMARY_DARK = 0xFF006600;
    private static final int COLOR_WHITE = Color.WHITE;

    public enum DotStyle {
        SQUARE,
        ROUNDED,
        EXTRA_ROUNDED,
        DOTS,
        CLASSY,
        CLASSY_ROUNDED
    }

    public enum CornerStyle {
        SQUARE,
        EXTRA_ROUNDED,
        DOT
    }

    public enum GradientType {
        NONE,
        LINEAR,
        RADIAL
    }

    public enum FrameStyle {
        NONE,
        SIMPLE,
        ROUNDED,
        DOUBLE,
        BOLD
    }

    public enum ColorTemplate {
        DEFAULT(COLOR_PRIMARY, COLOR_PRIMARY_DARK),
        BITCOIN(0xFFF7931A, 0xFFE67E22),
        FACEBOOK(0xFF1877F2, 0xFF0D5DB8),
        STARBUCKS(0xFF00704A, 0xFF004B35),
        INSTAGRAM(0xFFE1306C, 0xFFC13584),
        TELEGRAM(0xFF0088CC, 0xFF006699),
        WHATSAPP(0xFF25D366, 0xFF1DB954),
        YOUTUBE(0xFFFF0000, 0xFFCC0000),
        GREEN_LIGHT(0xFF11AA11, 0xFF008800),
        PURPLE(0xFF9B59B6, 0xFF8E44AD),
        ORANGE(0xFFF39C12, 0xFFD68910);

        public final int primary;
        public final int secondary;

        ColorTemplate(int primary, int secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }
    }

    public static class Builder {
        private int size = 512;
        private int dotColor = COLOR_PRIMARY;
        private int backgroundColor = COLOR_WHITE;
        private DotStyle dotStyle = DotStyle.EXTRA_ROUNDED;
        private CornerStyle cornerStyle = CornerStyle.EXTRA_ROUNDED;
        private FrameStyle frameStyle = FrameStyle.ROUNDED;
        private ColorTemplate colorTemplate = ColorTemplate.DEFAULT;
        private GradientType gradientType = GradientType.LINEAR;
        private Bitmap logo = null;
        private int logoSizePercent = 20;
        private boolean showLogoBackground = true;
        private int logoBackgroundColor = COLOR_WHITE;
        private int logoPadding = 8;
        private ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.H;
        private int margin = 16;

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder dotColor(int color) {
            this.dotColor = color;
            this.colorTemplate = ColorTemplate.DEFAULT;
            return this;
        }

        public Builder backgroundColor(int color) {
            this.backgroundColor = color;
            return this;
        }

        public Builder dotStyle(DotStyle style) {
            this.dotStyle = style;
            return this;
        }

        public Builder cornerStyle(CornerStyle style) {
            this.cornerStyle = style;
            return this;
        }

        public Builder frameStyle(FrameStyle style) {
            this.frameStyle = style;
            return this;
        }

        public Builder colorTemplate(ColorTemplate template) {
            this.colorTemplate = template;
            this.dotColor = template.primary;
            return this;
        }

        public Builder gradientType(GradientType type) {
            this.gradientType = type;
            return this;
        }

        public Builder logo(Bitmap logo) {
            this.logo = logo;
            return this;
        }

        public Builder logoResource(Context context, @DrawableRes int resId) {
            this.logo = BitmapFactory.decodeResource(context.getResources(), resId);
            return this;
        }

        public Builder logoSizePercent(int percent) {
            this.logoSizePercent = Math.min(percent, 25);
            return this;
        }

        public Builder showLogoBackground(boolean show) {
            this.showLogoBackground = show;
            return this;
        }

        public Builder logoBackgroundColor(int color) {
            this.logoBackgroundColor = color;
            return this;
        }

        public Builder logoPadding(int padding) {
            this.logoPadding = padding;
            return this;
        }

        public Builder margin(int margin) {
            this.margin = margin;
            return this;
        }

        public Builder errorCorrectionLevel(ErrorCorrectionLevel level) {
            this.errorCorrectionLevel = level;
            return this;
        }

        public Bitmap build(String content) {
            if (content == null || content.isEmpty()) {
                Log.e(TAG, "Content cannot be empty");
                return null;
            }

            try {
                int qrSize = size - (margin * 2);

                Map<EncodeHintType, Object> hints = new HashMap<>();
                hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
                hints.put(EncodeHintType.MARGIN, 2);
                hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

                QRCodeWriter writer = new QRCodeWriter();
                BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, qrSize, qrSize, hints);

                int totalSize = size;
                Bitmap qrBitmap = Bitmap.createBitmap(totalSize, totalSize, Config.ARGB_8888);
                Canvas canvas = new Canvas(qrBitmap);

                canvas.drawColor(backgroundColor);

                if (frameStyle != FrameStyle.NONE) {
                    drawFrame(canvas, totalSize);
                }

                canvas.save();
                canvas.translate(margin, margin);

                drawStyledQR(canvas, matrix, qrSize);

                canvas.restore();

                if (logo != null) {
                    qrBitmap = addLogo(qrBitmap, logo);
                }

                return qrBitmap;

            } catch (WriterException e) {
                Log.e(TAG, "Error generating QR code", e);
                return null;
            }
        }

        private void drawFrame(Canvas canvas, int totalSize) {
            Paint framePaint = new Paint();
            framePaint.setAntiAlias(true);
            framePaint.setStyle(Paint.Style.STROKE);
            framePaint.setColor(dotColor);

            int frameMargin = 4;
            int strokeWidth = 4;

            switch (frameStyle) {
                case SIMPLE:
                    canvas.drawRect(frameMargin, frameMargin, 
                        totalSize - frameMargin, totalSize - frameMargin, framePaint);
                    break;

                case ROUNDED:
                    framePaint.setStrokeWidth(strokeWidth);
                    RectF rect = new RectF(frameMargin, frameMargin, 
                        totalSize - frameMargin, totalSize - frameMargin);
                    canvas.drawRoundRect(rect, 16, 16, framePaint);
                    break;

                case DOUBLE:
                    framePaint.setStrokeWidth(strokeWidth);
                    canvas.drawRect(frameMargin, frameMargin, 
                        totalSize - frameMargin, totalSize - frameMargin, framePaint);
                    canvas.drawRect(frameMargin + 6, frameMargin + 6, 
                        totalSize - frameMargin - 6, totalSize - frameMargin - 6, framePaint);
                    break;

                case BOLD:
                    framePaint.setStrokeWidth(strokeWidth * 2);
                    RectF boldRect = new RectF(frameMargin, frameMargin, 
                        totalSize - frameMargin, totalSize - frameMargin);
                    canvas.drawRoundRect(boldRect, 12, 12, framePaint);
                    break;
            }
        }

        private void drawStyledQR(Canvas canvas, BitMatrix matrix, int qrSize) {
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int cellSize = qrSize / width;

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);

            boolean isPositionMarker = false;
            int markerSize = 7;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (matrix.get(x, y)) {
                        int color = getColorForPosition(x, y, width, height);
                        paint.setColor(color);

                        isPositionMarker = isPositionMarker(x, y, width, markerSize);

                        if (!isPositionMarker) {
                            drawDot(canvas, paint, x * cellSize, y * cellSize, cellSize);
                        }
                    }
                }
            }

            drawCorners(canvas, width, cellSize);
        }

        private boolean isPositionMarker(int x, int y, int size, int markerSize) {
            int margin = 4;
            return (x >= margin && x < margin + markerSize && y >= margin && y < margin + markerSize) ||
                   (x >= size - markerSize - margin && x < size - margin && y >= margin && y < margin + markerSize) ||
                   (x >= margin && x < margin + markerSize && y >= size - markerSize - margin && y < size - margin);
        }

        private void drawDot(Canvas canvas, Paint paint, float x, float y, int cellSize) {
            switch (dotStyle) {
                case DOTS:
                    float radius = cellSize * 0.45f;
                    canvas.drawCircle(x + cellSize / 2f, y + cellSize / 2f, radius, paint);
                    break;

                case ROUNDED:
                    float cornerR1 = cellSize * 0.3f;
                    drawRoundedRect(canvas, paint, x, y, cellSize, cornerR1);
                    break;

                case EXTRA_ROUNDED:
                    float cornerR2 = cellSize * 0.5f;
                    drawRoundedRect(canvas, paint, x, y, cellSize, cornerR2);
                    break;

                case CLASSY:
                    float classyR = cellSize * 0.35f;
                    drawRoundedRect(canvas, paint, x, y, cellSize, classyR);
                    break;

                case CLASSY_ROUNDED:
                    float classyRoundR = cellSize * 0.5f;
                    drawRoundedRect(canvas, paint, x, y, cellSize, classyRoundR);
                    break;

                case SQUARE:
                default:
                    canvas.drawRect(x, y, x + cellSize, y + cellSize, paint);
                    break;
            }
        }

        private void drawRoundedRect(Canvas canvas, Paint paint, float x, float y, float size, float cornerRadius) {
            RectF rect = new RectF(x, y, x + size, y + size);
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);
        }

        private void drawCorners(Canvas canvas, int width, int cellSize) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);

            int markerSize = 7;
            int margin = 4;

            int[][] positions = {
                {margin, margin},
                {width - markerSize - margin, margin},
                {margin, width - markerSize - margin}
            };

            for (int[] pos : positions) {
                int px = pos[0] * cellSize;
                int py = pos[1] * cellSize;
                float totalSize = markerSize * cellSize;

                switch (cornerStyle) {
                    case DOT:
                        paint.setColor(dotColor);
                        canvas.drawCircle(px + totalSize / 2, py + totalSize / 2, totalSize / 2, paint);

                        paint.setColor(backgroundColor);
                        canvas.drawCircle(px + totalSize / 2, py + totalSize / 2, totalSize / 3.5f, paint);

                        paint.setColor(dotColor);
                        canvas.drawCircle(px + totalSize / 2, py + totalSize / 2, totalSize / 5, paint);
                        break;

                    case EXTRA_ROUNDED:
                        float outerR = totalSize * 0.5f;
                        paint.setColor(dotColor);
                        drawRoundedRect(canvas, paint, px, py, totalSize, outerR);

                        paint.setColor(backgroundColor);
                        float innerMargin = cellSize * 1.2f;
                        drawRoundedRect(canvas, paint, px + innerMargin, py + innerMargin, 
                            totalSize - innerMargin * 2, outerR * 0.5f);

                        paint.setColor(dotColor);
                        canvas.drawCircle(px + totalSize / 2, py + totalSize / 2, cellSize * 1.5f, paint);
                        break;

                    case SQUARE:
                    default:
                        paint.setColor(dotColor);
                        canvas.drawRect(px, py, px + totalSize, py + totalSize, paint);

                        paint.setColor(backgroundColor);
                        float sqInnerMargin = cellSize * 1.2f;
                        canvas.drawRect(px + sqInnerMargin, py + sqInnerMargin,
                            px + totalSize - sqInnerMargin, py + totalSize - sqInnerMargin, paint);

                        paint.setColor(dotColor);
                        canvas.drawCircle(px + totalSize / 2, py + totalSize / 2, cellSize * 1.5f, paint);
                        break;
                }
            }
        }

        private int getColorForPosition(int x, int y, int width, int height) {
            if (gradientType == GradientType.NONE) {
                return dotColor;
            }

            float ratio;
            if (gradientType == GradientType.RADIAL) {
                float cx = width / 2f;
                float cy = height / 2f;
                float dist = (float) Math.sqrt(Math.pow(x - cx, 2) + Math.pow(y - cy, 2));
                float maxDist = (float) Math.sqrt(cx * cx + cy * cy);
                ratio = dist / maxDist;
            } else {
                ratio = (float) y / height;
            }

            return interpolateColor(colorTemplate.primary, colorTemplate.secondary, ratio);
        }

        private int interpolateColor(int color1, int color2, float ratio) {
            ratio = Math.max(0, Math.min(1, ratio));
            int r = (int) (Color.red(color1) * (1 - ratio) + Color.red(color2) * ratio);
            int g = (int) (Color.green(color1) * (1 - ratio) + Color.green(color2) * ratio);
            int b = (int) (Color.blue(color1) * (1 - ratio) + Color.blue(color2) * ratio);
            return Color.rgb(r, g, b);
        }

        private Bitmap addLogo(Bitmap qrBitmap, Bitmap logo) {
            int qrWidth = qrBitmap.getWidth();
            int qrHeight = qrBitmap.getHeight();

            int logoWidth = qrWidth * logoSizePercent / 100;
            int logoHeight = qrHeight * logoSizePercent / 100;

            Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, logoWidth, logoHeight, true);

            int bgWidth = logoWidth + (logoPadding * 2);
            int bgHeight = logoHeight + (logoPadding * 2);
            Bitmap logoBackground = Bitmap.createBitmap(bgWidth, bgHeight, Config.ARGB_8888);
            Canvas bgCanvas = new Canvas(logoBackground);

            Paint bgPaint = new Paint();
            bgPaint.setAntiAlias(true);
            bgPaint.setColor(logoBackgroundColor);

            float cornerRadius = bgWidth * 0.25f;
            RectF rect = new RectF(0, 0, bgWidth, bgHeight);
            bgCanvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint);

            Paint shadowPaint = new Paint();
            shadowPaint.setAntiAlias(true);
            shadowPaint.setColor(0x22000000);
            bgCanvas.drawRoundRect(new RectF(2, 2, bgWidth + 2, bgHeight + 2), cornerRadius, cornerRadius, shadowPaint);

            Bitmap result = qrBitmap.copy(Config.ARGB_8888, true);
            Canvas canvas = new Canvas(result);

            int logoX = (qrWidth - bgWidth) / 2;
            int logoY = (qrHeight - bgHeight) / 2;

            if (showLogoBackground) {
                canvas.drawBitmap(logoBackground, logoX, logoY, null);
            }

            Paint logoPaint = new Paint();
            logoPaint.setAntiAlias(true);
            logoPaint.setFilterBitmap(true);
            canvas.drawBitmap(scaledLogo, logoX + logoPadding, logoY + logoPadding, logoPaint);

            return result;
        }
    }

    public static Bitmap generateSimpleQR(String content, int size) {
        return new Builder().size(size).build(content);
    }

    public static Bitmap generateQRWithLogo(String content, int size, Bitmap logo) {
        return new Builder().size(size).logo(logo).build(content);
    }

    public static Bitmap generateStylishQR(Context context, String content, 
            @DrawableRes int logoResId, DotStyle dotStyle) {

        Bitmap logo = logoResId != 0 ?
            BitmapFactory.decodeResource(context.getResources(), logoResId) : null;

        return new Builder()
            .size(512)
            .dotStyle(dotStyle)
            .cornerStyle(CornerStyle.EXTRA_ROUNDED)
            .frameStyle(FrameStyle.ROUNDED)
            .colorTemplate(ColorTemplate.DEFAULT)
            .gradientType(GradientType.LINEAR)
            .logo(logo)
            .logoSizePercent(20)
            .showLogoBackground(true)
            .logoBackgroundColor(Color.WHITE)
            .logoPadding(6)
            .errorCorrectionLevel(ErrorCorrectionLevel.H)
            .build(content);
    }

    public static Bitmap generateHASETQR(Context context, String content) {
        Bitmap logo = BitmapFactory.decodeResource(context.getResources(), 
            com.haset.hasetapp.R.drawable.logo_v1);

        return new Builder()
            .size(512)
            .dotStyle(DotStyle.EXTRA_ROUNDED)
            .cornerStyle(CornerStyle.EXTRA_ROUNDED)
            .frameStyle(FrameStyle.ROUNDED)
            .colorTemplate(ColorTemplate.DEFAULT)
            .gradientType(GradientType.LINEAR)
            .logo(logo)
            .logoSizePercent(20)
            .showLogoBackground(true)
            .logoBackgroundColor(Color.WHITE)
            .logoPadding(6)
            .errorCorrectionLevel(ErrorCorrectionLevel.H)
            .build(content);
    }

    public static Bitmap generateTemplateQR(Context context, String content, 
            ColorTemplate template, DotStyle dotStyle) {

        Bitmap logo = BitmapFactory.decodeResource(context.getResources(), 
            com.haset.hasetapp.R.drawable.logo_v1);

        return new Builder()
            .size(512)
            .dotStyle(dotStyle)
            .cornerStyle(CornerStyle.EXTRA_ROUNDED)
            .frameStyle(FrameStyle.ROUNDED)
            .colorTemplate(template)
            .gradientType(GradientType.LINEAR)
            .logo(logo)
            .logoSizePercent(20)
            .showLogoBackground(true)
            .logoBackgroundColor(Color.WHITE)
            .logoPadding(6)
            .errorCorrectionLevel(ErrorCorrectionLevel.H)
            .build(content);
    }

    public static Bitmap generateBitcoinQR(Context context, String content) {
        return generateTemplateQR(context, content, ColorTemplate.BITCOIN, DotStyle.CLASSY);
    }

    public static Bitmap generateFacebookQR(Context context, String content) {
        return generateTemplateQR(context, content, ColorTemplate.FACEBOOK, DotStyle.ROUNDED);
    }

    public static Bitmap generateWhatsAppQR(Context context, String content) {
        return generateTemplateQR(context, content, ColorTemplate.WHATSAPP, DotStyle.DOTS);
    }

    public static Bitmap generateCustomQR(String content, int size, int primaryColor, 
            int secondaryColor, Bitmap logo, DotStyle dotStyle, CornerStyle cornerStyle, 
            FrameStyle frameStyle) {

        return new Builder()
            .size(size)
            .dotStyle(dotStyle)
            .cornerStyle(cornerStyle)
            .frameStyle(frameStyle)
            .dotColor(primaryColor)
            .gradientType(GradientType.LINEAR)
            .logo(logo)
            .logoSizePercent(20)
            .showLogoBackground(true)
            .logoBackgroundColor(Color.WHITE)
            .logoPadding(6)
            .errorCorrectionLevel(ErrorCorrectionLevel.H)
            .build(content);
    }
}
