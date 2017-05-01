package net.devrand.kihon.kihonweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import net.devrand.kihon.kihonweather.event.FabEvent;
import net.devrand.kihon.kihonweather.event.SetTitleEvent;
import net.devrand.kihon.kihonweather.event.StationClickEvent;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

public class WeatherActivity extends AppCompatActivity {

    @Bind(R.id.main_fragment)
    FrameLayout fragmentContainer;
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.fab)
    FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventBus.getDefault().post(new FabEvent());
            }
        });

        Fragment fragment = new WeatherActivityFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(fragmentContainer.getId(), fragment).commit();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

        if (pref.getString(getString(R.string.pref_key_api_key), getString(R.string.pref_default_api_key)).contains("XXXX")) {
            Toast.makeText(this, "Please set a valid Wunderground API Key", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SettingsActivity.class));
        }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_weather, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_weather, menu);
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            menu.removeItem(R.id.action_stations);
        } else {
            menu.removeItem(R.id.action_forecast);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Fragment fragment;
        FragmentTransaction transaction;

        fab.setVisibility(View.VISIBLE);
        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_forecast:
                getSupportFragmentManager().popBackStack();
                return true;
            case R.id.action_stations:
                fragment = new StationSelectionFragment();
                transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(fragmentContainer.getId(), fragment)
                        .addToBackStack(null)
                        .commit();
                return true;
            case R.id.action_locations:
                fab.setVisibility(View.GONE);
                fragment = new AutoCompleteFragment();
                transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(fragmentContainer.getId(), fragment)
                        .addToBackStack(null)
                        .commit();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onEvent(SetTitleEvent event) {
        if (!TextUtils.isEmpty(event.title)) {
            toolbar.setTitle(event.title);
        }
        toolbar.setSubtitle(event.subTitle);
    }
}