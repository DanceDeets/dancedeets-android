package com.dancedeets.dancedeets.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by lambert on 2014/10/15.
 */
public class NamedPerson implements Serializable {

    protected String mId;
    protected String mName;

    public NamedPerson() {
    }

    static public NamedPerson parse(JSONObject jsonObject) throws JSONException {
        NamedPerson namedPerson = new NamedPerson();
        namedPerson.mId = jsonObject.getString("id");
        namedPerson.mName = jsonObject.getString("name");
        return namedPerson;
    }

    public String toString() {
        return mId + ": " + mName;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (((Object)this).getClass() != o.getClass()) return false;
        NamedPerson other = (NamedPerson)o;
        return (mId.equals(other.mId) &&
                mName.equals(other.mName)
        );
    }
}
