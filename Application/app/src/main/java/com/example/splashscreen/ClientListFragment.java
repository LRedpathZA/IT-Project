package com.example.splashscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.R;
import com.example.splashscreen.adapters.ClientListAdapter;
import com.example.splashscreen.data.models.ClientModel;

import java.util.ArrayList;
import java.util.List;

public class ClientListFragment extends Fragment implements ClientListAdapter.OnClientClickListener, HeaderUpdatable{

    private RecyclerView recyclerView;
    private ClientListAdapter adapter;

    public ClientListFragment() {
        // Required empty public constructor
    }

    public static ClientListFragment newInstance() {
        return new ClientListFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_client_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TODO: In a later step, you'll set up a ViewModel to fetch real Firestore data here.
        List<ClientModel> dummyClients = createDummyClientList();

        recyclerView = view.findViewById(R.id.rv_client_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        // You might need to adjust the divider drawable for a thin, clean line
        recyclerView.addItemDecoration(dividerItemDecoration);

        adapter = new ClientListAdapter(dummyClients, this);
        recyclerView.setAdapter(adapter);
    }
    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            String title =  "Client List";
            ((MainActivity) getActivity()).updateHeader(title, true, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }

    // --- Data Simulation ---
    private List<ClientModel> createDummyClientList() {
        List<ClientModel> clients = new ArrayList<>();
        clients.add(new ClientModel("101", "Rozaan Viljoen", "123 Somewhere", true));
        clients.add(new ClientModel("102", "Line Redpath", "456 Elsewhere", true));
        clients.add(new ClientModel("103", "Tinotenda Mhedziso", "789 Hidden Leaf Village", true));
        clients.add(new ClientModel("104", "Devon Lane", "New Pool Install", false)); // Not Active/New
        clients.add(new ClientModel("105", "Albert Flores", "Filter Backwash", false));
        clients.add(new ClientModel("106", "Courtney Henry", "Weekly Maintenance", false));
        clients.add(new ClientModel("107", "Kristin Watson", "Spa Service", false));
        return clients;
    }

    // --- Navigation ---
    @Override
    public void onClientClick(ClientModel client) {
        // 💥 The next step: Navigate to the client profile fragment
        Toast.makeText(getContext(), "Navigating to Profile for: " + client.getName(), Toast.LENGTH_SHORT).show();

        // TODO: Replace with the actual ClientProfileFragment instance and navigation logic
        // navigateToClientProfile(client);
    }

//    private void navigateToClientProfile(ClientModel client) {
//        // Example navigation placeholder (assuming you create ClientProfileFragment next)
//        Fragment clientProfileFragment = ClientProfileFragment.newInstance(client.getClientId());
//
//        if (getActivity() != null) {
//            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
//            transaction.replace(R.id.fragment_container, clientProfileFragment); // Assuming the main container ID
//            transaction.addToBackStack(null);
//            transaction.commit(); Will update soon but for now let's leave this blank
//        }
//    }
}