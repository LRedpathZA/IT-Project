package com.example.splashscreen;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import com.google.android.material.button.MaterialButton;

public class PO_Profile extends Fragment {


    public PO_Profile() {

    }

    public static PO_Profile newInstance() {
        PO_Profile fragment = new PO_Profile();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.po_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton btnLogout = view.findViewById(R.id.btn_logout);
        ImageButton btnBack = view.findViewById(R.id.btn_back);

        btnLogout.setOnClickListener(v -> logoutUser());
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().finish();
            }
        });
    }

    private void logoutUser() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).logoutUser();
        }
    }
}