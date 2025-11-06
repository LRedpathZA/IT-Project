package com.example.splashscreen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.data.models.PoolModel;
import com.example.splashscreen.data.models.UserViewModel;
import com.example.splashscreen.ui.weather.WeatherContainerFragment;
import com.example.splashscreen.utils.ProfilePictureManager;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SP_HomeScreen extends Fragment implements HeaderUpdatable {

    private RecyclerView rvClients;
    private RecyclerView rvServices;
    private RecyclerView rvPublicPools;
    private RecyclerView rvProducts;
    private TextView spGreeting;
    private ImageView ivSPProfileIcon;

    private UserViewModel userViewModel;
    private FirebaseFirestore db;
    private ListenerRegistration publicPoolsListener;
    private GeoPoint spCurrentLocation = null;

    public SP_HomeScreen() {
    }

    // --- Lifecycle Methods ---

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Initialize Firestore here or in onCreate
        db = FirebaseFirestore.getInstance(); // Initializing Firestore
        return inflater.inflate(R.layout.sp_home_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvClients = view.findViewById(R.id.rv_my_clients);
        rvServices = view.findViewById(R.id.rv_upcoming_services);
        rvPublicPools = view.findViewById(R.id.rv_public_pools);
        rvProducts = view.findViewById(R.id.rv_products);
        spGreeting = view.findViewById(R.id.spGreeting);
        ivSPProfileIcon = view.findViewById(R.id.ivSPProfileIcon);
        ivSPProfileIcon.setOnClickListener(v -> navigateToFragment(new SP_Profile()));
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        View weatherBanner = view.findViewById(R.id.weatherCard);
        weatherBanner.setOnClickListener(v -> navigateToWeatherScreen());
        userViewModel.userData.observe(getViewLifecycleOwner(), document -> {
            if (getContext() != null) {
                ProfilePictureManager.loadPicture(getContext(), document,  ivSPProfileIcon);
            }
        });

        // 3. Load Data
        loadClientData();
        loadServiceData();
        setupPublicPoolsListener(); // Uses real-time Firestore query
        loadProductData();

        // 4. Set up Weather Card (Simulated UI Update)
        TextView tvWeatherTemp = view.findViewById(R.id.tv_weather_temp);
        TextView tvWeatherLocation = view.findViewById(R.id.tv_weather_location);
        ImageView ivWeatherIcon = view.findViewById(R.id.iv_weather_icon);

        observeLocationAndUserData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Crucial: Remove listener to prevent memory leaks
        if (publicPoolsListener != null) {
            publicPoolsListener.remove();
        }
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
        updateActivityHeader();
    }

    // --- Data Observation and Navigation ---

    private void observeLocationAndUserData() {
        userViewModel.spLocationGeoPoint.observe(getViewLifecycleOwner(), geoPoint -> {
            this.spCurrentLocation = geoPoint;
            // TODO: In a later step, you'll call a weather API fetch here
        });

        userViewModel.username.observe(getViewLifecycleOwner(), username -> {
            if (username != null && !username.isEmpty()) {
                spGreeting.setText(String.format("Hello, %s", username));
            } else {
                spGreeting.setText("Hello, Service Provider");
            }
        });

        if (userViewModel.userData.getValue() == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            userViewModel.fetchUserData(FirebaseAuth.getInstance().getCurrentUser().getUid());
        }
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

    public void navigateToWeatherScreen() {
        if (spCurrentLocation == null) {
            Toast.makeText(getContext(), "Location not available. Please ensure your business location is set.", Toast.LENGTH_LONG).show();
            return;
        }

        double lat = spCurrentLocation.getLatitude();
        double lon = spCurrentLocation.getLongitude();

        WeatherContainerFragment weatherScreen = WeatherContainerFragment.newInstance(lat, lon);
        navigateToFragment(weatherScreen);
    }

    // --- Pool Data Listener (REVISED) ---

    private void setupPublicPoolsListener() {
        // 1. Initialize RecyclerView and Adapter, implementing the click listener
        List<PoolModel> publicPoolsList = new ArrayList<>();
        PublicPoolCardAdapter adapter = new PublicPoolCardAdapter(publicPoolsList, new PublicPoolCardAdapter.OnPoolClickListener() {
            @Override
            public void onPoolClick(PoolModel pool) {
                // Navigate to the new detail fragment
                navigateToFragment(SP_PoolDetailFragment.newInstance(pool.getPoolId()));
            }
        });

        rvPublicPools.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPublicPools.setAdapter(adapter);

        // 2. Query Firestore for public pools
        Query query = db.collection("pools")
                .whereEqualTo("isPublic", true) // CRITICAL FILTER
                .limit(10); // Limit the list size for performance

        // 3. Set up the real-time listener
        publicPoolsListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e("SP_HomeScreen", "Listen failed for public pools: " + e.getMessage());
                return;
            }

            if (snapshots != null) {
                publicPoolsList.clear();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    PoolModel pool = new PoolModel(doc);
                    publicPoolsList.add(pool);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    // ------------------------------------------
    // DUMMY DATA LOADING METHODS
    // ------------------------------------------

    private void loadClientData() {
        List<ClientItem> clients = new ArrayList<>();
        clients.add(new ClientItem("Rozaan.", R.drawable.ic_profile_placeholder));
        clients.add(new ClientItem("Line", R.drawable.ic_profile_placeholder));
        clients.add(new ClientItem("Client C", R.drawable.ic_profile_placeholder));
        clients.add(new ClientItem("Apt. Cmplx", R.drawable.ic_profile_placeholder));
        clients.add(new ClientItem("Client E", R.drawable.ic_profile_placeholder));
        clients.add(new ClientItem("Patrick", R.drawable.ic_profile_placeholder));

        ClientIconAdapter adapter = new ClientIconAdapter(clients);
        rvClients.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvClients.setAdapter(adapter);
    }

    private void loadServiceData() {
        List<ServiceEventItem> events = new ArrayList<>();
        events.add(new ServiceEventItem("TODAY, Jan 12", "10:00 AM", "John D.'s Pool", "Weekly Maintenance", "CONFIRMED"));
        events.add(new ServiceEventItem("TODAY, Jan 12", "02:30 PM", "Apt. Cmplx", "Chemical Treatment", "PENDING"));
        events.add(new ServiceEventItem("TOMORROW, Jan 13", "09:00 AM", "Pool B", "Filter Backwash", "CONFIRMED"));

        ServiceEventAdapter adapter = new ServiceEventAdapter(events);
        rvServices.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
//        rvServices.setHasFixedSize(true);
        rvServices.setAdapter(adapter);
    }

    private void loadProductData() {
        // Since we don't have item_generic_list_card, we'll assume the ProductAdapter uses TextViews for binding.
        List<ProductItem> products = new ArrayList<>();
        products.add(new ProductItem("Chlorine Granules", "10kg bucket", "R 450.00"));
        products.add(new ProductItem("pH Increaser", "5L bottle", "R 120.00"));
        products.add(new ProductItem("Algaecide", "1L bottle", "R 90.00"));

        ProductAdapter adapter = new ProductAdapter(products);
        rvProducts.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvProducts.setAdapter(adapter);
    }


    // ------------------------------------------
    // DUMMY DATA MODELS (Kept for other RecyclerViews)
    // ------------------------------------------

    private static class ClientItem {
        final String name;
        final int avatarResId;

        public ClientItem(String name, int avatarResId) {
            this.name = name;
            this.avatarResId = avatarResId;
        }
    }

    private static class ServiceEventItem {
        final String date;
        final String time;
        final String clientPoolName;
        final String serviceType;
        final String status;

        public ServiceEventItem(String date, String time, String clientPoolName, String serviceType, String status) {
            this.date = date;
            this.time = time;
            this.clientPoolName = clientPoolName;
            this.serviceType = serviceType;
            this.status = status;
        }
    }

    private static class ProductItem {
        final String name;
        final String description;
        final String price;

        public ProductItem(String name, String description, String price) {
            this.name = name;
            this.description = description;
            this.price = price;
        }
    }

    // ------------------------------------------
    // ADAPTER IMPLEMENTATIONS (Binding Logic Added)
    // ------------------------------------------

    private static class ClientIconAdapter extends RecyclerView.Adapter<ClientIconAdapter.ViewHolder> {
        private final List<ClientItem> items;

        public ClientIconAdapter(List<ClientItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_client_icon, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ClientItem item = items.get(position);
            holder.name.setText(item.name);
            holder.avatar.setImageResource(item.avatarResId);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView avatar;
            TextView name;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.iv_client_avatar);
                name = itemView.findViewById(R.id.tv_client_name);
            }
        }
    }

    private static class ServiceEventAdapter extends RecyclerView.Adapter<ServiceEventAdapter.ViewHolder> {
        private final List<ServiceEventItem> items;

        public ServiceEventAdapter(List<ServiceEventItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sp_service_event, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ServiceEventItem item = items.get(position);
            holder.date.setText(item.date);
            holder.time.setText(item.time);
            holder.poolName.setText(item.clientPoolName);
            holder.serviceType.setText(item.serviceType);
            holder.status.setText(item.status);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView date, time, poolName, serviceType, status;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                date = itemView.findViewById(R.id.tv_service_date);
                time = itemView.findViewById(R.id.tv_service_time);
                poolName = itemView.findViewById(R.id.tv_client_pool_name);
                serviceType = itemView.findViewById(R.id.tv_service_type);
                status = itemView.findViewById(R.id.tv_service_status);
            }
        }
    }

    // 💥 UPDATED ADAPTER: Now a non-static inner class to access navigateToFragment
    public static class PublicPoolCardAdapter extends RecyclerView.Adapter<PublicPoolCardAdapter.ViewHolder> {

        private final List<PoolModel> items;
        private final OnPoolClickListener listener;

        public interface OnPoolClickListener {
            void onPoolClick(PoolModel pool);
        }

        public PublicPoolCardAdapter(List<PoolModel> items, OnPoolClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        public PublicPoolCardAdapter(List<PoolModel> items) { // Added simpler constructor for initial setup
            this.items = items;
            this.listener = null;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_public_pool_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PoolModel item = items.get(position);

            // --- Image ---
            if (item.getPhotoUrl() != null && !item.getPhotoUrl().isEmpty()) {
                // TODO: Load image using Glide/Picasso
                holder.image.setImageResource(R.drawable.ic_wavy_background_placeholder);
            } else {
                holder.image.setImageResource(R.drawable.ic_wavy_background_placeholder);
            }

            // --- Capacity ---
            String capacityText = String.format(Locale.getDefault(), "%dL", item.getWaterCapacityLiters());
            holder.capacity.setText(capacityText);

            // --- Location ---
            String locationText = item.getLocationAddress() != null && !item.getLocationAddress().isEmpty()
                    ? item.getLocationAddress()
                    : "Location Unknown";
            holder.location.setText(locationText);

            // --- Summary ---
            String summaryText = String.format("%s | %s", item.getType(), item.getSanitizerType());
            holder.summary.setText(summaryText);

            // --- Click Listener ---
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPoolClick(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView image;
            TextView capacity, location, summary;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.iv_public_pool_image);
                capacity = itemView.findViewById(R.id.tv_public_pool_capacity);
                location = itemView.findViewById(R.id.tv_public_pool_location);
                summary = itemView.findViewById(R.id.tv_public_pool_details_summary);
            }
        }
    }

    private static class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
        private final List<ProductItem> items;

        public ProductAdapter(List<ProductItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_generic_list_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProductItem item = items.get(position);
            holder.name.setText(item.name);
            holder.description.setText(item.description);
            holder.price.setText(item.price);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, description, price;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tv_item_title);
                description = itemView.findViewById(R.id.tv_item_subtext);
                price = itemView.findViewById(R.id.tv_item_title);
            }
        }
    }
}