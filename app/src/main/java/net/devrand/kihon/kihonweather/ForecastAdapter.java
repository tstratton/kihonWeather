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
import java.util.TimeZone;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

/**
 * Created by tstratto on 12/24/2015.
 */
public class ForecastAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private AllData data;

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_CURRENT_CONDITIONS = 1;
    public static final int TYPE_GRAPH = 2;
    public static final int TYPE_FORECAST = 3;

    public static final int FORECAST_FIRST_INDEX = 2;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_FORECAST:
                return new ForecastHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.forecast_item, parent, false));
            case TYPE_CURRENT_CONDITIONS:
                return new CurrentWeatherHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.current_weather_item, parent, false));
            case TYPE_GRAPH:
                return new GraphHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.graph_item, parent, false));
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

    public boolean nullOrEmpty(List list) {
        if (list == null) {
            return true;
        }
        return list.isEmpty();
    }

    public int forecastDataCount () {
        if (hasForecastData()) {
            return data.forecast.txt_forecast.forecastday.size();
        }
        return 0;
    }

    @Override
    public int getItemCount() {
        return 2 + forecastDataCount();
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= FORECAST_FIRST_INDEX) {
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
        if (position >= FORECAST_FIRST_INDEX) {
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
        TextView text_time;
        TextView text_status;
        TextView text_sunrise;
        TextView text_sunset;
        TextView text_temperature;
        ImageView icon_status;

        CurrentWeatherHolder(View row) {
            super(row);
            text_location = ButterKnife.findById(row, R.id.location_text);
            text_time = ButterKnife.findById(row, R.id.time_text);
            text_status = ButterKnife.findById(row, R.id.status_text);
            text_sunrise = ButterKnife.findById(row, R.id.sunrise_text);
            text_sunset = ButterKnife.findById(row, R.id.sunset_text);
            text_temperature = ButterKnife.findById(row, R.id.temperature_text);
            icon_status = ButterKnife.findById(row, R.id.status_icon);
        }

        void setup(AllData data) {
            text_location.setText(data.current_observation.display_location.full);
            String temp = data.current_observation.temperature_string;
            text_temperature.setText(temp);
            text_status.setText(data.current_observation.weather);
            Calendar now = Calendar.getInstance();
            //FIXME assumes timezones are the same
            if (data.sun_phase.afterSunset(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))) {
                text_sunrise.setText("Sunset: " + data.sun_phase.getSunset());
                text_sunset.setText("Sunrise: around " + data.sun_phase.getSunrise());
            } else {
                text_sunrise.setText("Sunrise: " + data.sun_phase.getSunrise());
                text_sunset.setText("Sunset: " + data.sun_phase.getSunset());
            }
            long observation_time_ms = data.current_observation.observation_epoch * 1000;
            CharSequence timeString = DateUtils.getRelativeDateTimeString(text_time.getContext(), observation_time_ms,
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL);
            text_time.setText(timeString);

            Picasso.with(icon_status.getContext()).load(getAssetUrl(data.current_observation.icon)).into(icon_status);
        }
    }

    static class ForecastHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView description;
        ImageView icon;

        ForecastHolder(View row) {
            super(row);
            title = ButterKnife.findById(row, R.id.forecast_title);
            description = ButterKnife.findById(row, R.id.forecast_description);
            icon = ButterKnife.findById(row, R.id.forecast_icon);
        }

        void setup(Forecast.TextForecastDay data) {
            title.setText(data.title);
            description.setText(data.fcttext);

            Picasso.with(icon.getContext()).load(getAssetUrl(data.icon)).into(icon);
        }
    }

    public static String getAssetUrl(String iconName) {
        return String.format("file:///android_asset/%s.gif", iconName);
    }
}
