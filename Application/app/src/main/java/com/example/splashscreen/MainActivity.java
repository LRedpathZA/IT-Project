package com.example.splashscreen;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import com.example.splashscreen.data.models.UserViewModel;
import com.example.splashscreen.utils.ProfilePictureManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class MainActivity extends AppCompatActivity {
    private UserViewModel userViewModel;
    private FirebaseAuth auth;

    private DrawerLayout drawerLayout;
    public BottomNavigationView bottomNavigationView;
    private FrameLayout drawerContainer;

    // Header Views
    private CardView cardTopBar;
    private TextView tvTitle;
    private ImageButton btnBack;
    private ImageButton btnProfile;
    private ImageView navDrawerProfileImage; // Reference for the Drawer Image

    private static final int ROLE_POOL_OWNER = 1;
    private static final int ROLE_SERVICE_PROVIDER = 2;
    public static String homePoolId;

    // Flag to track if the initial fragment has been set
    private boolean isInitialFragmentSet = false;

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

        // Initialize Header Views
        cardTopBar = findViewById(R.id.card_top_bar);
        tvTitle = findViewById(R.id.tv_title);
        btnBack = findViewById(R.id.btn_back);
        btnProfile = findViewById(R.id.btn_profile);

        setupHeaderListeners();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            userViewModel.fetchUserData(currentUser.getUid());
            observeUserData();
        } else {
            logoutUser();
        }
    }

    // -------------------------------------------------------------------------
    // CORE LOGIC & DATA OBSERVATION
    // -------------------------------------------------------------------------

    private void observeUserData() {
        userViewModel.userData.observe(this, document -> {
            // Update the Header Profile Button
            if (btnProfile != null) {
                ProfilePictureManager.loadPicture(this, document, btnProfile);
            }

            // Update the Drawer Profile Image (if the view has been inflated)
            if (navDrawerProfileImage != null) {
                ProfilePictureManager.loadPicture(this, document, navDrawerProfileImage);
            }
        });

        userViewModel.userRole.observe(this, userRole -> {
            if (userRole != null) {
                setupRoleBasedUI(userRole);
                userViewModel.username.observe(this, this::populateDrawerUsername);

                if (isInitialFragmentSet) {
                    return;
                }

                Fragment initialFragment = null;
                if (ROLE_POOL_OWNER == userRole) {
                    initialFragment = new PO_HomeScreen();
                    bottomNavigationView.setSelectedItemId(R.id.nav_home_po);
                } else if (ROLE_SERVICE_PROVIDER == userRole) {
                    initialFragment = new SP_HomeScreen();
                    bottomNavigationView.setSelectedItemId(R.id.nav_home_sp);
                }

                if (initialFragment != null) {
                    replaceFragment(initialFragment, false);
                    isInitialFragmentSet = true;
                }
            } else {
                Log.w("MainActivity", "User role is null after fetch. Logging out.");
                logoutUser();
            }
        });
    }

    private void setupDrawer(int drawerLayoutResId) {
        drawerContainer.removeAllViews();
        getLayoutInflater().inflate(drawerLayoutResId, drawerContainer, true);
        MenuItem menuButton = bottomNavigationView.getMenu().findItem(R.id.nav_menu);

        // Set the reference immediately after inflation
        navDrawerProfileImage = drawerContainer.findViewById(R.id.nav_profile_image);

        // Manually trigger the drawer image update immediately after inflation
        if (navDrawerProfileImage != null && userViewModel.userData.getValue() != null) {
            ProfilePictureManager.loadPicture(this, userViewModel.userData.getValue(), navDrawerProfileImage);
        }

        if (menuButton != null) {
            menuButton.setOnMenuItemClickListener(item -> {
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            });
        } else {
            Log.e("MainActivity", "Failed to find Menu button (R.id.nav_menu) in Bottom Navigation.");
        }
    }

    private void setupRoleBasedUI(int role_id) {
        bottomNavigationView.getMenu().clear();

        if (ROLE_POOL_OWNER == role_id) {
            bottomNavigationView.inflateMenu(R.menu.menu_po_bottom_nav);
            setupDrawer(R.layout.po_navigation_drawer);
            setupPoDrawerListeners(drawerContainer);
        } else if (ROLE_SERVICE_PROVIDER == role_id) {
            bottomNavigationView.inflateMenu(R.menu.menu_sp_bottom_nav);
            setupDrawer(R.layout.sp_navigation_drawer);
            setupSpDrawerListeners(drawerContainer);
        }
        bottomNavigationView.setOnItemSelectedListener(this::onNavigationItemSelected);
    }

    // -------------------------------------------------------------------------
    // DRAWER LISTENERS (NEW)
    // -------------------------------------------------------------------------

    private void setupPoDrawerListeners(FrameLayout drawerContainer) {
        View.OnClickListener clickListener = v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            String title = ((TextView) ((LinearLayout) v).getChildAt(1)).getText().toString();

            // Placeholder logic for actual navigation
            if (v.getId() == R.id.btnMessages) {
                Toast.makeText(this, "Coming Soon: " + title, Toast.LENGTH_SHORT).show();
            } else if (v.getId() == R.id.btnSummary) {
                 navigateToFragment(new PoolHealth(), true);
            } else if (v.getId() == R.id.btnBubbles) {
                 navigateToFragment(new BubblesFragment(), true);
            } else if (v.getId() == R.id.btnLoadshedding) {
                Toast.makeText(this, "Coming Soon: " + title, Toast.LENGTH_SHORT).show();
            } else if (v.getId() == R.id.btnRestrictions) {
                navigateToFragment(new BubblesFragment(), true);
            } else if (v.getId() == R.id.btnServiceRequest) {
                 navigateToFragment(new PO_ServiceRequestList(),true  );
            } else if (v.getId() == R.id.btnHelp) {
                navigateToFragment(new BubblesFragment(), true);
            } else if (v.getId() == R.id.btnSettings) {
                 navigateToFragment(new PO_Profile(), true);
            } else if (v.getId() == R.id.btnRegisterBusiness) {
                logoutUser();
            }
        };

        drawerContainer.findViewById(R.id.btnMessages).setOnClickListener(clickListener);
        drawerContainer.findViewById(R.id.btnSummary).setOnClickListener(clickListener);
        drawerContainer.findViewById(R.id.btnBubbles).setOnClickListener(clickListener);
        drawerContainer.findViewById(R.id.btnLoadshedding).setOnClickListener(clickListener);
        drawerContainer.findViewById(R.id.btnRestrictions).setOnClickListener(clickListener);
        drawerContainer.findViewById(R.id.btnServiceRequest).setOnClickListener(clickListener);
        drawerContainer.findViewById(R.id.btnHelp).setOnClickListener(clickListener);
        drawerContainer.findViewById(R.id.btnSettings).setOnClickListener(clickListener);
        drawerContainer.findViewById(R.id.btnRegisterBusiness).setOnClickListener(clickListener);
    }
    private void registerAsBusiness()
    {
        logoutUser();
         navigateToFragment(new SP_SignUp(), false);
    }
    private void setupSpDrawerListeners(FrameLayout drawerContainer) {
        View.OnClickListener clickListener = v -> {
            // 1. Close the drawer regardless of the click target
            drawerLayout.closeDrawer(GravityCompat.START);

            String title = "SP Navigation Item";
            if (v instanceof LinearLayout && ((LinearLayout) v).getChildCount() > 1 && ((LinearLayout) v).getChildAt(1) instanceof TextView) {
                title = ((TextView) ((LinearLayout) v).getChildAt(1)).getText().toString();
            }




            if (v.getId() == R.id.btnMessages) {

            } else if (v.getId() == R.id.btnServiceRequest) {

                navigateToFragment(new SP_ServiceRequestList(), true);
            } else if (v.getId() == R.id.btnBubbles) {
                 navigateToFragment(new BubblesFragment(), true); // Assuming shared fragments
            } else if (v.getId() == R.id.btnLoadshedding) {
                 navigateToFragment(new BubblesFragment(), true);
            } else if (v.getId() == R.id.btnRestrictions) {
                 navigateToFragment(new BubblesFragment(), true);
            } else if (v.getId() == R.id.btnHelp) {
                 navigateToFragment(new BubblesFragment(),true);
            } else if (v.getId() == R.id.btnSettings) {
                navigateToFragment(new SP_Profile(), true);
            }

            // Note: The SP drawer XML provided does not include btnSummary or btnRegisterBusiness.
        };

        // Attach listeners to all SP drawer buttons (must match the IDs in your XML)
        drawerContainer.findViewById(R.id.btnMessages).setOnClickListener(clickListener);
        drawerContainer.findViewById(R.id.btnServiceRequest).setOnClickListener(clickListener);
        drawerContainer.findViewById(R.id.btnBubbles).setOnClickListener(clickListener);
        drawerContainer.findViewById(R.id.btnLoadshedding).setOnClickListener(clickListener);
        drawerContainer.findViewById(R.id.btnRestrictions).setOnClickListener(clickListener);
        drawerContainer.findViewById(R.id.btnHelp).setOnClickListener(clickListener);
        drawerContainer.findViewById(R.id.btnSettings).setOnClickListener(clickListener);
    }
    // -------------------------------------------------------------------------
    // UTILITIES
    // -------------------------------------------------------------------------

    public void logoutUser() {
        auth.signOut();
        userViewModel.userData.removeObservers(this);
        Intent intent = new Intent(this, AuthenticationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupHeaderListeners() {
        btnBack.setOnClickListener(v -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                onBackPressed();
            }
        });

        btnProfile.setOnClickListener(v -> {
            if(userViewModel.isPoolOwner()) {
                navigateToFragment(new PO_Profile(), true);
            } else {
                navigateToFragment(new SP_Profile(), true);
            }
        });

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean isRoot = getSupportFragmentManager().getBackStackEntryCount() == 0;
            btnBack.setVisibility(isRoot ? View.GONE : View.VISIBLE);

            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment != null) {
                if (currentFragment instanceof HeaderUpdatable) {
                    ((HeaderUpdatable) currentFragment).updateActivityHeader();
                } else {
                    updateHeader("Detail Screen", true, !isRoot);
                }
            }
        });

        btnBack.setVisibility(View.GONE);
    }

    public void updateHeader(String title, boolean showHeader, boolean showBackButton) {
        cardTopBar.setVisibility(showHeader ? View.VISIBLE : View.GONE);
        tvTitle.setText(title);
        btnProfile.setVisibility(showHeader ? View.VISIBLE : View.GONE);
        btnBack.setVisibility(showBackButton ? View.VISIBLE : View.GONE);
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_menu) {
            return false;
        }

        if (userViewModel.isPoolOwner()) {
            if (itemId == R.id.nav_home_po) {
                selectedFragment = new PO_HomeScreen();
            } else if (itemId == R.id.nav_calculator) {
                selectedFragment = new Calculator_Selector();
            } else if (itemId == R.id.nav_calendar) {
                navigateToPO_Calendar();
                return true;
            } else if (itemId == R.id.nav_marketplace_po) {
                selectedFragment = new PO_Marketplace();
            }
        } else if (userViewModel.isServiceProvider()) {
            if (itemId == R.id.nav_home_sp) {
                selectedFragment = new SP_HomeScreen();
            } else if (itemId == R.id.nav_clients) {
                selectedFragment = new ClientListFragment();
            }
            else if (itemId == R.id.nav_inventory) {
                selectedFragment = new SP_ProductList();
            }
            // Add other SP bottom nav logic here...
        }

        if (selectedFragment != null) {
            replaceFragment(selectedFragment, false);
            return true;
        }
        return false;
    }

    private void populateDrawerUsername(String username) {
        TextView tvDrawerUsername = drawerContainer.findViewById(R.id.nav_username);
        if (tvDrawerUsername != null) {
            tvDrawerUsername.setText(username);
        }
    }

    /**
     * Replaces the current fragment with a new one.
     * @param fragment The new fragment to display.
     * @param addToBackStack True to add the transaction to the back stack (for history), false for main navigation.
     */
    public void navigateToFragment(Fragment fragment, boolean addToBackStack) {
        replaceFragment(fragment, addToBackStack);
    }

    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        if (!addToBackStack) {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
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
            getSupportFragmentManager().popBackStack();
        } else {
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

        navigateToFragment(calendarFragment, true);
    }
}