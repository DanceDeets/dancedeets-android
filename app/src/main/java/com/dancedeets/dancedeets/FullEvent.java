package com.dancedeets.dancedeets;

import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents an Event as returned by /api/events/XXX with the full set of fields.
 */
public class FullEvent extends Event {

    static String LOG_TAG = "FullEvent";

    static DateFormat isoDateFormatWithTZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    protected FullEvent() {}

    static public FullEvent parse(Bundle b) {
        return (FullEvent)Event.parse(b);
    }

    static public FullEvent parse(JSONObject jsonEvent) throws JSONException {
        FullEvent event = new FullEvent();

        event.mId = jsonEvent.getString("id");
        event.mTitle = jsonEvent.getString("name");
        event.mDescription = jsonEvent.getString("description");

        String startTimeString = jsonEvent.getString("start_time");
        try {
            Date date = isoDateFormatWithTZ.parse(startTimeString);
            event.mStartTime = date.getTime();
        } catch (ParseException e) {
            throw new JSONException("ParseException on start_time string: " + startTimeString + ": " + e);
        }
        String endTimeString = jsonEvent.optString("end_time", null);
        if (endTimeString != null) {
            try {
                Date date = isoDateFormatWithTZ.parse(endTimeString);
                event.mEndTime = date.getTime();
            } catch (ParseException e) {
                // Don't make this a fatal error, so we still see the events in the list view!
                Log.e(LOG_TAG, "ParseException on end_time string: " + endTimeString + ": " + e);
            }
        }

        if (!jsonEvent.isNull("cover")) {
            JSONObject cover = jsonEvent.getJSONObject("cover");
            cover.getString("cover_id");
            cover.getJSONArray("images");
        }
        //TODO: Do we even return an imageurl anymore? Isn't this deprecated and what we want to move away from?
        // event.mImageUrl = jsonEvent.getString("image_url");
        event.mCoverUrl = ((JSONObject) jsonEvent.getJSONObject("cover").getJSONArray("images").get(0)).getString("source");
        event.mLocation = jsonEvent.getJSONObject("venue").getString("name");

        return event;
    }
}