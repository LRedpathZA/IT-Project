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
    private static boolean isCloudinaryInitialized = false;

    /**
     * Uploads an image or document to Cloudinary using a secure signed request from Firebase Functions.
     * ACCEPTS URI DIRECTLY, which is compatible with both file paths (file://) and content URIs (content://).
     * @param context Application context.
     * @param fileUri Local URI of the file.
     * @param folder The Cloudinary folder name (e.g., "profile_pictures", "service_requests").
     * @param listener Callback for upload events.
     */
    public static void uploadImage(Context context, Uri fileUri, String folder, UploadListener listener) {

        if (fileUri == null) {
            listener.onFailure("File URI is null. Cannot upload.");
            return;
        }

        // 1. Call the Firebase Function to get the secure signature
        Map<String, Object> data = new HashMap<>();
        data.put("folder", folder);

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

                    // 2. Initialize Cloudinary if not already done
                    if (!isCloudinaryInitialized) {
                        try {
                            Map config = new HashMap();
                            config.put("cloud_name", cloudName);
                            MediaManager.init(appCtx, config);
                            isCloudinaryInitialized = true;
                            Log.d(TAG, "Cloudinary initialized dynamically.");
                        } catch (IllegalStateException e) {
                            isCloudinaryInitialized = true;
                            Log.w(TAG, "Cloudinary was already initialized by another call.");
                        } catch (Exception e) {
                            listener.onFailure("Cloudinary Initialization Failed: " + e.getMessage());
                            return;
                        }
                    }

                    // 3. Perform the Signed Upload
                    // Using the Uri object directly here
                    MediaManager.get().upload(fileUri)
                            .option("signature", signature)
                            .option("timestamp", timestamp)
                            .option("api_key", apiKey)
                            .option("folder", folder)
                            .callback(new UploadCallback() {
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


    public static void uploadProfileImage(Context context, Uri imageUri, UploadListener listener) {
        uploadImage(context, imageUri, "profile_pictures", listener);
    }
}