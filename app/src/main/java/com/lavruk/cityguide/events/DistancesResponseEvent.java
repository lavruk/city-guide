package com.lavruk.cityguide.events;

import com.android.volley.VolleyError;
import com.lavruk.cityguide.net.DistanceRequest.DistanceResponse;

import lombok.Value;
import lombok.experimental.Builder;

@Builder @Value
public class DistancesResponseEvent {

    DistanceResponse response;
    VolleyError error;

}
