package com.dancedeets.android.models;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Represents an Event as returned by /api/events/XXX with the full set of fields.
 */
public class FullEvent implements Parcelable, Serializable {

    static DateFormat localizedDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
    static DateFormat localizedDateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    static DateFormat localizedTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

    static DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    static DateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    static DateFormat isoDateTimeFormatWithTZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    protected String mId;


    protected String mTitle;
    protected String mDescription;
    protected long mStartTime;
    protected long mEndTime;
    protected boolean mAllDayEvent;

    protected String mImageUrl;
    protected String mCoverUrl;

    protected CoverData mCoverData;
    protected Venue mVenue;
    protected List<NamedPerson> mAdminList;

    protected FullEvent() {
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
                Date date = isoDateTimeFormat.parse(startTimeString);
                event.mStartTime = date.getTime();
            } catch (ParseException e2) {
                try {
                    Date date = isoDateFormat.parse(startTimeString);
                    event.mStartTime = date.getTime();
                    event.mAllDayEvent = true;
                } catch (ParseException e3) {
                    throw new JSONException("ParseException on start_time string: " + startTimeString);
                }
            }
        }
        if (!jsonEvent.isNull("end_time")) {
            String endTimeString = jsonEvent.getString("end_time");
            try {
                Date date = isoDateTimeFormatWithTZ.parse(endTimeString);
                event.mEndTime = date.getTime();
            } catch (ParseException e) {
                try {
                    Date date = isoDateTimeFormat.parse(endTimeString);
                    event.mEndTime = date.getTime();
                } catch (ParseException e2) {
                    try {
                        Date date = isoDateFormat.parse(endTimeString);
                        event.mEndTime = date.getTime();
                    } catch (ParseException e3) {
                        throw new JSONException("ParseException on end_time string: " + endTimeString);
                    }
                }
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

    public String getId() {
        return mId;
    }

    public String getUrl() {
        return "http://www.dancedeets.com/events/" + getId() + "/";
    }

    public String getFacebookUrl() {
        return "http://www.facebook.com/events/" + getId() + "/";
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
            if (mEndTime - mStartTime < 1000*60*60*12) {
                return localizedTimeFormat.format(getEndTimeLong());
            } else if (mAllDayEvent) {
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
            if (mEndTime - mStartTime < 1000*60*60*12) {
                return localizedTimeFormat.format(getEndTimeLong());
            } else if (mAllDayEvent) {
                return DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(getEndTimeLong());
            } else {
                return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(getEndTimeLong());
            }
        } else {
            return null;
        }
    }

    public String getFullTimeString() {
        String fullTime = getStartTimeString();
        if (mEndTime != 0) {
            fullTime += " - " + getEndTimeString();
        }
        return fullTime;
    }

    public String getDescription() {
        return mDescription;
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
                mTitle.equals(other.mTitle) &&
                mDescription.equals(other.mDescription) &&
                (mImageUrl == null ? other.mImageUrl == null : mImageUrl.equals(other.mImageUrl)) &&
                (mCoverUrl == null ? other.mCoverUrl == null : mCoverUrl.equals(other.mCoverUrl)) &&
                mStartTime == other.mStartTime &&
                mEndTime == other.mEndTime &&
                mAllDayEvent == other.mAllDayEvent &&
                (mCoverData == null ? other.mCoverData == null : mCoverData.equals(other.mCoverData)) &&
                mVenue.equals(other.mVenue) &&
                mAdminList.equals(other.mAdminList)
        );
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeString(mTitle);
        dest.writeString(mDescription);
        dest.writeLong(mStartTime);
        dest.writeLong(mEndTime);
        dest.writeByte((byte) (mAllDayEvent ? 1 : 0));
        dest.writeString(mImageUrl);
        dest.writeString(mCoverUrl);
        dest.writeParcelable(mCoverData, 0);
        dest.writeParcelable(mVenue, 0);
        dest.writeList(mAdminList);
    }

    public static final Parcelable.Creator<FullEvent> CREATOR
            = new Parcelable.Creator<FullEvent>() {
        public FullEvent createFromParcel(Parcel in) {
            return new FullEvent(in);
        }

        public FullEvent[] newArray(int size) {
            return new FullEvent[size];
        }
    };

    static public FullEvent parse(Bundle b) {
        return (FullEvent)b.getParcelable("EVENT");
    }

    public Bundle getBundle() {
        Bundle b = new Bundle();
        b.putParcelable("EVENT", this);
        return b;
    }

    @SuppressWarnings("unchecked")
    private FullEvent(Parcel in) {
        mId = in.readString();

        mTitle = in.readString();
        mDescription = in.readString();
        mStartTime = in.readLong();
        mEndTime = in.readLong();
        mAllDayEvent = in.readByte() != 0;

        mImageUrl = in.readString();
        mCoverUrl = in.readString();

        mCoverData = in.readParcelable(CoverData.class.getClassLoader());
        mVenue = in.readParcelable(Venue.class.getClassLoader());
        mAdminList = in.readArrayList(NamedPerson.class.getClassLoader());
    }}