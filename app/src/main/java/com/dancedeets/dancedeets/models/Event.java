package com.dancedeets.dancedeets.models;

import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by lambert on 2014/10/02.
 */
public class Event extends IdEvent {

    static String LOG_TAG = "Event";

    static DateFormat localizedDateFormat = DateFormat.getDateInstance();
    static DateFormat localizedDateTimeFormat = DateFormat.getDateTimeInstance();

    static DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    protected String mTitle;
    protected String mLocation;
    protected String mDescription;
    protected long mStartTime;
    protected long mEndTime;
    protected boolean mAllDayEvent;

    protected String mImageUrl;
    protected String mCoverUrl;


    protected Event() {
    }

    static public Event parse(Bundle b) {
        Event event = (Event)b.getSerializable("EVENT");
        return event;
    }

    static public Event parse(JSONObject jsonEvent) throws JSONException {
        Event event = new Event();
        event.mImageUrl = jsonEvent.getString("image_url");
        if (!jsonEvent.isNull("cover_url")) {
            event.mCoverUrl = jsonEvent.getJSONObject("cover_url").getString("source");
        }

        event.mId = jsonEvent.getString("id");
        event.mTitle = jsonEvent.getString("title");
        event.mLocation = jsonEvent.getString("location");
        event.mDescription = jsonEvent.getString("description");

        String startTimeString = jsonEvent.getString("start_time");
        try {
            Date date = isoDateFormat.parse(startTimeString);
            event.mStartTime = date.getTime();
        } catch (ParseException e) {
            throw new JSONException("ParseException on start_time string: " + startTimeString + ": " + e);
        }
        String endTimeString = jsonEvent.getString("end_time");
        try {
            Date date = isoDateFormat.parse(endTimeString);
            event.mEndTime = date.getTime();
        } catch (ParseException e) {
            // Don't make this a fatal error, so we still see the events in the list view!
            Log.e(LOG_TAG, "ParseException on end_time string: " + endTimeString + ": " + e);
        }
        return event;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getCoverUrl() {
        return mCoverUrl;
    }

    public String getThumbnailUrl() {
        return mImageUrl;
    }

    public long getStartTimeLong() {
        return mStartTime;
    }

    public String getStartTimeString() {
        if (mAllDayEvent) {
            return localizedDateFormat.format(getStartTimeLong());
        } else {
            return localizedDateTimeFormat.format(getStartTimeLong());
        }
    }

    public long getEndTimeLong() {
        return mEndTime;
    }

    public String getEndTimeString() {
        if (mAllDayEvent) {
            return localizedDateFormat.format(getEndTimeLong());
        } else {
            return localizedDateTimeFormat.format(getEndTimeLong());
        }
    }

    public String getLocation() {
        return mLocation;
    }

    public String getDescription() {
        return mDescription;
    }
}
