package net.devrand.kihon.kihonweather;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.stetho.okhttp.StethoInterceptor;
import com.google.gson.Gson;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import net.devrand.kihon.kihonweather.data.AllData;
import net.devrand.kihon.kihonweather.data.Forecast;
import net.devrand.kihon.kihonweather.event.FabEvent;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

/**
 * A placeholder fragment containing a simple view.
 */
public class WeatherActivityFragment extends Fragment {

    private static final String TAG = "WeatherActivity";
    private static final String BASE_URL = "http://api.wunderground.com/api/%s/conditions/forecast/astronomy/hourly10day/q/%s.json";

    @Bind(R.id.text)
    TextView textView;
    @Bind(R.id.graph)
    GraphView graph;

    @Bind(R.id.location_text)
    TextView text_location;
    @Bind(R.id.status_text)
    TextView text_status;
    @Bind(R.id.sunrise_text)
    TextView text_sunrise;
    @Bind(R.id.sunset_text)
    TextView text_sunset;
    @Bind(R.id.temperture_text)
    TextView text_temperture;

    @Bind(R.id.graphscroll)
    View graph_panel;
    @Bind(R.id.current_card)
    View current_panel;

    public WeatherActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_weather, container, false);
        ButterKnife.bind(this, root);

        graph_panel.setVisibility(View.GONE);
        current_panel.setVisibility(View.GONE);

        return root;
    }

    @Override
    public void onDestroyView() {
        ButterKnife.unbind(this);

        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    private class GetDataTask extends AsyncTask<String, Integer, AllData> {
        float maxTemp = -100;
        float minTemp = 200;
        long startTime = -1;

        protected AllData doInBackground(String... urls) {
            OkHttpClient client = new OkHttpClient();
            client.networkInterceptors().add(new StethoInterceptor());
            Response response;

            String url = String.format(urls[0], urls[1], urls[2]);

            Log.d(TAG, "Getting " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try {
                response = client.newCall(request).execute();
                String json = response.body().string();

                Gson gson = new Gson();
                AllData data = gson.fromJson(json, AllData.class);

                return data;
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPreExecute() {
            textView.setText("Getting Data..");
            graph.removeAllSeries();
            graph.getSecondScale().getSeries().clear();
            graph_panel.scrollTo(0, 0);
            graph_panel.setVisibility(View.GONE);
            current_panel.setVisibility(View.GONE);
        }

        protected void onPostExecute(AllData data) {
            StringBuilder text = new StringBuilder();
            try {
                if (data.hasError()) {
                    text.append("Got error\n");
                    text.append(data.getError().type);
                    text.append("\n");
                    text.append(data.getError().description);
                    text.append("\n");
                } else {
                    text_location.setText(data.current_observation.display_location.full);
                    text_temperture.setText(data.current_observation.temperature_string);
                    text_status.setText(data.current_observation.weather);
                    text_sunrise.setText("Sunrise: " + data.sun_phase.getSunrise());
                    text_sunset.setText("Sunset: " + data.sun_phase.getSunset());
                    current_panel.setVisibility(View.VISIBLE);

                    text.append("Forecast:\n\n");
                    for (Forecast.TextForecastDay forecast : data.forecast.txt_forecast.forecastday) {
                        text.append(forecast.title);
                        text.append("\n");
                        text.append(forecast.fcttext);
                        text.append("\n");
                        text.append(forecast.pop);
                        text.append("% chance of precipitation\n");
                        text.append("\n");
                    }
                    int idx = 0;
                    List<DataPoint> precipPoints = new ArrayList<>();
                    List<DataPoint> tempPoints = new ArrayList<>();

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

                    graph_panel.setVisibility(View.VISIBLE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                text = new StringBuilder();
                text.append("Error getting data:\n");
                text.append(ex.toString());
                text.append("\n");
            }

            text.append("\n----- ----- ----- -----\n");
            text.append("API key '");
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
            text.append(pref.getString(getString(R.string.pref_key_api_key), getString(R.string.pref_default_api_key)));
            text.append("'\n");
            text.append(BASE_URL);
            text.append("\n");
            textView.setText(text);
        }
    }

    // This method will be called when a MessageEvent is posted
    public void onEvent(FabEvent event) {
        SharedPreferences pref = PreferenceManager
                .getDefaultSharedPreferences(getContext());
        new GetDataTask().execute(BASE_URL,
                pref.getString(getString(R.string.pref_key_api_key), getString(R.string.pref_default_api_key)),
                pref.getString(getString(R.string.pref_key_zip_code), getString(R.string.pref_default_zip_code)));
    }
}
