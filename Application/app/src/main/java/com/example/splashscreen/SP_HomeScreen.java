package com.example.splashscreen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.data.models.ClientModel;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FieldPath; // For documentId() queries

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SP_HomeScreen extends Fragment implements HeaderUpdatable {

    private static final String TAG = "SP_HomeScreen";

    // Views
    private RecyclerView rvClients;
    private RecyclerView rvServices;
    private RecyclerView rvPublicPools;
    private RecyclerView rvProducts;
    private TextView spGreeting;
    private ImageView ivSPProfileIcon;
    private CardView productManagementCard;

    // Firebase & Data
    private UserViewModel userViewModel;
    private FirebaseFirestore db;
    private GeoPoint spCurrentLocation = null;
    private String currentUserId = null; // Store SP's UID

    // ⭐ REAL-TIME LISTENERS
    private ListenerRegistration publicPoolsListener;
    private ListenerRegistration clientListener;
    private ListenerRegistration serviceListener;

    // ⭐ REAL-TIME ADAPTERS
    private ClientIconAdapter clientAdapter;
    private ServiceEventAdapter serviceAdapter;

    public SP_HomeScreen() {
    }

    // --- Lifecycle Methods ---

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        return inflater.inflate(R.layout.sp_home_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Check User ID & Initialize Views/ViewModels
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            Toast.makeText(getContext(), "User not logged in. Cannot load SP data.", Toast.LENGTH_LONG).show();
            return;
        }

        rvClients = view.findViewById(R.id.rv_my_clients);
        rvServices = view.findViewById(R.id.rv_upcoming_services);
        rvPublicPools = view.findViewById(R.id.rv_public_pools);
        rvProducts = view.findViewById(R.id.rv_products);
        spGreeting = view.findViewById(R.id.spGreeting);
        ivSPProfileIcon = view.findViewById(R.id.ivSPProfileIcon);
        productManagementCard = view.findViewById(R.id.productManagementCard);
        View weatherBanner = view.findViewById(R.id.weatherCard);

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        // 2. Setup Adapters for Real Data
        // CLIENTS
        clientAdapter = new ClientIconAdapter(new ArrayList<>());
        rvClients.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvClients.setAdapter(clientAdapter);

        // SERVICES
        serviceAdapter = new ServiceEventAdapter(new ArrayList<>());
        rvServices.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        rvServices.setAdapter(serviceAdapter);


        // 3. Set Listeners (UI Navigation)
        ivSPProfileIcon.setOnClickListener(v -> navigateToFragment(new SP_Profile()));
        weatherBanner.setOnClickListener(v -> navigateToWeatherScreen());
        productManagementCard.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SP_ProductList())
                        .addToBackStack(null)
                        .commit();
            }
        });

        // 4. Load Data using Real-Time Listeners
        setupClientsListener(currentUserId);
        setupServiceListener(currentUserId);
        setupPublicPoolsListener();

        // 5. Observe LiveData
        observeLocationAndUserData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Crucial: Remove all listeners to prevent memory leaks
        if (publicPoolsListener != null) { publicPoolsListener.remove(); }
        if (clientListener != null) { clientListener.remove(); }
        if (serviceListener != null) { serviceListener.remove(); }
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

        userViewModel.userData.observe(getViewLifecycleOwner(), document -> {
            if (getContext() != null) {
                ProfilePictureManager.loadPicture(getContext(), document,  ivSPProfileIcon);
            }
        });
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

    // --- Clients Listener (FIXED: Only shows clients with "Scheduled" bookings) ---

    private void setupClientsListener(String spId) {
        if (clientListener != null) clientListener.remove();

        // Query the 'bookings' collection for documents where:
        // 1. businessId matches the current SP's ID
        // 2. The status is "Scheduled" (accepted quote/booking)
        Query clientQuery = db.collection("bookings")
                .whereEqualTo("businessId", spId)
                .whereEqualTo("status", "Scheduled"); // <<-- FIX APPLIED HERE

        clientListener = clientQuery.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed for SP clients.", e);
                return;
            }

            if (snapshots != null) {
                Set<String> uniquePoIds = new HashSet<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    // Extract the Pool Owner's ID (the client)
                    String poId = doc.getString("userId");
                    if (poId != null) {
                        uniquePoIds.add(poId);
                    }
                }

                // Fetch the full User data for each unique PO ID
                fetchClientDetails(new ArrayList<>(uniquePoIds));
            }
        });
    }

    private void fetchClientDetails(List<String> poIds) {
        List<ClientModel> clients = new ArrayList<>();
        if (poIds.isEmpty()) {
            clientAdapter.updateList(clients);
            return;
        }

        // Using whereIn is limited to 10 items in Firestore.
        if (poIds.size() > 10) {
            poIds = poIds.subList(0, 10);
        }

        // Ensure poIds is not empty after subList if the original list was small
        if (poIds.isEmpty()) {
            clientAdapter.updateList(clients);
            return;
        }

        db.collection("users").whereIn(FieldPath.documentId(), poIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        // ⭐ 1. Get the Client ID from the DocumentSnapshot
                        String clientId = doc.getId();

                        String name = doc.getString("name");
                        String photoUrl = doc.getString("profilePictureUrl");
                        Long avatarResId = doc.getLong("profileAvatarResId");

                        // Default/Placeholder values for fields not in the 'users' document
                        String description = ""; // Set description placeholder
                        boolean isActive = true; // Assume active based on booking query
                        GeoPoint poolLocation = null; // Pool location is not in the user document

                        clients.add(new ClientModel(
                                clientId, // Pass the ID
                                name != null ? name : "Client", // Pass the name
                                description, // Pass the description placeholder
                                photoUrl, // Pass the photo URL
                                avatarResId, // Pass the resource ID
                                isActive, // Pass the active status
                                poolLocation // Pass null for pool location
                        ));
                    }
                    clientAdapter.updateList(clients);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching client details: " + e.getMessage()));
    }


    // --- Upcoming Services Listener (Uses real data) ---

    private void setupServiceListener(String spId) {
        if (serviceListener != null) serviceListener.remove();

        // Query the 'bookings' collection for services linked to the current SP, ordered by date
        Query serviceQuery = db.collection("bookings")
                .whereEqualTo("businessId", spId)
                .whereEqualTo("status", "Scheduled")
                .orderBy("serviceDate", Query.Direction.ASCENDING); 

        serviceListener = serviceQuery.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed for SP services.", e);
                return;
            }

            if (snapshots != null) {
                List<ServiceBookingModel> services = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    services.add(new ServiceBookingModel(doc));
                }
                serviceAdapter.updateList(services);
            }
        });
    }

    // --- Public Pool Data Listener (Retained: Check DB type for fix) ---

    private void setupPublicPoolsListener() {
        // 1. Initialize RecyclerView and Adapter, implementing the click listener
        List<PoolModel> publicPoolsList = new ArrayList<>();
        PublicPoolCardAdapter adapter = new PublicPoolCardAdapter(publicPoolsList, new PublicPoolCardAdapter.OnPoolClickListener() {
            @Override
            public void onPoolClick(PoolModel pool) {
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
                Log.e(TAG, "Listen failed for public pools: " + e.getMessage());
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
    // REAL DATA MODELS
    // ------------------------------------------

    private static class ServiceBookingModel {
        final String title;
        final Date serviceDate;
        final double price;
        final String status;

        public ServiceBookingModel(DocumentSnapshot doc) {
            this.title = doc.getString("title");
            this.serviceDate = doc.getDate("serviceDate");
            // FIXED: Use doc.getDouble() to safely retrieve the double value.
            Double priceDouble = doc.getDouble("price");
            this.price = priceDouble != null ? priceDouble : 0.0;
            this.status = doc.getString("status");
        }
    }


    // ------------------------------------------
    // ADAPTER IMPLEMENTATIONS
    // ------------------------------------------

    /** Adapter for the horizontal client icon list. */
    private class ClientIconAdapter extends RecyclerView.Adapter<ClientIconAdapter.ViewHolder> {
        private List<ClientModel> items;

        public ClientIconAdapter(List<ClientModel> items) {
            this.items = items;
        }

        public void updateList(List<ClientModel> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_client_icon, parent, false);
            return new ViewHolder(view);
        }

        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ClientModel item = items.get(position);

            // Use the getter method for the client's name
            holder.name.setText(item.getName());

            // ⭐ FIX: Use the correct getter method getAvatarResId()
            Long avatarResId = item.getAvatarResId();
            String photoUrl = item.getPhotoUrl();

            if (avatarResId != null && avatarResId > 0) {
                // Load built-in avatar using Resource ID
                holder.avatar.setImageResource(avatarResId.intValue());
                holder.avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
            else if (photoUrl != null && !photoUrl.isEmpty()) {
                // Use the existing ProfilePictureManager for asynchronous URL loading
                ProfilePictureManager.loadPicture(getContext(), photoUrl, holder.avatar, R.drawable.ic_profile_placeholder);
            }
            else {
                ProfilePictureManager.setPlaceholder(holder.avatar);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView avatar;
            TextView name;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.iv_client_avatar);
                name = itemView.findViewById(R.id.tv_client_name);
            }
        }
    }

    /** Adapter for the vertical service event list. */
    private static class ServiceEventAdapter extends RecyclerView.Adapter<ServiceEventAdapter.ViewHolder> {
        private List<ServiceBookingModel> items;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "ZA"));


        public ServiceEventAdapter(List<ServiceBookingModel> items) {
            this.items = items;
        }

        public void updateList(List<ServiceBookingModel> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sp_service_event, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ServiceBookingModel item = items.get(position);

            if (item.serviceDate != null) {
                holder.date.setText(dateFormat.format(item.serviceDate));
                holder.time.setText(timeFormat.format(item.serviceDate));
            } else {
                holder.date.setText("Date TBD");
                holder.time.setText("Time TBD");
            }

            holder.poolName.setText(item.title);
            // This is displaying the price where the service type should be.
            // If the title is the service type, remove this line or change the layout.
            holder.serviceType.setText(currencyFormat.format(item.price));
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

    // --- Public Pool Adapter (Retained) ---

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

        public PublicPoolCardAdapter(List<PoolModel> items) {
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
                // TODO: Load image using Glide/Picasso or ProfilePictureManager if extended
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

    // --- Product Adapter (Retained for dummy data) ---
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
    private static class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
        private final List<ProductItem> items;

        public ProductAdapter(List<ProductItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Assuming R.layout.item_generic_list_card exists
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
                // Assuming IDs are correct in item_generic_list_card
                name = itemView.findViewById(R.id.tv_item_title);
                description = itemView.findViewById(R.id.tv_item_subtext);
                price = itemView.findViewById(R.id.tv_item_title); // WARNING: This ID is used twice (name & price)
            }
        }
    }
}