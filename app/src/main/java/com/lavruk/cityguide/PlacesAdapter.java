package com.lavruk.cityguide;

import com.lavruk.cityguide.PlacesFragment.PlaceType;
import com.lavruk.cityguide.PlacesFragment.PlacesAdapterBridge;
import com.lavruk.cityguide.net.DistanceRequest.Distance;
import com.lavruk.cityguide.net.DistanceRequest.DistanceElement;
import com.lavruk.cityguide.net.DistanceRequest.DistanceRow;
import com.lavruk.cityguide.net.NearbySearchRequest.GooglePlacesResponse.GooglePlace;
import com.lavruk.cityguide.tools.BindableAdapter;
import com.lavruk.cityguide.tools.LocationHelper;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class PlacesAdapter extends BindableAdapter<GooglePlace> implements PlacesAdapterBridge {

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
}
