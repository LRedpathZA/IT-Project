package com.example.splashscreen.data.models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class PoolViewModel extends ViewModel {

    private final MutableLiveData<PoolModel> _currentPoolModel = new MutableLiveData<>();
    public LiveData<PoolModel> currentPoolModel = _currentPoolModel;

    private final MutableLiveData<String> _poolId = new MutableLiveData<>();
    public LiveData<String> poolId = _poolId;

    private final MutableLiveData<Integer> _waterCapacityLiters = new MutableLiveData<>();
    public LiveData<Integer> waterCapacityLiters = _waterCapacityLiters;

    private final MutableLiveData<String> _poolName = new MutableLiveData<>();
    public LiveData<String> poolName = _poolName;

    private final MutableLiveData<Boolean> _isPublic = new MutableLiveData<>();
    public LiveData<Boolean> isPublic = _isPublic;


    private final MutableLiveData<Map<String, Object>> _lastPhTest = new MutableLiveData<>(new HashMap<>());
    public LiveData<Map<String, Object>> lastPhTest = _lastPhTest;


    private final MutableLiveData<Map<String, Object>> _lastChlorineTest = new MutableLiveData<>(new HashMap<>());
    public LiveData<Map<String, Object>> lastChlorineTest = _lastChlorineTest;

    private final MutableLiveData<Map<String, Object>> _lastAlkalinityTest = new MutableLiveData<>(new HashMap<>());
    public LiveData<Map<String, Object>> lastAlkalinityTest = _lastAlkalinityTest;


    private final MutableLiveData<Map<String, Object>> _lastStabilizerTest = new MutableLiveData<>(new HashMap<>());
    public LiveData<Map<String, Object>> lastStabilizerTest = _lastStabilizerTest;


    private final MutableLiveData<Boolean> _isLoadingPool = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoadingPool = _isLoadingPool;

    public void setPoolDataManually(PoolModel pool) {
        if (pool != null) {
            _currentPoolModel.setValue(pool);
            updateIndividualFields(pool);
        }
    }

    public void fetchPoolData(String pId) {
        if (pId == null || pId.isEmpty()) {
            clearPoolData();
            return;
        }


        if (pId.equals(_poolId.getValue())) {

            return;
        }

        _isLoadingPool.setValue(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("pools").document(pId).get().addOnCompleteListener(task -> {
            _isLoadingPool.setValue(false);
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                DocumentSnapshot document = task.getResult();

                PoolModel pool = new PoolModel(document);

                _currentPoolModel.setValue(pool);
                updateIndividualFields(pool);

            } else {
                clearPoolData();
            }
        });
    }

    private void updateIndividualFields(PoolModel pool) {
        _poolId.setValue(pool.getPoolId());
        _poolName.setValue(pool.getName());

        _isPublic.setValue(pool.isPublic());

        Long capacityLong = pool.getWaterCapacityLiters();
        if (capacityLong != null) {
            _waterCapacityLiters.setValue(capacityLong.intValue());
        } else {
            _waterCapacityLiters.setValue(0);
        }
    }

    public void clearPoolData() {
        _currentPoolModel.setValue(null);
        _poolId.setValue(null);
        _poolName.setValue(null);
        _waterCapacityLiters.setValue(null);
        _isPublic.setValue(false); // ⭐ NEW: Reset isPublic state
        _lastPhTest.setValue(new HashMap<>());
        _lastChlorineTest.setValue(new HashMap<>());
        _lastAlkalinityTest.setValue(new HashMap<>());
        _lastStabilizerTest.setValue(new HashMap<>());
    }

    public void savePoolVisibility(String poolId, boolean isPublic) {
        if (poolId == null || poolId.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("pools").document(poolId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("isPublic", isPublic);

        docRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update the LiveData/Model on success
                    PoolModel current = _currentPoolModel.getValue();
                    if (current != null) {
                        current.setPublic(isPublic);
                        _currentPoolModel.setValue(current); // Trigger LiveData update
                        _isPublic.setValue(isPublic); // Update individual field LiveData
                        Log.d("PoolViewModel", "Pool visibility saved successfully to: " + isPublic);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PoolViewModel", "Error updating pool visibility: " + e.getMessage());
                });
    }

    public void savePoolLocation(String poolId, double lat, double lon) {
        if (poolId == null || poolId.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("pools").document(poolId);

        GeoPoint newLocation = new GeoPoint(lat, lon);

        Map<String, Object> updates = new HashMap<>();
        updates.put("location", newLocation); // Save as GeoPoint

        docRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update the LiveData/Model on success
                    PoolModel current = _currentPoolModel.getValue();
                    if (current != null) {
                        current.setLocation(newLocation);
                        _currentPoolModel.setValue(current); // Trigger LiveData update
                        Log.d("PoolViewModel", "Pool location saved successfully.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PoolViewModel", "Error updating pool location: " + e.getMessage());
                });
    }

    // ... (Test Setters are unchanged) ...
    public void setLastPhTest(Double pH, Long date) {
        Map<String, Object> newTest = new HashMap<>();
        newTest.put("pH", pH);
        newTest.put("date", date);
        _lastPhTest.setValue(newTest);
    }

    public void setLastChlorineTest(Double chlorine, Long date) {
        Map<String, Object> newTest = new HashMap<>();
        newTest.put("chlorine", chlorine);
        newTest.put("date", date);
        _lastChlorineTest.setValue(newTest);
    }

    public void setLastAlkalinityTest(Double alkalinity, Long date) {
        Map<String, Object> newTest = new HashMap<>();
        newTest.put("alkalinity", alkalinity);
        newTest.put("date", date);
        _lastAlkalinityTest.setValue(newTest);
    }
    public void setLastStabilizerTest(Double stabilizer, Long date) {
        Map<String, Object> newTest = new HashMap<>();
        newTest.put("stabilizer", stabilizer);
        newTest.put("date", date);
        _lastStabilizerTest.setValue(newTest);
    }
}