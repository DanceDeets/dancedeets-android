package com.dancedeets.android;

import com.dancedeets.android.models.FullEvent;

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
        String result = "Check out this dance event!" +
                "\n" + event.getUrl() +
                "\n" +
                "\nEvent: " + event.getTitle() +
                "\nWhen: " + event.getStartTimeString();
        if (event.getVenue().hasName()) {
            result += "\nWhere: " + event.getVenue().getName();
        }
        return result;
    }
}
