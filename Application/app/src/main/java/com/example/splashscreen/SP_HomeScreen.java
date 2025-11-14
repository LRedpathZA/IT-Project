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

// ⭐ NEW IMPORTS FOR MANUAL IMAGE LOADING ⭐
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
// ⭐ END NEW IMPORTS ⭐

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
    private String currentUserId = null;

    private final Executor networkExecutor = Executors.newSingleThreadExecutor();

    private ListenerRegistration publicPoolsListener;
    private ListenerRegistration clientListener;
    private ListenerRegistration serviceListener;

    private ClientIconAdapter clientAdapter;
    private ServiceEventAdapter serviceAdapter;

    public interface OnPoolClickListener {
        void onPoolClick(PoolModel pool);
    }

    public SP_HomeScreen() {
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        return inflater.inflate(R.layout.sp_home_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
        clientAdapter = new ClientIconAdapter(new ArrayList<>());
        rvClients.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvClients.setAdapter(clientAdapter);

        serviceAdapter = new ServiceEventAdapter(new ArrayList<>());
        rvServices.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        rvServices.setAdapter(serviceAdapter);


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

        setupClientsListener(currentUserId);
        setupServiceListener(currentUserId);
        setupPublicPoolsListener();

        observeLocationAndUserData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Crucial: We Remove all listeners to prevent memory leaks
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
            // TODO: LATER ON, we'll call the weather API fetch here
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

    private void loadPoolImageFromUrl(String url, ImageView targetImageView) {
        if (url == null || url.isEmpty()) {
            if (targetImageView != null) {

                targetImageView.setImageResource(R.drawable.ic_wavy_background_placeholder);
            }
            return;
        }

        networkExecutor.execute(() -> {
            Bitmap bitmap = null;
            try {
                InputStream in = new URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(in);
                Log.d(TAG, "Successfully decoded pool bitmap from URL.");
            } catch (Exception e) {
                Log.e(TAG, "Error loading pool bitmap from URL: " + e.getMessage());
            }

            Bitmap finalBitmap = bitmap;


            if (isAdded()) {

                requireActivity().runOnUiThread(() -> {
                    if (targetImageView != null) {
                        if (finalBitmap != null) {
                            targetImageView.setImageBitmap(finalBitmap);
                        } else {
                            targetImageView.setImageResource(R.drawable.ic_wavy_background_placeholder);
                            Toast.makeText(getContext(), "Failed to load pool image.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }


    private void setupClientsListener(String spId) {
        if (clientListener != null) clientListener.remove();
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
                    String poId = doc.getString("userId");
                    if (poId != null) {
                        uniquePoIds.add(poId);
                    }
                }


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
        if (poIds.isEmpty()) {
            clientAdapter.updateList(clients);
            return;
        }

        db.collection("users").whereIn(FieldPath.documentId(), poIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String clientId = doc.getId();

                        String name = doc.getString("name");
                        String photoUrl = doc.getString("profilePictureUrl");
                        Long avatarResId = doc.getLong("profileAvatarResId");


                        String description = "";
                        boolean isActive = true;
                        GeoPoint poolLocation = null;

                        clients.add(new ClientModel(
                                clientId,
                                name != null ? name : "Client",
                                description,
                                photoUrl,
                                avatarResId,
                                isActive,
                                poolLocation
                        ));
                    }
                    clientAdapter.updateList(clients);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching client details: " + e.getMessage()));
    }



    private void setupServiceListener(String spId) {
        if (serviceListener != null) serviceListener.remove();

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

    private void setupPublicPoolsListener() {
        List<PoolModel> publicPoolsList = new ArrayList<>();
        PublicPoolCardAdapter adapter = new PublicPoolCardAdapter(publicPoolsList, this::loadPoolImageFromUrl, this, new OnPoolClickListener() {
            @Override
            public void onPoolClick(PoolModel pool) {
                navigateToFragment(SP_PoolDetailFragment.newInstance(pool.getPoolId()));
            }
        });

        rvPublicPools.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPublicPools.setAdapter(adapter);

        Query query = db.collection("pools")
                .whereEqualTo("isPublic", true)
                .limit(10);

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
            holder.name.setText(item.getName());

            Long avatarResId = item.getAvatarResId();
            String photoUrl = item.getPhotoUrl();

            if (avatarResId != null && avatarResId > 0) {
                holder.avatar.setImageResource(avatarResId.intValue());
                holder.avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
            else if (photoUrl != null && !photoUrl.isEmpty()) {
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

    public static class PublicPoolCardAdapter extends RecyclerView.Adapter<PublicPoolCardAdapter.ViewHolder> {

        private final List<PoolModel> items;
        private final OnPoolClickListener listener;
        private final SP_HomeScreen fragment;

        private final ImageLoader imageLoader;

        private interface ImageLoader {
            void load(String url, ImageView targetImageView);
        }

        public PublicPoolCardAdapter(List<PoolModel> items, ImageLoader imageLoader, SP_HomeScreen fragment, OnPoolClickListener listener) {
            this.items = items;
            this.imageLoader = imageLoader;
            this.fragment = fragment;
            this.listener = listener;
        }

        public PublicPoolCardAdapter(List<PoolModel> items, ImageLoader imageLoader, SP_HomeScreen fragment) {
            this.items = items;
            this.imageLoader = imageLoader;
            this.fragment = fragment;
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
            imageLoader.load(item.getPhotoUrl(), holder.image);


            String capacityText = String.format(Locale.getDefault(), "%dL", item.getWaterCapacityLiters());
            holder.capacity.setText(capacityText);

            String locationText = item.getLocationAddress() != null && !item.getLocationAddress().isEmpty()
                    ? item.getLocationAddress()
                    : "Location Unknown";
            holder.location.setText(locationText);

            String summaryText = String.format("%s | %s", item.getType(), item.getSanitizerType());
            holder.summary.setText(summaryText);

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
                price = itemView.findViewById(R.id.tv_item_title); // WARNING: This ID is used twice (name & price)
            }
        }
    }
}