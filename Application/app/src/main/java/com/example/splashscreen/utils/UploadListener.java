package com.example.splashscreen.utils; // Use your desired package name

public interface UploadListener {
    void onStart();
    void onProgress(int percent);
    void onSuccess(String url);
    void onFailure(String error);
}