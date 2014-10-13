package com.dancedeets.dancedeets;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by lambert on 2014/10/02.
 */
public class Event implements Parcelable {
    Bundle mBundle;
    static DateFormat localizedFormat = DateFormat.getDateTimeInstance();

    static DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public Event(Bundle b) {
        mBundle = (Bundle)b.clone();
    }

    public Event(JSONObject jsonEvent) throws JSONException {
        mBundle = new Bundle();
        mBundle.putString("image_url", jsonEvent.getString("image_url"));
        if (!jsonEvent.isNull("cover_url")) {
            mBundle.putString("cover_url", jsonEvent.getJSONObject("cover_url").getString("source"));
        }

        mBundle.putString("id", jsonEvent.getString("id"));
        mBundle.putString("title", jsonEvent.getString("title"));
        mBundle.putString("location", jsonEvent.getString("location"));
        mBundle.putString("description", jsonEvent.getString("description"));

        String startTimeString = jsonEvent.getString("end_time");
        try {
            Date date = isoDateFormat.parse(startTimeString);
            mBundle.putLong("start_time", date.getTime());
        } catch (ParseException e) {
            new JSONException("ParseException on date string: " + startTimeString + ": " + e);
        }
        String endTimeString = jsonEvent.getString("end_time");
        try {
            Date date = isoDateFormat.parse(endTimeString);
            mBundle.putLong("end_time", date.getTime());
        } catch (ParseException e) {
            new JSONException("ParseException on date string: " + endTimeString + ": " + e);
        }
    }

    public String getId() {
        return mBundle.getString("id");
    }

    public String getTitle() {
        return mBundle.getString("title");
    }

    public String getCoverUrl() {
        if (mBundle.containsKey("cover_url")) {
            return mBundle.getString("cover_url");
        } else {
            return null;
        }
    }

    public Bundle getBundle() {
        return (Bundle)mBundle.clone();
    }

    public String getThumbnailUrl() {
        return mBundle.getString("image_url");
    }

    public long getStartTimeLong() {
        return mBundle.getLong("start_time");
    }

    public String getStartTimeString() {
        return localizedFormat.format(getStartTimeLong());
    }

    public long getEndTimeLong() {
        return mBundle.getLong("end_time");
    }

    public String getEndTimeString() {
        return localizedFormat.format(getEndTimeLong());
    }

    public String getLocation() {
        return mBundle.getString("location");
    }

    public String getDescription() {
        return mBundle.getString("description");
    }

    public String getUrl() {
        return "http://www.dancedeets.com/events/" + getId() + "/";
    }

    public String getFacebookUrl() {
        return "http://www.facebook.com/events/" + getId() + "/";
    }

    public String getApiDataUrl() {
        return "http://www.dancedeets.com/api/events/" + getId();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeBundle(mBundle);
    }

    public static final Parcelable.Creator<Event> CREATOR
            = new Parcelable.Creator<Event>() {
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

    private Event(Parcel in) {
        mBundle = in.readBundle();
    }
}
