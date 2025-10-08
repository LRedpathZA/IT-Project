package com.example.splashscreen;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserViewModel extends ViewModel {
    private final MutableLiveData<DocumentSnapshot> _userData = new MutableLiveData<>();
    public LiveData<DocumentSnapshot> userData = _userData;

    private final MutableLiveData<Integer> _userRole = new MutableLiveData<>();
    public LiveData<Integer> userRole = _userRole;


    private final MutableLiveData<String> _username = new MutableLiveData<>();
    public LiveData<String> username = _username;


    private static final int ROLE_POOL_OWNER = 1;
    private static final int ROLE_SERVICE_PROVIDER = 2;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;


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
}