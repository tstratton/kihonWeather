package net.devrand.kihon.kihonweather;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

public class WeatherActivity extends AppCompatActivity {

    TextView textView;

    private static final String TAG = "WeatherActivity";
    private static final String BASE_URL = "http://api.wunderground.com/api/%s/conditions/forecast/q/%s/%s.json";

    private static final String API_KEY = "XXXX_WUNDERGROUND_API_KEY_XXXX";
    private static final String STATE_STRING = "CA";
    private static final String CITY_STRING = "San_Francisco";

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

    private class GetDataTask extends AsyncTask<String, Integer, String> {
        protected String doInBackground(String... urls) {
            OkHttpClient client = new OkHttpClient();
            Response response;

            String url = String.format(urls[0], urls[1], urls[2], urls[3]);

            Log.d(TAG, "Getting " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try {
                response = client.newCall(request).execute();
                String json = response.body().string();
                return String.format("%d chars\n%s\nAPI key '%s'\nResult for %s, %s\n%s",
                        json.length(), urls[0], urls[1], urls[2], urls[3], json);
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

        protected void onPostExecute(String result) {
            textView.setText(result);
        }
    }
}
