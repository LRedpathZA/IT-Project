package com.example.splashscreen.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import java.util.HashMap;
import java.util.Map;

public class ImageUploadManager {
    private static final String TAG = "ImageUploadManager";

    // 💡 NEW: Static flag to track initialization status
    private static boolean isCloudinaryInitialized = false;

    public static void uploadProfileImage(Context context, Uri imageUri, UploadListener listener) {
        String photoPath = FilePathUtil.getRealPathFromURI(context, imageUri);

        if (photoPath == null) {
            listener.onFailure("Could not resolve photo path. Cannot upload.");
            return;
        }

        // 1. Call the Firebase Function to get the secure signature
        Map<String, Object> data = new HashMap<>();
        data.put("folder", "profile_pictures");

        FirebaseFunctions.getInstance()
                .getHttpsCallable("generateCloudinarySignature")
                .call(data)
                .addOnSuccessListener(task -> {
                    Map<String, Object> result = (Map<String, Object>) ((HttpsCallableResult) task).getData();
                    String signature = (String) result.get("signature");
                    long timestamp = ((Number) result.get("timestamp")).longValue();
                    String cloudName = (String) result.get("cloudName");
                    String apiKey = (String) result.get("apiKey");

                    Context appCtx = context.getApplicationContext();
                    if (appCtx == null) {
                        listener.onFailure("Application context unavailable.");
                        return;
                    }
                    if (!isCloudinaryInitialized) {
                        try {
                            Map config = new HashMap();
                            config.put("cloud_name", cloudName);
                            MediaManager.init(appCtx, config);
                            isCloudinaryInitialized = true;
                            Log.d(TAG, "Cloudinary initialized dynamically.");
                        } catch (IllegalStateException e) {
                            // This catches the case where another thread might have initialized it
                            // just before this check. We can safely ignore the exception here.
                            isCloudinaryInitialized = true;
                            Log.w(TAG, "Cloudinary was already initialized by another call.");
                        } catch (Exception e) {
                            listener.onFailure("Cloudinary Initialization Failed: " + e.getMessage());
                            return;
                        }
                    }

                    // 3. Perform the Signed Upload
                    MediaManager.get().upload(photoPath)
                            .option("signature", signature)
                            .option("timestamp", timestamp)
                            .option("api_key", apiKey)
                            .option("folder", "profile_pictures")
                            .callback(new UploadCallback() {
                                // ... rest of the callback logic (unchanged) ...
                                @Override public void onStart(String requestId) {
                                    listener.onStart();
                                }
                                @Override public void onProgress(String requestId, long bytes, long totalBytes) {
                                    int percent = (int) (100 * bytes / totalBytes);
                                    listener.onProgress(percent);
                                }

                                @Override
                                public void onSuccess(String requestId, Map resultData) {
                                    String photoUrl = (String) resultData.get("secure_url");
                                    listener.onSuccess(photoUrl);
                                }

                                @Override
                                public void onError(String requestId, ErrorInfo error) {
                                    Log.e(TAG, "Cloudinary Upload error: " + error.getDescription());
                                    listener.onFailure("Cloudinary Upload Failed: " + error.getDescription());
                                }

                                @Override public void onReschedule(String requestId, ErrorInfo error) { }
                            }).dispatch();

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase Function call failed: " + e.getMessage());
                    listener.onFailure("Secure connection failed: " + e.getMessage());
                });
    }
}