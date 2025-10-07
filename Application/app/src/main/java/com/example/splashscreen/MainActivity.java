package com.example.splashscreen;

import android.content.Intent;
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
import androidx.fragment.app.FragmentTransaction;

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

    private DrawerLayout drawerLayout;
    public BottomNavigationView bottomNavigationView;
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

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            fetchUserDataAndSetup(currentUser.getUid());
        } else {
            // Redirect to login/authentication screen
            logoutUser();
        }
    }
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
                            initialFragment = new PO_HomeScreen();

                            bottomNavigationView.setSelectedItemId(R.id.nav_home_po);
                        } else if (ROLE_SERVICE_PROVIDER == userRole ) {

                            initialFragment = new SP_HomeScreen();

                            bottomNavigationView.setSelectedItemId(R.id.nav_home_sp);
                        } else {
                            initialFragment = new Fragment(); // Fallback
                        }
                        replaceFragment(initialFragment);
                    }
                } else {
                    Log.w("MainActivity", "User document not found. Logging out.");
                    logoutUser();
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
    public void logoutUser() {
        auth.signOut();
        userDataDocument = null;
        Intent intent = new Intent(this, AuthenticationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupRoleBasedUI(int role_id) {
        bottomNavigationView.getMenu().clear();
        int drawerLayoutResId;


        if (ROLE_POOL_OWNER == role_id) {
            bottomNavigationView.inflateMenu(R.menu.menu_po_bottom_nav);
            drawerLayoutResId = R.layout.po_navigation_drawer;
            setupDrawer(drawerLayoutResId);
            populatePoMenu(drawerContainer);


        } else if (ROLE_SERVICE_PROVIDER == role_id) {
            bottomNavigationView.inflateMenu(R.menu.menu_sp_bottom_nav);
            drawerLayoutResId = R.layout.sp_navigation_drawer;
            setupDrawer(drawerLayoutResId);
            populateSpMenu(drawerContainer);
        }
        bottomNavigationView.setOnItemSelectedListener(this::onNavigationItemSelected);
    }


    private void setupDrawer(int drawerLayoutResId) {
        // Loads the correct drawer layout (PO or SP) and sets the Menu button listener.
        drawerContainer.removeAllViews();
        getLayoutInflater().inflate(drawerLayoutResId, drawerContainer, true);
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

        // Check if the Drawer Menu button was pressed
        if (itemId == R.id.nav_menu) {

            return false;
        }

        // --- Pool Owner Navigation Mapping ---
        if (bottomNavigationView.getMenu().findItem(R.id.nav_home_po) != null) {
            if (itemId == R.id.nav_home_po) {
                selectedFragment = new PO_HomeScreen();
            } else if (itemId == R.id.nav_marketplace_po) {
                selectedFragment = new PO_Marketplace();
            }
            // --- Service Provider Navigation Mapping ---
        } else if (bottomNavigationView.getMenu().findItem(R.id.nav_home_sp) != null) {
            if (itemId == R.id.nav_home_sp) {
                selectedFragment = new SP_HomeScreen();
            }
        }


        if (selectedFragment != null) {
            replaceFragment(selectedFragment);
            return true;
        }
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

        // TESTTTTINNNNNNNNNGGGG
        findViewById(R.id.btnTips).setOnClickListener(v -> navTest());
    }
    private void navTest()
    {
        // Example navigation for TESTING
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new PO_AddPool())
                .addToBackStack(null)
                .commit();
    }

    private void populateSpMenu(FrameLayout drawerContainer) {
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

        isSpeedDialOpen = !isSpeedDialOpen;

        if (isSpeedDialOpen) {
            fabOverlay.setVisibility(View.VISIBLE);

            fabAdd.setImageResource(R.drawable.ic_close_white_24dp);
        } else {
            fabOverlay.setVisibility(View.GONE);

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
            // TODO: Navigate to PO_AddPool fragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new PO_AddPool())
                    .addToBackStack(null)
                    .commit();
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