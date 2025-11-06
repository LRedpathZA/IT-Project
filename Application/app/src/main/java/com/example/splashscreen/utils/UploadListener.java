package com.example.splashscreen.utils;

public interface UploadListener {
    void onStart();
    void onProgress(int percent);
    void onSuccess(String url);
    void onFailure(String error);
}