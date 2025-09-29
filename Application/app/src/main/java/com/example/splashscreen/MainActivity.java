package com.example.splashscreen;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
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

// Import your Fragment classes (You need to create these)
// import com.example.splashscreen.fragments.PO_HomeScreen;
// import com.example.splashscreen.fragments.SP_HomeScreen;
// import com.example.splashscreen.fragments.MarketplaceFragment;
// import com.example.splashscreen.fragments.SettingsFragment;


public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // UI Components
    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAdd;
    private FrameLayout drawerContainer; // Used to dynamically load drawer content


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

        // Initialize UI components from activity_main.xml
        drawerLayout = findViewById(R.id.drawer_layout);
        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        fabAdd = findViewById(R.id.fab_add);
        // Note: You need to add 'drawer_container' FrameLayout to activity_main.xml (see previous response)
        drawerContainer = findViewById(R.id.drawer_container);

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
                    String username = document.getString("name");
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
        // Clear previous menu items
        bottomNavigationView.getMenu().clear();

        if (ROLE_POOL_OWNER == role_id) {
            bottomNavigationView.inflateMenu(R.menu.menu_po_bottom_nav);
            setupDrawer(R.layout.po_navigation_drawer);

            // FAB Action: Add Water Reading/Pool (Primary PO action)
            fabAdd.setOnClickListener(v -> {
                Toast.makeText(this, "Opening Add Pool/Reading screen", Toast.LENGTH_SHORT).show();
                // TODO: Implement navigation to AddPoolFragment or AddWaterReadingFragment
            });
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

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }



    /**
     * Helper method for fragments to control the FAB visibility if needed.
     * @param visibility View.VISIBLE, View.INVISIBLE, or View.GONE
     */
    public void setFabVisibility(int visibility) {
        fabAdd.setVisibility(visibility);
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