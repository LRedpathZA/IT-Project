package com.example.splashscreen;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.Date;
import java.util.Locale;

public class PoolHealth extends Fragment {

    private ImageButton btnBack, btnProfile;
    private TextView tvHealthScore, tvHealthStatus, tvLastTestDate;
    private TextView tvPhValue, tvChlorineValue, tvAlkalinityValue, tvStabilizerValue;
    private ProgressBar pbPhHealth, pbChlorineHealth, pbAlkalinityHealth, pbStabilizerHealth;
    private CardView cardMetricPh, cardMetricChlorine, cardMetricAlkalinity, cardMetricStabilizer;
    private MaterialButton btnGoToCalculators;
    private View gaugeRingPlaceholder;

    private FirebaseFirestore db;
    private String poolId = "CURRENT_POOL_ID"; // Placeholder: Should be loaded from ViewModel

    public PoolHealth() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pool_health, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupListeners();
        loadLatestPoolHealthData();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btn_back);
        btnProfile = view.findViewById(R.id.btn_profile);
        tvHealthScore = view.findViewById(R.id.tv_health_score);
        tvHealthStatus = view.findViewById(R.id.tv_health_status);
        tvLastTestDate = view.findViewById(R.id.tv_last_test_date);
        gaugeRingPlaceholder = view.findViewById(R.id.gauge_ring_placeholder);

        tvPhValue = view.findViewById(R.id.tv_ph_value);
        tvChlorineValue = view.findViewById(R.id.tv_chlorine_value);
        tvAlkalinityValue = view.findViewById(R.id.tv_alkalinity_value);
        tvStabilizerValue = view.findViewById(R.id.tv_stabilizer_value);

        pbPhHealth = view.findViewById(R.id.pb_ph_health);
        pbChlorineHealth = view.findViewById(R.id.pb_chlorine_health);
        pbAlkalinityHealth = view.findViewById(R.id.pb_alkalinity_health);
        pbStabilizerHealth = view.findViewById(R.id.pb_stabilizer_health);

        cardMetricPh = view.findViewById(R.id.card_metric_ph);
        cardMetricChlorine = view.findViewById(R.id.card_metric_chlorine);
        cardMetricAlkalinity = view.findViewById(R.id.card_metric_alkalinity);
        cardMetricStabilizer = view.findViewById(R.id.card_metric_stabilizer);

        btnGoToCalculators = view.findViewById(R.id.btn_go_to_calculators);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnProfile.setOnClickListener(v -> {
            Fragment profileFragment = new PO_Profile();
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, profileFragment); // Use your main container ID
            transaction.addToBackStack(null);
            transaction.commit();
        });

        View.OnClickListener metricClickListener = v -> {
            String metric = (String) v.getTag();
            // TODO: Implement navigation to a detailed visualization screen (e.g., pH Trend)
            Toast.makeText(getContext(), "Showing detailed trend for: " + metric, Toast.LENGTH_SHORT).show();
        };

        cardMetricPh.setTag("pH");
        cardMetricChlorine.setTag("Chlorine");
        cardMetricAlkalinity.setTag("Alkalinity");
        cardMetricStabilizer.setTag("Stabilizer");

        cardMetricPh.setOnClickListener(metricClickListener);
        cardMetricChlorine.setOnClickListener(metricClickListener);
        cardMetricAlkalinity.setOnClickListener(metricClickListener);
        cardMetricStabilizer.setOnClickListener(metricClickListener);

        btnGoToCalculators.setOnClickListener(v -> {
            // TODO: Implement navigation to your main Calculator selection screen
            Toast.makeText(getContext(), "Navigating to Calculators", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadLatestPoolHealthData() {
        if (poolId == null || poolId.isEmpty()) {
            tvHealthStatus.setText("Pool Not Selected");
            return;
        }

        db.collection("pools").document(poolId).collection("testLogs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        pHLogModel latestLog = queryDocumentSnapshots.getDocuments().get(0).toObject(pHLogModel.class);
                        if (latestLog != null) {
                            // NOTE: Since PHLogModel only stores pH data, we must assume other metrics
                            // are stored elsewhere or must be integrated here. For this example,
                            // we'll simulate a full PoolHealthData object.

                            PoolHealthData data = simulateFullPoolData(latestLog);
                            updateUI(data);
                        }
                    } else {
                        tvHealthStatus.setText("No Test Data");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading logs: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Placeholder to simulate a full health record based on the latest pH log
    private PoolHealthData simulateFullPoolData(pHLogModel latestPhLog) {
        // In a real app, you would load Chlorine and Alkalinity data here too.
        // For now, we use the latest pH and placeholder values for others.

        PoolHealthData data = new PoolHealthData();
        data.latestTestDate = latestPhLog.getTimestamp();
        data.ph = latestPhLog.getCurrentPh();
        data.chlorine = 3.0;
        data.alkalinity = 110.0;
        data.stabilizer = 45.0;
        return data;
    }

    private void updateUI(PoolHealthData data) {
        // 1. Calculate Health Score
        OverallHealthResult result = calculateOverallHealth(data);

        // 2. Update Main Gauge
        tvHealthScore.setText(String.format(Locale.getDefault(), "%.1f", result.score));
        tvHealthStatus.setText(result.status);
        tvHealthStatus.setTextColor(ContextCompat.getColor(requireContext(), result.colorResId));
        // Note: Dynamically setting the gauge ring color would require custom drawing logic.
        // For simplicity, we can set the background color of the placeholder view:
        gaugeRingPlaceholder.setBackground(ContextCompat.getDrawable(requireContext(), result.gaugeDrawableResId));

        if (data.latestTestDate != null) {
            tvLastTestDate.setText(String.format("Last Test: %s", android.text.format.DateFormat.getMediumDateFormat(getContext()).format(data.latestTestDate)));
        }

        // 3. Update Metric Blocks
        updateMetricBlock(tvPhValue, pbPhHealth, data.ph, 7.4, 7.6, "%.1f");
        updateMetricBlock(tvChlorineValue, pbChlorineHealth, data.chlorine, 1.0, 3.0, "%.1f ppm");
        updateMetricBlock(tvAlkalinityValue, pbAlkalinityHealth, data.alkalinity, 80.0, 120.0, "%.0f ppm");
        updateMetricBlock(tvStabilizerValue, pbStabilizerHealth, data.stabilizer, 30.0, 50.0, "%.0f ppm");
    }

    // -------------------------------------------------------------------------
    // ALGORITHMS AND HELPER METHODS
    // -------------------------------------------------------------------------

    private OverallHealthResult calculateOverallHealth(PoolHealthData data) {
        // Scoring is based on how far each metric deviates from the center of its ideal range.

        // IDEAL RANGES (Mid-point, Min, Max)
        final double PH_MID = 7.5; // (7.4 - 7.6)
        final double CHL_MID = 2.0; // (1.0 - 3.0)
        final double ALK_MID = 100.0; // (80 - 120)
        final double STAB_MID = 40.0; // (30 - 50)

        // MAX DEVIATION (Max value - Mid point)
        final double PH_DEV = 0.5; // max deviation considered critical (e.g., 7.0 or 8.0)
        final double CHL_DEV = 3.0; // max deviation considered critical (e.g., 0.0 or 5.0)
        final double ALK_DEV = 40.0; // max deviation considered critical (e.g., 60 or 140)
        final double STAB_DEV = 30.0; // max deviation considered critical (e.g., 20 or 70)

        // Calculate raw deviation from mid-point for each metric
        double phDevPercent = Math.min(1.0, Math.abs(data.ph - PH_MID) / PH_DEV);
        double chlDevPercent = Math.min(1.0, Math.abs(data.chlorine - CHL_MID) / CHL_DEV);
        double alkDevPercent = Math.min(1.0, Math.abs(data.alkalinity - ALK_MID) / ALK_DEV);
        double stabDevPercent = Math.min(1.0, Math.abs(data.stabilizer - STAB_MID) / STAB_DEV);

        // Calculate Individual Health Scores (100% - Deviation)
        double phHealth = 100 * (1.0 - phDevPercent);
        double chlHealth = 100 * (1.0 - chlDevPercent);
        double alkHealth = 100 * (1.0 - alkDevPercent);
        double stabHealth = 100 * (1.0 - stabDevPercent);

        // Overall Score: Weighted average (pH and Chlorine are most critical)
        double score = (phHealth * 0.35 + chlHealth * 0.35 + alkHealth * 0.20 + stabHealth * 0.10);
        score = Math.max(0.0, Math.min(100.0, score)) / 10.0; // Convert back to 0-10 scale

        // Determine Status
        String status;
        int colorResId;
        int gaugeDrawableResId;

        if (score >= 9.0) {
            status = "EXCELLENT";
            colorResId = R.color.health_excellent;
            gaugeDrawableResId = R.drawable.circular_gauge_placeholder; // Use a distinct 'Excellent' gauge drawable
        } else if (score >= 7.0) {
            status = "GOOD";
            colorResId = R.color.health_good;
            gaugeDrawableResId = R.drawable.circular_gauge_placeholder; // Use a distinct 'Good' gauge drawable
        } else if (score >= 4.0) {
            status = "CAUTION";
            colorResId = R.color.health_caution;
            gaugeDrawableResId = R.drawable.circular_gauge_placeholder; // Use a distinct 'Caution' gauge drawable
        } else if (score >= 2.0) {
            status = "CRITICAL";
            colorResId = R.color.health_critical;
            gaugeDrawableResId = R.drawable.circular_gauge_placeholder; // Use a distinct 'Critical' gauge drawable
        } else {
            status = "HAZARDOUS";
            colorResId = R.color.health_hazardous;
            gaugeDrawableResId = R.drawable.circular_gauge_placeholder; // Use a distinct 'Hazardous' gauge drawable
        }

        return new OverallHealthResult(score, status, colorResId, gaugeDrawableResId);
    }

    private void updateMetricBlock(TextView valueView, ProgressBar progressBar, double currentValue, double minOptimal, double maxOptimal, String format) {
        // Calculate health percentage for the progress bar (0-100)
        double midOptimal = (minOptimal + maxOptimal) / 2.0;
        double range = (maxOptimal - minOptimal);

        // Max sensible deviation outside range
        double maxDeviation = range * 1.5;

        double deviation = Math.abs(currentValue - midOptimal);

        // Health is 100 - (deviation mapped to 100 scale)
        int healthPercentage = (int) (100 - Math.min(100.0, (deviation / maxDeviation) * 100.0));

        // Set values
        valueView.setText(String.format(Locale.getDefault(), format, currentValue));
        progressBar.setProgress(healthPercentage);

        // Set color based on percentage
        int color;
        if (healthPercentage >= 90) {
            color = R.color.health_excellent;
        } else if (healthPercentage >= 65) {
            color = R.color.health_good;
        } else if (healthPercentage >= 40) {
            color = R.color.health_caution;
        } else {
            color = R.color.health_critical;
        }
        progressBar.getProgressDrawable().setColorFilter(
                ContextCompat.getColor(requireContext(), color), android.graphics.PorterDuff.Mode.SRC_IN);
    }

    private static class PoolHealthData {
        Date latestTestDate;
        double ph;
        double chlorine;
        double alkalinity;
        double stabilizer;
    }

    private static class OverallHealthResult {
        double score;
        String status;
        int colorResId;
        int gaugeDrawableResId;

        public OverallHealthResult(double score, String status, int colorResId, int gaugeDrawableResId) {
            this.score = score;
            this.status = status;
            this.colorResId = colorResId;
            this.gaugeDrawableResId = gaugeDrawableResId;
        }
    }
}