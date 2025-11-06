package com.example.splashscreen.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.example.splashscreen.R; // Ensure this is correct for your R file

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfilePictureManager {
    private static final String TAG = "ProfilePictureManager";
    // Reuseable executors to prevent creating new threads for every call
    private static final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Default placeholder. Ensure you have R.drawable.ic_profile_placeholder
    private static final int DEFAULT_PLACEHOLDER = R.drawable.ic_profile_placeholder;

    /**
     * Loads the profile picture (custom URL or built-in ID) into the target ImageView.
     * This method handles fetching from Firestore data, network loading, and setting the placeholder.
     * * @param context The application context.
     * @param document The DocumentSnapshot containing user data.
     * @param targetImageView The ImageView to update.
     */
    public static void loadPicture(Context context, DocumentSnapshot document, ImageView targetImageView) {
        if (document == null || targetImageView == null) {
            setPlaceholder(targetImageView);
            return;
        }

        String profileUrl = document.getString("profilePictureUrl");
        Long avatarResIdLong = document.getLong("profileAvatarResId");

        if (profileUrl != null && !profileUrl.isEmpty()) {
            loadBitmapFromUrl(profileUrl, targetImageView);
        } else if (avatarResIdLong != null && avatarResIdLong > 0) {
            // Load built-in avatar using Resource ID
            int avatarResId = avatarResIdLong.intValue();
            targetImageView.setImageResource(avatarResId);
            targetImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            setPlaceholder(targetImageView);
        }
    }

    /**
     * Sets the default placeholder.
     */
    public static void setPlaceholder(ImageView targetImageView) {
        if (targetImageView == null) return;
        targetImageView.setImageResource(DEFAULT_PLACEHOLDER);
        targetImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

    /**
     * Handles the network loading of a URL into an ImageView on a background thread.
     */
    private static void loadBitmapFromUrl(String url, ImageView targetImageView) {
        networkExecutor.execute(() -> {
            Bitmap bitmap = null;
            try {
                InputStream in = new URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e(TAG, "Error loading profile bitmap from URL: " + e.getMessage());
            }

            Bitmap finalBitmap = bitmap;
            mainHandler.post(() -> {
                if (targetImageView == null) return;
                if (finalBitmap != null) {
                    targetImageView.setImageBitmap(finalBitmap);
                    targetImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                } else {
                    // Fallback to placeholder on network failure
                    setPlaceholder(targetImageView);
                }
            });
        });
    }
}