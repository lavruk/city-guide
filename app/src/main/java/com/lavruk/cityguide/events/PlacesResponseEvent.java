package com.lavruk.cityguide.events;

import com.android.volley.VolleyError;
import com.lavruk.cityguide.net.NearbySearchRequest.GooglePlacesResponse;

import lombok.Value;
import lombok.experimental.Builder;

@Builder @Value
public class PlacesResponseEvent {

    String type;
    GooglePlacesResponse response;
    VolleyError error;

}
