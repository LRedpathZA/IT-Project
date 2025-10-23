package com.example.splashscreen;

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
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.Date;
import java.util.Locale;

public class PoolHealth extends Fragment implements HeaderUpdatable {

    private TextView tvHealthScore, tvHealthStatus, tvLastTestDate;
    private TextView tvPhValue, tvChlorineValue, tvAlkalinityValue, tvStabilizerValue;
    private ProgressBar pbPhHealth, pbChlorineHealth, pbAlkalinityHealth, pbStabilizerHealth;
    private CardView cardMetricPh, cardMetricChlorine, cardMetricAlkalinity, cardMetricStabilizer;
    private MaterialButton btnGoToCalculators;
    private View gaugeRingPlaceholder;

    private FirebaseFirestore db;
    private PoolViewModel poolViewModel;
    private String userId;

    public PoolHealth() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        // Retrieve User ID once
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Initialize ViewModel
        poolViewModel = new ViewModelProvider(requireActivity()).get(PoolViewModel.class);
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

        // Observe poolId changes from ViewModel
        poolViewModel.poolId.observe(getViewLifecycleOwner(), pId -> {
            if (pId != null && !pId.isEmpty()) {
                loadLatestPoolHealthData(pId);
            } else {
                tvHealthStatus.setText("Pool Not Selected");
                clearHealthMetrics();
            }
        });

        // Trigger loading if poolId is already set in ViewModel
        String initialPoolId = poolViewModel.poolId.getValue();
        if (initialPoolId != null && !initialPoolId.isEmpty()) {
            loadLatestPoolHealthData(initialPoolId);
        }
    }

    private void initViews(View view) {
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
            // EXAMPLE: Replace this with the desired navigation logic to your calculator Fragment
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new pHCalculator()) // Assuming pHCalculator is your entry point for now
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            String title =  "Pool Health";
            ((MainActivity) getActivity()).updateHeader(title, true, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }

    private void loadLatestPoolHealthData(String poolId) {
        if (userId == null || userId.isEmpty()) {
            tvHealthStatus.setText("User Not Logged In");
            Toast.makeText(getContext(), "Authentication error. Please log in.", Toast.LENGTH_LONG).show();
            clearHealthMetrics();
            return;
        }

        // 💥 CRITICAL FIX: Use the correct collection path: pools/{poolId}/testLogs
        db.collection("pools").document(poolId)
                .collection("testLogs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        TestLogModel latestLog = queryDocumentSnapshots.getDocuments().get(0).toObject(TestLogModel.class);

                        // Check if the log is valid (has at least one metric recorded)
                        if (latestLog != null && (latestLog.getPh() != 0.0 || latestLog.getChlorine() != 0.0 || latestLog.getAlkalinity() != 0.0 || latestLog.getStabilizer() != 0.0)) {
                            updateUI(latestLog);
                        } else {

                            tvHealthStatus.setText("Invalid Test Data");
                            clearHealthMetrics();
                        }
                    } else {
                        // Data NOT found (Graceful Handling)
                        tvHealthStatus.setText("No Test Data Found");
                        tvHealthScore.setText("0.0");
                        clearHealthMetrics();
                    }
                })
                .addOnFailureListener(e -> {
                    // Firebase access error
                    Toast.makeText(getContext(), "Error loading pool health: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    tvHealthStatus.setText("Loading Error");
                    clearHealthMetrics();
                });
    }

    // Reset all metric views when no data is found
    private void clearHealthMetrics() {
        // Main Display
        tvHealthScore.setText("0.0");
        tvLastTestDate.setText("Last Test: N/A");
        // Note: Check if R.drawable.circular_gauge_placeholder exists and is a suitable default
        gaugeRingPlaceholder.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.circular_gauge_placeholder));

        // Metric Blocks
        updateMetricBlock(tvPhValue, pbPhHealth, 0.0, 7.4, 7.6, "N/A");
        updateMetricBlock(tvChlorineValue, pbChlorineHealth, 0.0, 1.0, 3.0, "N/A");
        updateMetricBlock(tvAlkalinityValue, pbAlkalinityHealth, 0.0, 80.0, 120.0, "N/A");
        updateMetricBlock(tvStabilizerValue, pbStabilizerHealth, 0.0, 30.0, 50.0, "N/A");

        pbPhHealth.setProgress(0);
        pbChlorineHealth.setProgress(0);
        pbAlkalinityHealth.setProgress(0);
        pbStabilizerHealth.setProgress(0);
    }


    private void updateUI(TestLogModel data) { // 💥 FIX 3: Update parameter type to TestLogModel
        // 1. Calculate Health Score
        OverallHealthResult result = calculateOverallHealth(data);

        // 2. Update Main Gauge
        tvHealthScore.setText(String.format(Locale.getDefault(), "%.1f", result.score));
        tvHealthStatus.setText(result.status);
        tvHealthStatus.setTextColor(ContextCompat.getColor(requireContext(), result.colorResId));
        gaugeRingPlaceholder.setBackground(ContextCompat.getDrawable(requireContext(), result.gaugeDrawableResId));

        // 💥 FIX 4: Access timestamp via getter
        if (data.getTimestamp() != null) {
            tvLastTestDate.setText(String.format("Last Test: %s", android.text.format.DateFormat.getMediumDateFormat(getContext()).format(data.getTimestamp())));
        } else {
            tvLastTestDate.setText("Last Test: Unknown Date");
        }

        // 3. Update Metric Blocks
        // 💥 FIX 5: Access metric values via getters
        updateMetricBlock(tvPhValue, pbPhHealth, data.getPh(), 7.4, 7.6, "%.1f");
        updateMetricBlock(tvChlorineValue, pbChlorineHealth, data.getChlorine(), 1.0, 3.0, "%.1f ppm");
        updateMetricBlock(tvAlkalinityValue, pbAlkalinityHealth, data.getAlkalinity(), 80.0, 120.0, "%.1f ppm");
        updateMetricBlock(tvStabilizerValue, pbStabilizerHealth, data.getStabilizer(), 30.0, 50.0, "%.1f ppm");
    }

    // -------------------------------------------------------------------------
    // ALGORITHMS AND HELPER METHODS
    // -------------------------------------------------------------------------

    private OverallHealthResult calculateOverallHealth(TestLogModel data) { // 💥 FIX 6: Update parameter type
        // IDEAL RANGES (Mid-point, Min, Max)
        final double PH_MID = 7.5;
        final double CHL_MID = 2.0;
        final double ALK_MID = 100.0;
        final double STAB_MID = 40.0;

        // MAX DEVIATION
        final double PH_DEV = 0.5;
        final double CHL_DEV = 3.0;
        final double ALK_DEV = 40.0;
        final double STAB_DEV = 30.0;

        // 💥 FIX 7: Access metric values via getters
        // Calculate raw deviation from mid-point for each metric
        double phDevPercent = Math.min(1.0, Math.abs(data.getPh() - PH_MID) / PH_DEV);
        double chlDevPercent = Math.min(1.0, Math.abs(data.getChlorine() - CHL_MID) / CHL_DEV);
        double alkDevPercent = Math.min(1.0, Math.abs(data.getAlkalinity() - ALK_MID) / ALK_DEV);
        double stabDevPercent = Math.min(1.0, Math.abs(data.getStabilizer() - STAB_MID) / STAB_DEV);

        // Calculate Individual Health Scores (100% - Deviation)
        double phHealth = 100 * (1.0 - phDevPercent);
        double chlHealth = 100 * (1.0 - chlDevPercent);
        double alkHealth = 100 * (1.0 - alkDevPercent);
        double stabHealth = 100 * (1.0 - stabDevPercent);

        // Overall Score: Weighted average
        double score = (phHealth * 0.35 + chlHealth * 0.35 + alkHealth * 0.20 + stabHealth * 0.10);
        score = Math.max(0.0, Math.min(100.0, score)) / 10.0;

        // Determine Status
        String status;
        int colorResId;
        int gaugeDrawableResId;

        if (score >= 9.0) {
            status = "EXCELLENT";
            colorResId = R.color.health_excellent;
            gaugeDrawableResId = R.drawable.circular_gauge_placeholder;
        } else if (score >= 7.0) {
            status = "GOOD";
            colorResId = R.color.health_good;
            gaugeDrawableResId = R.drawable.circular_gauge_placeholder;
        } else if (score >= 4.0) {
            status = "CAUTION";
            colorResId = R.color.health_caution;
            gaugeDrawableResId = R.drawable.circular_gauge_placeholder;
        } else if (score >= 2.0) {
            status = "CRITICAL";
            colorResId = R.color.health_critical;
            gaugeDrawableResId = R.drawable.circular_gauge_placeholder;
        } else {
            status = "HAZARDOUS";
            colorResId = R.color.health_hazardous;
            gaugeDrawableResId = R.drawable.circular_gauge_placeholder;
        }

        return new OverallHealthResult(score, status, colorResId, gaugeDrawableResId);
    }

    private void updateMetricBlock(TextView valueView, ProgressBar progressBar, double currentValue, double minOptimal, double maxOptimal, String format) {
        // Handle "N/A" display when no data is found
        if (format.equals("N/A")) {
            valueView.setText("N/A");
            return;
        }
        if (currentValue == 0.0 && minOptimal > 0.0) {
            valueView.setText(String.format(Locale.getDefault(), format, currentValue));
            progressBar.setProgress(0);

            // Set color to Critical/Hazardous for 0.0 of these metrics
            int criticalColor = R.color.health_critical;
            progressBar.getProgressDrawable().setColorFilter(
                    ContextCompat.getColor(requireContext(), criticalColor), android.graphics.PorterDuff.Mode.SRC_IN);
            return;
        }
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