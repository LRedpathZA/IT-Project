package com.example.splashscreen.data.models;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class UserViewModel extends ViewModel {
    private final MutableLiveData<DocumentSnapshot> _userData = new MutableLiveData<>();
    public LiveData<DocumentSnapshot> userData = _userData;

    private final MutableLiveData<Integer> _userRole = new MutableLiveData<>();
    public LiveData<Integer> userRole = _userRole;

    private final MutableLiveData<String> _username = new MutableLiveData<>();
    public LiveData<String> username = _username;

    // Persisted location data (for after registration)
    private final MutableLiveData<GeoPoint> _spLocationGeoPoint = new MutableLiveData<>();
    public LiveData<GeoPoint> spLocationGeoPoint = _spLocationGeoPoint;

    private final MutableLiveData<String> _spLocationAddress = new MutableLiveData<>();
    public LiveData<String> spLocationAddress = _spLocationAddress;

    // ADDED: LiveData for the Service Provider's Business Name
    private final MutableLiveData<String> _businessName = new MutableLiveData<>();
    public LiveData<String> businessName = _businessName;

    // ⭐ NEW TRANSIENT FIELDS for Sign-up Data Transfer (The Fix)
    private String currentPhoneNumber;
    private GeoPoint tempGeoPoint; // Temporary storage for location during sign-up
    private String tempLocationAddress; // Temporary storage for address during sign-up

    private static final int ROLE_POOL_OWNER = 1;
    private static final int ROLE_SERVICE_PROVIDER = 2;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private ListenerRegistration userListenerRegistration;

    // -----------------------------------------------------------------
    // ⭐ GETTER & SETTER FOR TRANSIENT SIGN-UP DATA (THE FIX)
    // -----------------------------------------------------------------

    public String getCurrentPhone() {
        return currentPhoneNumber;
    }

    public void setCurrentPhone(String phone) {
        this.currentPhoneNumber = phone;
    }

    public void setTempGeoPoint(GeoPoint geoPoint) {
        this.tempGeoPoint = geoPoint;
    }

    public GeoPoint getTempGeoPoint() {
        return tempGeoPoint;
    }

    public void setTempLocationAddress(String address) {
        this.tempLocationAddress = address;
    }

    public String getTempLocationAddress() {
        return tempLocationAddress;
    }

    // Existing methods that were slightly redundant but kept for back-compat/clarity
    public GeoPoint getSpLocationGeoPoint() {
        return _spLocationGeoPoint.getValue();
    }
    // -----------------------------------------------------------------

    /**
     * Fetches the user's data in real-time and populates all associated LiveData.
     * @param userId The ID of the authenticated user.
     */
    public void fetchUserData(String userId) {
        // Only start the listener if it's not already running
        if (userListenerRegistration != null) {
            return;
        }

        _isLoading.setValue(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("users").document(userId);

        userListenerRegistration = docRef.addSnapshotListener((document, e) -> {
            _isLoading.setValue(false);
            if (e != null) {
                // Handle error
                return;
            }

            if (document != null && document.exists()) {
                // This block runs immediately and every time data changes in Firestore
                _userData.setValue(document);
                _username.setValue(document.getString("name"));
                Long roleLong = document.getLong("role_id");

                if (roleLong != null) {
                    int role = roleLong.intValue();
                    _userRole.setValue(role);

                    if (role == ROLE_SERVICE_PROVIDER) {
                        _spLocationGeoPoint.setValue(document.getGeoPoint("location"));
                        _spLocationAddress.setValue(document.getString("locationAddress"));
                        // Fetch the business name
                        _businessName.setValue(document.getString("businessName")); // ADDED LOGIC
                    } else {
                        // Clear SP-specific data if the role is not SP
                        _spLocationGeoPoint.setValue(null);
                        _spLocationAddress.setValue(null);
                        _businessName.setValue(null);
                    }
                }
            } else {
                // Document not found
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
        }
    }


    public boolean isPoolOwner() {
        Integer role = _userRole.getValue();
        return role != null && role == ROLE_POOL_OWNER;
    }

    public boolean isServiceProvider() {
        Integer role = _userRole.getValue();
        return role != null && role == ROLE_SERVICE_PROVIDER;
    }

    /**
     * Creates a Map of fields used to update Firestore with location data.
     * @param lat Latitude.
     * @param lon Longitude.
     * @param address Formatted address string.
     * @return Map of fields.
     */
    public Map<String, Object> updateLocationFields(double lat, double lon, String address) {
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("location", new GeoPoint(lat, lon));
        locationData.put("locationAddress", address);
        return locationData;
    }

    public void updateProfilePictureData(String userId, @Nullable String profilePictureUrl, @DrawableRes int avatarResId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        _isLoading.setValue(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userId);

        Map<String, Object> updates = new HashMap<>();

        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            updates.put("profilePictureUrl", profilePictureUrl);
            updates.put("profileAvatarResId", FieldValue.delete());
        } else if (avatarResId > 0) {
            updates.put("profileAvatarResId", (long) avatarResId);
            updates.put("profilePictureUrl", FieldValue.delete());
        } else {
            updates.put("profilePictureUrl", FieldValue.delete());
            updates.put("profileAvatarResId", FieldValue.delete());
        }

        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Success! The real-time listener will automatically pick up this change
                    // from Firestore and update the LiveData (_userData).
                    _isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    _isLoading.setValue(false);
                    // Handle failure
                });
    }
}