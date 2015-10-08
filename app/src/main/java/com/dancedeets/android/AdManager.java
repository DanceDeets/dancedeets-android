package com.dancedeets.android;

import android.location.Location;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;

/**
 * Created by lambert on 2015/10/08.
 */
public class AdManager {

    public static PublisherAdRequest getAdRequest(Location location) {
        PublisherAdRequest adRequest = new PublisherAdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("F92F46CF903B1E0BD86D386FC9813E7F")
                .setLocation(location)
                .build();
        return adRequest;
    }
}
