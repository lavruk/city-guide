package com.lavruk.cityguide.net;

import com.google.gson.Gson;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.lavruk.cityguide.net.DistanceRequest.DistanceResponse;
import com.lavruk.cityguide.net.NearbySearchRequest.GoogleGeometry.GoogleLocation;

import android.location.Location;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;


public class DistanceRequest extends BaseGoogleRequest<DistanceResponse> {

    private static final String TAG = DistanceRequest.class.getSimpleName();

    public DistanceRequest(Location origin, GoogleLocation[] destinations, Listener<DistanceResponse> listener,
            ErrorListener errorListener) {
        super(listener, errorListener);
        mParams.put("origins", origin.getLatitude() + "," + origin.getLongitude());

        List<String> destinationsList = new ArrayList<>();
        for (GoogleLocation destination : destinations) {
            destinationsList.add(destination.lat + "," + destination.lng);
        }
        mParams.put("destinations", TextUtils.join("|", destinationsList));

        mParams.put("units", "imperial");
        mParams.put("language", "en");
    }

    @Override
    public DistanceResponse parse(Gson gson, String result) {
        return gson.fromJson(result, DistanceResponse.class);
    }

    @Override public String getUrl() {
        return "https://maps.googleapis.com/maps/api/distancematrix/json?" + encodeParameters(mParams,
                getParamsEncoding());
    }


    public static class DistanceResponse extends BaseGoogleResponse {

        public DistanceRow[] rows;
    }

    public static class DistanceElement {

        public String status;// "OK"
        public Distance distance;
    }

    public static class Distance {

        public int value;// 1734542,
        public String text;// "1 735 km"
    }

    public static class DistanceRow {

        public DistanceElement[] elements;
    }

}
