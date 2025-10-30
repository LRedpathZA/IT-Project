package com.example.splashscreen;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.data.models.EventModel;
import com.example.splashscreen.data.models.PoolViewModel;
import com.example.splashscreen.data.models.UserViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PO_Calendar extends Fragment implements EventAdapter.OnEventClickListener, HeaderUpdatable {

    public static final String REQUEST_KEY_EVENT_UPDATED = "event_updated_key";
    public static final String BUNDLE_KEY_EVENT_ID = "event_id";
    public static final String ARG_EVENT_ID = "EVENT_ID";
    private static final String TAG = "PO_Calendar";

    private CalendarView calendarView;
    private RecyclerView rvEvents;
    private View tvNoEvents;
    private TextView tvEventsHeader;

    private FirebaseFirestore db;
    private PoolViewModel poolViewModel;
    private UserViewModel userViewModel; // ⭐ ADDED UserViewModel
    private String currentUserId; // ⭐ ADDED field to store the current user's ID
    private Calendar selectedDate = Calendar.getInstance();
    private EventAdapter eventAdapter;
    private List<EventModel> eventList = new ArrayList<>();

    private String HOME_POOL_ID;

    public PO_Calendar() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        // Initialize currentUserId right away using FirebaseAuth
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.po_calendar, container, false);
    }

    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateHeader("Pool Calendar", true, true);
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

        poolViewModel = new ViewModelProvider(requireActivity()).get(PoolViewModel.class);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class); // ⭐ INIT UserViewModel

        FloatingActionButton fabAddEvent = view.findViewById(R.id.fab_add_event);

        // Check if user ID is available
        if (currentUserId == null) {
            Toast.makeText(getContext(), "User authentication missing. Cannot load events.", Toast.LENGTH_LONG).show();
            return;
        }

        // --- View Initialization (similar to before) ---
        calendarView = view.findViewById(R.id.calendar_view);
        rvEvents = view.findViewById(R.id.rv_events);
        tvNoEvents = view.findViewById(R.id.tv_no_events);
        tvEventsHeader = view.findViewById(R.id.tv_events_header);

        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        eventAdapter = new EventAdapter(eventList, this);
        rvEvents.setAdapter(eventAdapter);
        // ------------------------------------------------

        // Observe Pool ID from ViewModel
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

        // Load events when the selected date changes
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            selectedDate.set(year, month, dayOfMonth);
            loadEventsForSelectedDate(selectedDate);
        });

        fabAddEvent.setOnClickListener(v -> navigateToAddEvent(null));

        setupEventResultListener();
    }

    // =========================================================================================
    //                                      EVENT LOADING FIX
    // =========================================================================================

    private void loadEventsForSelectedDate(Calendar date) {
        // ⭐ Check for currentUserId in addition to pool ID
        if (getContext() == null || HOME_POOL_ID == null || currentUserId == null) return;

        SimpleDateFormat headerFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        String selectedDateString = headerFormat.format(date.getTime());
        tvEventsHeader.setText("Events for " + selectedDateString);

        // 1. Prepare start of day (00:00:00)
        Calendar startOfDay = (Calendar) date.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        final long startTime = startOfDay.getTimeInMillis();

        // 2. Prepare end of day (23:59:59)
        Calendar endOfDay = (Calendar) date.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);
        final long endTime = endOfDay.getTimeInMillis();

        // 3. Firestore Query FIX:
        // Changed filter from "poolId" to "poolOwnerId" to match the security rule:
        // allow read: if ... resource.data.poolOwnerId == request.auth.uid
        db.collection("events")
                .whereEqualTo("poolOwnerId", currentUserId) // ⭐ FIXED: Use poolOwnerId for security rule compliance
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

                            // 4. LOCAL FILTER: Check for overlap (Event ENDS after the start of the selected day)
                            Long eventEndDate = document.getLong("endDate");

                            // If the event is related to the pool (poolId check is implicit by poolOwnerId filter
                            // if the user is only an owner of one pool, but let's re-add the poolId check
                            // for multi-pool support if necessary, though currentUserId is safer)
                            if (!document.getString("poolId").equals(HOME_POOL_ID)) {
                                // This check ensures we only show events for the currently selected pool (HOME_POOL_ID)
                                // even if the user owns multiple pools.
                                continue;
                            }

                            // 5. Apply date overlap filter
                            if (eventEndDate != null && eventEndDate >= startTime) {
                                eventList.add(event);
                            } else {
                                Log.d(TAG, "Event excluded by local date filter: " + event.id);
                            }
                        }
                    } else if (task.getException() != null) {
                        Log.e(TAG, "Error fetching events: " + task.getException().getMessage());
                        if (getContext() != null) Toast.makeText(getContext(), "Error fetching events: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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

    // =========================================================================================
    //                                      OTHER METHODS
    // =========================================================================================

    private void setupEventResultListener() {
        getParentFragmentManager().setFragmentResultListener(REQUEST_KEY_EVENT_UPDATED, this, (requestKey, bundle) -> {
            if (requestKey.equals(REQUEST_KEY_EVENT_UPDATED)) {
                Toast.makeText(getContext(), "Events list reloaded.", Toast.LENGTH_SHORT).show();

                // Reload for the currently selected date, which is already stored in selectedDate
                loadEventsForSelectedDate(selectedDate);
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