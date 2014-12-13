package com.dancedeets.android;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationServices;

import java.util.Locale;

/**
 * Created by lambert on 2014/11/26.
 */
public class FetchLocation implements GoogleApiClient.ConnectionCallbacks {

    private static final String LOG_TAG = "FetchLocation";

    private GoogleApiClient mGoogleApiClient;
    private FusedLocationProviderApi mLocationProviderApi = LocationServices.FusedLocationApi;
    private Geocoder mGeocoder;

    private Location mLocation;
    private ReverseGeocodeTask mReverseGeocodeTask;
    private AddressListener mAddressListener;

    public interface AddressListener {
        public void onAddressFound(Location location, Address address);
    }

    public FetchLocation() {
    }

    private void onAddressFound(Address address) {
        if (mAddressListener != null) {
            mAddressListener.onAddressFound(mLocation, address);
        }
    }

    // FetchLocation should not store this Activity beyond the lifetime of the Activity.
    // So we can safely use it to initialize variables here, but that's it.
    // If we do want to store it (like in FetchLocationWithDialog),
    // then we need to make sure the local reference gets cleaned up appropriately.
    public void onStart(Activity activity, AddressListener addressListener) {
        Log.i(LOG_TAG, "onStart, called from " + activity);
        mAddressListener = addressListener;
        // Connect the client.
        mGeocoder = new Geocoder(activity, Locale.getDefault());
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
            Log.i(LOG_TAG, "Unable to connect to Google Play Services");
        }
    }

    public void onStop() {
        Log.i(LOG_TAG, "onStop");
        // Disconnecting the client invalidates it.
        mGoogleApiClient.disconnect();
        mAddressListener = null;
        if (mReverseGeocodeTask != null) {
            mReverseGeocodeTask.cancel(true);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(LOG_TAG, "GoogleApiClient.onConnected: " + bundle);
        // We reconnect every time the app wakes up, but we only need
        // to fetch on start if we have no location data (ie, app startup).
        mLocation = mLocationProviderApi.getLastLocation(mGoogleApiClient);
        Log.i(LOG_TAG, "Reverse geocoding: " + mLocation);
        if (mLocation != null) {
            // It's okay that this has an implicit reference to this parent class.
            // This class will be around as long as the underlying geocode task is running,
            // and when this class is destroyed via onStop, we cancel this task.
            mReverseGeocodeTask = new ReverseGeocodeTask(mGeocoder) {
                protected void onPostExecute(Address address) {
                    onAddressFound(address);
                }
            };
            mReverseGeocodeTask.execute(mLocation);
        } else {
            onAddressFound(null);
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(LOG_TAG, "GoogleApiClient.onConnectionSuspended: " + cause);
    }
}
