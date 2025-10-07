package com.example.splashscreen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView; // Import the CalendarView
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

// Import for RecyclerView setup
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PO_Calendar extends Fragment {

    public static final String REQUEST_KEY_EVENT_UPDATED = "event_updated_key";
    public static final String BUNDLE_KEY_EVENT_ID = "event_id";
    public static final String ARG_EVENT_ID = "EVENT_ID";

    // New Member Variables
    private CalendarView calendarView;
    private RecyclerView rvEvents;
    private View tvNoEvents; // The "No events" TextView

    public PO_Calendar() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.po_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnBack = view.findViewById(R.id.btn_back);
        FloatingActionButton fabAddEvent = view.findViewById(R.id.fab_add_event);

        // Initialize Calendar and Event List views
        calendarView = view.findViewById(R.id.calendar_view);
        rvEvents = view.findViewById(R.id.rv_events);
        tvNoEvents = view.findViewById(R.id.tv_no_events);

        // Setup RecyclerView
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        // rvEvents.setAdapter(new EventAdapter(events, this)); // TODO: Use a proper Adapter

        // Setup Calendar Date Change Listener
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            loadEventsForSelectedDate(selectedDate);
        });

        // Load events for today when the fragment starts
        loadEventsForSelectedDate(Calendar.getInstance());

        // Setup back navigation
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().finish();
            }
        });

        // Setup Add Event FAB
        fabAddEvent.setOnClickListener(v -> navigateToAddEvent(null));

        // Listen for results from PO_AddEvent (when an event is saved/deleted)
        setupEventResultListener();
    }

    // =========================================================================================
    //                                  EVENT LOADING / DISPLAY
    // =========================================================================================

    private void loadEventsForSelectedDate(Calendar date) {
        if (getContext() == null) return;

        // Format the selected date for logging/querying Firestore
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String selectedDateString = sdf.format(date.getTime());

        // Debug Toast to confirm date selection works
        Toast.makeText(getContext(), "Loading events for: " + selectedDateString, Toast.LENGTH_SHORT).show();

        // 1. TODO: Query Firestore for events on this date and for the current user's pool
        // db.collection("events")
        //    .whereEqualTo("poolId", currentPoolId)
        //    .whereEqualTo("startDate", selectedDateString)
        //    .get()
        //    .addOnSuccessListener(...)

        // 2. SIMULATED EVENT LIST UPDATE
        // Assuming your query returns a list of Event objects (e.g., eventList)

        // For demonstration, we'll just show the empty state:
        boolean eventsFound = false; // Set this based on your Firestore result

        if (eventsFound) {
            rvEvents.setVisibility(View.VISIBLE);
            tvNoEvents.setVisibility(View.GONE);
            // TODO: Update your RecyclerView adapter here
        } else {
            rvEvents.setVisibility(View.GONE);
            tvNoEvents.setVisibility(View.VISIBLE);
        }
    }

    private void setupEventResultListener() {
        getParentFragmentManager().setFragmentResultListener(REQUEST_KEY_EVENT_UPDATED, this, (requestKey, bundle) -> {
            if (requestKey.equals(REQUEST_KEY_EVENT_UPDATED)) {
                // When an event is updated/added/deleted, reload the calendar view for the currently selected date.
                Toast.makeText(getContext(), "Calendar updated. Reloading events.", Toast.LENGTH_SHORT).show();

                // Reload events for the currently selected date (which is usually the current time, or the last selected date)
                Calendar currentDate = Calendar.getInstance();
                currentDate.setTimeInMillis(calendarView.getDate()); // Get the date currently selected in the CalendarView
                loadEventsForSelectedDate(currentDate);
            }
        });
    }

    private void navigateToAddEvent(@Nullable String eventId) {
        PO_AddEvent addEventFragment = new PO_AddEvent();
        if (eventId != null) {
            Bundle args = new Bundle();
            args.putString(ARG_EVENT_ID, eventId);
            addEventFragment.setArguments(args);
        }

        if (getActivity() != null) {
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, addEventFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
}