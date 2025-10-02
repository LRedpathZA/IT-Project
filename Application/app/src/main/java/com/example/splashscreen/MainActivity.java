package com.example.splashscreen;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;



public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // UI Components
    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAdd;

    private LinearLayout speedDialContainer;
    private boolean isSpeedDialOpen = false;
    private Button[] speedDialButtons = new Button[6];
    private FrameLayout drawerContainer; // Used to dynamically load drawer content
    private FrameLayout fabOverlay;

    private String username;
    private static final int ROLE_POOL_OWNER = 1;
    private static final int ROLE_SERVICE_PROVIDER = 2;
   // private static final int ROLE_ADMIN = 3;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main); // activity_main.xml must contain DrawerLayout
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        drawerLayout = findViewById(R.id.drawer_layout);
        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        fabAdd = findViewById(R.id.fab_add);
        drawerContainer = findViewById(R.id.drawer_container);
        fabOverlay = findViewById(R.id.fab_overlay);
        speedDialContainer = findViewById(R.id.speed_dial_menu_container);
        speedDialButtons[0] = speedDialContainer.findViewById(R.id.btn_action_1);
        speedDialButtons[1] = speedDialContainer.findViewById(R.id.btn_action_2);
        speedDialButtons[2] = speedDialContainer.findViewById(R.id.btn_action_3);
        speedDialButtons[3] = speedDialContainer.findViewById(R.id.btn_action_4);
        speedDialButtons[4] = speedDialContainer.findViewById(R.id.btn_action_5);
        speedDialButtons[5] = speedDialContainer.findViewById(R.id.btn_action_6);

        // Check if a user is logged in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // User is signed in, fetch their data and role
            fetchUserDataAndSetup(currentUser.getUid());
        } else {
            // Handle scenario where user is unexpectedly null (e.g., redirect to AuthActivity)
            // finish();
            // startActivity(new Intent(this, AuthenticationActivity.class));
        }
    }

    private void fetchUserDataAndSetup(String userId) {
        DocumentReference docRef = db.collection("users").document(userId);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                     username = document.getString("name");
                    Long roleLong = document.getLong("role_id");
                    assert roleLong != null;
                    int userRole = roleLong.intValue();

                    Log.d("MainActivity", "User: " + username + ", Role: " + userRole);

                    // 1. Setup UI based on Role
                    setupRoleBasedUI(userRole);

                    // 2. Load initial fragment
                    Fragment initialFragment = new Fragment();
                    if (ROLE_POOL_OWNER == userRole) {
                        // Replace with your actual PO Home Screen Fragment instance
                        initialFragment = new PO_HomeScreen();
                    } else if (ROLE_SERVICE_PROVIDER == userRole ) {
                        // Replace with your actual SP Home Screen Fragment instance
                        initialFragment = new SP_HomeScreen();
                    } // else {
                      // Default or Error Fragment
                     //  initialFragment = new DefaultFragment();
                 //   }
                    replaceFragment(initialFragment);

                } else {
                    Log.w("MainActivity", "User document not found.");
                }
            } else {
                Log.e("MainActivity", "Error fetching user data: ", task.getException());
            }
        });
    }



    /**
     * Sets up the Bottom Navigation and Drawer based on the user's role_id.
     * @param role_id The user's role_id (POOL_OWNER or SERVICE_PROVIDER).
     */
    private void setupRoleBasedUI(int role_id) {
        bottomNavigationView.getMenu().clear();

        if (ROLE_POOL_OWNER == role_id) {
            bottomNavigationView.inflateMenu(R.menu.menu_po_bottom_nav);
            setupDrawer(R.layout.po_navigation_drawer);

            // FAB Action: Add Water Reading/Pool (Primary PO action)
            fabAdd.setOnClickListener(v -> {
                Toast.makeText(this, "Opening Add Pool/Reading screen", Toast.LENGTH_SHORT).show();
                // TODO: Implement navigation to AddPoolFragment or AddWaterReadingFragment
            });

            populatePoMenu(drawerContainer);
        } else if (ROLE_SERVICE_PROVIDER == role_id) {
            bottomNavigationView.inflateMenu(R.menu.menu_sp_bottom_nav);
            setupDrawer(R.layout.sp_navigation_drawer);

            // FAB Action: Log New Job/Add Service (Primary SP action)
            fabAdd.setOnClickListener(v -> {
                Toast.makeText(this, "Opening Add Job/Service screen", Toast.LENGTH_SHORT).show();
                // TODO: Implement navigation to NewJobFragment or AddServiceFragment
            });
        }

        // Set the listener for fragment swapping
        bottomNavigationView.setOnItemSelectedListener(this::onNavigationItemSelected);
    }

    /**
     * Dynamically loads the correct Navigation Drawer layout into the container.
     * @param drawerLayoutResId The resource ID of the drawer layout (PO or SP).
     */
    private void setupDrawer(int drawerLayoutResId) {
        // Clear any previous drawer content
        drawerContainer.removeAllViews();
        // Inflate the new drawer content and add it to the container
        getLayoutInflater().inflate(drawerLayoutResId, drawerContainer, true);

        // Setup listener for the Menu button on the Bottom Nav to open the drawer
        bottomNavigationView.getMenu().findItem(R.id.nav_menu_po).setOnMenuItemClickListener(item -> {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        });

        // TODO: Attach listeners to the buttons inside the inflated drawer layout here
        // e.g., findViewById(R.id.btnSettings).setOnClickListener(...)
    }


    /**
     * Handles the selection logic for the Bottom Navigation View.
     */
    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        //TODO: INIT ALL THESE NAV ITEMS
        // --- PO Navigation Items ---
       /* if (itemId == R.id.nav_home_po) {
            selectedFragment = new PO_HomeScreen();
            setFabVisibility(View.VISIBLE); // Show FAB on Home screen
        } else if (itemId == R.id.nav_marketplace) {
            selectedFragment = new MarketplaceFragment();
            setFabVisibility(View.GONE); // Hide FAB on Marketplace (if only searching is allowed)
        } else if (itemId == R.id.nav_cart) {
            selectedFragment = new CartFragment();
            setFabVisibility(View.GONE);
            // --- SP Navigation Items ---
        } else if (itemId == R.id.nav_dashboard_sp) {
            selectedFragment = new SP_HomeScreen();
            setFabVisibility(View.VISIBLE);
        } else if (itemId == R.id.nav_clients_sp) {
            selectedFragment = new ClientsFragment();
            setFabVisibility(View.GONE);

            // --- Drawer Open Item (Already handled in setupDrawer, but good for completeness) ---
        } else if (itemId == R.id.nav_menu_icon) {
            drawerLayout.openDrawer(GravityCompat.START);
            return false; // Prevent item from being highlighted
        }

        */

        if (selectedFragment != null) {
            replaceFragment(selectedFragment);
            return true;
        }
        return false;
    }
    private void setMenuItemLabel(View parentView, int rootIncludeId, String labelText) {

        View rootMenuButton = parentView.findViewById(rootIncludeId);

        // Check for the included view's existence (essential safety check)
        if (rootMenuButton != null) {
            // 2. Find the TextView inside the included layout
            // Assuming the TextView ID is R.id.menu_label
            TextView label = rootMenuButton.findViewById(R.id.tvMenuItemText);

            // 3. Set the text
            if (label != null) {
                label.setText(labelText);
            }
        }
    }

    private void populatePoMenu(FrameLayout drawerContainer) {
        // Header Logic (Assuming IDs: R.id.tv_drawer_username and R.id.tv_login_status)
        TextView tvDrawerUsername = drawerContainer.findViewById(R.id.nav_username);
        if (tvDrawerUsername != null) {
            tvDrawerUsername.setText(username);
        }

//        TextView tvLoginStatus = drawerContainer.findViewById(R.id.tv_login_status);
//        if (tvLoginStatus != null) {
//            tvLoginStatus.setText("Log Out"); // Change "Login" to "Log Out"
//        }


        // --- Optimized Menu Item Population ---

        // We only need to check the result of findViewById once inside the helper method.
        setMenuItemLabel(drawerContainer, R.id.btnMessages, "Messages");
        setMenuItemLabel(drawerContainer, R.id.btnSummary, "My Pool Summary");
        setMenuItemLabel(drawerContainer, R.id.btnTips, "Pool Tips & Articles");
        setMenuItemLabel(drawerContainer, R.id.btnLoadshedding, "Load Shedding Alerts");
        setMenuItemLabel(drawerContainer, R.id.btnRestrictions, "Water Restrictions");
        setMenuItemLabel(drawerContainer, R.id.btnHelp, "Help & Support");
        setMenuItemLabel(drawerContainer, R.id.btnSettings, "Settings");
        setMenuItemLabel(drawerContainer, R.id.btnTutorial, "Tutorial Videos");
        setMenuItemLabel(drawerContainer, R.id.btnRegisterBusiness, "Register as Business");

        // Note: You would set the click listeners here for each button ID as well.
        // E.g., drawerContainer.findViewById(R.id.btnMessages).setOnClickListener(...)

        setupFabActions("POOL_OWNER");
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void toggleSpeedDialMenu() {
        isSpeedDialOpen = !isSpeedDialOpen;

        if (isSpeedDialOpen) {
            fabOverlay.setVisibility(View.VISIBLE);
            fabAdd.setImageResource(R.drawable.ic_close_white_24dp);
        } else {
            fabOverlay.setVisibility(View.GONE);
            fabAdd.setImageResource(R.drawable.ic_add_white_24dp);
        }
    }
    private void setupFabActions(String role) {
        // Ensure all speed dial buttons are hidden if the user is NOT a PO
        if (!"POOL_OWNER".equals(role)) {
            speedDialContainer.setVisibility(View.GONE);
            // Set the SP's primary action here (e.g., Log New Job)
            fabAdd.setOnClickListener(v -> Toast.makeText(this, "SP Primary Action: New Job", Toast.LENGTH_SHORT).show());
            return;
        }

        // --- PO LOGIC: Setup Multi-Action FAB ---
        final String[] poLabels = new String[] {
                "Add Pool", "Add a Note", "Add Chemicals",
                "Water Reading", "Maintenance", "Calculator"
        };

        // 1. Set the FAB's click listener to toggle the menu
        fabAdd.setOnClickListener(v -> toggleSpeedDialMenu());

        // 2. Loop through the buttons to set labels and listeners
        for (int i = 0; i < speedDialButtons.length; i++) {
            final Button button = speedDialButtons[i];
            final String actionLabel = poLabels[i];

            if (button != null) {
                button.setText(actionLabel);

                // Set the listener to handle the action and close the menu
                button.setOnClickListener(v -> {
                    handlePoSpeedDialAction(actionLabel);
                    toggleSpeedDialMenu(); // Close menu after selection
                });
            }
        }
    }
    private void handlePoSpeedDialAction(String action) {
        // Implement navigation logic here using your replaceFragment method

        if ("Add Pool".equals(action)) {
            // This caters to the user with 0 pools OR the user adding a second pool
//            replaceFragment(new AddPoolFragment());
        } else if ("Water Reading".equals(action)) {
            // Ensure the fragment can handle the primary pool ID passed from MainActivity
//            replaceFragment(new WaterReadingFragment());
        } else if ("Maintenance".equals(action)) {
//            replaceFragment(new MaintenanceFragment());
        } else {
            // Generic handler for other actions for now
            Toast.makeText(this, "Navigating to: " + action, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}