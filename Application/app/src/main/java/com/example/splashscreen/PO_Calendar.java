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
import java.util.TimeZone;

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
    private Calendar selectedDate = Calendar.getInstance();
    private EventAdapter eventAdapter;
    private List<EventModel> eventList = new ArrayList<>();


    // PLACEHOLDER: Replace with actual pool ID fetched from user profile/preferences
    private  String HOME_POOL_ID = "YOUR_CURRENT_HOME_POOL_ID";

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

        ImageButton btnBack = view.findViewById(R.id.btn_back);
        FloatingActionButton fabAddEvent = view.findViewById(R.id.fab_add_event);
        if (getArguments() != null) {
            HOME_POOL_ID = getArguments().getString("POOL_ID");
            if (HOME_POOL_ID == null) {
                Toast.makeText(getContext(), "Cannot load event screen: Pool ID missing.", Toast.LENGTH_LONG).show();
            }
        }

        calendarView = view.findViewById(R.id.calendar_view);
        rvEvents = view.findViewById(R.id.rv_events);
        tvNoEvents = view.findViewById(R.id.tv_no_events);
        tvEventsHeader = view.findViewById(R.id.tv_events_header);

        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        eventAdapter = new EventAdapter(eventList, this); // 'this' is the OnEventClickListener
        rvEvents.setAdapter(eventAdapter);

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            selectedDate.set(year, month, dayOfMonth);
            loadEventsForSelectedDate(selectedDate);
        });


        loadEventsForSelectedDate(selectedDate);

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
        final String TAG = "PO_Calendar"; //
        if (getContext() == null || HOME_POOL_ID.equals("YOUR_CURRENT_HOME_POOL_ID")) return;

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
        Log.d(TAG, "Query Start Time (ms): " + startTime);
        Log.d(TAG, "Query Start Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(startOfDay.getTime()));

        // 2. Prepare end of day (23:59:59)
        Calendar endOfDay = (Calendar) date.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);
        long endTime = endOfDay.getTimeInMillis();
        Log.d(TAG, "Query End Time (ms): " + endTime);
        Log.d(TAG, "Query End Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(endOfDay.getTime()));


        // 3. Firestore Query: Find events for the home pool where the event's start date is between the start and end of the selected day.
        db.collection("events")
                .whereEqualTo("poolId", HOME_POOL_ID)
                .whereGreaterThanOrEqualTo("startDate", startTime)
                .whereLessThanOrEqualTo("startDate", endTime)
                .orderBy("startDate", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return; // Prevent crash if fragment detached

                    eventList.clear();
                    if (task.isSuccessful() && task.getResult() != null) {
                        Log.d(TAG, "Query successful. Documents found: " + task.getResult().size()); // LOG 1: Documents found
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            EventModel event = document.toObject(EventModel.class);
                            event.id = document.getId(); // Set the document ID
                            eventList.add(event);
                            Log.d(TAG, "Event found: " + event.title + " StartDate: " + event.startDate);
                        }
                    }
                    else {
                        Log.e(TAG, "Query failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
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

                // Get the date from the CalendarView's current state to reload
                Calendar dateToReload = Calendar.getInstance();
                dateToReload.setTimeInMillis(calendarView.getDate());
                loadEventsForSelectedDate(dateToReload);
            }
        });
    }

    // Handles click on an event in the list -> Navigates to edit screen
    @Override
    public void onEventClick(EventModel event) {
        navigateToAddEvent(event.id);
    }

    // In PO_Calendar.java

    private void navigateToAddEvent(@Nullable String eventId) {
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