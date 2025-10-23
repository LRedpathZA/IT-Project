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

    // Drawing objects
    private Paint backgroundPaint;
    private Paint progressPaint;
    // 💥 REMOVED: textScorePaint and textStatusPaint
    private RectF bounds;

    // Data
    private float score = 0.0f; // 0.0 to 10.0
    private int statusColor = Color.GRAY;

    // Dimensions
    private float strokeWidth = 50f; // Width of the progress ring

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
        // Initialize Paints for drawing

        // 1. Background Ring (The light colored track)
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        backgroundPaint.setColor(ContextCompat.getColor(getContext(), R.color.grey));
        backgroundPaint.setStrokeWidth(strokeWidth);

        // 2. Progress Arc (The dynamic colored health score)
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setStrokeWidth(strokeWidth);

        bounds = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Define the drawing area (RectF) for the arc
        float padding = strokeWidth / 2;
        bounds.set(padding, padding, w - padding, h - padding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // --- 1. Draw Background Ring ---
        canvas.drawOval(bounds, backgroundPaint);

        // --- 2. Draw Progress Arc ---
        float sweepAngle = (score / 10.0f) * 360f;

        // Update color dynamically
        progressPaint.setColor(statusColor);

        // Start from the top (270 degrees)
        canvas.drawArc(bounds, 270, sweepAngle, false, progressPaint);

    }

    // Public setter to update data and redraw the view
    // The 'status' string is still accepted but ONLY used to determine the color.
    public void setHealthData(float score, String status, int colorResId) {
        this.score = score;
        this.statusColor = ContextCompat.getColor(getContext(), colorResId);

        // Schedule a redraw
        invalidate();
    }
}