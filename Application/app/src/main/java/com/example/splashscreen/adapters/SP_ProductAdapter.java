package com.example.splashscreen.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.R;
import com.example.splashscreen.data.models.ProductModel;
import com.google.android.material.button.MaterialButton;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SP_ProductAdapter extends RecyclerView.Adapter<SP_ProductAdapter.ProductViewHolder> {

    private static final String TAG = "SP_ProductAdapter";

    // ⭐ Threading setup for custom image loading
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ProductActionListener {
        void onEditClick(ProductModel product);
        void onDeleteClick(String productId);
    }

    private final Context context;
    private final List<ProductModel> productList;
    private final ProductActionListener listener;

    public SP_ProductAdapter(Context context, List<ProductModel> productList, ProductActionListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sp_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductModel product = productList.get(position);

        holder.productName.setText(product.getName());
        holder.productPrice.setText(String.format(Locale.getDefault(), "R %.2f", product.getPrice()));

        String stockText = String.format(Locale.getDefault(), "Stock: %.2f %s", product.getQuantity(), product.getUnit());
        holder.productStock.setText(stockText);

        // ⭐ Custom Image Loading Logic
        String photoUrl = product.getPhotoUrl();

        if (!TextUtils.isEmpty(photoUrl)) {
            // ⭐ Set tag for recycling check
            holder.productImage.setTag(photoUrl);
            // Load the image in the background
            loadProductImage(holder.productImage, photoUrl);
        } else {
            // Display Placeholder immediately
            holder.productImage.setImageResource(R.drawable.fake_chlorine);
            holder.productImage.setTag(null);
        }

        // Set listeners for actions
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(product));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(product.getProductId()));
    }

    /**
     * Executes the image loading from a URL in a background thread.
     * @param imageView The ImageView to update.
     * @param url The image URL.
     */
    private void loadProductImage(ImageView imageView, String url) {
        // Show placeholder while loading
        imageView.setImageResource(R.drawable.fake_chlorine);

        executorService.execute(() -> {
            Bitmap bitmap = null;
            try {
                InputStream in = new URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(in);
                Log.d(TAG, "Successfully decoded bitmap from URL: " + url);
            } catch (Exception e) {
                Log.e(TAG, "Error loading bitmap from URL: " + url + " - " + e.getMessage());
            }

            Bitmap finalBitmap = bitmap;

            // Update the UI on the main thread
            mainHandler.post(() -> {
                // ⭐ CRUCIAL: Check if the ViewHolder has been recycled
                // The ImageView's tag must still match the URL we fetched
                if (url.equals(imageView.getTag())) {
                    if (finalBitmap != null) {
                        imageView.setImageBitmap(finalBitmap);
                    } else {
                        // Fallback to placeholder if load failed
                        imageView.setImageResource(R.drawable.fake_chlorine);
                    }
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void updateList(List<ProductModel> newList) {
        this.productList.clear();
        this.productList.addAll(newList);
        notifyDataSetChanged();
    }

    public void shutdownExecutor() {
        executorService.shutdown();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productPrice, productStock;
        MaterialButton btnEdit, btnDelete;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            // Assuming these IDs exist in R.layout.item_sp_product
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            productStock = itemView.findViewById(R.id.product_stock);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}