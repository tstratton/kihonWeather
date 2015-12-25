package net.devrand.kihon.kihonweather;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.devrand.kihon.kihonweather.data.AllData;
import net.devrand.kihon.kihonweather.data.Forecast;

import butterknife.ButterKnife;

/**
 * Created by tstratto on 12/24/2015.
 */
public class ForecastAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private AllData data;

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_CURRENT_CONDITIONS = 1;
    public static final int TYPE_GRAPH = 2;
    public static final int TYPE_FORECAST = 3;

    public static final int FORECAST_FIRST_INDEX = 1;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_FORECAST:
                return new ForecastHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.forecast_item, parent, false));
            case TYPE_CURRENT_CONDITIONS:
                return new CurrentWeatherHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.current_weather_item, parent, false));
        }
        return null;
    }

    public boolean hasForecastData() {
        if (data == null || data.forecast == null || data.forecast.txt_forecast == null ||
                data.forecast.txt_forecast.forecastday == null || data.forecast.txt_forecast.forecastday.size() < 1) {
            return false;
        }
        return true;
    }

    @Override
    public int getItemCount() {
        if (hasForecastData()) {
            return 1 + data.forecast.txt_forecast.forecastday.size();//FIXME 2 + data.forecast.txt_forecast.forecastday.size();
        } else {
            return 2;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= FORECAST_FIRST_INDEX) {
            return TYPE_FORECAST;
        }
        if (position == 0) {
            return TYPE_CURRENT_CONDITIONS;
        }
        if (position == 1) {
            return TYPE_GRAPH;
        }
        return TYPE_UNKNOWN;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position >= FORECAST_FIRST_INDEX) {
            ((ForecastHolder) holder).setup(data.forecast.txt_forecast.forecastday.get(position - FORECAST_FIRST_INDEX));
        }
        if (position == 0) {
            ((CurrentWeatherHolder) holder).setup(data);
        }
    }

    public void addData(AllData data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public class CurrentWeatherHolder extends RecyclerView.ViewHolder {

        TextView text_location;
        TextView text_status;
        TextView text_sunrise;
        TextView text_sunset;
        TextView text_temperture;

        CurrentWeatherHolder(View row) {
            super(row);
            text_location = ButterKnife.findById(row, R.id.location_text);
            text_status = ButterKnife.findById(row, R.id.status_text);
            text_sunrise = ButterKnife.findById(row, R.id.sunrise_text);
            text_sunset = ButterKnife.findById(row, R.id.sunset_text);
            text_temperture = ButterKnife.findById(row, R.id.temperture_text);
        }

        void setup(AllData data) {
            text_location.setText(data.current_observation.display_location.full);
            text_temperture.setText(data.current_observation.temperature_string);
            text_status.setText(data.current_observation.weather);
            text_sunrise.setText("Sunrise: " + data.sun_phase.getSunrise());
            text_sunset.setText("Sunset: " + data.sun_phase.getSunset());
        }
    }

    public class ForecastHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView description;

        ForecastHolder(View row) {
            super(row);
            title = ButterKnife.findById(row, R.id.forecast_title);
            description = ButterKnife.findById(row, R.id.forecast_description);
        }

        void setup(Forecast.TextForecastDay data) {
            title.setText(data.title);
            description.setText(data.fcttext);
        }
    }
}
