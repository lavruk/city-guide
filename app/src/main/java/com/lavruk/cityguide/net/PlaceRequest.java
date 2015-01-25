package com.lavruk.cityguide.net;

import com.google.gson.Gson;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.lavruk.cityguide.net.PlaceRequest.GooglePlaceResponse;
import com.lavruk.cityguide.net.NearbySearchRequest.GoogleGeometry;

public class PlaceRequest extends BaseGoogleRequest<GooglePlaceResponse> {

    private static final String TAG = PlaceRequest.class.getSimpleName();

    public PlaceRequest(String placeId, Listener<GooglePlaceResponse> listener,
            ErrorListener errorListener) {
        super(listener, errorListener);
        mParams.put("sensor", "true");
        mParams.put("placeid", placeId);
    }

    @Override public GooglePlaceResponse parse(Gson gson, String result) {
        return gson.fromJson(result, GooglePlaceResponse.class);
    }

    @Override public String getUrl() {
        return "https://maps.googleapis.com/maps/api/place/details/json?" + encodeParameters(mParams,
                getParamsEncoding());
    }

    public static class GooglePlaceResponse extends BaseGoogleResponse {

        public GooglePlaceFull result;

        public static class GooglePlaceFull {

            public String adr_address;
            public String formatted_address;
            public String formatted_phone_number;
            public String international_phone_number;
            public GoogleGeometry geometry;
            public String icon;//"http://maps.gstatic.com/mapfiles/place_api/icons/shopping-71.png",
            public String id;//"81287ef4a724f0115676143f7c0922b875bffa97",
            public String name;//"City Convenience Store",
            public String place_id;//"ChIJ1-v38TauEmsRJyNyOau3KU8",
            public String reference;
            public String scope;//"GOOGLE",
            public String[] types;//["convenience_store","food","store","establishment"],
            public String vicinity;//"r2/80 Pyrmont St, Pyrmont"
            public float rating;// 3.6,
            public GooglePlaceReview[] reviews;
            public String url;// "https://plus.google.com/111053982514455096989/about?hl=en",
            public float user_ratings_total;// 59,
            public long utc_offset;// 0,
            public String website;// "http://www.shanghaiblues.co.uk/"
            public OpeningHours opening_hours;
        }


        public static class OpeningHours {

            public String[] weekday_text;
            public boolean open_now;
        }
    }

    public static class GooglePlaceReview {

        public GooglePlaceAspect[] aspects;
        public String author_name;// "Lorraine Bailey",
        public String author_url;// "https://plus.google.com/109549515595232923356",
        public String language;// "en",
        public float rating;// 4,
        public String text;
        // "A friend of mine who is a real Chinese food connoisseur brought me here. I was also told by colleagues that this is one of the most authentic Chinese restaurants in London. So my expectations for the place were pretty high. I had dim sum and hands down it was great. In short, my experience here wasn’t bad but it could be better.",
        public long time;// 1400834535
    }


    public static class GooglePlaceAspect {

        public float rating;// 2,
        public String type;// "overall"
    }

}
