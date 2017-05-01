package net.devrand.kihon.kihonweather;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.squareup.picasso.Picasso;

import net.devrand.kihon.kihonweather.data.AllData;
import net.devrand.kihon.kihonweather.data.Forecast;
import net.devrand.kihon.kihonweather.data.WeatherStation;
import net.devrand.kihon.kihonweather.event.StationClickEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

/**
 * Created by tstratto on 12/24/2015.
 */
public class StationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private AllData data;

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_CURRENT_CONDITIONS = 1;
    public static final int TYPE_GRAPH = 2;
    public static final int TYPE_FORECAST = 3;
    public static final int TYPE_STATION = 4;

    public static final int FORECAST_FIRST_INDEX = 2;

    public static int stationFirstIndex = 100;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_STATION:
                return new StationHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.station_item, parent, false));        }
        return null;
    }

    public boolean nullOrEmpty(List list) {
        if (list == null) {
            return true;
        }
        return list.isEmpty();
    }

    public boolean hasStationData() {
        return stationDataCount() > 0;
    }

    public int stationDataCount () {
        int count = 0;
        if (data == null || data.location == null || data.location.nearby_weather_stations == null) {
            return count;
        }
        if (data.location.nearby_weather_stations.airport != null &&
                !nullOrEmpty(data.location.nearby_weather_stations.airport.station)) {
            count = count + data.location.nearby_weather_stations.airport.station.size();
        }
        if (data.location.nearby_weather_stations.pws != null &&
                !nullOrEmpty(data.location.nearby_weather_stations.pws.station)) {
            count = count + data.location.nearby_weather_stations.pws.station.size();
        }
        return count;
    }

    @Override
    public int getItemCount() {
        return stationDataCount();
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= stationFirstIndex) {
            return TYPE_STATION;
        } else if (position >= FORECAST_FIRST_INDEX) {
            return TYPE_FORECAST;
        } else if (position == 0) {
            return TYPE_CURRENT_CONDITIONS;
        } else if (position == 1) {
            return TYPE_GRAPH;
        } else {
            return TYPE_UNKNOWN;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position >= stationFirstIndex) {
            String currentStation = data.current_observation.station_id;
            int index = position;
            if ((data.location.nearby_weather_stations.airport != null) &&
                    !nullOrEmpty(data.location.nearby_weather_stations.airport.station)) {
                if (index < data.location.nearby_weather_stations.airport.station.size()) {
                    ((StationHolder) holder).setup(data.location.nearby_weather_stations.airport.station.get(index), currentStation);
                    return;
                } else {
                    index -= data.location.nearby_weather_stations.airport.station.size();
                }
            }
            ((StationHolder) holder).setup(data.location.nearby_weather_stations.pws.station.get(index), currentStation);
        }
    }

    public void addData(AllData data) {
        this.data = data;
        stationFirstIndex = 0;
        notifyDataSetChanged();
    }

    static class StationHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView title;
        TextView description;
        String stationId;
        boolean isPwsStation;

        StationHolder(View row) {
            super(row);
            title = ButterKnife.findById(row, R.id.forecast_title);
            description = ButterKnife.findById(row, R.id.forecast_description);
        }

        void setup(WeatherStation data, String currentStationId) {
            isPwsStation = data.id != null;
            stationId = isPwsStation ? data.id : data.icao;
            title.setText(data.city + ", " + data.state);
            description.setText(isPwsStation ? data.neighborhood : data.icao);
            boolean isCurrentStation = currentStationId.equalsIgnoreCase(stationId);
            description.setTextColor(isCurrentStation ? Color.RED : Color.BLACK);
            title.setTextColor(isCurrentStation ? Color.RED : Color.BLACK);

            this.itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            EventBus.getDefault().post(new StationClickEvent(isPwsStation ? "pws:" + stationId : stationId));
        }
    }
}
