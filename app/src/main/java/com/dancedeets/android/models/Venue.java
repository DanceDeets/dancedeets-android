package com.dancedeets.android.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
* Created by lambert on 2014/10/14.
*/
public class Venue implements Parcelable, Serializable {

    protected String mId;
    protected String mName;

    protected LatLong mLatLong;

    protected String mStreet;
    protected String mCity;
    protected String mState;
    protected String mZip;
    protected String mCountry;


    public String getAddress(String delimiter) {
        List<String> addressParts = new ArrayList<>();
        if (mStreet != null) {
            addressParts.add(mStreet);
        }
        if (mCity != null || mState != null) {
            List<String> cityStateParts = new ArrayList<>();
            if (mCity != null) {
                cityStateParts.add(mCity);
            }
            if (mState != null) {
                cityStateParts.add(mState);
            }
            addressParts.add(TextUtils.join(", ", cityStateParts));
        }
        if (mCountry != null) {
            addressParts.add(mCountry);
        }
        return TextUtils.join(delimiter, addressParts);
    }

    protected Venue() {
    }

    static public Venue parse(JSONObject jsonObject) throws JSONException {
        Venue venue = new Venue();
        //{"geocode": {"latitude": 40.677948804741, "longitude": -73.956656826886999},
        // "address": {"city": "Brooklyn", "state": "NY", "street": "1011 Dean St", "zip": "11238", "country": "United States"},
        // "name": "KAI studio",
        // "id": "158623004183893"},

        venue.mId = jsonObject.optString("id", null);
        venue.mName = jsonObject.optString("name", null);
        if (!jsonObject.isNull("geocode")) {
            JSONObject geocode = jsonObject.getJSONObject("geocode");
            venue.mLatLong = new LatLong(geocode.getDouble("latitude"), geocode.getDouble("longitude"));
        }
        if (!jsonObject.isNull("address")) {
            JSONObject address = jsonObject.getJSONObject("address");
            venue.mStreet = address.optString("street", null);
            venue.mCity = address.optString("city", null);
            venue.mState = address.optString("state", null);
            venue.mZip = address.optString("zip", null);
            venue.mCountry = address.optString("country", null);
        }
        return venue;
    }

    public String getName() {
        return mName;
    }

    public boolean hasName() {
        return mName != null && !mName.isEmpty();
    }

    public String getCityStateCountry() {
        List<String> addressParts = new ArrayList<>();
        if (mCity != null) {
            addressParts.add(mCity);
        }
        if (mState != null) {
            addressParts.add(mState);
        }
        if (mCountry != null) {
            addressParts.add(mCountry);
        }
        return TextUtils.join(", ", addressParts);
    }

    public String getCountry() {
        List<String> addressParts = new ArrayList<>();
        if (mState != null) {
            addressParts.add(mState);
        }
        if (mCountry != null) {
            addressParts.add(mCountry);
        }
        return TextUtils.join(", ", addressParts);
    }


    public LatLong getLatLong() {
        return mLatLong;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (((Object)this).getClass() != o.getClass()) return false;
        Venue other = (Venue)o;
        return ((mId == null ? other.mId == null : mId.equals(other.mId)) &&
                (mName == null ? other.mName == null : mName.equals(other.mName)) &&
                (mLatLong == null ? other.mLatLong == null : mLatLong.equals(other.mLatLong)) &&
                (mStreet == null ? other.mStreet == null : mStreet.equals(other.mStreet)) &&
                (mCity == null ? other.mCity == null : mCity.equals(other.mCity)) &&
                (mState == null ? other.mState == null : mState.equals(other.mState)) &&
                (mZip == null ? other.mZip == null : mZip.equals(other.mZip)) &&
                (mCountry == null ? other.mCountry == null : mCountry.equals(other.mCountry))
        );
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeString(mName);
        dest.writeParcelable(mLatLong, 0);
        dest.writeString(mStreet);
        dest.writeString(mCity);
        dest.writeString(mState);
        dest.writeString(mZip);
        dest.writeString(mCountry);
    }

    public static final Parcelable.Creator<Venue> CREATOR
            = new Parcelable.Creator<Venue>() {
        public Venue createFromParcel(Parcel in) {
            return new Venue(in);
        }

        public Venue[] newArray(int size) {
            return new Venue[size];
        }
    };

    @SuppressWarnings("unchecked")
    private Venue(Parcel in) {
        mId = in.readString();
        mName = in.readString();
        mLatLong = in.readParcelable(LatLong.class.getClassLoader());
        mStreet = in.readString();
        mCity = in.readString();
        mState = in.readString();
        mZip = in.readString();
        mCountry = in.readString();
    }
}
