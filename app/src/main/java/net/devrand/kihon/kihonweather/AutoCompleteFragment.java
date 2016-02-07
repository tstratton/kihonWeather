package net.devrand.kihon.kihonweather;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.ArrayList;

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
    @Bind(R.id.auto_list)
    ListView autoList;
    @Bind(R.id.edit_text)
    EditText editText;

    ArrayAdapter<AutoCompleteItem> adapter;
    ArrayList<AutoCompleteItem> items = new ArrayList<>();

    DelayedData delayed;

    public AutoCompleteFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_autocomplete, container, false);
        ButterKnife.bind(this, root);

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, items);
        autoList.setAdapter(adapter);
        textCard.setVisibility(View.GONE);

        //FIXME get soft keyboard to show

        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "onTextChanged " + s.toString());

                editText.removeCallbacks(delayed);
                if (delayed == null) {
                    delayed = new DelayedData();
                }
                delayed.query = s.toString();
                editText.postDelayed(delayed, AUTOCOMPLETE_DELAY_MS);
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

        autoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                AutoCompleteItem item = (AutoCompleteItem) adapterView.getItemAtPosition(position);
                if (item.l == null) {
                    return;
                }
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
                pref.edit()
                        .putString(getString(R.string.pref_key_zip_code), item.l)
                        .commit();
                Toast.makeText(getContext(), "Location updated to\n" + item.name, Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    public class DelayedData implements Runnable {
        public String query;

        @Override
        public void run() {
            if (TextUtils.isEmpty(query)) {
                Log.d(TAG, "delayed clear query");
                adapter.clear();
                adapter.notifyDataSetChanged();
                return;
            }
            Log.d(TAG, "delayed run query: " + query);
            getTask = new GetDataTask();
            getTask.execute(BASE_URL, query);
        }
    }


    @Override
    public void onDestroyView() {
        editText.removeCallbacks(delayed);
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
        editText.removeCallbacks(delayed);
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
}
