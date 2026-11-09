package com.techpp.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Metal UI: high-contrast chromatic edge accents, not translucent glass panels.
 * Static shader-based rendering avoids large blur passes on low-power TV GPUs.
 */
public final class MetalAccents extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float density;

    public MetalAccents(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setFocusable(false);
        setClickable(false);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float line = Math.max(1f, density * 1.5f);
        paint.setShader(new LinearGradient(0, 0, w, 0,
                new int[]{0xFF00E5FF, 0xFF3478FF, 0xFFFF3D9A, 0xFFFFC857},
                null, Shader.TileMode.CLAMP));
        paint.setAlpha(220);
        canvas.drawRect(0, 0, w, line, paint);
        paint.setAlpha(95);
        canvas.drawRect(0, h - line, w, h, paint);
        paint.setShader(null);
        paint.setAlpha(255);
    }
}
