package com.example.splashscreen.data.models;

import android.support.annotation.Nullable;

import androidx.annotation.DrawableRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

public class UserViewModel extends ViewModel {
    private final MutableLiveData<DocumentSnapshot> _userData = new MutableLiveData<>();
    public LiveData<DocumentSnapshot> userData = _userData;

    private final MutableLiveData<Integer> _userRole = new MutableLiveData<>();
    public LiveData<Integer> userRole = _userRole;

    private final MutableLiveData<String> _username = new MutableLiveData<>();
    public LiveData<String> username = _username;

    private final MutableLiveData<GeoPoint> _spLocationGeoPoint = new MutableLiveData<>();
    public LiveData<GeoPoint> spLocationGeoPoint = _spLocationGeoPoint;

    private final MutableLiveData<String> _spLocationAddress = new MutableLiveData<>();
    public LiveData<String> spLocationAddress = _spLocationAddress;

    private static final int ROLE_POOL_OWNER = 1;
    private static final int ROLE_SERVICE_PROVIDER = 2;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    public GeoPoint getSpLocationGeoPoint() {
        return _spLocationGeoPoint.getValue();
    }
    public void fetchUserData(String userId) {
        if (_userData.getValue() != null) {
            return;
        }

        _isLoading.setValue(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("users").document(userId);

        docRef.get().addOnCompleteListener(task -> {
            _isLoading.setValue(false);
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    _userData.setValue(document);
                    _username.setValue(document.getString("name"));
                    Long roleLong = document.getLong("role_id");

                    if (roleLong != null) {
                        int role = roleLong.intValue();
                        _userRole.setValue(role);

                        if (role == ROLE_SERVICE_PROVIDER) {
                            _spLocationGeoPoint.setValue(document.getGeoPoint("location"));
                            _spLocationAddress.setValue(document.getString("locationAddress"));
                        }
                    }
                } else {
                    //((MainActivity) getActivity()).logoutUser();
                }
            } else {
                // Handle error
            }
        });
    }


    public boolean isPoolOwner() {
        Integer role = _userRole.getValue();
        return role != null && role == ROLE_POOL_OWNER;
    }

    public boolean isServiceProvider() {
        Integer role = _userRole.getValue();
        return role != null && role == ROLE_SERVICE_PROVIDER;
    }

    public Map<String, Object> updateLocationFields(double lat, double lon, String address) {
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("location", new GeoPoint(lat, lon));
        locationData.put("locationAddress", address);
        return locationData;
    }
    public void updateProfilePictureData(String userId, @Nullable String profilePictureUrl, @DrawableRes int avatarResId) {
        if (userId == null || userId.isEmpty()) {
            // In a real app, you might log this error or notify the user
            return;
        }

        _isLoading.setValue(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userId);

        Map<String, Object> updates = new HashMap<>();

        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            // Case 1: Custom photo was uploaded
            updates.put("profilePictureUrl", profilePictureUrl);
            // Remove the built-in ID field
            updates.put("profileAvatarResId", com.google.firebase.firestore.FieldValue.delete());
        } else if (avatarResId > 0) {
            // Case 2: Built-in avatar was selected
            updates.put("profileAvatarResId", avatarResId);
            // Remove the custom URL field
            updates.put("profilePictureUrl", com.google.firebase.firestore.FieldValue.delete());
        } else {
            // Case 3: Both fields should be cleared (e.g., if a remove option was added)
            updates.put("profilePictureUrl", com.google.firebase.firestore.FieldValue.delete());
            updates.put("profileAvatarResId", com.google.firebase.firestore.FieldValue.delete());
        }

        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Success: Force a refresh to update all observers (like the Main Header)
                    fetchUserData(userId);
                    // fetchUserData will set _isLoading to false
                })
                .addOnFailureListener(e -> {
                    _isLoading.setValue(false);
                    // TODO: Handle Firestore error (e.g., log error, show Toast to user)
                });
    }
}