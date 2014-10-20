package com.dancedeets.dancedeets.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
* Created by lambert on 2014/10/14.
*/
public class Venue implements Serializable {

    public static class LatLong implements Serializable {
        protected double mLatitude;
        protected double mLongitude;

        public LatLong(double latitude, double longitude) {
            mLatitude = latitude;
            mLongitude = longitude;
        }

        public double getLatitude() {
            return mLatitude;
        }

        public double getLongitude() {
            return mLongitude;
        }

        public boolean equals(Object o) {
            if (!(o instanceof LatLong)) {
                return false;
            }
            LatLong otherLatLong = (LatLong)o;
            return mLatitude == otherLatLong.mLatitude && mLongitude == otherLatLong.mLongitude;
        }
    }

    protected String mId;
    protected String mName;

    protected LatLong mLatLong;

    protected String mStreet;
    protected String mCity;
    protected String mState;
    protected String mZip;
    protected String mCountry;


    protected Venue() {
    }

    static public Venue parse(JSONObject jsonObject) throws JSONException {
        Venue venue = new Venue();
        //{"geocode": {"latitude": 40.677948804741, "longitude": -73.956656826886999},
        // "address": {"city": "Brooklyn", "state": "NY", "street": "1011 Dean St", "zip": "11238", "country": "United States"},
        // "name": "KAI studio",
        // "id": "158623004183893"},

        venue.mId = jsonObject.optString("id", null);
        venue.mName = jsonObject.getString("name");
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
}
