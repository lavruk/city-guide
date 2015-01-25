package com.lavruk.cityguide;

import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.lavruk.cityguide.events.DistancesResponseEvent;
import com.lavruk.cityguide.events.PlacesResponseEvent;
import com.lavruk.cityguide.net.Api;
import com.lavruk.cityguide.tools.LocationHelper;
import com.lavruk.cityguide.tools.LocationHelper.LocationCallback;
import com.lavruk.cityguide.views.MultiSwitch;
import com.lavruk.cityguide.views.MultiSwitch.OnStateChangedListener;

import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

public class PlacesFragment extends Fragment implements OnRefreshListener, OnStateChangedListener, LocationCallback {

    public static final float LIST_ALPHA = .5f;
    private PlacesAdapter mPlacesAdapter;
    private PlaceType mPlaceType;
    private Location mLastLocation;
    private boolean mWaitingForLocation;

    public static Fragment newInstance() {
        return new PlacesFragment();
    }

    @Inject EventBus mBus;
    @Inject Api mApi;
    @Inject LocationHelper mLocationHelper;

    @InjectView(R.id.swipe_refresh_layout) SwipeRefreshLayout mSwipeRefreshLayout;
    @InjectView(R.id.list) ListView mListView;
    @InjectView(R.id.multi_switch) MultiSwitch mMultiSwitch;

    @Nullable @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_places, null);
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        App.inject(getActivity(), this);
        ButterKnife.inject(this, view);
        mBus.register(this);
        mLocationHelper.addLocationCallbackListener(this);

        CharSequence[] placeTypes = new CharSequence[PlaceType.values().length];
        for (int i = 0; i < PlaceType.values().length; i++) {
            placeTypes[i] = PlaceType.values()[i].title;
        }
        mMultiSwitch.setStates(placeTypes);
        mMultiSwitch.setOnStateChangedListener(this);

        mPlaceType = PlaceType.bar;

        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                R.color.selected_type_color,
                R.color.primary_color);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mPlacesAdapter = new PlacesAdapter(getActivity());
        mPlacesAdapter.onNewPlaceType(mPlaceType);
        mListView.setAdapter(mPlacesAdapter);
        mListView.setEmptyView(view.findViewById(R.id.empty));

        reloadPlaces();
    }

    private void reloadPlaces() {
        ObjectAnimator.ofFloat(mListView, "alpha", 1f, LIST_ALPHA).start();
        mSwipeRefreshLayout.post(new Runnable() {
            @Override public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });
        onRefresh();
    }

    @Override public void onDestroyView() {
        mBus.unregister(this);
        mLocationHelper.removeLocationCallbackListener(this);
        super.onDestroyView();
    }

    @Override public void onRefresh() {

        if (mLastLocation == null) {
            mWaitingForLocation = true;
        } else {
            mApi.requestPlaces(mPlaceType.name(), mLastLocation);
        }
    }

    @Override public void onStateChanged(int state) {
        PlaceType placeType = PlaceType.values()[state];
        mPlacesAdapter.onNewPlaceType(placeType);
        mPlaceType = placeType;
        reloadPlaces();
    }

    @Override public void onLocationChanged(Location location, long time) {
        mLastLocation = location;
        if (mWaitingForLocation) {
            mWaitingForLocation = false;
            onRefresh();
        }
        mPlacesAdapter.onNewLocation(location);
    }

    public void onEvent(PlacesResponseEvent event) {
        String responsePlaceType = event.getType();

        // not our response
        if (!mPlaceType.name().equals(responsePlaceType)) {
            return;
        }

        ObjectAnimator.ofFloat(mListView, "alpha", LIST_ALPHA, 1f).start();
        mSwipeRefreshLayout.setRefreshing(false);
        if (event.getError() != null) {
            showError(event.getError());
            return;
        }

        // all is ok
        mPlacesAdapter.swapItems(event.getResponse().results);
    }

    public void onEvent(DistancesResponseEvent event) {
        if (event.getError() != null) {
            // error loading distances
            return;
        }

        mPlacesAdapter.swapDistances(event.getResponse().rows);

    }

    private void showError(VolleyError error) {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("Request failed! ");
        if (error instanceof NoConnectionError || error instanceof NetworkError) {
            errorMessage.append("Check your connection, please");
        } else if (error instanceof ServerError) {
            errorMessage.append("Server error");
            if (error.networkResponse != null) {
                errorMessage.append(" (").append(error.networkResponse.statusCode).append(")");
            }
        }

        Toast.makeText(getActivity(), errorMessage.toString(), Toast.LENGTH_LONG).show();
    }

    enum PlaceType {
        bar("Bar"), restaurant("Bistro"), cafe("Café");

        final String title;

        PlaceType(String title) {
            this.title = title;
        }
    }

    interface PlacesAdapterBridge {

        void onNewLocation(Location location);

        void onNewPlaceType(PlaceType placeType);
    }
}
