package com.lavruk.cityguide.net;

import com.google.gson.Gson;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public abstract class BaseGoogleRequest<T extends BaseGoogleResponse> extends Request<T> {

    private static final String TAG = BaseGoogleRequest.class.getSimpleName();
    public static final String STATUS_OK = "OK";

    public static final String GOOGLE_API_KEY = "AIzaSyDmWXXyPDgRVVqWohiNLAjMHqPgdfl76aA";

    private final Listener<T> mListener;
    private final Gson mGson;
    Map<String, String> mParams = new HashMap<>();

    public BaseGoogleRequest(Listener<T> listener,
            ErrorListener errorListener) {
        super(Method.GET, null, errorListener);
        mListener = listener;
        mGson = new Gson();
        mParams.put("key", GOOGLE_API_KEY);
        mParams.put("sensor", "true");
    }

    @Override protected Response<T> parseNetworkResponse(NetworkResponse response) {
        String jsonString = new String(response.data);
        Timber.d("%s\n%s", getUrl(), jsonString);

        T googlePlacesResponse = parse(mGson, jsonString);

        if (STATUS_OK.equals(googlePlacesResponse.status)) {
            return Response.success(googlePlacesResponse, getCacheEntry());
        } else {
            return Response.error(new VolleyError(googlePlacesResponse.error_message));
        }
    }

    public abstract T parse(Gson gson, String result);

    @Override protected void deliverResponse(T response) {
        if (mListener != null) {
            mListener.onResponse(response);
        }
    }

    @Override public String getUrl() {
        return "https://maps.googleapis.com/maps/api/place/details/json?" + encodeParameters(mParams,
                getParamsEncoding());
    }

    String encodeParameters(Map<String, String> params, String paramsEncoding) {
        StringBuilder encodedParams = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                encodedParams.append(URLEncoder.encode(entry.getKey(), paramsEncoding));
                encodedParams.append('=');
                encodedParams.append(URLEncoder.encode(entry.getValue(), paramsEncoding));
                encodedParams.append('&');
            }
            return encodedParams.toString();
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("Encoding not supported: " + paramsEncoding, uee);
        }
    }

}
