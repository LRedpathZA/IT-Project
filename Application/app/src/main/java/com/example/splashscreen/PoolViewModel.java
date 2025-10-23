package com.example.splashscreen;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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

    private final MutableLiveData<Map<String, Object>> _lastPhTest = new MutableLiveData<>(new HashMap<>());
    public LiveData<Map<String, Object>> lastPhTest = _lastPhTest;


    private final MutableLiveData<Map<String, Object>> _lastChlorineTest = new MutableLiveData<>(new HashMap<>());
    public LiveData<Map<String, Object>> lastChlorineTest = _lastChlorineTest;


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

                // 1. Update core LiveData
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
        _lastPhTest.setValue(new HashMap<>());
        _lastChlorineTest.setValue(new HashMap<>());
    }

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
}