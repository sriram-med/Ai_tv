package com.techpp.glass;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Subtle, non-interactive cinematic ambient lighting behind launcher content.
 * Pauses when the window is hidden and respects the system animation setting.
 */
public final class AmbientGlow extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animator;
    private float phase;

    public AmbientGlow(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float drift = (float) Math.sin(phase * Math.PI * 2);
        drawGlow(canvas, w * (0.18f + drift * 0.035f), h * 0.83f,
                w * 0.58f, Color.argb(39, 44, 104, 255));
        drawGlow(canvas, w * (0.85f - drift * 0.025f), h * 0.22f,
                w * 0.47f, Color.argb(24, 134, 64, 239));
    }

    private void drawGlow(Canvas canvas, float x, float y, float radius, int color) {
        paint.setShader(new RadialGradient(x, y, radius,
                new int[]{color, Color.TRANSPARENT}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(null);
    }

    private boolean motionAllowed() {
        if (!ValueAnimator.areAnimatorsEnabled()) return false;
        if (getContext().getSharedPreferences("home", Context.MODE_PRIVATE)
                .getBoolean("lite_effects", false)) return false;
        try {
            return Settings.Global.getFloat(getContext().getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
        } catch (Exception ignored) {
            return true;
        }
    }

    private void start() {
        if (animator != null || !isShown() || !motionAllowed()) return;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(18000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            phase = (Float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private void stop() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) start(); else stop();
    }
}
