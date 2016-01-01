package net.devrand.kihon.kihonweather;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import net.devrand.kihon.kihonweather.data.AllData;
import net.devrand.kihon.kihonweather.data.Forecast;
import net.devrand.kihon.kihonweather.data.WeatherStation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    public static final int TYPE_STATION = 4;

    public static final int FORECAST_FIRST_INDEX = 2;

    public static int stationFirstIndex = 100;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_FORECAST:
                return new ForecastHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.forecast_item, parent, false));
            case TYPE_CURRENT_CONDITIONS:
                return new CurrentWeatherHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.current_weather_item, parent, false));
            case TYPE_GRAPH:
                return new GraphHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.graph_item, parent, false));
            case TYPE_STATION:
                return new StationHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.forecast_item, parent, false));        }
        return null;
    }

    public boolean hasForecastData() {
        if (data == null || data.forecast == null || data.forecast.txt_forecast == null ||
                data.forecast.txt_forecast.forecastday == null || data.forecast.txt_forecast.forecastday.size() < 1) {
            return false;
        }
        return true;
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

    public int forecastDataCount () {
        if (hasForecastData()) {
            return data.forecast.txt_forecast.forecastday.size();
        }
        return 0;
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
        return 2 + forecastDataCount() + stationDataCount();
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
            int index = position - FORECAST_FIRST_INDEX - forecastDataCount();
            if ((data.location.nearby_weather_stations.airport != null) &&
                    !nullOrEmpty(data.location.nearby_weather_stations.airport.station)) {
                if (index < data.location.nearby_weather_stations.airport.station.size()) {
                    ((StationHolder) holder).setup(data.location.nearby_weather_stations.airport.station.get(index));
                    return;
                } else {
                    index -= data.location.nearby_weather_stations.airport.station.size();
                }
                ((StationHolder) holder).setup(data.location.nearby_weather_stations.pws.station.get(index));
            }
        }
        else if (position >= FORECAST_FIRST_INDEX) {
            ((ForecastHolder) holder).setup(data.forecast.txt_forecast.forecastday.get(position - FORECAST_FIRST_INDEX));
        }
        else if (position == 0) {
            ((CurrentWeatherHolder) holder).setup(data);
        }
        else if (position == 1) {
            ((GraphHolder) holder).setup(data);
        }
    }

    public void addData(AllData data) {
        this.data = data;
        stationFirstIndex = 2 + forecastDataCount();
        notifyDataSetChanged();
    }

    static class GraphHolder extends RecyclerView.ViewHolder {
        GraphView graph;
        HorizontalScrollView graph_panel;

        float maxTemp, minTemp;
        long startTime;

        GraphHolder(View row) {
            super(row);
            graph = ButterKnife.findById(row, R.id.graph);
            graph_panel = ButterKnife.findById(row, R.id.graphscroll);
        }

        void setup(AllData data) {
            int idx = 0;
            List<DataPoint> precipPoints = new ArrayList<>();
            List<DataPoint> tempPoints = new ArrayList<>();

            maxTemp = -100;
            minTemp = 200;
            startTime = -1;

            graph.removeAllSeries();
            graph.getSecondScale().getSeries().clear();
            graph_panel.scrollTo(0, 0);

            for (AllData.Hourly hourly : data.hourly_forecast) {
                if (startTime == -1) {
                    startTime = hourly.FCTTIME.epoch;
                }
                if (idx < 24 * 4) {
                    precipPoints.add(new DataPoint(idx, hourly.pop));
                    tempPoints.add(new DataPoint(idx, hourly.temp.english));
                    maxTemp = hourly.temp.english > maxTemp ? hourly.temp.english : maxTemp;
                    minTemp = hourly.temp.english < minTemp ? hourly.temp.english : minTemp;
                }
                idx++;
            }
            //System.out.format("Before: Min %03.2f Max %03.2f\n", minTemp, maxTemp);
            minTemp = (Math.round(minTemp) - 1);
            maxTemp = (Math.round(maxTemp) + 1);
            //System.out.format("After: Min %03.2f Max %03.2f\n", minTemp, maxTemp);

            BarGraphSeries<DataPoint> barGraphSeries = new BarGraphSeries<DataPoint>(precipPoints.toArray(new DataPoint[precipPoints.size()]));
            graph.addSeries(barGraphSeries);
            graph.getViewport().setYAxisBoundsManual(true);
            graph.getViewport().setMaxY(100);
            graph.getViewport().setMinY(0);

            LineGraphSeries<DataPoint> lineGraphSeries = new LineGraphSeries<DataPoint>(tempPoints.toArray(new DataPoint[tempPoints.size()]));
            lineGraphSeries.setColor(Color.CYAN);
            graph.getSecondScale().addSeries(lineGraphSeries);
            graph.getSecondScale().setMinY(minTemp);
            graph.getSecondScale().setMaxY(maxTemp);

            //graph.getViewport().setScrollable(true);
            graph.getViewport().setXAxisBoundsManual(true);
            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(precipPoints.size());

            //hide Second Scale text
            graph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.WHITE);

            // custom label formatter to show temp and time
            graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                final Date startDate = new Date(startTime * 1000);
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("ha EEE", Locale.US);

                @Override
                public String formatLabel(double value, boolean isValueX) {
                    if (isValueX) {
                        // show date for x values
                        int intValue = (int) value;
                        calendar.setTime(startDate);
                        //System.out.println("start: " + sdf.format(calendar.getTime()));
                        calendar.add(Calendar.HOUR_OF_DAY, intValue);
                        //System.out.println("offset: " + sdf.format(calendar.getTime()));
                        return sdf.format(calendar.getTime());
                    } else {
                        // show temperature for y values
                        double calcvalue = (value / 100) * (maxTemp - minTemp) + minTemp;
                        return super.formatLabel(calcvalue, isValueX) + "\u00B0 C ";
                    }
                }
            });

        }
    }

    static class CurrentWeatherHolder extends RecyclerView.ViewHolder {

        TextView text_location;
        TextView text_status;
        TextView text_sunrise;
        TextView text_sunset;
        TextView text_temperature;

        CurrentWeatherHolder(View row) {
            super(row);
            text_location = ButterKnife.findById(row, R.id.location_text);
            text_status = ButterKnife.findById(row, R.id.status_text);
            text_sunrise = ButterKnife.findById(row, R.id.sunrise_text);
            text_sunset = ButterKnife.findById(row, R.id.sunset_text);
            text_temperature = ButterKnife.findById(row, R.id.temperature_text);
        }

        void setup(AllData data) {
            text_location.setText(data.current_observation.display_location.full);
            text_temperature.setText(data.current_observation.temperature_string);
            text_status.setText(data.current_observation.weather);
            text_sunrise.setText("Sunrise: " + data.sun_phase.getSunrise());
            text_sunset.setText("Sunset: " + data.sun_phase.getSunset());
        }
    }

    static class ForecastHolder extends RecyclerView.ViewHolder {
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

    static class StationHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView description;

        StationHolder(View row) {
            super(row);
            title = ButterKnife.findById(row, R.id.forecast_title);
            description = ButterKnife.findById(row, R.id.forecast_description);
        }

        void setup(WeatherStation data) {
            title.setText(data.city + ", " + data.state);
            description.setText(data.icao == null ? data.neighborhood : data.icao);
        }
    }
}
