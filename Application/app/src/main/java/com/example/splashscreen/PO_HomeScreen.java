package com.example.splashscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.data.models.ItemModel;
import com.example.splashscreen.data.models.PoolModel;
import com.example.splashscreen.data.models.PoolViewModel;
import com.example.splashscreen.data.models.UserViewModel;
import com.example.splashscreen.ui.weather.WeatherContainerFragment;
import com.example.splashscreen.utils.ProfilePictureManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class PO_HomeScreen extends Fragment implements HeaderUpdatable {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private UserViewModel userViewModel;
    private PoolViewModel poolViewModel;

    private TextView tvGreetingName;
    private ImageView ivProfileIcon;
    private LinearLayout llAddPoolPlaceholder;
    private FrameLayout flHomePoolContent;

    private RecyclerView rvProducts;
    private ItemAdapter productAdapter;

    public static final String REQUEST_KEY_POOL_ADDED = "pool_added_key";
    public static final String BUNDLE_KEY_POOL_ID = "new_pool_id";
    public static final String ARG_POOL_ID = "POOL_ID";

    private TextView tvPoolName;
    private TextView tvPoolType;
    private TextView tvPoolCapacity;
    private TextView tvPoolLocation, tvWeatherLocation;
    private ImageView ivPoolImage;
    private View poolCardView;

    private String homePoolId;

    public PO_HomeScreen() {
    }

    public static PO_HomeScreen newInstance() {
        return new PO_HomeScreen();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.po_home_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CardView poolHealthCard = view.findViewById(R.id.poolHealthCard);
        poolHealthCard.setOnClickListener(v -> navigateToPoolHealth());
        CardView weatherBanner = view.findViewById(R.id.weatherCard);
        weatherBanner.setOnClickListener(v -> navigateToWeatherScreen());
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        poolViewModel = new ViewModelProvider(requireActivity()).get(PoolViewModel.class);

        tvGreetingName = view.findViewById(R.id.poGreeting);
        ivProfileIcon = view.findViewById(R.id.ivProfileIcon);
        llAddPoolPlaceholder = view.findViewById(R.id.ll_add_pool_placeholder);
        flHomePoolContent = view.findViewById(R.id.fl_home_pool_content);
        rvProducts = view.findViewById(R.id.rv_products);
        tvWeatherLocation = view.findViewById(R.id.tv_weather_location);

        View calendarCardContainer = view.findViewById(R.id.calendarCard);
        if (calendarCardContainer != null) {
            calendarCardContainer.setOnClickListener(v -> navigateToPO_Calendar());
        }
        userViewModel.userData.observe(getViewLifecycleOwner(), document -> {
            // This runs instantly whenever the data changes anywhere in the app
            if (getContext() != null) {
                // 4. Use the reusable Manager to load the latest picture
                ProfilePictureManager.loadPicture(getContext(), document, ivProfileIcon);
            }
        });

        setupPoolResultListener();
        setupProductRecyclerView();
        initNavigation();

        observeUserData();
        observePoolData();
    }


    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateHeader("", false, false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // 💥 Call the method to hide the main activity's header
        updateActivityHeader();

        // This ensures the user data is fresh, which in turn triggers pool data load via handleHomePoolDocument
        if (auth.getCurrentUser() != null) {
            userViewModel.fetchUserData(auth.getCurrentUser().getUid());
        }
    }

    // =========================================================================================
    //                                 VIEW MODEL OBSERVATION
    // =========================================================================================

    private void observeUserData() {
        userViewModel.username.observe(getViewLifecycleOwner(), username -> {
            if (username != null && !username.isEmpty()) {
                tvGreetingName.setText("Hello, " + username);
            } else {
                tvGreetingName.setText("Hello, Guest User");
            }
        });

        userViewModel.userData.observe(getViewLifecycleOwner(), userDocument -> {
            if (userDocument != null && userDocument.exists()) {
                handleHomePoolDocument(userDocument);
            }
        });
    }

    private void observePoolData() {
        poolViewModel.currentPoolModel.observe(getViewLifecycleOwner(), poolModel -> {
            if (poolModel != null) {
                displayPoolCard(poolModel);
            } else {
                if (flHomePoolContent != null) {
                    flHomePoolContent.removeAllViews();
                    flHomePoolContent.addView(llAddPoolPlaceholder);
                    llAddPoolPlaceholder.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void handleHomePoolDocument(DocumentSnapshot userDocument) {
        String fetchedPoolId = userDocument.getString("homePoolId");

        if (fetchedPoolId == null || fetchedPoolId.isEmpty()) {
            this.homePoolId = null;
            MainActivity.homePoolId = null;
            poolViewModel.clearPoolData();

            if (flHomePoolContent != null) {
                flHomePoolContent.removeAllViews();
                flHomePoolContent.addView(llAddPoolPlaceholder);
                llAddPoolPlaceholder.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (!fetchedPoolId.equals(this.homePoolId)) {
            this.homePoolId = fetchedPoolId;
            MainActivity.homePoolId = this.homePoolId;
            poolViewModel.fetchPoolData(fetchedPoolId);
        }
    }

    private void initNavigation() {
        // These listeners remain here because the icons (ivProfileIcon) are part of the PO_HomeScreen XML.
        ivProfileIcon.setOnClickListener(v -> navigateToFragment(new PO_Profile()));
        llAddPoolPlaceholder.setOnClickListener(v -> navigateToFragment(new PO_AddPool()));
    }

    private void navigateToFragment(Fragment fragment) {
        if (getActivity() != null) {
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        } else if (getContext() != null) {
            Toast.makeText(getContext(), "Navigation failed.", Toast.LENGTH_SHORT).show();
        }
    }

    public void navigateToPO_Calendar() {
        if (homePoolId == null) {
            Toast.makeText(getContext(), "Pool details not loaded yet. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }

        PO_Calendar calendarFragment = new PO_Calendar();
        Bundle args = new Bundle();
        args.putString(ARG_POOL_ID, homePoolId);
        calendarFragment.setArguments(args);

        navigateToFragment(calendarFragment);
    }
    public void navigateToPoolHealth() {
        if (homePoolId == null) {
            Toast.makeText(getContext(), "Pool details not loaded yet. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }
        PoolHealth poolHealthFragment = new PoolHealth();
        navigateToFragment(poolHealthFragment);
    }
    public void navigateToWeatherScreen() {
        PoolModel poolModel = poolViewModel.currentPoolModel.getValue();

        if (poolModel == null) {
            Toast.makeText(getContext(), "Pool location not loaded. Cannot fetch weather.", Toast.LENGTH_SHORT).show();
            return;
        }

        GeoPoint location = poolModel.getLocation();

        if (location == null) {
            Toast.makeText(getContext(), "Pool location is missing coordinates.", Toast.LENGTH_SHORT).show();
            return;
        }

        double lat = location.getLatitude();
        double lon = location.getLongitude();
        WeatherContainerFragment weatherScreen = WeatherContainerFragment.newInstance(lat, lon);
        navigateToFragment(weatherScreen);
    }

    private void setupProductRecyclerView() {
        List<ItemModel> initialList = new ArrayList<>();
        initialList.add(new ItemModel("Chlorine Tabs", "Top Seller", R.drawable.fake_chlorine));
        initialList.add(new ItemModel("pH Up", "Essential", R.drawable.fake_chlorine));
        initialList.add(new ItemModel("Algaecide", "Best Price", R.drawable.fake_chlorine));

        productAdapter = new ItemAdapter(initialList);
        rvProducts.setAdapter(productAdapter);
    }

    private void setupPoolResultListener() {
        getParentFragmentManager().setFragmentResultListener(REQUEST_KEY_POOL_ADDED, this, (requestKey, bundle) -> {
            if (requestKey.equals(REQUEST_KEY_POOL_ADDED)) {
                String resultPoolId = bundle.getString(BUNDLE_KEY_POOL_ID);

                if (auth.getCurrentUser() != null) {
                    userViewModel.fetchUserData(auth.getCurrentUser().getUid());
                }
            }
        });
    }

    private void displayPoolCard(PoolModel poolModel) {
        if (poolModel != null && flHomePoolContent != null && getContext() != null) {

            String poolName = poolModel.getName();
            String poolType = poolModel.getType();
            String sanitizerType = poolModel.getSanitizerType();
            String poolLocation = poolModel.getLocationAddress();
            Long capacity = poolModel.getWaterCapacityLiters();
            String poolId = poolModel.getPoolId();

            boolean poolCardExists = flHomePoolContent.getChildCount() > 0 &&
                    flHomePoolContent.getChildAt(0) == poolCardView;

            if (poolCardView == null || !poolCardExists) {

                flHomePoolContent.removeAllViews();
                poolCardView = LayoutInflater.from(getContext()).inflate(R.layout.item_pool_card, flHomePoolContent, false);
                flHomePoolContent.addView(poolCardView);
                poolCardView.setOnClickListener(v -> navigateToEditPool(poolId));

                tvPoolName = poolCardView.findViewById(R.id.tv_pool_name);
                tvPoolType = poolCardView.findViewById(R.id.tv_pool_type);
                tvPoolCapacity = poolCardView.findViewById(R.id.tv_pool_capacity);
                tvPoolLocation = poolCardView.findViewById(R.id.tv_pool_location);


                ivPoolImage = poolCardView.findViewById(R.id.iv_pool_image);

                if (ivPoolImage != null) {
                    ivPoolImage.setImageResource(R.drawable.fake_pool);
                }
            }

            if (tvPoolName != null) tvPoolName.setText(poolName);
            if (tvPoolType != null) tvPoolType.setText(String.format("%s | %s", poolType, sanitizerType));
            if (tvPoolCapacity != null && capacity != null) tvPoolCapacity.setText(String.format("%dL", capacity));
            if (tvPoolLocation != null) tvPoolLocation.setText(poolLocation);
            tvWeatherLocation.setText(poolLocation);


            llAddPoolPlaceholder.setVisibility(View.GONE);

        } else {
            if (flHomePoolContent != null) {
                flHomePoolContent.removeAllViews();
                flHomePoolContent.addView(llAddPoolPlaceholder);
                llAddPoolPlaceholder.setVisibility(View.VISIBLE);
            }
        }
    }

    private void navigateToEditPool(String poolId) {
        PO_AddPool editFragment = new PO_AddPool();
        Bundle args = new Bundle();
        args.putString(ARG_POOL_ID, poolId);
        editFragment.setArguments(args);
        navigateToFragment(editFragment);
    }
}