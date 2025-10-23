package com.example.splashscreen; // Ensure this matches your package name

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

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChlorineCalculator extends Fragment implements HeaderUpdatable {

    // Views
    private EditText etCurrentChlorine, etTargetChlorine, etPoolVolume;
    private AutoCompleteTextView actvChlorineType;
    private MaterialButton btnCalculate, btnSaveLog;
    private TextView tvDosageResult, tvChlorineDetails;
    private CardView cvResult;

    // Data and ViewModel
    private PoolViewModel poolViewModel;
    private String poolId;

    private Map<String, ChemicalDosageInfo> chemicalInfoMap;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private double savedDosageAmount = 0.0;
    private String savedDosageUnit = "";
    private String savedChemicalName = "";

    // Constants for calculation
    private static final double DEFAULT_TARGET_PPM = 3.0;

    public ChlorineCalculator() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeChemicalData();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout: chlorine_calculator.xml
        return inflater.inflate(R.layout.chlorine_calculator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        poolViewModel = new ViewModelProvider(requireActivity()).get(PoolViewModel.class);

        initViews(view);
        setupPoolDataObservation();
        setupChlorineTypeDropdown();
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }

    // --- Header Updatable Implementation ---
    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            // Update the header title, showing the back button and pool selector
            ((MainActivity) getActivity()).updateHeader("Chlorine Calculator", true, true);
        }
    }
    // ----------------------------------------

    private void initViews(View view) {
        etCurrentChlorine = view.findViewById(R.id.et_current_chlorine);
        etTargetChlorine = view.findViewById(R.id.et_target_chlorine);
        etPoolVolume = view.findViewById(R.id.et_pool_volume_chl); // Note the ID suffix
        actvChlorineType = view.findViewById(R.id.actv_chlorine_type);
        btnCalculate = view.findViewById(R.id.btn_calculate_chl);
        btnSaveLog = view.findViewById(R.id.btn_save_log_chl);
        cvResult = view.findViewById(R.id.cv_result_chl);
        tvDosageResult = view.findViewById(R.id.tv_dosage_result_chl);
        tvChlorineDetails = view.findViewById(R.id.tv_chlorine_details);

        // Hide result card initially
        cvResult.setVisibility(View.GONE);
    }

    private void initializeChemicalData() {
        chemicalInfoMap = new HashMap<>();

        // Dosage factors are example rates to achieve a 1.0 ppm increase in 10,000L
        chemicalInfoMap.put("Liquid Chlorine (10% Sodium Hypochlorite)", new ChemicalDosageInfo(
                "Unstabilized, high pH. Dosage rate: ~1000 ml per 10,000L for 1.0 ppm boost.",
                "Liquid",
                1000.0 // ml/10,000L for 1.0 ppm
        ));
        chemicalInfoMap.put("Granular (65% Calcium Hypochlorite)", new ChemicalDosageInfo(
                "Strong oxidizer, raises pH and calcium. Dosage rate: ~15 grams per 10,000L for 1.0 ppm boost.",
                "Granular",
                15.0 // grams/10,000L for 1.0 ppm
        ));
        chemicalInfoMap.put("Stabilized (90% Trichlor)", new ChemicalDosageInfo(
                "Acidic, adds CYA. Dosage rate: ~10 grams per 10,000L for 1.0 ppm boost.",
                "Granular",
                10.0 // grams/10,000L for 1.0 ppm
        ));
    }

    private void setupChlorineTypeDropdown() {
        String[] chlorineOptions = chemicalInfoMap.keySet().toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                chlorineOptions
        );
        actvChlorineType.setAdapter(adapter);

        // Set an initial chemical and details
        if (chlorineOptions.length > 0) {
            actvChlorineType.setText(chlorineOptions[0], false);
            updateChemicalDetails(chlorineOptions[0]);
        }

        actvChlorineType.setOnItemClickListener((parent, view, position, id) -> {
            String selectedChemical = (String) parent.getItemAtPosition(position);
            updateChemicalDetails(selectedChemical);
            cvResult.setVisibility(View.GONE); // Hide result on new chemical selection
        });
    }

    private void updateChemicalDetails(String chemicalType) {
        ChemicalDosageInfo info = chemicalInfoMap.get(chemicalType);
        if (info != null) {
            tvChlorineDetails.setText(info.details);
        } else {
            tvChlorineDetails.setText("Select a chlorine product above to see safety instructions, strength details, and application procedures.");
        }
    }

    private void setupPoolDataObservation() {
        poolViewModel.currentPoolModel.observe(getViewLifecycleOwner(), poolModel -> {
            if (poolModel != null) {
                poolId = poolModel.getPoolId();
                if (poolModel.getWaterCapacityLiters() != null && poolModel.getWaterCapacityLiters() > 0) {
                    // Pre-fill pool volume from PoolModel
                    etPoolVolume.setText(String.valueOf(poolModel.getWaterCapacityLiters()));
                }
            } else {
                poolId = null;
            }
        });
    }

    private void setupListeners() {
        btnCalculate.setOnClickListener(v -> calculateDosage());
        btnSaveLog.setOnClickListener(v -> saveLogToFirestore());

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

        etCurrentChlorine.addTextChangedListener(inputWatcher);
        etTargetChlorine.addTextChangedListener(inputWatcher);
        etPoolVolume.addTextChangedListener(inputWatcher);
        actvChlorineType.addTextChangedListener(inputWatcher);
    }

    private void calculateDosage() {
        // Reset saved values
        savedDosageAmount = 0.0;
        savedDosageUnit = "";
        savedChemicalName = "";

        String currentChlorineStr = etCurrentChlorine.getText().toString();
        String targetChlorineStr = etTargetChlorine.getText().toString();
        String poolVolumeStr = etPoolVolume.getText().toString();
        String chemicalName = actvChlorineType.getText().toString();

        if (currentChlorineStr.isEmpty() || poolVolumeStr.isEmpty() || chemicalName.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in current chlorine, volume, and chemical type.", Toast.LENGTH_LONG).show();
            cvResult.setVisibility(View.GONE);
            return;
        }

        try {
            double currentChlorine = Double.parseDouble(currentChlorineStr);
            double targetChlorine = targetChlorineStr.isEmpty() ? DEFAULT_TARGET_PPM : Double.parseDouble(targetChlorineStr);
            double volume = Double.parseDouble(poolVolumeStr);

            if (volume <= 0 || currentChlorine < 0 || targetChlorine < 0) {
                Toast.makeText(getContext(), "Chlorine levels and volume must be positive numbers.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentChlorine >= targetChlorine) {
                tvDosageResult.setText(String.format(Locale.getDefault(),
                        "%.1f ppm. No Addition Required.", currentChlorine));
                cvResult.setVisibility(View.VISIBLE);
                return;
            }

            ChemicalDosageInfo info = chemicalInfoMap.get(chemicalName);
            if (info == null) {
                Toast.makeText(getContext(), "Selected chlorine product is invalid.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Determine Required Change (in ppm)
            double requiredPPMIncrease = targetChlorine - currentChlorine;

            // 2. Dosage Calculation
            double dosageRatePerPPM = info.dosageRate; // e.g., 1000 ml / 10,000L / 1.0 ppm
            double baseVolume = 10000.0;

            // Dosage = (Volume / BaseVolume) * RequiredPPMChange * DosageRatePerPPM
            double dosageRequiredMetric = (volume / baseVolume) * requiredPPMIncrease * dosageRatePerPPM;

            // 3. Determine Units for Display
            String finalUnit;
            String chemicalTypeDisplay = "of " + chemicalName.split("\\(")[0].trim();
            String amountFormat;
            double dosageToDisplay;

            if (info.baseType.equals("Liquid")) {
                dosageToDisplay = dosageRequiredMetric / 1000.0; // Convert ml to L
                finalUnit = "L";
                amountFormat = "%.2f";
            } else { // Granular
                if (dosageRequiredMetric >= 1000) {
                    dosageToDisplay = dosageRequiredMetric / 1000.0; // Convert g to kg
                    finalUnit = "kg";
                    amountFormat = "%.2f";
                } else {
                    dosageToDisplay = dosageRequiredMetric; // Display in g
                    finalUnit = "g";
                    amountFormat = "%.0f";
                }
            }

            // 4. Save values for logging
            savedDosageAmount = dosageToDisplay;
            savedDosageUnit = finalUnit;
            savedChemicalName = chemicalName;

            // 5. Display Result
            String resultText = String.format(Locale.getDefault(), amountFormat + " %s %s", dosageToDisplay, finalUnit, chemicalTypeDisplay);

            tvDosageResult.setText(resultText);
            cvResult.setVisibility(View.VISIBLE);

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number input. Please check your values.", Toast.LENGTH_SHORT).show();
            cvResult.setVisibility(View.GONE);
        }
    }

    // Helper to generate a consistent document ID for the current day's log
    private String generateDailyLogId(String poolId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateString = sdf.format(new Date());
        return poolId + "_" + dateString;
    }

    private void saveLogToFirestore() {
        if (poolId == null || poolId.isEmpty()) {
            Toast.makeText(getContext(), "Error: No pool selected. Cannot log test.", Toast.LENGTH_LONG).show();
            return;
        }

        if (cvResult.getVisibility() != View.VISIBLE || savedDosageAmount == 0.0) {
            Toast.makeText(getContext(), "Please Calculate the dosage before saving the log.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double currentChlorine = Double.parseDouble(etCurrentChlorine.getText().toString());
            double targetChlorine = etTargetChlorine.getText().toString().isEmpty() ? DEFAULT_TARGET_PPM : Double.parseDouble(etTargetChlorine.getText().toString());
            double volume = Double.parseDouble(etPoolVolume.getText().toString());

            // 1. Prepare data (only the fields we want to update/set)
            Map<String, Object> logUpdates = new HashMap<>();

            // Core Metric
            logUpdates.put("chlorine", currentChlorine);

            // Dosage/Calculator Metadata - 💥 USING CHLORINE-SPECIFIC KEYS
            logUpdates.put("targetChlorine", targetChlorine);
            // Re-saving volume helps ensure the log has all context, though it may be redundant with pool data
            logUpdates.put("poolVolume", volume);
            logUpdates.put("chlDosageAmount", savedDosageAmount);
            logUpdates.put("chlDosageUnit", savedDosageUnit);
            logUpdates.put("chlChemicalName", savedChemicalName);

            // Explicitly set timestamp
            logUpdates.put("timestamp", new Date());

            // 2. Generate the consistent Document ID
            String dailyLogId = generateDailyLogId(poolId);

            // 3. Perform the MERGE update
            db.collection("pools").document(poolId)
                    .collection("testLogs").document(dailyLogId)
                    .set(logUpdates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Chlorine Test Log recorded successfully.", Toast.LENGTH_SHORT).show();
                        // Optional: Pop back to Pool Health after successful log
//                        if (getParentFragmentManager() != null) {
//                            getParentFragmentManager().popBackStack();
//                        }
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
        final String baseType; // e.g., "Liquid", "Granular"
        final double dosageRate; // Amount per 10,000L per 1.0 ppm change

        ChemicalDosageInfo(String details, String baseType, double dosageRate) {
            this.details = details;
            this.baseType = baseType;
            this.dosageRate = dosageRate;
        }
    }
}