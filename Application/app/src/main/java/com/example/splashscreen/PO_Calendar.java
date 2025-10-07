package com.example.splashscreen;

import android.os.Bundle;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PO_Calendar extends Fragment {

    public static final String REQUEST_KEY_EVENT_UPDATED = "event_updated_key";
    public static final String BUNDLE_KEY_EVENT_ID = "event_id";
    public static final String ARG_EVENT_ID = "EVENT_ID";

    private CalendarView calendarView;
    private RecyclerView rvEvents;
    private View tvNoEvents;
    private TextView tvEventsHeader;

    private FirebaseFirestore db;
    private Calendar selectedDate = Calendar.getInstance();

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

        // FIX: The CalendarView ID should match your XML (R.id.calendar_view)
        calendarView = view.findViewById(R.id.calendar_view);
        rvEvents = view.findViewById(R.id.rv_events);
        tvNoEvents = view.findViewById(R.id.tv_no_events);
        tvEventsHeader = view.findViewById(R.id.tv_events_header);

        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            // Calendar month is 0-indexed (Jan=0), Calendar.set handles this correctly.
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
        if (getContext() == null) return;

        SimpleDateFormat headerFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        String selectedDateString = headerFormat.format(date.getTime());
        tvEventsHeader.setText("Events for " + selectedDateString);

        // TODO: Implement Firestore query logic here.
        // db.collection("events").whereEqualTo("date", selectedDate).get()...

        boolean eventsFound = false; // Replace with result from Firestore query

        if (eventsFound) {
            rvEvents.setVisibility(View.VISIBLE);
            tvNoEvents.setVisibility(View.GONE);
            // TODO: Update your RecyclerView adapter with fetched events
        } else {
            rvEvents.setVisibility(View.GONE);
            tvNoEvents.setVisibility(View.VISIBLE);
        }
    }

    private void setupEventResultListener() {
        getParentFragmentManager().setFragmentResultListener(REQUEST_KEY_EVENT_UPDATED, this, (requestKey, bundle) -> {
            if (requestKey.equals(REQUEST_KEY_EVENT_UPDATED)) {
                Toast.makeText(getContext(), "Events updated. Reloading list.", Toast.LENGTH_SHORT).show();

                Calendar dateToReload = Calendar.getInstance();
                dateToReload.setTimeInMillis(calendarView.getDate());
                loadEventsForSelectedDate(dateToReload);
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