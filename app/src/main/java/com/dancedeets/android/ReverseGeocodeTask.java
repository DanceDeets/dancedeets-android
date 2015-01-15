package com.dancedeets.android;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.List;

/**
 * A subclass of AsyncTask that calls getFromLocation() in the
 * background. The class definition has these generic types:
 * Location - A Location object containing
 * the current location.
 * Void     - indicates that progress units are not used
 * String   - An city passed to onPostExecute()
 */
public abstract class ReverseGeocodeTask extends
        AsyncTask<Location, Void, Address> {

    private static final String LOG_TAG = "ReverseGeocodeTask";

    private Geocoder mGeocoder;

    public ReverseGeocodeTask(Geocoder geocoder) {
        super();
        mGeocoder = geocoder;
    }

    /**
     * Get a Geocoder instance, get the latitude and longitude
     * look up the city, and return it
     *
     * @params params One or more Location objects
     * @return An Address of the current location,
     * or an empty string if no address can be found,
     * or an error message
     */
    @Override
    protected Address doInBackground(Location... params) {
        // Get the current location from the input parameter list
        Location loc = params[0];
        // Create a list to contain the result address
        List<Address> addresses = null;
        try {
            /*
             * Return 1 address.
             */
            addresses = mGeocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
        } catch (IOException e1) {
            Log.e(LOG_TAG,
                    "IO Exception in getFromLocation()");
            e1.printStackTrace();
            return null;
        } catch (IllegalArgumentException e2) {
            // Error message to post in the log
            String errorString = "Illegal arguments " +
                    Double.toString(loc.getLatitude()) +
                    " , " +
                    Double.toString(loc.getLongitude()) +
                    " passed to address service";
            Log.e(LOG_TAG, errorString);
            e2.printStackTrace();
            return null;
        }
        // If the reverse geocode returned an address
        if (addresses != null && addresses.size() > 0) {
            // Get the first address
            Address address = addresses.get(0);
            return address;
        } else {
            return null;
        }
    }
    /**
     * A method that's called once doInBackground() completes. Turn
     * off the indeterminate activity indicator and set
     * the text of the UI element that shows the address. If the
     * lookup failed, display the error message.
     */
    @Override
    protected abstract void onPostExecute(Address address);
}
