package com.example.splashscreen.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.R;
import com.example.splashscreen.data.models.ClientModel;
// ⭐ REQUIRED IMPORT for ProfilePictureManager
import com.example.splashscreen.utils.ProfilePictureManager;

import java.util.List;

public class ClientListAdapter extends RecyclerView.Adapter<ClientListAdapter.ClientViewHolder> {

    private final List<ClientModel> clientList;
    private final OnClientClickListener listener;
    private Context context; // ⭐ Added Context for ProfilePictureManager

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
        this.context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_client_list, parent, false);
        return new ClientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClientViewHolder holder, int position) {
        ClientModel client = clientList.get(position);
        holder.bind(client, listener, context);
    }

    @Override
    public int getItemCount() {
        return clientList.size();
    }

    public void updateList(List<ClientModel> newClientList) {
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


        public void bind(final ClientModel client, final OnClientClickListener listener, final Context context) {
            name.setText(client.getName());
            description.setText(client.getDescription());


            Long avatarResId = client.getAvatarResId();
            String photoUrl = client.getPhotoUrl();

            if (avatarResId != null && avatarResId > 0) {

                avatar.setImageResource(avatarResId.intValue());
                avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
            else if (photoUrl != null && !photoUrl.isEmpty()) {

                ProfilePictureManager.loadPicture(context, photoUrl, avatar, R.drawable.ic_profile_placeholder);
            }
            else {

                ProfilePictureManager.setPlaceholder(avatar);
            }


            if (client.isActive()) {
                status.setText("ACTIVE");
                status.setVisibility(View.VISIBLE);

                status.setBackgroundColor(Color.parseColor("#4CAF50")); // Example Green
                status.setTextColor(Color.WHITE);
            } else {
                status.setVisibility(View.GONE);

            }


            itemView.setOnClickListener(v -> listener.onClientClick(client));
        }
    }
}