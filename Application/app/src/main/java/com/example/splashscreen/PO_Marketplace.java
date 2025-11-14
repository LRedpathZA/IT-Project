package com.example.splashscreen;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.example.splashscreen.data.models.UserViewModel;
import com.example.splashscreen.utils.ProfilePictureManager;
import com.google.android.material.imageview.ShapeableImageView;

public class PO_Marketplace extends Fragment implements HeaderUpdatable {
    private UserViewModel userViewModel;
    private ShapeableImageView btnProfilePic;

    public PO_Marketplace() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.po_marketplace, container, false);
    }

    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateHeader("", false, false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        btnProfilePic = view.findViewById(R.id.btn_profile);
        userViewModel.userData.observe(getViewLifecycleOwner(), document -> {
            if (getContext() != null) {
                // 4. Use the reusable Manager to load the latest picture
                ProfilePictureManager.loadPicture(getContext(), document, btnProfilePic);
            }
        });
    }
}