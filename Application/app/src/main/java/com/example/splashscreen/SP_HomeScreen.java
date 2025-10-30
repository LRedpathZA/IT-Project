package com.example.splashscreen;

import android.os.Bundle;
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
import com.example.splashscreen.ui.weather.WeatherContainerFragment;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class SP_HomeScreen extends Fragment implements HeaderUpdatable {

    private RecyclerView rvClients;
    private RecyclerView rvServices;
    private RecyclerView rvPublicPools;
    private RecyclerView rvProducts;
    private TextView spGreeting;
    private ImageView ivSPProfileIcon;

    private UserViewModel userViewModel;
    private GeoPoint spCurrentLocation = null;

    public SP_HomeScreen() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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


        // 2. Set Greeting (Simulated User Data)
//        String spName = "Swimpool Centre";
//        spGreeting.setText(String.format("Hello, %s", spName));

        // 3. Load Dummy Data
        loadClientData();
        loadServiceData();
        loadPublicPoolData();
        loadProductData();

        // 4. Set up Weather Card (Simulated UI Update)
        TextView tvWeatherTemp = view.findViewById(R.id.tv_weather_temp);
        TextView tvWeatherLocation = view.findViewById(R.id.tv_weather_location);
        ImageView ivWeatherIcon = view.findViewById(R.id.iv_weather_icon);

//        tvWeatherTemp.setText("24°C / Sunny");
//        tvWeatherLocation.setText("Pretoria, South Africa");
          observeLocationAndUserData();
        // Ensure you have a 'sunny' drawable defined for this to work
        // ivWeatherIcon.setImageResource(R.drawable.sunny);
    }
    private void observeLocationAndUserData() {
        // Observe the GeoPoint, which is populated when fetchUserData runs
        userViewModel.spLocationGeoPoint.observe(getViewLifecycleOwner(), geoPoint -> {
            this.spCurrentLocation = geoPoint;
            // TODO: In a later step, you'll call a weather API fetch here to update the home screen banner.
        });

        // Observe the user name to set the greeting
        userViewModel.username.observe(getViewLifecycleOwner(), username -> {
            if (username != null && !username.isEmpty()) {
                spGreeting.setText(String.format("Hello, %s", username));
            } else {
                spGreeting.setText("Hello, Service Provider");
            }
        });

        // Ensure user data is fetched if it wasn't already (e.g., if the user came straight from login)
        if (userViewModel.userData.getValue() == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            userViewModel.fetchUserData(FirebaseAuth.getInstance().getCurrentUser().getUid());
        }
    }
//TODO: REMOVE ALL THIS DUMMY TESTING DATA FOR NOW !!!! :>
    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            // Assuming MainActivity handles the top header structure
            ((MainActivity) getActivity()).updateHeader("", false, false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
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
        rvServices.setHasFixedSize(true);
        rvServices.setAdapter(adapter);
    }

    private void loadPublicPoolData() {
        List<PublicPoolItem> pools = new ArrayList<>();
        // Note: Using placeholders, ensure you have these drawables
        pools.add(new PublicPoolItem(R.drawable.ic_wavy_background_placeholder, "30000L", "Centurion, ZA", "Fiberglass | Chlorine"));
        pools.add(new PublicPoolItem(R.drawable.ic_wavy_background_placeholder, "80000L", "Lynnwood, ZA", "Concrete | Salt"));
        pools.add(new PublicPoolItem(R.drawable.ic_wavy_background_placeholder, "5000L", "Sunnyside, ZA", "Above Ground"));

        PublicPoolCardAdapter adapter = new PublicPoolCardAdapter(pools);
        rvPublicPools.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPublicPools.setAdapter(adapter);
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
    // DUMMY DATA MODELS
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

    private static class PublicPoolItem {
        final int imageResId;
        final String capacity;
        final String location;
        final String summary;

        public PublicPoolItem(int imageResId, String capacity, String location, String summary) {
            this.imageResId = imageResId;
            this.capacity = capacity;
            this.location = location;
            this.summary = summary;
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
            // ADDED BINDING LOGIC
            holder.name.setText(item.name);
            holder.avatar.setImageResource(item.avatarResId);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            // Mapped to item_client_icon.xml
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
            // ADDED BINDING LOGIC
            holder.date.setText(item.date);
            holder.time.setText(item.time);
            holder.poolName.setText(item.clientPoolName);
            holder.serviceType.setText(item.serviceType);
            holder.status.setText(item.status);
            // In a real app, you would set the status TextView's background color here
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            // Mapped to item_sp_service_event.xml
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

    private static class PublicPoolCardAdapter extends RecyclerView.Adapter<PublicPoolCardAdapter.ViewHolder> {
        private final List<PublicPoolItem> items;

        public PublicPoolCardAdapter(List<PublicPoolItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_public_pool_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PublicPoolItem item = items.get(position);
            // ADDED BINDING LOGIC
            holder.image.setImageResource(item.imageResId);
            holder.capacity.setText(item.capacity);
            holder.location.setText(item.location);
            holder.summary.setText(item.summary);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            // Mapped to item_public_pool_card.xml
            ImageView image;
            TextView capacity, location, summary;
            Button viewDetailsButton; // Though not bound here, it should be defined

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.iv_public_pool_image);
                capacity = itemView.findViewById(R.id.tv_public_pool_capacity);
                location = itemView.findViewById(R.id.tv_public_pool_location);
                summary = itemView.findViewById(R.id.tv_public_pool_details_summary);
            }
        }
    }

    // Since we don't have item_generic_list_card XML, this is an educated guess based on product data.
    private static class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
        private final List<ProductItem> items;

        public ProductAdapter(List<ProductItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Assume the product card is simple and uses standard TextViews with IDs like tv_product_name, etc.
            // Using a placeholder layout ID for now.
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_generic_list_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProductItem item = items.get(position);
            // ADDED BINDING LOGIC
            holder.name.setText(item.name);
            holder.description.setText(item.description);
            holder.price.setText(item.price);
            // Assuming the layout has these IDs
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            // Placeholder IDs based on common product card structure
            TextView name, description, price;
            // ImageView image; // Assuming an image view exists

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                // NOTE: The IDs below are guesses based on standard naming convention
                name = itemView.findViewById(R.id.tv_item_title);
                description = itemView.findViewById(R.id.tv_item_subtext);
                price = itemView.findViewById(R.id.tv_item_title);
            }
        }
    }
}