package com.example.splashscreen;

import android.content.Context;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

public class NotificationHelper {

    public enum NotificationType {
        INFORMATIONAL,
        ERROR,
        CONFIRMATION
    }
// TODO: Implement a proper toast, current one works but not Flexible
    public static void showNotification(Context context, String message, NotificationType type) {
        switch (type) {
            case INFORMATIONAL:
            case CONFIRMATION:
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                break;
            case ERROR:
                // For errors, we'll show a more prominent Toast
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                break;
        }
    }

    public static void showAlertDialog(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}