package com.techpp.glass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** Original, static atmospheric material: no frame loop, blur layer, or network request. */
final class AuroraBackdrop extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    AuroraBackdrop(Context context) { super(context); setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); }
    @Override protected void onDraw(Canvas canvas) {
        int w=getWidth(),h=getHeight();if(w==0||h==0)return;
        paint.setShader(new LinearGradient(0,0,w,h,new int[]{0xFF08151B,0xFF0B1D26,0xFF101722},null,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,w,h,paint);
        glow(canvas,w*.78f,h*.06f,w*.75f,0x6647D4BD);
        glow(canvas,w*.11f,h*.76f,w*.49f,0x554163A0);
        glow(canvas,w*.92f,h*.93f,w*.37f,0x444ED4B3);
        paint.setShader(null);
    }
    private void glow(Canvas c,float x,float y,float r,int color) {
        paint.setShader(new RadialGradient(x,y,r,new int[]{color,0x00202B34},null,Shader.TileMode.CLAMP));
        c.drawCircle(x,y,r,paint);
    }
}
