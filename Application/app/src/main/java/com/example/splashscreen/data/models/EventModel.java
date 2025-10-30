

package com.example.splashscreen.data.models;

// Imports for Firestore (Long) and formatting (Calendar)
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EventModel {
    public String id;
    public String title;
    public String type;
    public String status;
    public Long startDate;
    public Long endDate;
    public String description;
    public String poolId;

    public EventModel() {
        // Required for Firestore
    }

    public String getFormattedTime() {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(startDate);

        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(endDate);

        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return timeFormat.format(start.getTime()) + " - " + timeFormat.format(end.getTime());
    }
}