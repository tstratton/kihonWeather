package net.devrand.kihon.kihonweather;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.facebook.stetho.okhttp.StethoInterceptor;
import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import net.devrand.kihon.kihonweather.data.AutoCompleteItem;
import net.devrand.kihon.kihonweather.data.AutoCompleteResult;
import net.devrand.kihon.kihonweather.event.FabEvent;
import net.devrand.kihon.kihonweather.event.StationClickEvent;

import java.io.IOException;
import java.net.URLEncoder;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

/**
 * Created by tstratto on 1/7/2016.
 */
public class AutoCompleteFragment extends Fragment {

    private static final String TAG = "AutoCompleteFragment";
    private static final String BASE_URL = "http://autocomplete.wunderground.com/aq?c=US&h=0&cities=1&query=%s";
    private static final int AUTOCOMPLETE_DELAY_MS = 1500;

    GetDataTask getTask;

    @Bind(R.id.text)
    TextView textView;
    @Bind(R.id.text_card)
    View textCard;

    @Bind(R.id.auto_text)
    AutoCompleteTextView autoText;
    ArrayAdapter<AutoCompleteItem> adapter;
    AutoCompleteItem[] emptyItem = new AutoCompleteItem[0];

    DelayedData delayed;

    public AutoCompleteFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_autocomplete, container, false);
        ButterKnife.bind(this, root);

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, emptyItem);
        autoText.setAdapter(adapter);
        autoText.setThreshold(2);

        autoText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "onTextChanged " + s.toString());

                autoText.removeCallbacks(delayed);
                if (s.toString().length() < 1) {
                    return;
                }
                if (delayed == null) {
                    delayed = new DelayedData();
                }
                delayed.query = s.toString();
                autoText.postDelayed(delayed, AUTOCOMPLETE_DELAY_MS);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }
        });

        return root;
    }

    public class DelayedData implements Runnable {
        public String query;

        @Override
        public void run() {
            Log.d(TAG, "delayed run query: " + query);
            getTask = new GetDataTask();
            getTask.execute(BASE_URL, query);
        }
    }


    @Override
    public void onDestroyView() {
        autoText.removeCallbacks(delayed);
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
        autoText.removeCallbacks(delayed);
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    private class GetDataTask extends AsyncTask<String, Integer, AutoCompleteResult> {

        protected AutoCompleteResult doInBackground(String... urls) {
            OkHttpClient client = new OkHttpClient();
            client.networkInterceptors().add(new StethoInterceptor());
            Response response;

            String url = String.format(urls[0], URLEncoder.encode(urls[1]));

            Log.d(TAG, "Getting " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try {
                response = client.newCall(request).execute();
                String json = response.body().string();

                Gson gson = new Gson();
                AutoCompleteResult data = gson.fromJson(json, AutoCompleteResult.class);

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
        }

        protected void onPostExecute(AutoCompleteResult data) {
            Log.d(TAG, "Got data " + (data != null && data.RESULTS != null ? data.RESULTS.size() : "null"));
            StringBuilder text = new StringBuilder();
            for (AutoCompleteItem item : data.RESULTS) {
                text.append(item.name + " -- ");
            }
            Log.d(TAG, text.toString());

            //FIXME adapter not updating correctly -- threading issue?
            adapter.clear();
            adapter.addAll(data.RESULTS);
            adapter.notifyDataSetChanged();
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
