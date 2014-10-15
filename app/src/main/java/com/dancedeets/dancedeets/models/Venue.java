package com.dancedeets.dancedeets.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
* Created by lambert on 2014/10/14.
*/
public class Venue implements Serializable {

    protected String mId;
    protected String mName;

    protected double mLatitude;
    protected double mLongitude;

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

        venue.mId = jsonObject.getString("id");
        venue.mName = jsonObject.getString("name");
        if (!jsonObject.isNull("geocode")) {
            JSONObject geocode = jsonObject.getJSONObject("geocode");
            venue.mLatitude = geocode.getDouble("latitude");
            venue.mLongitude = geocode.getDouble("longitude");
        }
        if (!jsonObject.isNull("address")) {
            JSONObject address = jsonObject.getJSONObject("address");
            venue.mStreet = address.getString("street");
            venue.mCity = address.getString("city");
            venue.mState = address.getString("state");
            venue.mZip = address.getString("zip");
            venue.mCountry = address.getString("country");
        }
        return venue;
    }

    public String getName() {
        return mName;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

}
