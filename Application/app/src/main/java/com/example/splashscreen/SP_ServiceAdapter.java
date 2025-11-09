package com.example.splashscreen.ui.adapters; // Adjust package as needed

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.R; // Ensure R points to your resource file
import com.example.splashscreen.data.models.Service;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Locale;

public class SP_ServiceAdapter extends RecyclerView.Adapter<SP_ServiceAdapter.ServiceViewHolder> {

    private final Context context;
    private final List<Service> serviceList;
    private final OnServiceClickListener listener;

    public interface OnServiceClickListener {
        void onEditClick(Service service);
        void onDeleteClick(Service service);
    }

    public SP_ServiceAdapter(Context context, List<Service> serviceList, OnServiceClickListener listener) {
        this.context = context;
        this.serviceList = serviceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sp_service, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        Service service = serviceList.get(position);

        holder.tvName.setText(service.getName());
        holder.tvPrice.setText(String.format(Locale.getDefault(), "R %.2f", service.getPrice()));
        holder.tvDuration.setText(String.format(Locale.getDefault(), "%d min", service.getDurationMinutes()));

        // Load image using Picasso
        if (service.getImageUrl() != null && !service.getImageUrl().isEmpty()) {
            Picasso.get().load(service.getImageUrl())
                    .placeholder(R.drawable.placeholder_service) // Add your placeholder drawable
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.placeholder_service);
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(service));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(service));
    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }

    public static class ServiceViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public TextView tvName;
        public TextView tvPrice;
        public TextView tvDuration;
        public Button btnEdit;
        public Button btnDelete;

        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.service_image);
            tvName = itemView.findViewById(R.id.service_name);
            tvPrice = itemView.findViewById(R.id.service_price);
            tvDuration = itemView.findViewById(R.id.service_duration);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}