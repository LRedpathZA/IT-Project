package com.example.splashscreen; // Use your project's package

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AvatarSelectDialogFragment extends DialogFragment implements AvatarSelectionListener {


    private static final int CUSTOM_UPLOAD_RES_ID = 0;

    private AvatarSelectionListener hostListener;

    public static AvatarSelectDialogFragment newInstance() {
        return new AvatarSelectDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AppTheme_Dialog);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_avatar_select, container, false);


        if (getParentFragment() instanceof AvatarSelectionListener) {
            hostListener = (AvatarSelectionListener) getParentFragment();
        } else if (getActivity() instanceof AvatarSelectionListener) {
            hostListener = (AvatarSelectionListener) getActivity();
        } else {
            throw new RuntimeException("Hosting Fragment or Activity must implement AvatarSelectionListener");
        }

        RecyclerView recyclerView = view.findViewById(R.id.rv_avatars);
        setupRecyclerView(recyclerView);

        return view;
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        List<Integer> avatarIds = new ArrayList<>();
        avatarIds.add(R.drawable.f1);
        avatarIds.add(R.drawable.m1);
        avatarIds.add(R.drawable.f2);
        avatarIds.add(R.drawable.m2);
        avatarIds.add(R.drawable.f3);
        avatarIds.add(R.drawable.m3);
        avatarIds.add(R.drawable.f4);
        avatarIds.add(R.drawable.m4);

        // Add the Custom Upload slot marker last
        avatarIds.add(CUSTOM_UPLOAD_RES_ID);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        AvatarAdapter adapter = new AvatarAdapter(requireContext(), avatarIds, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onAvatarSelected(@DrawableRes int selectedResId) {

        if (hostListener != null) {
            hostListener.onAvatarSelected(selectedResId);
        }

        dismiss();
    }
}