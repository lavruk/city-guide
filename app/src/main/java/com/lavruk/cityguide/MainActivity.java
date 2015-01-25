package com.lavruk.cityguide;

import com.android.volley.RequestQueue;
import com.lavruk.cityguide.NavigationFragment.NavigationDrawerCallbacks;
import com.lavruk.cityguide.net.Api;
import com.lavruk.cityguide.tools.LocationHelper;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import javax.inject.Inject;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerCallbacks {

    private NavigationFragment mNavigationDrawerFragment;

    @Inject RequestQueue mRequestQueue;
    @Inject LocationHelper mLocationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        App.inject(this, this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setElevation(0);
        actionBar.setDisplayShowTitleEnabled(true);

        mNavigationDrawerFragment = (NavigationFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // connecting to the google location api
        mLocationHelper.connect();
    }

    @Override protected void onDestroy() {
        mRequestQueue.cancelAll(Api.PLACES_REQUESTS_TAG);
        mLocationHelper.disconnect();
        super.onDestroy();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

        Fragment fragment = PlacesFragment.newInstance();

        if (fragment != null) {
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        } else {
            Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show();
        }
    }

}
