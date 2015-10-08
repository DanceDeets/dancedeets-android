package com.dancedeets.android;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationServices;

/**
 * Created by lambert on 2014/11/26.
 */
public class FetchLocation implements GoogleApiClient.ConnectionCallbacks {

    private static final String LOG_TAG = "FetchLocation";

    private GoogleApiClient mGoogleApiClient;
    private FusedLocationProviderApi mLocationProviderApi = LocationServices.FusedLocationApi;

    private LocationListener mLocationListener;

    public interface LocationListener {
        void onLocationFound(Location location);
    }

    public FetchLocation() {
    }

    private void onLocationFound(Location location) {
        if (mLocationListener != null) {
            mLocationListener.onLocationFound(location);
        }
    }

    // FetchLocation should not store this Activity beyond the lifetime of the Activity.
    // So we can safely use it to initialize variables here, but that's it.
    // If we do want to store it (like in FetchLocationWithDialog),
    // then we need to make sure the local reference gets cleaned up appropriately.
    public void onStart(Activity activity, LocationListener addressListener) {
        Log.i(LOG_TAG, "onStart, called from " + activity);
        mLocationListener = addressListener;
        // Connect the client.
        initializeGoogleApiClient(activity);
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    protected boolean areGooglePlayServicesConnected(Activity activity) {
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(activity);
        return (ConnectionResult.SUCCESS == resultCode);
    }

    protected void initializeGoogleApiClient(Activity activity) {
        if (areGooglePlayServicesConnected(activity)) {
            mGoogleApiClient = new GoogleApiClient.Builder(activity.getBaseContext())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .build();
        } else {
            Log.e(LOG_TAG, "Unable to connect to Google Play Services");
            onLocationFound(null);
        }
    }

    public void onStop() {
        Log.i(LOG_TAG, "onStop");
        // Disconnecting the client invalidates it.
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        mLocationListener = null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(LOG_TAG, "GoogleApiClient.onConnected: " + bundle);
        // We reconnect every time the app wakes up, but we only need
        // to fetch on start if we have no location data (ie, app startup).
        Location location = mLocationProviderApi.getLastLocation(mGoogleApiClient);
        Log.i(LOG_TAG, "Reverse geocoding: " + location);
        onLocationFound(location);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(LOG_TAG, "GoogleApiClient.onConnectionSuspended: " + cause);
    }
}
