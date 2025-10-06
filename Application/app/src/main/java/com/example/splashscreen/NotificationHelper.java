package com.example.splashscreen;

import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

public class NotificationHelper {

    public enum NotificationType {
        INFORMATIONAL,
        ERROR,
        CONFIRMATION
    }
// TODO: Implement a proper dialogue, current one works but not Flexible
    //Liné was here 2025
    public static void showNotification(View parentView, String title, String message, NotificationType type) {

        ViewGroup rootView = (ViewGroup) parentView.getRootView();


        LayoutInflater inflater = LayoutInflater.from(parentView.getContext());
        final View customNotificationView = inflater.inflate(R.layout.custom_notification, rootView, false);


        CardView cardView = customNotificationView.findViewById(R.id.cardView_notification);
        ImageView icon = customNotificationView.findViewById(R.id.notification_icon);
        TextView notificationTitle = customNotificationView.findViewById(R.id.notification_title);
        TextView notificationMessage = customNotificationView.findViewById(R.id.notification_message);

        // Set the content based on the notification type
        switch (type) {
            case CONFIRMATION:
                notificationTitle.setText(title);
                icon.setImageResource(R.drawable.launcher_icon); // We need to change this
                cardView.setCardBackgroundColor(ContextCompat.getColor(parentView.getContext(), R.color.PrimaryColour)); // And change this
                break;
            case ERROR:
                notificationTitle.setText(title);
                icon.setImageResource(R.drawable.launcher_icon);
                cardView.setCardBackgroundColor(ContextCompat.getColor(parentView.getContext(), R.color.PrimaryColour));
                break;
            case INFORMATIONAL:
                notificationTitle.setText(title);
                icon.setImageResource(R.drawable.launcher_icon);
                cardView.setCardBackgroundColor(ContextCompat.getColor(parentView.getContext(), R.color.PrimaryColour));
                break;
        }
        notificationMessage.setText(message);

        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                // Calculate 80% of the screen's width
                (int) (parentView.getContext().getResources().getDisplayMetrics().widthPixels * 0.8),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        int statusBarHeight = 0;
        int resourceId = parentView.getContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = parentView.getContext().getResources().getDimensionPixelSize(resourceId);
        }

        params.topMargin = statusBarHeight + (int) (16 * parentView.getContext().getResources().getDisplayMetrics().density); // 16dp extra margin
        int horizontalMargin = (int) (parentView.getContext().getResources().getDisplayMetrics().widthPixels * 0.1);
        params.leftMargin = horizontalMargin;
        params.rightMargin = horizontalMargin;

        customNotificationView.setLayoutParams(params);

        rootView.addView(customNotificationView);
        Animation slideDown = AnimationUtils.loadAnimation(parentView.getContext(), R.anim.slide_down);
        customNotificationView.startAnimation(slideDown);

        new Handler().postDelayed(() -> {
            Animation slideUp = AnimationUtils.loadAnimation(parentView.getContext(), R.anim.slide_up);
            slideUp.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) { }
                @Override
                public void onAnimationEnd(Animation animation) {
                    rootView.removeView(customNotificationView);
                }
                @Override
                public void onAnimationRepeat(Animation animation) { }
            });
            customNotificationView.startAnimation(slideUp);
        }, 3000);
    }
}