package com.dancedeets.dancedeets.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
* Created by lambert on 2014/10/14.
*/
class Venue implements Serializable {

    protected Venue() {
    }

    static public Venue parse(JSONObject jsonObject) throws JSONException {
        Venue venue = new Venue();
        //venue.mSourceUrl = jsonObject.getString("source");
        return venue;
    }

}
