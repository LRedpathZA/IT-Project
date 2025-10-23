package com.example.splashscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class Calculator_Selector extends Fragment implements HeaderUpdatable {


    public Calculator_Selector() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.calculator_selector, container, false);
        CardView cardPhCalculator = view.findViewById(R.id.card_ph_calculator);
        cardPhCalculator.setOnClickListener(v -> {
            replaceFragment(new pHCalculator());
        });


        return view;
    }
    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            String title =  "pH Calculator";
            ((MainActivity) getActivity()).updateHeader(title, true, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Use a transition animation -- We need to get good looking ones
//        fragmentTransaction.setCustomAnimations(
//                R.anim.slide_in_right, // New fragment enters
//                R.anim.slide_out_left,  // Current fragment exits
//                R.anim.slide_in_left,   // Fragment returns (pop)
//                R.anim.slide_out_right  // Fragment exits (pop)
//        );

        // Replace the current fragment, add to back stack for back navigation
        fragmentTransaction.replace(R.id.fragment_container, fragment); // Replace R.id.fragment_container with your actual container ID
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}