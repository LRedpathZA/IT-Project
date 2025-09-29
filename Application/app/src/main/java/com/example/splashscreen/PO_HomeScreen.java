package com.example.splashscreen;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.Nullable;


public class PO_HomeScreen extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private TextView GreetingName;

    public PO_HomeScreen() {
        // Required empty public constructor
    }

    // Factory Method
    public static PO_HomeScreen newInstance(String param1, String param2) {
        PO_HomeScreen fragment = new PO_HomeScreen();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    // Initialization for the creation ting
    /*
    This is where you initialize non-visual components like Firebase instances, database objects,
    or retrieve arguments (mParam1, mParam2).
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    /*
   his is where the Fragment's UI is drawn. It takes your XML layout file (R.layout.fragment_po_home_screen)
   and inflates it into an actual View object, which is then returned to the hosting Activity.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_po_home_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize UI components (finding them in the inflated 'view')
        GreetingName = view.findViewById(R.id.poGreeting);

        // 2. Fetch data (including the name) and update the UI
        fetchUserName();
        // ... call fetchPoolData() and fetchFeaturedProducts() here
    }

    private void fetchUserName() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) return;

        DocumentReference docRef = db.collection("users").document(userId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("name");
                if (username != null) {
                    // Update the TextView on the screen
                    GreetingName.setText("Hello, " + username + "!");
                }
            }
        }).addOnFailureListener(e -> {
            // Log error
            GreetingName.setText("Hello, User!");
        });
    }
}