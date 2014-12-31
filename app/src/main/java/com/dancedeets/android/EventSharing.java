package com.dancedeets.android;

import android.net.Uri;
import android.text.TextUtils;

import com.dancedeets.android.models.FullEvent;
import com.dancedeets.android.models.Venue;

/**
 * Encapsulates the common methods used by intents when sharing our events.
 */
public class EventSharing {
    public static String getTitle(FullEvent event) {
        return event.getTitle();
    }

    public static String getBodyText(FullEvent event) {
        //TODO: Localize this.
        //TODO: Fill out location with proper data
        return "Check out this dance event!" +
                "\n" + event.getUrl() +
                "\n" +
                "\nEvent: " + event.getTitle() +
                "\nWhen: " + event.getStartTimeString() +
                "\nWhere: " + event.getLocation();
    }

    public static String getBodyHtml(FullEvent event) {
        Venue venue = event.getVenue();
        Venue.LatLong latLong = venue.getLatLong();
        String locationUrl = "https://www.google.com/maps/place/" + Uri.encode(venue.getName()) + "/@" + latLong.getLatitude() + "," + latLong.getLongitude();
        return (
                "Check out this dance event!" +
                "<br>\nEvent: <a href=\"" + TextUtils.htmlEncode(event.getUrl()) + "\">" + TextUtils.htmlEncode(event.getTitle()) + "</a>" +
                "<br>\nWhen: " + TextUtils.htmlEncode(event.getStartTimeString()) +
                "<br>\nWhere: <a href=\"" + TextUtils.htmlEncode(locationUrl) + "\">" + TextUtils.htmlEncode(event.getLocation()) + "</a>"
        );
    }

}
