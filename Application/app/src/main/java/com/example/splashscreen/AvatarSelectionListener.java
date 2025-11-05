package com.example.splashscreen; // Use your project's package

import androidx.annotation.DrawableRes;

public interface AvatarSelectionListener {
    /**
     * Called when an avatar is selected.
     * @param selectedResId The drawable resource ID of the selected avatar,
     * or 0 to signal the custom photo upload action.
     */
    void onAvatarSelected(@DrawableRes int selectedResId);
}