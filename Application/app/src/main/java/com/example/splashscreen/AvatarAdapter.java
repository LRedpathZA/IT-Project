package com.example.splashscreen; // Use your project's package

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    private final List<Integer> avatarDrawables;
    private final AvatarSelectionListener listener;
    private final Context context;

    // Constant to represent the "Custom Upload" slot
    private static final int CUSTOM_UPLOAD_RES_ID = 0;

    public AvatarAdapter(Context context, List<Integer> avatarDrawables, AvatarSelectionListener listener) {
        this.context = context;
        this.avatarDrawables = avatarDrawables;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avatar_circle, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        @DrawableRes final int resId = avatarDrawables.get(position);

        if (resId == CUSTOM_UPLOAD_RES_ID) {
            // Special Case: Custom Upload Button
            holder.avatarImage.setImageResource(R.drawable.camera); // Placeholder for a camera icon
            holder.avatarImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            holder.avatarContainer.setStrokeWidth(0);
            holder.avatarImage.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_light));
        } else {
            // Default Avatar
            holder.avatarImage.setImageResource(resId);
            holder.avatarImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.avatarContainer.setStrokeWidth(2); // Set stroke for selection highlight
            holder.avatarImage.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAvatarSelected(resId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return avatarDrawables.size();
    }

    public static class AvatarViewHolder extends RecyclerView.ViewHolder {
        final ImageView avatarImage;
        final MaterialCardView avatarContainer;

        public AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.iv_avatar_image);
            avatarContainer = itemView.findViewById(R.id.cv_avatar_container);
        }
    }
}