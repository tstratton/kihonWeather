package net.devrand.kihon.kihonweather;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import net.devrand.kihon.kihonweather.event.FabEvent;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

public class WeatherActivity extends AppCompatActivity {

    @Bind(R.id.main_fragment) FrameLayout fragmentContainer;
    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.fab) FloatingActionButton fab;

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
                fragment = new AutoCompleteFragment();
                transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(fragmentContainer.getId(), fragment)
                        .addToBackStack(null)
                        .commit();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
