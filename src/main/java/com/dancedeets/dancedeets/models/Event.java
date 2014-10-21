package com.dancedeets.dancedeets.models;

import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by lambert on 2014/10/02.
 */
public class Event extends IdEvent {

    static String LOG_TAG = "Event";

    static DateFormat localizedDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
    static DateFormat localizedDateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

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
        if (!jsonEvent.isNull("end_time")) {
            String endTimeString = jsonEvent.getString("end_time");
            try {
                Date date = isoDateFormat.parse(endTimeString);
                event.mEndTime = date.getTime();
            } catch (ParseException e) {
                // Don't make this a fatal error, so we still see the events in the list view!
                Log.e(LOG_TAG, "ParseException on end_time string: " + endTimeString + ": " + e);
            }
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
        if (getStartTimeLong() != 0) {
            if (mAllDayEvent) {
                return localizedDateFormat.format(getStartTimeLong());
            } else {
                return localizedDateTimeFormat.format(getStartTimeLong());
            }
        } else {
            return null;
        }
    }

    public String getStartTimeString(Locale locale) {
        if (getStartTimeLong() != 0) {
            if (mAllDayEvent) {
                return DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(getStartTimeLong());
            } else {
                return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(getStartTimeLong());
            }
        } else {
            return null;
        }
    }

    public long getEndTimeLong() {
        return mEndTime;
    }

    public String getEndTimeString() {
        if (getEndTimeLong() != 0) {
            if (mAllDayEvent) {
                return localizedDateFormat.format(getEndTimeLong());
            } else {
                return localizedDateTimeFormat.format(getEndTimeLong());
            }
        } else {
            return null;
        }
    }

    public String getEndTimeString(Locale locale) {
        if (getEndTimeLong() != 0) {
            if (mAllDayEvent) {
                return DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(getEndTimeLong());
            } else {
                return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(getEndTimeLong());
            }
        } else {
            return null;
        }
    }

    public String getLocation() {
        return mLocation;
    }

    public String getDescription() {
        return mDescription;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (((Object)this).getClass() != o.getClass()) return false;
        Event other = (Event)o;
        return (super.equals(o) &&
                mTitle.equals(other.mTitle) &&
                mLocation.equals(other.mLocation) &&
                mDescription.equals(other.mDescription) &&
                (mImageUrl == null ? other.mImageUrl == null : mImageUrl.equals(other.mImageUrl)) &&
                (mCoverUrl == null ? other.mCoverUrl == null : mCoverUrl.equals(other.mCoverUrl)) &&
                mStartTime == other.mStartTime &&
                mEndTime == other.mEndTime &&
                mAllDayEvent == other.mAllDayEvent
        );
    }
}
