package com.example.splashscreen;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private final List<ItemModel> itemList;

    public ItemAdapter(List<ItemModel> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflates the reusable card layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_list_item_generic, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ItemModel item = itemList.get(position);

        holder.titleTextView.setText(item.getTitle());
        holder.subTextView.setText(item.getSubText());
        holder.imageView.setImageResource(item.getImageResId());


        holder.itemView.setOnClickListener(v -> {
            // Log.d("ItemAdapter", "Clicked on: " + item.getTitle());
        });
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
            // Link the ViewHolder variables to the IDs in card_list_item_generic.xml
            imageView = itemView.findViewById(R.id.iv_item_image);
            titleTextView = itemView.findViewById(R.id.tv_item_title);
            subTextView = itemView.findViewById(R.id.tv_item_subtext);
        }
    }
}
