package com.example.splashscreen;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;
import android.util.Log;

public class App extends Application {
// This might not work but left it for future scaling
    private static final String TAG = "AppInit";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            Map config = new HashMap();
            // **REPLACE "YOUR_STATIC_CLOUD_NAME" with the actual Cloudinary Cloud Name**
            config.put("cloud_name", "YOUR_STATIC_CLOUD_NAME");

            // You can optionally add resource_type here if needed globally
            // config.put("resource_type", "auto");

            MediaManager.init(this, config);
            Log.d(TAG, "Cloudinary MediaManager initialized successfully.");

        } catch (IllegalStateException e) {
            Log.e(TAG, "Cloudinary MediaManager already initialized: " + e.getMessage());
            // This happens if init() is called outside of onCreate().
            // Since we're in Application.onCreate(), this indicates a severe bug
            // if it were to happen, but it shouldn't here.
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Cloudinary MediaManager: " + e.getMessage());
        }
    }
}