package com.example.splashscreen.data.weather;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.splashscreen.R;
import com.example.splashscreen.utils.IconMapper;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyForecastAdapter extends RecyclerView.Adapter<DailyForecastAdapter.DailyForecastViewHolder> {

    private final List<DailyForecast> dailyForecasts;
    // Optional: private OnItemClickListener listener;

    public DailyForecastAdapter(List<DailyForecast> dailyForecasts) {
        this.dailyForecasts = dailyForecasts;
    }

    // Optional: public interface OnItemClickListener { void onItemClick(DailyForecast forecast); }
    // Optional: public void setOnItemClickListener(OnItemClickListener listener) { this.listener = listener; }

    @NonNull
    @Override
    public DailyForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_daily_forecast, parent, false);
        return new DailyForecastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DailyForecastViewHolder holder, int position) {
        DailyForecast forecast = dailyForecasts.get(position);
        holder.bind(forecast);

        // holder.itemView.setOnClickListener(v -> {
        //     if (listener != null) listener.onItemClick(forecast);
        // });
    }

    @Override
    public int getItemCount() {
        return dailyForecasts.size();
    }

    public static class DailyForecastViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDayOfWeek;
        private final ImageView ivDayForecastIcon;
        private final TextView tvDayForecastTemp;
        private final TextView tvDayForecastDesc;

        public DailyForecastViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayOfWeek = itemView.findViewById(R.id.tv_day_of_week);
            ivDayForecastIcon = itemView.findViewById(R.id.iv_day_forecast_icon);
            tvDayForecastTemp = itemView.findViewById(R.id.tv_day_forecast_temp);
            tvDayForecastDesc = itemView.findViewById(R.id.tv_day_forecast_desc); // 💥 BIND NEW FIELD
        }

        public void bind(DailyForecast forecast) {
            // Day of Week
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            String dayText = dateFormat.format(new Date(forecast.getDateMillis()));
            tvDayOfWeek.setText(dayText);

            // Temperature (Using Max Temp for the main display)
            tvDayForecastTemp.setText(String.format(Locale.getDefault(), "%.0f°C", forecast.getMaxTemp()));

            // Description 💥 NEW
            tvDayForecastDesc.setText(forecast.getWeatherDescription());
            ivDayForecastIcon.setImageResource(IconMapper.getIcon(forecast.getIconCode()));
        }
    }
}