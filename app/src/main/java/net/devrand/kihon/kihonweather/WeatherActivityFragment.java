package net.devrand.kihon.kihonweather;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.stetho.okhttp.StethoInterceptor;
import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import net.devrand.kihon.kihonweather.data.AllData;
import net.devrand.kihon.kihonweather.event.FabEvent;
import net.devrand.kihon.kihonweather.event.StationClickEvent;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

/**
 * A placeholder fragment containing a simple view.
 */
public class WeatherActivityFragment extends Fragment {

    private static final String TAG = "WeatherActivityFrag";
    private static final String BASE_URL = "http://api.wunderground.com/api/%s/conditions/geolookup/forecast/astronomy/hourly10day/q/%s.json";

    @Bind(R.id.text)
    TextView textView;
    @Bind(R.id.text_card)
    View textCard;

    @Bind(R.id.forecastList)
    RecyclerView forecastList;

    public WeatherActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_weather, container, false);
        ButterKnife.bind(this, root);

        forecastList.setVisibility(View.GONE);

        forecastList.setLayoutManager(new LinearLayoutManager(getContext()));
        forecastList.setAdapter(new ForecastAdapter());
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
            textCard.setVisibility(View.VISIBLE);
            forecastList.setVisibility(View.GONE);
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
                    textCard.setVisibility(View.VISIBLE);
                } else {
                    ((ForecastAdapter) forecastList.getAdapter()).addData(data);
                    forecastList.setVisibility(View.VISIBLE);
                    textCard.setVisibility(View.GONE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                textCard.setVisibility(View.VISIBLE);
                text = new StringBuilder();
                text.append("Error getting data:\n");
                text.append(ex.toString());
                text.append("\n");
            }
            text.append("API key '");
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
            text.append(pref.getString(getString(R.string.pref_key_api_key), getString(R.string.pref_default_api_key)));
            text.append("'");
            textView.setText(text);
        }
    }

    // This method will be called when a MessageEvent is posted
    public void onEvent(FabEvent event) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        new GetDataTask().execute(BASE_URL,
                pref.getString(getString(R.string.pref_key_api_key), getString(R.string.pref_default_api_key)),
                pref.getString(getString(R.string.pref_key_zip_code), getString(R.string.pref_default_zip_code)));
    }

    public void onEvent(StationClickEvent event) {
        if (event.stationId == null) {
            return;
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        pref.edit()
                .putString(getString(R.string.pref_key_zip_code), event.stationId)
                .commit();
        EventBus.getDefault().post(new FabEvent());
    }
}
