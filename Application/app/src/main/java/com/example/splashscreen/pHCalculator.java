package com.example.splashscreen;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.splashscreen.data.models.PoolViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
// 💥 FIREBASE IMPORTS
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class pHCalculator extends Fragment implements HeaderUpdatable {

    private EditText etCurrentPh, etTargetPh, etPoolVolume;
    private AutoCompleteTextView actvVolumeUnit, actvChemicalType;
    private TextInputLayout tilChemicalType;
    private TextView tvChemicalDetails, tvDosageResult;
    private CardView cvResult;
    private MaterialButton btnCalculate, btnSaveLog;

    private PoolViewModel poolViewModel;
    private String poolId;

    private static final String UNIT_LITRES = "L";

    private Map<String, ChemicalDosageInfo> chemicalInfoMap;

    private FirebaseFirestore db;
    private FirebaseAuth auth;


    private double savedDosageAmount = 0.0;
    private String savedDosageUnit = "";
    private String savedChemicalName = "";

    public pHCalculator() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeChemicalData();
        // 💥 INITIALIZE FIREBASE
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ph_calculator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        poolViewModel = new ViewModelProvider(requireActivity()).get(PoolViewModel.class);

        etCurrentPh = view.findViewById(R.id.et_current_ph);
        etTargetPh = view.findViewById(R.id.et_target_ph);
        etPoolVolume = view.findViewById(R.id.et_pool_volume);
        actvVolumeUnit = view.findViewById(R.id.actv_volume_unit);
        actvChemicalType = view.findViewById(R.id.actv_chemical_type);
        tilChemicalType = view.findViewById(R.id.til_chemical_type);
        tvChemicalDetails = view.findViewById(R.id.tv_chemical_details);
        tvDosageResult = view.findViewById(R.id.tv_dosage_result);
        cvResult = view.findViewById(R.id.cv_result);
        btnCalculate = view.findViewById(R.id.btn_calculate);
        // btnBack = view.findViewById(R.id.btn_back); // <-- REMOVED
        btnSaveLog = view.findViewById(R.id.btn_save_log);

        setupListeners();
        setupSpinners();
        observePoolData();
    }

    private void setupListeners() {
        // btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack()); // <-- REMOVED to fix NPE
        btnCalculate.setOnClickListener(v -> calculateDosage());

        // 💥 Listener for Save Log Button
        btnSaveLog.setOnClickListener(v -> saveLogToFirestore());

        actvChemicalType.setOnItemClickListener((parent, view, position, id) -> updateChemicalDetails(actvChemicalType.getText().toString()));

        TextWatcher inputWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cvResult.setVisibility(View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        etCurrentPh.addTextChangedListener(inputWatcher);
        etTargetPh.addTextChangedListener(inputWatcher);
        etPoolVolume.addTextChangedListener(inputWatcher);
        actvVolumeUnit.addTextChangedListener(inputWatcher);
        actvChemicalType.addTextChangedListener(inputWatcher);
    }

    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            String title =  "pH Calculator";
            ((MainActivity) getActivity()).updateHeader(title, true, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }
    private void setupSpinners() {
        String[] volumeUnits = new String[]{UNIT_LITRES};
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, volumeUnits);
        actvVolumeUnit.setAdapter(unitAdapter);
        actvVolumeUnit.setText(UNIT_LITRES, false);

        String[] chemicalTypes = chemicalInfoMap.keySet().toArray(new String[0]);
        ArrayAdapter<String> chemicalAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, chemicalTypes);
        actvChemicalType.setAdapter(chemicalAdapter);
    }

    private void observePoolData() {
        poolViewModel.currentPoolModel.observe(getViewLifecycleOwner(), poolModel -> {
            if (poolModel != null) {
                poolId = poolModel.getPoolId();
                if (poolModel.getWaterCapacityLiters() != null) {
                    etPoolVolume.setText(String.valueOf(poolModel.getWaterCapacityLiters()));
                    actvVolumeUnit.setText(UNIT_LITRES, false);
                }
            } else {
                poolId = null;
                // Consider showing a warning if no pool is selected/loaded
            }
        });
    }

    private void updateChemicalDetails(String chemicalName) {
        ChemicalDosageInfo info = chemicalInfoMap.get(chemicalName);
        if (info != null) {
            tvChemicalDetails.setText(info.details);
        } else {
            tvChemicalDetails.setText("Select a chemical above to see safety instructions, dosage caveats, and application procedures.");
        }
        cvResult.setVisibility(View.GONE);
    }

    private void initializeChemicalData() {
        chemicalInfoMap = new HashMap<>();

        // pH Decreasers
        chemicalInfoMap.put("pH Decreaser (Muriatic Acid - 31%)", new ChemicalDosageInfo(
                "Muriatic Acid (Hydrochloric Acid, HCl). Strong acid. Requires extreme caution. Add slowly to water, never water to acid. Ideal for high pH.",
                "pH Decreaser (Liquid)",
                1000.0 // ml/10,000L to drop pH by 0.1
        ));
        chemicalInfoMap.put("pH Decreaser (Sodium Bisulfate - Granular)", new ChemicalDosageInfo(
                "Sodium Bisulfate ($\\text{NaHSO}_4$). Safer alternative to liquid acid. Mix in bucket of water before adding to pool. Ideal for high pH.",
                "pH Decreaser (Granular)",
                100.0 // grams/10,000L to drop pH by 0.1
        ));

        // pH Increasers
        chemicalInfoMap.put("pH Increaser (Soda Ash / Sodium Carbonate)", new ChemicalDosageInfo(
                "Soda Ash ($\\text{Na}_2\\text{CO}_3$). Highly effective. Pre-dissolve in water and add over deep end. Also increases Total Alkalinity. Ideal for low pH.",
                "pH Increaser (Powder)",
                60.0 // grams/10,000L to raise pH by 0.1
        ));
    }

    private void calculateDosage() {
        // ... (calculateDosage logic remains the same) ...

        // Reset saved values
        savedDosageAmount = 0.0;
        savedDosageUnit = "";
        savedChemicalName = "";

        String currentPhStr = etCurrentPh.getText().toString();
        String targetPhStr = etTargetPh.getText().toString();
        String volumeStr = etPoolVolume.getText().toString();
        String chemicalName = actvChemicalType.getText().toString();

        if (currentPhStr.isEmpty() || volumeStr.isEmpty() || chemicalName.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all required fields (*).", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double currentPh = Double.parseDouble(currentPhStr);
            double targetPh = targetPhStr.isEmpty() ? 7.5 : Double.parseDouble(targetPhStr);
            double volume = Double.parseDouble(volumeStr);

            if (currentPh < 0 || currentPh > 14 || targetPh < 0 || targetPh > 14) {
                Toast.makeText(getContext(), "pH levels must be between 0 and 14.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (volume <= 0) {
                Toast.makeText(getContext(), "Pool volume must be greater than zero.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (Math.abs(currentPh - targetPh) < 0.05) {
                tvDosageResult.setText("No adjustment required. pH is stable.");
                cvResult.setVisibility(View.VISIBLE);
                return;
            }

            if (currentPh > targetPh && !chemicalName.toLowerCase().contains("decreaser")) {
                Toast.makeText(getContext(), "You need a pH Decreaser to lower the pH.", Toast.LENGTH_LONG).show();
                return;
            }

            if (currentPh < targetPh && !chemicalName.toLowerCase().contains("increaser")) {
                Toast.makeText(getContext(), "You need a pH Increaser to raise the pH.", Toast.LENGTH_LONG).show();
                return;
            }

            ChemicalDosageInfo info = chemicalInfoMap.get(chemicalName);
            if (info == null) {
                Toast.makeText(getContext(), "Selected chemical is invalid.", Toast.LENGTH_SHORT).show();
                return;
            }

            double requiredPhChange = targetPh - currentPh;
            double phChangeAbsolute = Math.abs(requiredPhChange);

            double dosageRate = info.dosageRate;
            double baseVolume = 10000.0;

            double dosageRequiredMetric;

            dosageRequiredMetric = (phChangeAbsolute / 0.1) * (volume / baseVolume) * dosageRate;

            String finalUnit;
            String chemicalType;
            String amountFormat;
            double dosageToDisplay;

            if (info.baseType.toLowerCase().contains("liquid")) {
                dosageToDisplay = dosageRequiredMetric / 1000.0; // Convert ml to L
                finalUnit = UNIT_LITRES;
                chemicalType = "of " + chemicalName.split("\\(")[0].trim();
                amountFormat = "%.2f";
            } else {
                if (volume > 40000) {
                    dosageToDisplay = dosageRequiredMetric / 1000.0; // Convert g to kg
                    finalUnit = "kg";
                    amountFormat = "%.2f";
                } else {
                    dosageToDisplay = dosageRequiredMetric; // Display in g
                    finalUnit = "g";
                    amountFormat = "%.0f";
                }
                chemicalType = "of " + chemicalName.split("\\(")[0].trim();
            }


            savedDosageAmount = dosageToDisplay;
            savedDosageUnit = finalUnit;
            savedChemicalName = chemicalName;

            String resultText = String.format(amountFormat + " %s %s", dosageToDisplay, finalUnit, chemicalType);

            tvDosageResult.setText(resultText);
            cvResult.setVisibility(View.VISIBLE);

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number input. Please check your values.", Toast.LENGTH_SHORT).show();
        }
    }


    // Helper to generate a consistent document ID for the current day's log
    private String generateDailyLogId(String poolId) {
        // Format the date as YYYY-MM-DD
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateString = sdf.format(new Date());

        // Use a composite key: [poolId]_[date]
        return poolId + "_" + dateString;
    }

    // 💥 UPDATED: Method to save the log to Firestore using MERGE/UPSERT
    private void saveLogToFirestore() {
        if (poolId == null || poolId.isEmpty()) {
            Toast.makeText(getContext(), "Error: No pool selected. Please select or create a pool first.", Toast.LENGTH_LONG).show();
            return;
        }

        if (cvResult.getVisibility() != View.VISIBLE || savedDosageAmount == 0.0) {
            Toast.makeText(getContext(), "Please Calculate the dosage before saving the log.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double currentPh = Double.parseDouble(etCurrentPh.getText().toString());
            double targetPh = etTargetPh.getText().toString().isEmpty() ? 7.5 : Double.parseDouble(etTargetPh.getText().toString());
            double volume = Double.parseDouble(etPoolVolume.getText().toString());

            // 1. Prepare data (only the fields we want to update/set)
            Map<String, Object> logUpdates = new HashMap<>();

            // Core Metric
            logUpdates.put("ph", currentPh);

            // Dosage/Calculator Metadata - 💥 UPDATE THESE KEYS
            logUpdates.put("targetPh", targetPh);
            logUpdates.put("poolVolume", volume);
            logUpdates.put("phDosageAmount", savedDosageAmount);
            logUpdates.put("phDosageUnit", savedDosageUnit);
            logUpdates.put("phChemicalName", savedChemicalName);

            // Explicitly set timestamp
            logUpdates.put("timestamp", new Date());

            // 2. Generate the consistent Document ID
            String dailyLogId = generateDailyLogId(poolId);

            // 3. Perform the MERGE update
            db.collection("pools").document(poolId)
                    .collection("testLogs").document(dailyLogId)

                    // Use set(logUpdates, SetOptions.merge()) to update only the fields in the map
                    // without affecting other fields (like 'chlorine' or 'alkalinity') if they exist.
                    .set(logUpdates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "pH Test Log recorded successfully.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error saving log: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to save log data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static class ChemicalDosageInfo {
        final String details;
        final String baseType;
        final double dosageRate;

        ChemicalDosageInfo(String details, String baseType, double dosageRate) {
            this.details = details;
            this.baseType = baseType;
            this.dosageRate = dosageRate;
        }
    }
}