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

public class StabilizerCalculator extends Fragment implements HeaderUpdatable {

    // 💥 UPDATED IDs to reference Stabilizer/CYA
    private EditText etCurrentCya, etTargetCya, etPoolVolume;
    private AutoCompleteTextView actvChemicalType; // Removed actvVolumeUnit
    private TextInputLayout tilChemicalType;
    private TextView tvChemicalDetails, tvDosageResult;
    private CardView cvResult;
    private MaterialButton btnCalculate, btnSaveLog;

    private PoolViewModel poolViewModel;
    private String poolId;

    private static final String UNIT_LITRES = "L";
    private static final double IDEAL_CYA = 40.0; // Default target

    private Map<String, ChemicalDosageInfo> chemicalInfoMap;

    private FirebaseFirestore db;
    private FirebaseAuth auth;


    private double savedDosageAmount = 0.0;
    private String savedDosageUnit = "";
    private String savedChemicalName = "";

    public StabilizerCalculator() {}

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
        // 💥 Uses the XML for the Stabilizer Calculator
        return inflater.inflate(R.layout.stabilizer_calculator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        poolViewModel = new ViewModelProvider(requireActivity()).get(PoolViewModel.class);

        // 💥 BINDING UPDATED STABILIZER/CYA VIEWS
        etCurrentCya = view.findViewById(R.id.et_current_cya);
        etTargetCya = view.findViewById(R.id.et_target_cya);
        etPoolVolume = view.findViewById(R.id.et_pool_volume);
        actvChemicalType = view.findViewById(R.id.actv_chemical_type);
        tilChemicalType = view.findViewById(R.id.til_chemical_type);
        tvChemicalDetails = view.findViewById(R.id.tv_chemical_details);
        tvDosageResult = view.findViewById(R.id.tv_dosage_result);
        cvResult = view.findViewById(R.id.cv_result);
        btnCalculate = view.findViewById(R.id.btn_calculate);
        btnSaveLog = view.findViewById(R.id.btn_save_log);

        setupListeners();
        setupSpinners();
        observePoolData();
    }

    private void setupListeners() {
        btnCalculate.setOnClickListener(v -> calculateDosage());

        // 💥 Listener for Save Log Button
        btnSaveLog.setOnClickListener(v -> saveLogToFirestore());

        // The CYA dropdown is read-only, so this listener is unnecessary but harmless.
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

        etCurrentCya.addTextChangedListener(inputWatcher);
        etTargetCya.addTextChangedListener(inputWatcher);
        etPoolVolume.addTextChangedListener(inputWatcher);
        actvChemicalType.addTextChangedListener(inputWatcher);
    }

    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            String title =  "Stabilizer (CYA) Calculator";
            ((MainActivity) getActivity()).updateHeader(title, true, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }

    private void setupSpinners() {
        // Since only one chemical is typically used to raise CYA, we preset the dropdown.
        String[] chemicalTypes = chemicalInfoMap.keySet().toArray(new String[0]);
        ArrayAdapter<String> chemicalAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, chemicalTypes);
        actvChemicalType.setAdapter(chemicalAdapter);
        actvChemicalType.setText(chemicalTypes[0], false); // Set default value
    }

    private void observePoolData() {
        poolViewModel.currentPoolModel.observe(getViewLifecycleOwner(), poolModel -> {
            if (poolModel != null) {
                poolId = poolModel.getPoolId();
                if (poolModel.getWaterCapacityLiters() != null) {
                    // Pool volume is pre-filled, unit is assumed to be Liters
                    etPoolVolume.setText(String.valueOf(poolModel.getWaterCapacityLiters()));
                }
                // Update chemical details on load
                updateChemicalDetails(actvChemicalType.getText().toString());
            } else {
                poolId = null;
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

    // 💥 UPDATED CHEMICAL DATA FOR STABILIZER (CYA)
    private void initializeChemicalData() {
        chemicalInfoMap = new HashMap<>();

        // Cyanuric Acid (Granular)
        chemicalInfoMap.put("Cyanuric Acid (CYA Granular)", new ChemicalDosageInfo(
                "Cyanuric Acid (CYA). Add slowly to the skimmer with the pump running, or pre-dissolve and add directly. It takes 48-72 hours to fully dissolve and register. DO NOT backwash or add fresh water for at least 48 hours after application.",
                "CYA Increaser (Granular)",
                100.0 // grams/10,000L to raise CYA by 10 ppm
        ));
    }

    private void calculateDosage() {
        // Reset saved values
        savedDosageAmount = 0.0;
        savedDosageUnit = "";
        savedChemicalName = "";

        String currentCyaStr = etCurrentCya.getText().toString();
        String targetCyaStr = etTargetCya.getText().toString();
        String volumeStr = etPoolVolume.getText().toString();
        String chemicalName = actvChemicalType.getText().toString();

        if (currentCyaStr.isEmpty() || volumeStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in current Stabilizer and pool volume (*).", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double currentCya = Double.parseDouble(currentCyaStr);
            // Default target from XML is 40, but use the EditText value if available
            double targetCya = targetCyaStr.isEmpty() ? IDEAL_CYA : Double.parseDouble(targetCyaStr);
            double volume = Double.parseDouble(volumeStr);

            if (currentCya < 0 || targetCya < 0 || currentCya > 200 || targetCya > 200) {
                Toast.makeText(getContext(), "CYA levels must be realistic (0-200 ppm).", Toast.LENGTH_SHORT).show();
                return;
            }

            if (volume <= 0) {
                Toast.makeText(getContext(), "Pool volume must be greater than zero.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentCya >= targetCya) {
                if (currentCya > targetCya + 5) {
                    tvDosageResult.setText("Stabilizer is too high. Dilute by draining and refilling water.");
                } else {
                    tvDosageResult.setText("No adjustment required. Stabilizer is stable.");
                }
                cvResult.setVisibility(View.VISIBLE);
                return;
            }

            // Since we only handle increasing CYA, this is a valid state
            if (currentCya < targetCya && !chemicalName.toLowerCase().contains("increaser")) {
                // This is a safety check, but the dropdown should only contain the increaser
                Toast.makeText(getContext(), "You must use a CYA Increaser to raise the Stabilizer.", Toast.LENGTH_LONG).show();
                return;
            }

            ChemicalDosageInfo info = chemicalInfoMap.get(chemicalName);
            if (info == null) {
                Toast.makeText(getContext(), "Selected chemical is invalid.", Toast.LENGTH_SHORT).show();
                return;
            }

            double requiredCyaChange = targetCya - currentCya;

            double dosageRate = info.dosageRate;
            double baseVolume = 10000.0;
            double baseCyaChange = 10.0; // Dosage rate is for every 10 ppm change

            // Dosage Required = (Delta CYA / 10 ppm) * (Volume / 10000L) * Dosage Rate
            double dosageRequiredMetric;

            dosageRequiredMetric = (requiredCyaChange / baseCyaChange) * (volume / baseVolume) * dosageRate;

            String finalUnit;
            String chemicalType;
            String amountFormat;
            double dosageToDisplay;

            // Stabilizer is always granular, so no 'liquid' check needed, but kept the structure
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
            double currentCya = Double.parseDouble(etCurrentCya.getText().toString());
            double targetCya = etTargetCya.getText().toString().isEmpty() ? IDEAL_CYA : Double.parseDouble(etTargetCya.getText().toString());
            double volume = Double.parseDouble(etPoolVolume.getText().toString());

            // 1. Prepare data (only the fields we want to update/set)
            Map<String, Object> logUpdates = new HashMap<>();

            // Core Metric 💥 UPDATED KEY
            logUpdates.put("stabilizer", currentCya);

            // Dosage/Calculator Metadata 💥 UPDATED KEYS
            logUpdates.put("targetStabilizer", targetCya);
            logUpdates.put("poolVolume", volume);
            logUpdates.put("cyaDosageAmount", savedDosageAmount);
            logUpdates.put("cyaDosageUnit", savedDosageUnit);
            logUpdates.put("cyaChemicalName", savedChemicalName);

            // Explicitly set timestamp
            logUpdates.put("timestamp", new Date());

            // 2. Generate the consistent Document ID
            String dailyLogId = generateDailyLogId(poolId);

            // 3. Perform the MERGE update
            db.collection("pools").document(poolId)
                    .collection("testLogs").document(dailyLogId)
                    // Use set(logUpdates, SetOptions.merge()) to update only the fields in the map
                    .set(logUpdates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Stabilizer (CYA) Test Log recorded successfully.", Toast.LENGTH_SHORT).show();
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