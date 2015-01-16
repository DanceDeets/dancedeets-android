package com.dancedeets.android.models;

import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents an Event as returned by /api/events/XXX with the full set of fields.
 */
public class FullEvent extends Event {

    static DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    static DateFormat isoDateTimeFormatWithTZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    protected CoverData mCoverData;
    protected Venue mVenue;
    protected List<NamedPerson> mAdminList;

    protected FullEvent() {
    }

    static public FullEvent parse(Bundle b) {
        return (FullEvent) Event.parse(b);
    }

    static public FullEvent parse(JSONObject jsonEvent) throws JSONException {
        FullEvent event = new FullEvent();

        event.mId = jsonEvent.getString("id");
        event.mTitle = jsonEvent.getString("name");
        event.mDescription = jsonEvent.getString("description");

        String startTimeString = jsonEvent.getString("start_time");
        try {
            Date date = isoDateTimeFormatWithTZ.parse(startTimeString);
            event.mStartTime = date.getTime();
        } catch (ParseException e1) {
            try {
                Date date = isoDateFormat.parse(startTimeString);
                event.mStartTime = date.getTime();
                event.mAllDayEvent = true;
            } catch (ParseException e2) {
                throw new JSONException("ParseException on start_time string: " + startTimeString);
            }
        }
        if (!jsonEvent.isNull("end_time")) {
            String endTimeString = jsonEvent.getString("end_time");
            try {
                Date date = isoDateTimeFormatWithTZ.parse(endTimeString);
                event.mEndTime = date.getTime();
            } catch (ParseException e) {
                // Don't make this a fatal error, so we still see the events in the list view!
                throw new JSONException("ParseException on end_time string: " + endTimeString);
            }
        }

        if (!jsonEvent.isNull("cover")) {
            JSONObject jsonCover = jsonEvent.getJSONObject("cover");
            event.mCoverData = CoverData.parse(jsonCover);
            event.mCoverUrl = event.mCoverData.getLargestCover().getSourceUrl();
        }
        event.mImageUrl = jsonEvent.getString("picture");

        JSONObject jsonVenue = jsonEvent.getJSONObject("venue");
        event.mVenue = Venue.parse(jsonVenue);

        if (jsonEvent.isNull("admins")) {
            event.mAdminList = new ArrayList<>();
        } else {
            JSONArray jsonAdmins = jsonEvent.getJSONArray("admins");
            event.mAdminList = new ArrayList<>(jsonAdmins.length());
            for (int i = 0; i < jsonAdmins.length(); i++) {
                JSONObject jsonAdmin = jsonAdmins.getJSONObject(i);
                NamedPerson admin = NamedPerson.parse(jsonAdmin);

                event.mAdminList.add(admin);
            }
        }
        return event;
    }

    public CoverData getCoverData() {
        return mCoverData;
    }

    public Venue getVenue() {
        return mVenue;
    }

    // TODO: this is returning a mutable list, and violates our immutability guarantees.
    public List<NamedPerson> getAdmins() {
        return mAdminList;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (((Object)this).getClass() != o.getClass()) return false;
        FullEvent other = (FullEvent)o;
        return (super.equals(o) &&
                (mCoverData == null ? other.mCoverData == null : mCoverData.equals(other.mCoverData)) &&
                mVenue.equals(other.mVenue) &&
                mAdminList.equals(other.mAdminList)
        );
    }
}