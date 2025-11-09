package com.example.splashscreen.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.R; // Ensure your R file is correct
import com.example.splashscreen.data.models.Product;
import com.squareup.picasso.Picasso; // You'll need Picasso/Glide for image loading

import java.util.List;
import java.util.Locale;

public class SP_ProductAdapter extends RecyclerView.Adapter<SP_ProductAdapter.ProductViewHolder> {

    public interface ProductActionListener {
        void onEditClick(Product product);
        void onDeleteClick(String productId);
    }

    private final Context context;
    private final List<Product> productList;
    private final ProductActionListener listener;

    public SP_ProductAdapter(Context context, List<Product> productList, ProductActionListener listener) {
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
        Product product = productList.get(position);

        holder.productName.setText(product.getName());
        holder.productPrice.setText(String.format(Locale.getDefault(), "R %.2f", product.getPrice()));
        holder.productStock.setText(String.format(Locale.getDefault(), "Stock: %d", product.getStock()));

        // Load image using Picasso (or Glide)
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Picasso.get().load(product.getImageUrl())
                    .placeholder(R.drawable.placeholder_product) // Add a placeholder drawable
                    .into(holder.productImage);
        } else {
            holder.productImage.setImageResource(R.drawable.placeholder_product);
        }

        // Set listeners for actions
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(product));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(product.getId()));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    // Optional: method to update the list easily after CRUD operations
    public void updateList(List<Product> newList) {
        this.productList.clear();
        this.productList.addAll(newList);
        notifyDataSetChanged();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productPrice, productStock;
        Button btnEdit, btnDelete; // Use Button or ImageView/MaterialButton

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            productStock = itemView.findViewById(R.id.product_stock);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
