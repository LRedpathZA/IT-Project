package com.example.splashscreen;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.Collection;
import java.util.HashSet;

/**
 * Decorates the calendar days that have events by drawing a colored dot beneath them.
 */
public class EventDecorator implements DayViewDecorator {

    private final int color;
    private final HashSet<CalendarDay> dates;
    private final Drawable dotDrawable;

    public EventDecorator(Context context, int color, Collection<CalendarDay> dates) {
        this.color = color;
        this.dates = new HashSet<>(dates);
        // Using a simple circular drawable for the dot.
        // You would typically use a custom shape drawable resource here.
        this.dotDrawable = new ColorDrawable(color);
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        // You can use view.setBackgroundDrawable(dotDrawable) to change the background,
        // or use view.addSpan(new DotSpan(5, color)) to add a dot (requires custom span implementation),
        // or view.setSelectionDrawable(dotDrawable) for selection highlight.

        // For simplicity, let's just use a simple circle/dot indicator (requires a custom library feature or span).
        // Since we don't have a custom span, we'll use a built-in one or assume we have a simple dot drawable.

        // A simple way to add a dot:
        view.addSpan(new com.prolificinteractive.materialcalendarview.spans.DotSpan(6, color));

        // Note: For a custom appearance, you'd define a ShapeDrawable resource.
    }
}