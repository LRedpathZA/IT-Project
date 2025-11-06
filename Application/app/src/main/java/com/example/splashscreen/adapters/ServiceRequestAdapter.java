package com.example.splashscreen.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.R;
import com.example.splashscreen.data.models.ServiceRequestModel;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ServiceRequestAdapter extends RecyclerView.Adapter<ServiceRequestAdapter.ServiceRequestViewHolder> {

    private final List<ServiceRequestModel> requestList;
    private final OnRequestClickListener clickListener;
    private final Context context;

    // Interface to handle all click events from the list item
    public interface OnRequestClickListener {
        void onRequestClick(ServiceRequestModel request);
        void onMenuClick(ServiceRequestModel request, View anchorView);
    }

    public ServiceRequestAdapter(Context context, List<ServiceRequestModel> requestList, OnRequestClickListener clickListener) {
        this.context = context;
        this.requestList = requestList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ServiceRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service_request_card, parent, false);
        return new ServiceRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceRequestViewHolder holder, int position) {
        ServiceRequestModel request = requestList.get(position);
        holder.bind(request);
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static String getTimeAgo(long time) {
        long diff = System.currentTimeMillis() - time;
        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Just now";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            return TimeUnit.MILLISECONDS.toMinutes(diff) + "m ago";
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            return TimeUnit.MILLISECONDS.toHours(diff) + "h ago";
        } else {
            return TimeUnit.MILLISECONDS.toDays(diff) + " days ago";
        }
    }

    public class ServiceRequestViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRequestType;
        private final TextView tvRequestStatus;
        private final TextView tvPoolName;
        private final TextView tvDatePosted;
        private final ImageButton btnRequestMenu;

        public ServiceRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRequestType = itemView.findViewById(R.id.tv_request_type);
            tvRequestStatus = itemView.findViewById(R.id.tv_request_status);
            tvPoolName = itemView.findViewById(R.id.tv_pool_name);
            tvDatePosted = itemView.findViewById(R.id.tv_date_posted);
            btnRequestMenu = itemView.findViewById(R.id.btn_request_menu);

            // Set the primary click listener for the entire card
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    clickListener.onRequestClick(requestList.get(position));
                }
            });

            // Set the click listener for the menu button
            btnRequestMenu.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    clickListener.onMenuClick(requestList.get(position), v);
                }
            });
        }

        public void bind(ServiceRequestModel request) {
            tvRequestType.setText(request.getServiceType());
            tvPoolName.setText(String.format("Pool: %s", request.getPoolName()));

            // 1. Format Status Text
            String statusText;
            int quoteCount = request.getQuoteCount();

            if (request.getStatus().equals("Open") && quoteCount > 0) {
                statusText = String.format("Quotes Received (%d)", quoteCount);
            } else if (request.getStatus().equals("Open")) {
                statusText = "Awaiting Quotes";
            } else {
                statusText = request.getStatus(); // e.g., "Booked", "Expired"
            }
            tvRequestStatus.setText(statusText);

            // 2. Format Status Background and Color (Uses R.drawable.rounded_status_open for a base)
            int bgColorId;
            switch (request.getStatus()) {
                case "Booked":
                    bgColorId = ContextCompat.getColor(context, R.color.colorPrimary); // Use a primary color for booked
                    break;
                case "Expired":
                    bgColorId = ContextCompat.getColor(context, android.R.color.darker_gray); // Gray for expired
                    break;
                case "Open":
                default:
                    // Assuming R.color.green_status is a suitable green for open requests
                    // Using the green color provided in the previous step's shape
                    bgColorId = Color.parseColor("#4CAF50");
                    break;
            }
            // Note: If you want different drawables, you need to use setBackgroundResource
            // Here, we'll just tint the existing R.drawable.rounded_status_open
            tvRequestStatus.setBackgroundTintList(ColorStateList.valueOf(bgColorId));


            // 3. Format Date Text (Time Posted and Expiry)
            String timeAgo = getTimeAgo(request.getCreatedAt());
            long timeRemaining = request.getExpiryDate() - System.currentTimeMillis();

            String expiryText;
            if (timeRemaining <= 0 || request.getStatus().equals("Expired")) {
                expiryText = "EXPIRED";
            } else if (timeRemaining < TimeUnit.DAYS.toMillis(1)) {
                expiryText = String.format("Expires: %d hours left", TimeUnit.MILLISECONDS.toHours(timeRemaining));
            } else {
                expiryText = String.format("Expires in %d days", TimeUnit.MILLISECONDS.toDays(timeRemaining));
            }

            tvDatePosted.setText(String.format("Posted: %s | %s", timeAgo, expiryText));
        }
    }
}