package com.example.splashscreen;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.R; // Ensure this is your correct R package
import com.example.splashscreen.data.weather.DailyForecast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyForecastAdapter extends RecyclerView.Adapter<DailyForecastAdapter.ViewHolder> {

    private final List<DailyForecast> dailyForecasts;
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault()); // EEE for Mon, Tue, etc.

    public DailyForecastAdapter(List<DailyForecast> dailyForecasts) {
        this.dailyForecasts = dailyForecasts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 1. Inflate item_daily_forecast.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_forecast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyForecast forecast = dailyForecasts.get(position);

        // Convert Unix timestamp (seconds) to Date object (milliseconds)
        Date date = new Date(forecast.getDatetime() * 1000L);

        // 2. Bind the day of the week
        holder.tvForecastDay.setText(dayFormat.format(date));

        // 3. Bind the temperature range
        String minTemp = String.format(Locale.getDefault(), "%.0f°", forecast.getTemperature().getMinTemp());
        String maxTemp = String.format(Locale.getDefault(), "%.0f°", forecast.getTemperature().getMaxTemp());

        holder.tvForecastMinTemp.setText(minTemp);
        holder.tvForecastMaxTemp.setText(maxTemp);

        // 4. Set the weather icon (Requires mapping from OWM code to a local drawable)
        // NOTE: This assumes you have local drawable resources named based on the OWM icon code (e.g., 'ic_01d')
        String iconCode = forecast.getWeather().get(0).getIconCode();
        int iconResId = holder.itemView.getContext().getResources().getIdentifier(
                "ic_" + iconCode, "drawable", holder.itemView.getContext().getPackageName());

        // Use a default icon if the specific one is not found
        if (iconResId != 0) {
            holder.ivForecastIcon.setImageResource(iconResId);
        } else {
            holder.ivForecastIcon.setImageResource(R.drawable.sunny); // Fallback icon
        }
    }

    @Override
    public int getItemCount() {
        return dailyForecasts.size();
    }

    /**
     * ViewHolder holds the view references for each item.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvForecastDay;
        final ImageView ivForecastIcon;
        final TextView tvForecastMinTemp;
        final TextView tvForecastMaxTemp;

        public ViewHolder(View view) {
            super(view);
            tvForecastDay = view.findViewById(R.id.tv_forecast_day);
            ivForecastIcon = view.findViewById(R.id.iv_forecast_icon);
            tvForecastMinTemp = view.findViewById(R.id.tv_forecast_min_temp);
            tvForecastMaxTemp = view.findViewById(R.id.tv_forecast_max_temp);
        }
    }
}