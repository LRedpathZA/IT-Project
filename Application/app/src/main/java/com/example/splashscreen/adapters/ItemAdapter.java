package com.example.splashscreen.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.R;
import com.example.splashscreen.data.models.ItemModel;
import com.example.splashscreen.utils.ProfilePictureManager;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private final List<ItemModel> itemList;
    private Context context;

    public ItemAdapter(List<ItemModel> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ⭐ Initialize Context here
        this.context = parent.getContext();

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_generic_list_card, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ItemModel item = itemList.get(position);

        holder.titleTextView.setText(item.getTitle());
        holder.subTextView.setText(item.getSubText());

        // ⭐ UPDATED IMAGE LOADING LOGIC
        String imageUrl = item.getImageUrl();
        int imageResId = item.getImageResId();

        if (imageUrl != null && !imageUrl.isEmpty() && context != null) {
            ProfilePictureManager.loadPicture(
                    context,
                    imageUrl,
                    holder.imageView,
                    R.drawable.ic_profile_placeholder // Fallback to a generic placeholder
            );
        } else if (imageResId != 0) {
            holder.imageView.setImageResource(imageResId);
        } else {
            holder.imageView.setImageResource(R.drawable.ic_profile_placeholder);
        }


        holder.itemView.setOnClickListener(v -> {

        });
    }

    public void updateList(List<ItemModel> newList) {
        this.itemList.clear();
        this.itemList.addAll(newList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;
        TextView subTextView;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_item_image);
            titleTextView = itemView.findViewById(R.id.tv_item_title);
            subTextView = itemView.findViewById(R.id.tv_item_subtext);
        }
    }
}