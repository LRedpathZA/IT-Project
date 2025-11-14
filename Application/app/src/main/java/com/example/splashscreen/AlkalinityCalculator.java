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

public class AlkalinityCalculator extends Fragment implements HeaderUpdatable {

    private EditText etCurrentAlkalinity, etTargetAlkalinity, etPoolVolume;
    private AutoCompleteTextView actvChemicalType; // Removed actvVolumeUnit
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
    private static final double IDEAL_ALKALINITY = 100.0; // Default target

    public AlkalinityCalculator() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeChemicalData();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.alkalinity_calculator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        poolViewModel = new ViewModelProvider(requireActivity()).get(PoolViewModel.class);
        etCurrentAlkalinity = view.findViewById(R.id.et_current_alkalinity);
        etTargetAlkalinity = view.findViewById(R.id.et_target_alkalinity);
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

        etCurrentAlkalinity.addTextChangedListener(inputWatcher);
        etTargetAlkalinity.addTextChangedListener(inputWatcher);
        etPoolVolume.addTextChangedListener(inputWatcher);
        actvChemicalType.addTextChangedListener(inputWatcher);
    }

    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            String title =  "Alkalinity Calculator";
            ((MainActivity) getActivity()).updateHeader(title, true, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }

    private void setupSpinners() {
        String[] chemicalTypes = chemicalInfoMap.keySet().toArray(new String[0]);
        ArrayAdapter<String> chemicalAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, chemicalTypes);
        actvChemicalType.setAdapter(chemicalAdapter);
    }

    private void observePoolData() {
        poolViewModel.currentPoolModel.observe(getViewLifecycleOwner(), poolModel -> {
            if (poolModel != null) {
                poolId = poolModel.getPoolId();
                if (poolModel.getWaterCapacityLiters() != null) {
                    // Pool volume is pre-filled, unit is assumed to be Liters
                    etPoolVolume.setText(String.valueOf(poolModel.getWaterCapacityLiters()));
                }
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

    private void initializeChemicalData() {
        chemicalInfoMap = new HashMap<>();

        chemicalInfoMap.put("Alkalinity Increaser (Sodium Bicarbonate)", new ChemicalDosageInfo(
                "Sodium Bicarbonate (Baking Soda, $\\text{NaHCO}_3$). Safest way to raise TA. Pre-dissolve in water and broadcast over the pool surface. Slowly raise TA by no more than 20 ppm per day.",
                "TA Increaser (Granular)",
                150.0 // grams/10,000L to raise TA by 10 ppm
        ));

        // Alkalinity Decreaser (Muriatic Acid - same as pH Decreaser, but applied differently)
        chemicalInfoMap.put("Alkalinity Decreaser (Muriatic Acid - 31%)", new ChemicalDosageInfo(
                "Muriatic Acid (Hydrochloric Acid, HCl). Decreases both TA and pH. Add slowly to water, never water to acid. Lower TA requires careful, localized addition to the deepest area.",
                "TA Decreaser (Liquid)",
                700.0 // ml/10,000L to drop TA by 10 ppm
        ));
    }

    private void calculateDosage() {
        savedDosageAmount = 0.0;
        savedDosageUnit = "";
        savedChemicalName = "";

        String currentAlkStr = etCurrentAlkalinity.getText().toString();
        String targetAlkStr = etTargetAlkalinity.getText().toString();
        String volumeStr = etPoolVolume.getText().toString();
        String chemicalName = actvChemicalType.getText().toString();

        if (currentAlkStr.isEmpty() || volumeStr.isEmpty() || chemicalName.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all required fields (*).", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double currentAlk = Double.parseDouble(currentAlkStr);
            double targetAlk = targetAlkStr.isEmpty() ? IDEAL_ALKALINITY : Double.parseDouble(targetAlkStr);
            double volume = Double.parseDouble(volumeStr);

            if (currentAlk < 0 || targetAlk < 0) {
                Toast.makeText(getContext(), "Alkalinity levels must be non-negative.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (volume <= 0) {
                Toast.makeText(getContext(), "Pool volume must be greater than zero.", Toast.LENGTH_SHORT).show();
                return;
            }


            if (Math.abs(currentAlk - targetAlk) <= 10.0) {
                tvDosageResult.setText("No major adjustment required. Alkalinity is stable.");
                cvResult.setVisibility(View.VISIBLE);
                return;
            }

            if (currentAlk > targetAlk && !chemicalName.toLowerCase().contains("decreaser")) {
                Toast.makeText(getContext(), "You need an Alkalinity Decreaser to lower the TA.", Toast.LENGTH_LONG).show();
                return;
            }

            if (currentAlk < targetAlk && !chemicalName.toLowerCase().contains("increaser")) {
                Toast.makeText(getContext(), "You need an Alkalinity Increaser to raise the TA.", Toast.LENGTH_LONG).show();
                return;
            }

            ChemicalDosageInfo info = chemicalInfoMap.get(chemicalName);
            if (info == null) {
                Toast.makeText(getContext(), "Selected chemical is invalid.", Toast.LENGTH_SHORT).show();
                return;
            }

            double requiredAlkChange = targetAlk - currentAlk;
            double alkChangeAbsolute = Math.abs(requiredAlkChange);

            double dosageRate = info.dosageRate;
            double baseVolume = 10000.0;
            double baseAlkChange = 10.0; // Dosage rate is for every 10 ppm change

            // Dosage Required = (|Delta TA| / 10 ppm) * (Volume / 10000L) * Dosage Rate
            double dosageRequiredMetric;

            dosageRequiredMetric = (alkChangeAbsolute / baseAlkChange) * (volume / baseVolume) * dosageRate;

            String finalUnit;
            String chemicalType;
            String amountFormat;
            double dosageToDisplay;

            if (info.baseType.toLowerCase().contains("liquid")) {
                dosageToDisplay = dosageRequiredMetric / 1000.0;
                finalUnit = "L";
                chemicalType = "of " + chemicalName.split("\\(")[0].trim();
                amountFormat = "%.2f";
            } else {
                if (volume > 40000) {
                    dosageToDisplay = dosageRequiredMetric / 1000.0;
                    finalUnit = "kg";
                    amountFormat = "%.2f";
                } else {
                    dosageToDisplay = dosageRequiredMetric;
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


    private String generateDailyLogId(String poolId) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateString = sdf.format(new Date());


        return poolId + "_" + dateString;
    }


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
            double currentAlk = Double.parseDouble(etCurrentAlkalinity.getText().toString());
            double targetAlk = etTargetAlkalinity.getText().toString().isEmpty() ? IDEAL_ALKALINITY : Double.parseDouble(etTargetAlkalinity.getText().toString());
            double volume = Double.parseDouble(etPoolVolume.getText().toString());

            Map<String, Object> logUpdates = new HashMap<>();


            logUpdates.put("alkalinity", currentAlk);


            logUpdates.put("targetAlkalinity", targetAlk);
            logUpdates.put("poolVolume", volume);
            logUpdates.put("alkDosageAmount", savedDosageAmount);
            logUpdates.put("alkDosageUnit", savedDosageUnit);
            logUpdates.put("alkChemicalName", savedChemicalName);


            logUpdates.put("timestamp", new Date());


            String dailyLogId = generateDailyLogId(poolId);


            db.collection("pools").document(poolId)
                    .collection("testLogs").document(dailyLogId)

                    .set(logUpdates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Alkalinity Test Log recorded successfully.", Toast.LENGTH_SHORT).show();
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