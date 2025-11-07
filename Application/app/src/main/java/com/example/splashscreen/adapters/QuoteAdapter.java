package com.example.splashscreen.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.R;
import com.example.splashscreen.data.models.QuoteModel;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class QuoteAdapter extends RecyclerView.Adapter<QuoteAdapter.QuoteViewHolder> {

    private final List<QuoteModel> quoteList;
    private final OnQuoteClickListener clickListener;
    private final Context context;
    private final NumberFormat currencyFormat;

    // Interface to handle click event
    public interface OnQuoteClickListener {
        void onQuoteClick(QuoteModel quote);
    }
    public QuoteAdapter(Context context, List<QuoteModel> quoteList, OnQuoteClickListener clickListener) {
        this.context = context;
        this.quoteList = quoteList;
        this.clickListener = clickListener;
        // Assuming South African Rand (R) formatting for currency
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "ZA"));
        this.currencyFormat.setMaximumFractionDigits(2);
    }

    @NonNull
    @Override
    public QuoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quote_card, parent, false);
        return new QuoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuoteViewHolder holder, int position) {
        QuoteModel quote = quoteList.get(position);
        holder.bind(quote);
    }

    @Override
    public int getItemCount() {
        return quoteList.size();
    }

    // Utility method (re-used from ServiceRequestAdapter logic)
    public static String getTimeAgo(long time) {
        long diff = System.currentTimeMillis() - time;
        if (diff < TimeUnit.MINUTES.toMillis(1)) return "Just now";
        if (diff < TimeUnit.HOURS.toMillis(1)) return TimeUnit.MILLISECONDS.toMinutes(diff) + "m ago";
        if (diff < TimeUnit.DAYS.toMillis(1)) return TimeUnit.MILLISECONDS.toHours(diff) + "h ago";
        return TimeUnit.MILLISECONDS.toDays(diff) + " days ago";
    }

    public class QuoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvQuoteBusinessName;
        private final TextView tvQuotePrice;
        private final TextView tvQuoteDescriptionShort;
        private final TextView tvQuoteDate;
        private final TextView tvQuoteStatus;

        public QuoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuoteBusinessName = itemView.findViewById(R.id.tv_quote_business_name);
            tvQuotePrice = itemView.findViewById(R.id.tv_quote_price);
            tvQuoteDescriptionShort = itemView.findViewById(R.id.tv_quote_description_short);
            tvQuoteDate = itemView.findViewById(R.id.tv_quote_date);
            tvQuoteStatus = itemView.findViewById(R.id.tv_quote_status);

            // Set the click listener for the entire card
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    clickListener.onQuoteClick(quoteList.get(position));
                }
            });
        }

        public void bind(QuoteModel quote) {
            tvQuoteBusinessName.setText(quote.getBusinessName());

            // Format price as currency (e.g., R 850.00)
            tvQuotePrice.setText(currencyFormat.format(quote.getQuotedPrice()));

            tvQuoteDescriptionShort.setText(quote.getDetailedDescription());

            // Format time posted
            if (quote.getCreatedAt() != null) {
                String timeAgo = getTimeAgo(quote.getCreatedAt().getTime());
                tvQuoteDate.setText(String.format("Quoted: %s", timeAgo));
            } else {
                tvQuoteDate.setText("Quoted: N/A");
            }

            // Set Status Text and Color
            String status = quote.getStatus();
            tvQuoteStatus.setText(status);

            int bgColorId;
            int textColor = ContextCompat.getColor(context, android.R.color.white);

            switch (status) {
                case "Accepted":
                    // Use primary/success color
                    bgColorId = ContextCompat.getColor(context, R.color.green_icon_bg);
                    break;
                case "Rejected":
                    // Use warning/red color
                    bgColorId = ContextCompat.getColor(context, R.color.red_icon_bg);
                    break;
                case "New":
                default:
                    // Use a neutral/blue color
                    bgColorId = ContextCompat.getColor(context, R.color.blue_icon_bg);
                    break;
            }
            // Note: You must define R.color.green_status, R.color.red_status, and R.color.blue_status
            tvQuoteStatus.setBackgroundTintList(ColorStateList.valueOf(bgColorId));
            tvQuoteStatus.setTextColor(textColor);
        }
    }
}