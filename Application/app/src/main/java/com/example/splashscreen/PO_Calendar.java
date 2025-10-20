package com.example.splashscreen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// Implements the click listener for the RecyclerView Adapter
public class PO_Calendar extends Fragment implements EventAdapter.OnEventClickListener {

    public static final String REQUEST_KEY_EVENT_UPDATED = "event_updated_key";
    public static final String BUNDLE_KEY_EVENT_ID = "event_id";
    public static final String ARG_EVENT_ID = "EVENT_ID";

    private CalendarView calendarView;
    private RecyclerView rvEvents;
    private View tvNoEvents;
    private TextView tvEventsHeader;

    private FirebaseFirestore db;
    private PoolViewModel poolViewModel;
    private Calendar selectedDate = Calendar.getInstance();
    private EventAdapter eventAdapter;
    private List<EventModel> eventList = new ArrayList<>();

    private String HOME_POOL_ID; // Now managed by the ViewModel

    public PO_Calendar() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.po_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        poolViewModel = new ViewModelProvider(requireActivity()).get(PoolViewModel.class);

        ImageButton btnBack = view.findViewById(R.id.btn_back);
        FloatingActionButton fabAddEvent = view.findViewById(R.id.fab_add_event);

        // Fetch pool ID from ViewModel's LiveData once it's available
        poolViewModel.poolId.observe(getViewLifecycleOwner(), poolId -> {
            if (poolId != null) {
                HOME_POOL_ID = poolId;
                // Once ID is available, load events for the current date
                loadEventsForSelectedDate(selectedDate);
                fabAddEvent.setEnabled(true);
            } else {
                Toast.makeText(getContext(), "Pool ID is missing. Cannot load events.", Toast.LENGTH_LONG).show();
                HOME_POOL_ID = null;
                fabAddEvent.setEnabled(false);
            }
        });

        // Arguments logic is redundant but can be kept as a fallback for initial navigation,
        // though the ViewModel Observer is the reliable source.
        if (getArguments() != null) {
            String argPoolId = getArguments().getString("POOL_ID");
            if (argPoolId != null && HOME_POOL_ID == null) {
                HOME_POOL_ID = argPoolId;
            }
        }


        calendarView = view.findViewById(R.id.calendar_view);
        rvEvents = view.findViewById(R.id.rv_events);
        tvNoEvents = view.findViewById(R.id.tv_no_events);
        tvEventsHeader = view.findViewById(R.id.tv_events_header);

        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        eventAdapter = new EventAdapter(eventList, this);
        rvEvents.setAdapter(eventAdapter);

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            selectedDate.set(year, month, dayOfMonth);
            loadEventsForSelectedDate(selectedDate);
        });

        // Initial call is handled by the LiveData observer, but keep this for immediate load if ID is already present
        if (HOME_POOL_ID != null) {
            loadEventsForSelectedDate(selectedDate);
        }

        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().finish();
            }
        });
        fabAddEvent.setOnClickListener(v -> navigateToAddEvent(null));

        setupEventResultListener();
    }

    private void loadEventsForSelectedDate(Calendar date) {
        final String TAG = "PO_Calendar";
        if (getContext() == null || HOME_POOL_ID == null) return;

        SimpleDateFormat headerFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        String selectedDateString = headerFormat.format(date.getTime());
        tvEventsHeader.setText("Events for " + selectedDateString);

        // 1. Prepare start of day (00:00:00)
        Calendar startOfDay = (Calendar) date.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        long startTime = startOfDay.getTimeInMillis();

        // 2. Prepare end of day (23:59:59)
        Calendar endOfDay = (Calendar) date.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);
        long endTime = endOfDay.getTimeInMillis();

        // 3. Firestore Query
        db.collection("events")
                .whereEqualTo("poolId", HOME_POOL_ID)
                .whereGreaterThanOrEqualTo("startDate", startTime)
                .whereLessThanOrEqualTo("startDate", endTime)
                .orderBy("startDate", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;

                    eventList.clear();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            EventModel event = document.toObject(EventModel.class);
                            event.id = document.getId();
                            eventList.add(event);
                        }
                    }

                    if (!eventList.isEmpty()) {
                        eventAdapter.updateList(eventList);
                        rvEvents.setVisibility(View.VISIBLE);
                        tvNoEvents.setVisibility(View.GONE);
                    } else {
                        eventAdapter.updateList(eventList);
                        rvEvents.setVisibility(View.GONE);
                        tvNoEvents.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void setupEventResultListener() {
        getParentFragmentManager().setFragmentResultListener(REQUEST_KEY_EVENT_UPDATED, this, (requestKey, bundle) -> {
            if (requestKey.equals(REQUEST_KEY_EVENT_UPDATED)) {
                Toast.makeText(getContext(), "Events list reloaded.", Toast.LENGTH_SHORT).show();

                Calendar dateToReload = Calendar.getInstance();
                dateToReload.setTimeInMillis(calendarView.getDate());
                loadEventsForSelectedDate(dateToReload);
            }
        });
    }

    @Override
    public void onEventClick(EventModel event) {
        navigateToAddEvent(event.id);
    }

    private void navigateToAddEvent(@Nullable String eventId) {
        if (HOME_POOL_ID == null) {
            Toast.makeText(getContext(), "Cannot add event: Pool ID not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        PO_AddEvent addEventFragment = new PO_AddEvent();
        Bundle args = new Bundle();
        args.putString("POOL_ID", HOME_POOL_ID);

        if (eventId != null) {
            args.putString(ARG_EVENT_ID, eventId);
        }
        addEventFragment.setArguments(args);
        if (getActivity() != null) {
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, addEventFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
}