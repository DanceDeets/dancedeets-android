package com.dancedeets.android.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by lambert on 2014/10/15.
 */
public class NamedPerson implements Parcelable,Serializable {

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeString(mName);
    }

    public static final Parcelable.Creator<NamedPerson> CREATOR
            = new Parcelable.Creator<NamedPerson>() {
        public NamedPerson createFromParcel(Parcel in) {
            return new NamedPerson(in);
        }

        public NamedPerson[] newArray(int size) {
            return new NamedPerson[size];
        }
    };

    @SuppressWarnings("unchecked")
    private NamedPerson(Parcel in) {
        mId = in.readString();
        mName = in.readString();
    }
}
