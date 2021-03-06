package net.devrand.kihon.kihonweather;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
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
import net.devrand.kihon.kihonweather.event.SetTitleEvent;
import net.devrand.kihon.kihonweather.event.StationClickEvent;

import java.io.IOException;
import java.net.UnknownHostException;

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

    @Bind(R.id.swipeRefresh)
    SwipeRefreshLayout swipeRefreshLayout;

    boolean displayApiKey = false;

    public WeatherActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_weather, container, false);
        ButterKnife.bind(this, root);

        swipeRefreshLayout.setVisibility(View.GONE);

        forecastList.setLayoutManager(new LinearLayoutManager(getContext()));
        forecastList.setAdapter(new ForecastAdapter());

        // from http://stackoverflow.com/a/25183693
        forecastList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int topRowVerticalPosition =
                        (recyclerView == null || recyclerView.getChildCount() == 0) ? 0 : recyclerView.getChildAt(0).getTop();
                swipeRefreshLayout.setEnabled(topRowVerticalPosition >= 0);

            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                EventBus.getDefault().post(new FabEvent());
            }
        });

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

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().post(new FabEvent());
        EventBus.getDefault().post(SetTitleEvent.createSubTitleEvent(getString(R.string.action_forecast)));
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
                if (ex instanceof UnknownHostException) {
                    return AllData.createError("Network Error", "Unable to contact server");
                }
                return null;
            }
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPreExecute() {
            textView.setText(R.string.getting_data);
            textCard.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setVisibility(View.GONE);
        }

        protected void onPostExecute(AllData data) {
            Log.d(TAG, "Got data");
            StringBuilder text = new StringBuilder();
            try {
                if (data.hasError()) {
                    text.append("Got error:\n");
                    text.append(data.getError().type);
                    text.append("\n");
                    text.append(data.getError().description);
                    text.append("\n");
                    textCard.setVisibility(View.VISIBLE);
                } else {
                    ((ForecastAdapter) forecastList.getAdapter()).addData(data);
                    swipeRefreshLayout.setVisibility(View.VISIBLE);
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
            if (displayApiKey) {
                text.append("API key '");
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
                text.append(pref.getString(getString(R.string.pref_key_api_key), getString(R.string.pref_default_api_key)));
                text.append("'");
            }
            textView.setText(text.toString().trim());
            swipeRefreshLayout.setRefreshing(false);
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
