package com.dancedeets.android;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
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
        void onLocationFound(Location location );
        void onAddressFound(Location location, Address address);
    }

    public FetchLocation() {
    }

    public static String formatAddress(Address address) {
        List<String> components = new ArrayList<>();
        // We really need *something* smaller than the AdminArea/State level.
        // Especially since there might not be a State sometimes (ie Helsinki, Finland).
        if (address.getLocality() != null) {
            // Sometimes there is only a Locality:
            // LatLong=35.6583942,139.6990928
            // SubLocality=null
            // Locality=Shibuya
            // SubAdminArea=null
            // AdminArea=Tokyo
            components.add(address.getLocality());
        } else if (address.getSubAdminArea() != null) {
            // Sometimes there is only a SubAdminArea:
            // LatLong=60.1836354,24.9206748
            // SubLocality=null
            // Locality=null
            // SubAdminArea=Helsinki
            // AdminArea=null
            components.add(address.getSubAdminArea());
        } else if (address.getSubLocality() != null) {
            // Sometimes there is only a SubLocality:
            // LatLong=40.790278,-73.959722
            // SubLocality=Dundas
            // Locality=null
            // SubAdminArea=null
            // AdminArea=Ontario
            // Dundas appears to be the smallest unit of geography,
            // so we check for it in the third if-block, hoping to find a proper city first.
            components.add(address.getSubLocality());
        }
        // Sometimes there is too much data, and we want to drop a lot of it:
        // LatLong=40.790278,-73.959722
        // SubLocality=Manhattan
        // Locality=New York (the city)
        // SubAdminArea=New York (the county)
        // AdminArea=New York (the state)
        // In this case, we just want to grab the Locality (first if-block above)

        // Then grab the States/Province/etc (for those who have it)
        if (address.getAdminArea() != null) {
            components.add(address.getAdminArea());
        }
        // And finally the Country, which should always be there...unless....I'm on a boat!
        // So let's be safe and make this optional, in which case we basically take whatever we can get
        if (address.getCountryName() != null) {
            components.add(address.getCountryName());
        }
        return TextUtils.join(", ", components);
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
            Log.e(LOG_TAG, "Unable to connect to Google Play Services");
            onAddressFound(null);
        }
    }

    public void onStop() {
        Log.i(LOG_TAG, "onStop");
        // Disconnecting the client invalidates it.
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
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
            if (mAddressListener != null) {
                mAddressListener.onLocationFound(mLocation);
            }
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
