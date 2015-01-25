package com.lavruk.cityguide.net;

import com.google.gson.Gson;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.lavruk.cityguide.net.NearbySearchRequest.GooglePlacesResponse;

import android.text.TextUtils;

import hrisey.Parcelable;


public class NearbySearchRequest extends BaseGoogleRequest<GooglePlacesResponse> {

    private static final String TAG = NearbySearchRequest.class.getSimpleName();

    public NearbySearchRequest(double lat, double lon, String[] types, Listener<GooglePlacesResponse> listener,
            ErrorListener errorListener) {
        super(listener, errorListener);
        mParams.put("language", "en");
        mParams.put("location", lat + "," + lon);
        mParams.put("rankby", "distance");
//        mParams.put("radius", "10000"); //1000m
        mParams.put("types", TextUtils.join("|", types));
    }

    public NearbySearchRequest(String nextPageToken, Listener<GooglePlacesResponse> listener,
            ErrorListener errorListener) {
        super(listener, errorListener);
        mParams.put("pagetoken", nextPageToken);
    }

    @Override
    public GooglePlacesResponse parse(Gson gson, String result) {
        return gson.fromJson(result, GooglePlacesResponse.class);
    }

    @Override public String getUrl() {
        return "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" + encodeParameters(mParams,
                getParamsEncoding());
    }


    public static class GooglePlacesResponse extends BaseGoogleResponse {

        public String next_page_token;
        public GooglePlace[] results;


        @Parcelable
        public static class GooglePlace implements android.os.Parcelable {

            public GoogleGeometry geometry;
            public String icon;//"http://maps.gstatic.com/mapfiles/place_api/icons/shopping-71.png",
            public String id;//"81287ef4a724f0115676143f7c0922b875bffa97",
            public String name;//"City Convenience Store",
            public String place_id;//"ChIJ1-v38TauEmsRJyNyOau3KU8",
            public String reference;
            public String scope;//"GOOGLE",
            public String[] types;//["convenience_store","food","store","establishment"],
            public String vicinity;//"r2/80 Pyrmont St, Pyrmont"
            public float rating;
        }


    }

    @Parcelable
    public static class GoogleGeometry {

        public GoogleLocation location;

        @Parcelable
        public static class GoogleLocation {

            public double lat;// -33.868173,
            public double lng;// 151.194652
        }
    }

}
