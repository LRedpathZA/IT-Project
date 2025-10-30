package com.example.splashscreen.adapters; // 💥 NEW PACKAGE

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.R;
import com.example.splashscreen.data.models.ClientModel;

import java.util.List;

public class ClientListAdapter extends RecyclerView.Adapter<ClientListAdapter.ClientViewHolder> {

    private final List<ClientModel> clientList;
    private final OnClientClickListener listener;

    public interface OnClientClickListener {
        void onClientClick(ClientModel client);
    }

    public ClientListAdapter(List<ClientModel> clientList, OnClientClickListener listener) {
        this.clientList = clientList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_client_list, parent, false);
        return new ClientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClientViewHolder holder, int position) {
        ClientModel client = clientList.get(position);
        holder.bind(client, listener);
    }

    @Override
    public int getItemCount() {
        return clientList.size();
    }

    // Optional: Method to update data set
    public void setClientList(List<ClientModel> newClientList) {
        this.clientList.clear();
        this.clientList.addAll(newClientList);
        notifyDataSetChanged();
    }

    static class ClientViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView name;
        TextView description;
        TextView status;

        ClientViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.iv_client_avatar);
            name = itemView.findViewById(R.id.tv_client_name);
            description = itemView.findViewById(R.id.tv_client_description);
            status = itemView.findViewById(R.id.tv_client_status);
        }

        public void bind(final ClientModel client, final OnClientClickListener listener) {
            name.setText(client.getName());
            description.setText(client.getDescription());

            // ⚠️ Placeholder image for now. Later use a library like Glide/Picasso for client.getAvatarUrl()
            avatar.setImageResource(R.drawable.ic_profile_placeholder);

            // Handle Active Status Chip appearance
            if (client.isActive()) {
                status.setText("Active");
                status.setVisibility(View.VISIBLE);
                // Set green background (assuming bg_status_active.xml is green/teal)
                status.setTextColor(Color.WHITE);
            } else {
                // If you want a 'New' or 'Inactive' status, you can check for that here.
                status.setVisibility(View.GONE);
            }

            // Handle item click to open the client profile
            itemView.setOnClickListener(v -> listener.onClientClick(client));
        }
    }
}