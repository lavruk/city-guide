package com.lavruk.cityguide.net;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.lavruk.cityguide.App;
import com.lavruk.cityguide.events.DistancesResponseEvent;
import com.lavruk.cityguide.events.PlacesResponseEvent;
import com.lavruk.cityguide.net.DistanceRequest.DistanceResponse;
import com.lavruk.cityguide.net.NearbySearchRequest.GoogleGeometry.GoogleLocation;
import com.lavruk.cityguide.net.NearbySearchRequest.GooglePlacesResponse;
import com.lavruk.cityguide.net.NearbySearchRequest.GooglePlacesResponse.GooglePlace;

import android.app.Application;
import android.location.Location;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class Api {

    public static final Object PLACES_REQUESTS_TAG = "places_requests_tag";

    @Inject EventBus mBus;
    @Inject RequestQueue mQueue;
    private Request<GooglePlacesResponse> mPlacesRequest;
    private Request<DistanceResponse> mDistanceRequest;

    public Api(Application app) {
        ((App) app).inject(this);
    }

    public void requestPlaces(final String type, final Location location) {

        if (mPlacesRequest != null && !mPlacesRequest.isCanceled()) {
            mPlacesRequest.cancel();
            mPlacesRequest = null;
        }

        mPlacesRequest = mQueue
                .add(new NearbySearchRequest(location.getLatitude(), location.getLongitude(), new String[]{type},
                        new Listener<GooglePlacesResponse>() {
                            @Override public void onResponse(GooglePlacesResponse response) {
                                mBus.post(PlacesResponseEvent.builder().type(type).response(response).build());
                                requestDistances(location, response.results);
                            }
                        }, new ErrorListener() {
                    @Override public void onErrorResponse(VolleyError error) {
                        mBus.post(PlacesResponseEvent.builder().type(type).error(error).build());
                    }
                }));
        mPlacesRequest.setTag(PLACES_REQUESTS_TAG);
    }

    private void requestDistances(Location location, GooglePlace[] places) {

        if (mDistanceRequest != null && !mDistanceRequest.isCanceled()) {
            mDistanceRequest.cancel();
            mDistanceRequest = null;
        }

        GoogleLocation[] destinations = new GoogleLocation[places.length];
        for (int i = 0; i < places.length; i++) {
            destinations[i] = places[i].geometry.location;
        }

        mDistanceRequest = mQueue.add(new DistanceRequest(location, destinations, new Listener<DistanceResponse>() {
            @Override public void onResponse(DistanceResponse response) {
                mBus.post(DistancesResponseEvent.builder().response(response).build());
            }
        }, new ErrorListener() {
            @Override public void onErrorResponse(VolleyError error) {
                mBus.post(DistancesResponseEvent.builder().error(error).build());
            }
        }));
        mDistanceRequest.setTag(PLACES_REQUESTS_TAG);
    }
}
