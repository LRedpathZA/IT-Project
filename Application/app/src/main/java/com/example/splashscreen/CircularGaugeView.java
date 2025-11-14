package com.example.splashscreen;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

// NOTE: java.util.Locale is no longer needed here as we are not drawing text.

public class CircularGaugeView extends View {


    private Paint backgroundPaint;
    private Paint progressPaint;

    private RectF bounds;

    private float score = 0.0f; // 0.0 to 10.0
    private int statusColor = Color.GRAY;


    private float strokeWidth = 50f;

    public CircularGaugeView(Context context) {
        super(context);
        init();
    }

    public CircularGaugeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularGaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {



        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        backgroundPaint.setColor(ContextCompat.getColor(getContext(), R.color.grey));
        backgroundPaint.setStrokeWidth(strokeWidth);


        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setStrokeWidth(strokeWidth);

        bounds = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);


        float padding = strokeWidth / 2;
        bounds.set(padding, padding, w - padding, h - padding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        canvas.drawOval(bounds, backgroundPaint);


        float sweepAngle = (score / 10.0f) * 360f;


        progressPaint.setColor(statusColor);


        canvas.drawArc(bounds, 270, sweepAngle, false, progressPaint);

    }

    public void setHealthData(float score, String status, int colorResId) {
        this.score = score;
        this.statusColor = ContextCompat.getColor(getContext(), colorResId);

        // Schedule a redraw
        invalidate();
    }
}