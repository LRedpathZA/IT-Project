package com.example.splashscreen;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private UserViewModel userViewModel;

    private FirebaseAuth auth;

    private DrawerLayout drawerLayout;
    public BottomNavigationView bottomNavigationView;
    private FrameLayout drawerContainer;

    // --- Header Views (NEW) ---
    private CardView cardTopBar;
    private TextView tvTitle;
    private ImageButton btnBack;
    private ImageButton btnProfile;
    // -------------------------

    private static final int ROLE_POOL_OWNER = 1;
    private static final int ROLE_SERVICE_PROVIDER = 2;
    public static String homePoolId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        drawerLayout = findViewById(R.id.drawer_layout);
        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        drawerContainer = findViewById(R.id.drawer_container);

        // --- Initialize Header Views (NEW) ---
        cardTopBar = findViewById(R.id.card_top_bar);
        tvTitle = findViewById(R.id.tv_title);
        btnBack = findViewById(R.id.btn_back);
        btnProfile = findViewById(R.id.btn_profile);
        // ------------------------------------

        setupHeaderListeners(); // Set up global header listeners

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            userViewModel.fetchUserData(currentUser.getUid());
            observeUserData();
        } else {
            logoutUser();
        }
    }

    // -------------------------------------------------------------------------
    // HEADER MANAGEMENT (NEW)
    // -------------------------------------------------------------------------

    private void setupHeaderListeners() {
        btnBack.setOnClickListener(v -> {
            // Handle back navigation using the FragmentManager back stack
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                // If on a root fragment, call the system back press
                onBackPressed();
            }
        });

        btnProfile.setOnClickListener(v -> {
            if(userViewModel.isPoolOwner())
            {
                Fragment profileFragment = new PO_Profile();
                replaceFragment(profileFragment, true);
            }
            else
            {
                Fragment profileFragment = new SP_Profile();
                replaceFragment(profileFragment, true);
            }
        });


        getSupportFragmentManager().addOnBackStackChangedListener(() -> {

            boolean isRoot = getSupportFragmentManager().getBackStackEntryCount() == 0;
            btnBack.setVisibility(isRoot ? View.GONE : View.VISIBLE);

            // Re-call fragment's onResume to ensure header settings are applied
            // This is a common pattern to ensure the header is updated after a back operation.
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment != null) {
                if (currentFragment instanceof HeaderUpdatable) {
                    ((HeaderUpdatable) currentFragment).updateActivityHeader();
                } else {
                    // Default header for any fragment that doesn't implement the interface
                    updateHeader("Detail Screen", true, !isRoot);
                }
            }
        });

        // Hide back button on initial load
        btnBack.setVisibility(View.GONE);
    }

    /**
     * Public method for fragments to call to update the header title and visibility.
     * This is the core method for the new centralized header system.
     */
    public void updateHeader(String title, boolean showHeader, boolean showBackButton) {
        cardTopBar.setVisibility(showHeader ? View.VISIBLE : View.GONE);
        tvTitle.setText(title);
        btnProfile.setVisibility(showHeader ? View.VISIBLE : View.GONE); // Generally hide profile if header is hidden
        btnBack.setVisibility(showBackButton ? View.VISIBLE : View.GONE);
    }

    // -------------------------------------------------------------------------
    // CORE LOGIC
    // -------------------------------------------------------------------------

    private void observeUserData() {
        userViewModel.userRole.observe(this, userRole -> {
            if (userRole != null) {
                setupRoleBasedUI(userRole);

                Fragment initialFragment = null;
                if (ROLE_POOL_OWNER == userRole) {
                    initialFragment = new PO_HomeScreen();
                    bottomNavigationView.setSelectedItemId(R.id.nav_home_po);
                } else if (ROLE_SERVICE_PROVIDER == userRole) {
                    initialFragment = new SP_HomeScreen();
                    bottomNavigationView.setSelectedItemId(R.id.nav_home_sp);
                }

                // Use the custom fragment replacement for the initial load
                if (initialFragment != null) {
                    replaceFragment(initialFragment, false);
                }

                userViewModel.username.observe(this, this::populateDrawerUsername);
            } else {
                Log.w("MainActivity", "User role is null after fetch. Logging out.");
                logoutUser();
            }
        });
    }

    public void logoutUser() {
        auth.signOut();
        userViewModel.userData.removeObservers(this);
        Intent intent = new Intent(this, AuthenticationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupRoleBasedUI(int role_id) {
        bottomNavigationView.getMenu().clear();

        if (ROLE_POOL_OWNER == role_id) {
            bottomNavigationView.inflateMenu(R.menu.menu_po_bottom_nav);
            setupDrawer(R.layout.po_navigation_drawer);
            populatePoMenu(drawerContainer);
        } else if (ROLE_SERVICE_PROVIDER == role_id) {
            bottomNavigationView.inflateMenu(R.menu.menu_sp_bottom_nav);
            setupDrawer(R.layout.sp_navigation_drawer);
            populateSpMenu(drawerContainer);
        }
        bottomNavigationView.setOnItemSelectedListener(this::onNavigationItemSelected);
    }


    private void setupDrawer(int drawerLayoutResId) {
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
        if (itemId == R.id.nav_menu) {
            return false;
        }

        // --- Pool Owner Navigation Mapping ---
        if (userViewModel.isPoolOwner()) {
            if (itemId == R.id.nav_home_po) {
                selectedFragment = new PO_HomeScreen();
            } else if (itemId == R.id.nav_calculator) {
                selectedFragment = new Calculator_Selector();
            } else if (itemId == R.id.nav_calendar) {
                navigateToPO_Calendar();
                return true;
            }else if (itemId == R.id.nav_marketplace_po) {
                selectedFragment = new PO_Marketplace();
            }
            // --- Service Provider Navigation Mapping ---
        } else if (userViewModel.isServiceProvider()) {
            if (itemId == R.id.nav_home_sp) {
                selectedFragment = new SP_HomeScreen();
            }
        }


        if (selectedFragment != null) {
            // Do not add bottom nav fragments to the back stack
            replaceFragment(selectedFragment, false);
            return true;
        }
        return false;
    }

    private void setMenuItemLabel(View parentView, int rootIncludeId, String labelText) {
        View rootMenuButton = parentView.findViewById(rootIncludeId);
        if (rootMenuButton != null) {
            TextView label = rootMenuButton.findViewById(R.id.tvMenuItemText);
            if (label != null) {
                label.setText(labelText);
            }
        }
    }

    private void populateDrawerUsername(String username) {
        TextView tvDrawerUsername = drawerContainer.findViewById(R.id.nav_username);
        if (tvDrawerUsername != null) {
            tvDrawerUsername.setText(username);
        }
    }

    private void populatePoMenu(FrameLayout drawerContainer) {
        // ... (Drawer population code remains the same)
        setMenuItemLabel(drawerContainer, R.id.btnMessages, "Messages");
        setMenuItemLabel(drawerContainer, R.id.btnSummary, "My Pool Health");
        setMenuItemLabel(drawerContainer, R.id.btnTips, "Pool Tips & Articles");
        setMenuItemLabel(drawerContainer, R.id.btnLoadshedding, "Load Shedding Alerts");
        setMenuItemLabel(drawerContainer, R.id.btnRestrictions, "Water Restrictions");
        setMenuItemLabel(drawerContainer, R.id.btnHelp, "Help & Support");
        setMenuItemLabel(drawerContainer, R.id.btnSettings, "Settings");
        setMenuItemLabel(drawerContainer, R.id.btnTutorial, "Tutorial Videos");
        setMenuItemLabel(drawerContainer, R.id.btnRegisterBusiness, "Register as Business");


    }

    private void navTest() {
        // Example of a fragment navigation that should show the back button
        replaceFragment(new PO_AddPool(), true);
    }

    private void populateSpMenu(FrameLayout drawerContainer) {
        // ... (Drawer population code remains the same)
        setMenuItemLabel(drawerContainer, R.id.btnMessages, "Message & Notifications");
        setMenuItemLabel(drawerContainer, R.id.btnSummary, "Summary");
        setMenuItemLabel(drawerContainer, R.id.btnLoadshedding, "Loadshedding");
        setMenuItemLabel(drawerContainer, R.id.btnRestrictions, "Water Restrictions");
        setMenuItemLabel(drawerContainer, R.id.btnHelp, "Help & Support");
        setMenuItemLabel(drawerContainer, R.id.btnSettings, "Settings");
        setMenuItemLabel(drawerContainer, R.id.btnTutorial, "Tutorial");
        findViewById(R.id.btnTips).setVisibility(View.GONE);
        findViewById(R.id.btnRegisterBusiness).setVisibility(View.GONE);
    }

    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        // Clear back stack when navigating to a root/main screen (like Home or Calculator Selector)
        if (!addToBackStack) {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        // Begin the transaction
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }


    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            // If the back stack is managed, pop it
            getSupportFragmentManager().popBackStack();
        } else {
            // Default behavior if on the root fragment
            super.onBackPressed();
        }
    }

    public void navigateToPO_Calendar() {
        if (homePoolId == null) {
            Toast.makeText(this, "Pool details not loaded yet. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }

        PO_Calendar calendarFragment = new PO_Calendar();
        Bundle args = new Bundle();
        args.putString("POOL_ID", homePoolId);
        calendarFragment.setArguments(args);


        replaceFragment(calendarFragment, true);
    }
}