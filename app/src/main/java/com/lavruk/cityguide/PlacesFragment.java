package com.lavruk.cityguide;

import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.lavruk.cityguide.events.DistancesResponseEvent;
import com.lavruk.cityguide.events.PlacesResponseEvent;
import com.lavruk.cityguide.net.Api;
import com.lavruk.cityguide.net.DistanceRequest.Distance;
import com.lavruk.cityguide.net.DistanceRequest.DistanceElement;
import com.lavruk.cityguide.net.DistanceRequest.DistanceRow;
import com.lavruk.cityguide.net.NearbySearchRequest.GooglePlacesResponse.GooglePlace;
import com.lavruk.cityguide.tools.BindableAdapter;
import com.lavruk.cityguide.tools.LocationHelper;
import com.lavruk.cityguide.tools.LocationHelper.LocationCallback;
import com.lavruk.cityguide.views.MultiSwitch;
import com.lavruk.cityguide.views.MultiSwitch.OnStateChangedListener;

import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        PlacesFragment fragment = new PlacesFragment();
        return fragment;
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

    public static class PlacesAdapter extends BindableAdapter<GooglePlace> implements PlacesAdapterBridge {

        List<GooglePlace> mItems = new ArrayList<>();
        List<Distance> mDistances = new ArrayList<>();

        private Location mLocation;
        private PlaceType mPlaceType;

        public PlacesAdapter(Context context) {
            super(context);
        }

        @Override public int getCount() {
            return mItems.size();
        }

        @Override public GooglePlace getItem(int position) {
            return mItems.get(position);
        }

        @Override public long getItemId(int position) {
            return position;
        }

        @Override public View newView(LayoutInflater inflater, int position, ViewGroup container) {
            return new PlaceViewHolder(inflater).getView();
        }

        @Override public void bindView(GooglePlace item, int position, View view) {
            Distance distance = mDistances.size() > position ? mDistances.get(position) : null;
            ((PlaceViewHolder) view.getTag())
                    .bind(item, mPlaceType, mLocation, distance);
        }

        public void swapItems(GooglePlace[] results) {
            mItems.clear();
            mDistances.clear();
            Collections.addAll(mItems, results);
            notifyDataSetChanged();
        }

        @Override public void onNewLocation(Location location) {
            mLocation = location;
            notifyDataSetChanged();
        }

        @Override public void onNewPlaceType(PlaceType placeType) {
            mPlaceType = placeType;
        }

        public void swapDistances(DistanceRow[] distanceRows) {
            mDistances.clear();
            if (distanceRows != null && distanceRows.length > 0 && distanceRows[0].elements != null) {
                DistanceElement[] elements = distanceRows[0].elements;
                for (DistanceElement element : elements) {
                    mDistances.add(element.distance);
                }
            }
            notifyDataSetChanged();
        }
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

    static class PlaceViewHolder {

        private static final boolean DISTANCE_FROM_GOOGLE = true;
        private final View mView;

        @InjectView(R.id.icon) ImageView mIconView;
        @InjectView(R.id.name) TextView mNameView;
        @InjectView(R.id.rating) RatingBar mRatingView;
        @InjectView(R.id.distance) TextView mDistanceView;

        public PlaceViewHolder(LayoutInflater inflater) {
            mView = inflater.inflate(R.layout.item_place, null);
            mView.setTag(this);
            ButterKnife.inject(this, mView);
        }

        public View getView() {
            return mView;
        }

        public void bind(GooglePlace place, PlaceType placeType, Location location, Distance googleDistance) {
            mIconView.setImageLevel(placeType.ordinal());
            mNameView.setText(place.name);
            mRatingView.setRating(place.rating);

            String distanceText = "--";
            if (DISTANCE_FROM_GOOGLE) {
                if (googleDistance != null) {
                    distanceText = googleDistance.text;
                }
            } else {
                if (location != null && place.geometry != null && place.geometry.location != null) {
                    float meters = LocationHelper
                            .distance(location.getLatitude(), location.getLongitude(), place.geometry.location.lat,
                                    place.geometry.location.lng);
                    NumberFormat format = NumberFormat.getInstance();
                    format.setMaximumFractionDigits(1);
                    distanceText = format.format(meters / 1000 * 0.621) + " mi";
                }
            }
            mDistanceView.setText(distanceText);

        }
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
