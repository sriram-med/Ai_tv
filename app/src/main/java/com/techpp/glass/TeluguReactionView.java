package com.techpp.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.SystemClock;
import android.view.View;
import java.io.InputStream;

/** Small original GIF loop. Runs only while visible; no third-party decoding library. */
public final class TeluguReactionView extends View {
    private final Movie movie;
    private long started;
    public TeluguReactionView(Context context) {
        super(context);
        Movie loaded = null;
        try (InputStream in = context.getAssets().open("telugu/reaction_sparkle.gif")) {
            loaded = Movie.decodeStream(in);
        } catch (Exception ignored) {}
        movie = loaded;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
    @Override protected void onMeasure(int w, int h) {
        int width = (int)(getResources().getDisplayMetrics().density * 144);
        int height = (int)(getResources().getDisplayMetrics().density * 96);
        setMeasuredDimension(resolveSize(width,w),resolveSize(height,h));
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (movie == null || MotionPrefs.level(getContext()) == 0) return;
        if (started == 0) started = SystemClock.uptimeMillis();
        int duration = movie.duration() > 0 ? movie.duration() : 960;
        movie.setTime((int)((SystemClock.uptimeMillis() - started) % duration));
        canvas.save();
        canvas.scale(getWidth() / (float)movie.width(),getHeight() / (float)movie.height());
        movie.draw(canvas,0,0);
        canvas.restore();
        if (isShown()) postInvalidateDelayed(80);
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        started=0;
        invalidate();
    }
}
