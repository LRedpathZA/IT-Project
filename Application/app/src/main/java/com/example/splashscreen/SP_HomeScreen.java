package com.example.splashscreen; // Ensure this package name matches your project structure

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

// Note: You may have other imports like for RecyclerView, Firebase, etc.,
// but these are the minimum needed for the navigation change.

public class SP_HomeScreen extends Fragment {

    // Declare the new CardView element
    private CardView productManagementCard;

    public SP_HomeScreen() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialization logic (e.g., Firebase, ViewModels) goes here
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.sp_home_screen, container, false);

        // --- Existing UI component initializations would go here ---
        // Example: TextView spGreeting = view.findViewById(R.id.spGreeting);
        // ...

        // --- New Product Management Card Initialization and Click Listener ---
        productManagementCard = view.findViewById(R.id.productManagementCard);

        productManagementCard.setOnClickListener(v -> {
            if (getActivity() != null) {
                // Navigate to the SP_ProductListFragment
                // R.id.fragment_container must be the ID of the container (e.g., FrameLayout)
                // in your main activity where fragments are displayed.
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SP_ProductListFragment())
                        .addToBackStack(null) // Allows the user to press back to return here
                        .commit();
            }
        });

        // --- Other existing logic (e.g., loading clients, services) ---

        return view;
    }

    // You might have other lifecycle methods or helper functions here (e.g., loadWeather, loadClients)
}