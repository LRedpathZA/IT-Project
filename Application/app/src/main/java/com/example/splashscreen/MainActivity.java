package com.example.splashscreen;

import android.content.Intent; // NEW: Needed for navigation
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


public class MainActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // DATA STORAGE: Stores the entire user document for easy access by fragments
    private DocumentSnapshot userDataDocument;

    // UI Components
    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAdd;
    private FrameLayout drawerContainer;
    private FrameLayout fabOverlay;

    private LinearLayout speedDialContainer;
    private boolean isSpeedDialOpen = false;
    private final Button[] speedDialButtons = new Button[6];

    private String username;
    private static final int ROLE_POOL_OWNER = 1;
    private static final int ROLE_SERVICE_PROVIDER = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        drawerLayout = findViewById(R.id.drawer_layout);
        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        fabAdd = findViewById(R.id.fab_add);
        drawerContainer = findViewById(R.id.drawer_container);
        fabOverlay = findViewById(R.id.fab_overlay);
        speedDialContainer = findViewById(R.id.speed_dial_menu_container);

        // Initialize speed dial buttons
        speedDialButtons[0] = speedDialContainer.findViewById(R.id.btn_action_1);
        speedDialButtons[1] = speedDialContainer.findViewById(R.id.btn_action_2);
        speedDialButtons[2] = speedDialContainer.findViewById(R.id.btn_action_3);
        speedDialButtons[3] = speedDialContainer.findViewById(R.id.btn_action_4);
        speedDialButtons[4] = speedDialContainer.findViewById(R.id.btn_action_5);
        speedDialButtons[5] = speedDialContainer.findViewById(R.id.btn_action_6);

        // Check for logged-in user and setup the main screen
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            fetchUserDataAndSetup(currentUser.getUid());
        } else {
            // Redirect to login/authentication screen if somehow here without a user
            logoutUser();
        }
       // findViewById(R.id.ivProfileIcon).setOnClickListener(v -> logoutUser());
    }
    /**
     * Fetches user data from Firestore and sets up the UI based on the user's role.
     * The entire DocumentSnapshot is saved for easy fragment access.
     */
    private void fetchUserDataAndSetup(String userId) {
        DocumentReference docRef = db.collection("users").document(userId);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {

                    // CORE DATA STEP: Store the entire document here
                    userDataDocument = document;

                    username = document.getString("name");
                    Long roleLong = document.getLong("role_id");
                    if (roleLong != null) {
                        int userRole = roleLong.intValue();
                        Log.d("MainActivity", "User: " + username + ", Role: " + userRole);

                        setupRoleBasedUI(userRole);

                        Fragment initialFragment;
                        if (ROLE_POOL_OWNER == userRole) {
                            // Data can now be retrieved by PO_HomeScreen using getUserDataDocument()
                            initialFragment = new PO_HomeScreen();
                        } else if (ROLE_SERVICE_PROVIDER == userRole ) {
                            // Data can now be retrieved by SP_HomeScreen using getUserDataDocument()
                            initialFragment = new SP_HomeScreen();
                        } else {
                            initialFragment = new Fragment(); // Fallback
                        }
                        replaceFragment(initialFragment);
                    }
                } else {
                    Log.w("MainActivity", "User document not found. Logging out.");
                    logoutUser(); // If user exists in Auth but not in DB, force logout
                }
            } else {
                Log.e("MainActivity", "Error fetching user data: ", task.getException());
                Toast.makeText(this, "Failed to load profile data.", Toast.LENGTH_LONG).show();
            }
        });
    }


    public DocumentSnapshot getUserDataDocument() {
        return userDataDocument;
    }

    /**
     * Logs the current Firebase user out and redirects to the AuthenticationActivity.
     */
    public void logoutUser() {
        // 1. Sign out the user from Firebase
        auth.signOut();

        // 2. Clear the cached document
        userDataDocument = null;

        // 3. Navigate back to the AuthenticationActivity
        Intent intent = new Intent(this, AuthenticationActivity.class);

        // Flags ensure the user cannot hit the back button to return here
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish(); // Close MainActivity
    }

    // --- Existing Methods Below ---

    private void setupRoleBasedUI(int role_id) {
        bottomNavigationView.getMenu().clear();
        int drawerLayoutResId;

        if (ROLE_POOL_OWNER == role_id) {
            bottomNavigationView.inflateMenu(R.menu.menu_po_bottom_nav);
            drawerLayoutResId = R.layout.po_navigation_drawer;
            setupDrawer(drawerLayoutResId); // Setup drawer view and listener
            populatePoMenu(drawerContainer); // Populate labels and set FAB actions

        } else if (ROLE_SERVICE_PROVIDER == role_id) {
            bottomNavigationView.inflateMenu(R.menu.menu_sp_bottom_nav);
            drawerLayoutResId = R.layout.sp_navigation_drawer;
            setupDrawer(drawerLayoutResId); // Setup drawer view and listener
            populateSpMenu(drawerContainer); // Populate labels and set FAB actions
        }

        // Set the listener for fragment swapping for both roles
        bottomNavigationView.setOnItemSelectedListener(this::onNavigationItemSelected);
    }

    private void setupDrawer(int drawerLayoutResId) {
        // Loads the correct drawer layout (PO or SP) and sets the Menu button listener.
        drawerContainer.removeAllViews();
        getLayoutInflater().inflate(drawerLayoutResId, drawerContainer, true);

        // The Drawer Menu button should have the consistent ID R.id.nav_menu in both bottom nav menus.
        MenuItem menuButton = bottomNavigationView.getMenu().findItem(R.id.nav_menu);

        if (menuButton != null) {
            menuButton.setOnMenuItemClickListener(item -> {
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            });
        } else {
            Log.e("MainActivity", "Failed to find Menu button (R.id.nav_menu) in Bottom Navigation.");
        }
    }


    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        // TODO: Map menu item IDs to actual Fragments for both PO and SP
        // if (itemId == R.id.nav_home_po) { ... }
        // if (selectedFragment != null) { replaceFragment(selectedFragment); return true; }
        return false;
    }

    private void setMenuItemLabel(View parentView, int rootIncludeId, String labelText) {
        // Helper method to set the text of the TextView inside the included menu item layout.
        View rootMenuButton = parentView.findViewById(rootIncludeId);
        if (rootMenuButton != null) {
            TextView label = rootMenuButton.findViewById(R.id.tvMenuItemText);
            if (label != null) {
                label.setText(labelText);
            }
        }
    }

    private void populatePoMenu(FrameLayout drawerContainer) {
        // Sets the specific menu item labels and prepares FAB actions for the Pool Owner role.
        TextView tvDrawerUsername = drawerContainer.findViewById(R.id.nav_username);
        if (tvDrawerUsername != null) {
            tvDrawerUsername.setText(username);
        }

        setMenuItemLabel(drawerContainer, R.id.btnMessages, "Messages");
        setMenuItemLabel(drawerContainer, R.id.btnSummary, "My Pool Summary");
        setMenuItemLabel(drawerContainer, R.id.btnTips, "Pool Tips & Articles");
        setMenuItemLabel(drawerContainer, R.id.btnLoadshedding, "Load Shedding Alerts");
        setMenuItemLabel(drawerContainer, R.id.btnRestrictions, "Water Restrictions");
        setMenuItemLabel(drawerContainer, R.id.btnHelp, "Help & Support");
        setMenuItemLabel(drawerContainer, R.id.btnSettings, "Settings");
        setMenuItemLabel(drawerContainer, R.id.btnTutorial, "Tutorial Videos");
        setMenuItemLabel(drawerContainer, R.id.btnRegisterBusiness, "Register as Business");

        setupFabActions("POOL_OWNER");

        // TESTTTTINNNNNNNNNNGGG (Removed to avoid confusion, but kept the functionality intact)
        // If btnTips is your placeholder for the Profile, ensure you update its ID later
        findViewById(R.id.btnTips).setOnClickListener(v -> navTest());
    }
    private void navTest()
    {
        // Example navigation to the PO_Profile Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SP_Profile())
                .addToBackStack(null)
                .commit();
    }

    private void populateSpMenu(FrameLayout drawerContainer) {
        // Sets the specific menu item labels and prepares FAB actions for the Service Provider role.
        TextView tvDrawerUsername = drawerContainer.findViewById(R.id.nav_username);
        if (tvDrawerUsername != null) {
            tvDrawerUsername.setText(username);
        }


        setMenuItemLabel(drawerContainer, R.id.btnMessages, "Message & Notifications");
        setMenuItemLabel(drawerContainer, R.id.btnSummary, "Summary");
        setMenuItemLabel(drawerContainer, R.id.btnLoadshedding, "Loadshedding");
        setMenuItemLabel(drawerContainer, R.id.btnRestrictions, "Water Restrictions");
        setMenuItemLabel(drawerContainer, R.id.btnHelp, "Help & Support");
        setMenuItemLabel(drawerContainer, R.id.btnSettings, "Settings");
        setMenuItemLabel(drawerContainer, R.id.btnTutorial, "Tutorial");
        // Assuming R.id.btnLogOut is the button used for log out in the SP drawer
        //setMenuItemLabel(drawerContainer, R.id.btnLogOut, "Log Out");
        findViewById(R.id.btnTips).setVisibility(View.GONE);
        findViewById(R.id.btnRegisterBusiness).setVisibility(View.GONE);

        setupFabActions("SERVICE_PROVIDER");
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void toggleSpeedDialMenu() {
        // Toggles the visibility of the full-screen FAB overlay.
        isSpeedDialOpen = !isSpeedDialOpen;

        if (isSpeedDialOpen) {
            fabOverlay.setVisibility(View.VISIBLE);
            // Change FAB icon to 'X' to indicate closing the menu
            fabAdd.setImageResource(R.drawable.ic_close_white_24dp);
        } else {
            fabOverlay.setVisibility(View.GONE);
            // Change FAB icon back to '+'
            fabAdd.setImageResource(R.drawable.ic_add_white_24dp);
        }
    }
    // These are the quick menu actions :>
    private void setupFabActions(String role) {


        final String[] poLabels = new String[] {
                "Add Pool", "Add a Note", "Add Chemicals",
                "Water Reading", "Maintenance", "Calculator"
        };
        final String[] spLabels = new String[] {
                "Clients", "Add a Note", "Add Chemicals",
                "Water Reading", "Maintenance", "Calculator"
        };

        String[] currentLabels = role.equals("POOL_OWNER") ? poLabels : spLabels;


        fabAdd.setOnClickListener(v -> toggleSpeedDialMenu());

        for (int i = 0; i < speedDialButtons.length; i++) {
            final Button button = speedDialButtons[i];
            final String actionLabel = currentLabels[i];

            if (button != null) {
                button.setText(actionLabel);
                button.setOnClickListener(v -> {
                    if (role.equals("POOL_OWNER")) {
                        handlePoSpeedDialAction(actionLabel);
                    } else {
                        handleSpSpeedDialAction(actionLabel);
                    }
                    toggleSpeedDialMenu();
                });
            }
        }
    }

    private void handlePoSpeedDialAction(String action) {

        if ("Add Pool".equals(action)) {
            // TODO: replaceFragment(new AddPoolFragment());
        } else if ("Water Reading".equals(action)) {
            // TODO: replaceFragment(new WaterReadingFragment());
        } else {
            Toast.makeText(this, "PO Navigating to: " + action, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSpSpeedDialAction(String action) {

        if ("Clients".equals(action)) {
            // TODO: replaceFragment(new AddClientFragment());
        } else if ("Water Reading".equals(action)) {
            // TODO: replaceFragment(new SpWaterReadingSelectionFragment());
        } else {
            Toast.makeText(this, "SP Navigating to: " + action, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (isSpeedDialOpen) {
            toggleSpeedDialMenu();
        } else {
            super.onBackPressed();
        }
    }
}