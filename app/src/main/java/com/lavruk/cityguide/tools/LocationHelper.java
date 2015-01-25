package com.lavruk.cityguide.tools;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocationHelper implements ConnectionCallbacks, OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = LocationHelper.class.getSimpleName();

    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL = 10 * DateUtils.SECOND_IN_MILLIS;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL = 5 * DateUtils.SECOND_IN_MILLIS;

    private final Context mContext;
    private final GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private long mLastLocationTime;

    private List<LocationCallback> mLocationCallbacks = new ArrayList<LocationCallback>();

    @Inject public LocationHelper(Application application) {
        mContext = application;
        mGoogleApiClient = buildGoogleApiClient();
    }

    public static float distance(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude) {
        float[] results = new float[1];
        Location.distanceBetween(fromLatitude, fromLongitude, toLatitude, toLongitude,
                results);
        return results[0];
    }

    protected GoogleApiClient buildGoogleApiClient() {
        return new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(mContext);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates",
                    "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason.
            // resultCode holds the error code.
        } else {
            String errorString = GooglePlayServicesUtil.getErrorString(resultCode);
            Toast.makeText(mContext, errorString, Toast.LENGTH_LONG).show();
            Log.e(TAG, errorString);
            return true;
        }
    }

    @Override public void onConnected(Bundle bundle) {
        startLocationUpdate();
    }

    public void startLocationUpdate() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override public void onConnectionSuspended(int i) {

    }

    @Override public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public void stopLocationUpdate() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    public void connect() {
        if (!servicesConnected()) {
            return;
        }
        mGoogleApiClient.connect();
    }

    public void disconnect() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override public void onLocationChanged(Location location) {
        mLastLocation = location;
        mLastLocationTime = System.currentTimeMillis();
        for (LocationCallback locationCallback : mLocationCallbacks) {
            if (locationCallback != null) {
                locationCallback.onLocationChanged(mLastLocation, mLastLocationTime);
            }
        }
    }

    public void addLocationCallbackListener(LocationCallback callback) {
        mLocationCallbacks.add(callback);
    }

    public void removeLocationCallbackListener(LocationCallback callback) {
        mLocationCallbacks.remove(callback);
    }

    public interface LocationCallback {

        void onLocationChanged(Location location, long time);
    }
}
