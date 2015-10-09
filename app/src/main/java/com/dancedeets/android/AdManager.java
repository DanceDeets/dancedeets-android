package com.dancedeets.android;

import android.location.Location;

import com.facebook.AccessToken;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;

/**
 * Created by lambert on 2015/10/08.
 */
public class AdManager {

    public static PublisherAdRequest getAdRequest(Location location) {
        PublisherAdRequest.Builder adRequestBuilder = new PublisherAdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("F92F46CF903B1E0BD86D386FC9813E7F") // Mike's Nexus 5
                .setLocation(location);
        AccessToken token = AccessToken.getCurrentAccessToken();
        if (token != null) {
            String userID = token.getUserId();
            adRequestBuilder.setPublisherProvidedId(Hashing.md5(userID));
        }
        return adRequestBuilder.build();
    }
}
