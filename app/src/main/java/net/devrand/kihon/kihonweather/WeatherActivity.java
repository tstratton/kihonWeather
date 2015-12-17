package net.devrand.kihon.kihonweather;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.facebook.stetho.okhttp.StethoInterceptor;
import com.google.gson.Gson;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import net.devrand.kihon.kihonweather.data.AllData;
import net.devrand.kihon.kihonweather.data.Forecast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WeatherActivity extends AppCompatActivity {

    TextView textView;

    private static final String TAG = "WeatherActivity";
    private static final String BASE_URL = "http://api.wunderground.com/api/%s/conditions/forecast/astronomy/hourly10day/q/%s/%s.json";

    private static final String API_KEY = "XXXX_WUNDERGROUND_API_KEY_XXXX";
    private static final String STATE_STRING = "CA";
    private static final String CITY_STRING = "San_Francisco";

    GraphView graph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textView = (TextView) findViewById(R.id.text);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new GetDataTask().execute(BASE_URL, API_KEY, STATE_STRING, CITY_STRING);
            }
        });
        graph = (GraphView) findViewById(R.id.graph);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_weather, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class GetDataTask extends AsyncTask<String, Integer, AllData> {
        protected AllData doInBackground(String... urls) {
            OkHttpClient client = new OkHttpClient();
            client.networkInterceptors().add(new StethoInterceptor());
            Response response;

            String url = String.format(urls[0], urls[1], urls[2], urls[3]);

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
                    text.append(data.current_observation.display_location.full);
                    text.append("\n");
                    text.append(data.current_observation.temperature_string);
                    text.append("\n");
                    text.append(data.current_observation.weather);
                    text.append("\nSunrise ");
                    text.append(data.sun_phase.getSunrise());
                    text.append("\nSunset ");
                    text.append(data.sun_phase.getSunset());
                    text.append("\n");

                    text.append("\nForecast:\n");
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
                        text.append(hourly.condition);
                        text.append("\n");
                        if (idx < 24 * 5) {
                            precipPoints.add(new DataPoint(idx, hourly.pop));
                            tempPoints.add(new DataPoint(idx, hourly.temp.english));
                        }
                        idx++;
                    }
                    BarGraphSeries<DataPoint> barGraphSeries = new BarGraphSeries<DataPoint>(precipPoints.toArray(new DataPoint[precipPoints.size()]));
                    graph.addSeries(barGraphSeries);

                    LineGraphSeries<DataPoint> lineGraphSeries = new LineGraphSeries<DataPoint>(tempPoints.toArray(new DataPoint[tempPoints.size()]));
                    lineGraphSeries.setColor(Color.CYAN);
                    graph.addSeries(lineGraphSeries);
                    graph.getViewport().setYAxisBoundsManual(true);
                    graph.getViewport().setMaxY(100);
                    graph.getViewport().setMinY(0);

                    //graph.getViewport().setScrollable(true);
                    graph.getViewport().setXAxisBoundsManual(true);
                    graph.getViewport().setMinX(0);
                    graph.getViewport().setMaxX(precipPoints.size());
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
            text.append(API_KEY);
            text.append("'\n");
            text.append(BASE_URL);
            text.append("\n");
            textView.setText(text);
        }
    }
}
