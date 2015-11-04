package com.dancedeets.android.models;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Represents a OneboxLink as returned by search results
 */
public class OneboxLink implements Parcelable, Serializable {

    protected String mTitle;
    protected String mUrl;

    protected OneboxLink() {
    }

    static public OneboxLink parse(JSONObject jsonObject) throws JSONException {
        OneboxLink onebox = new OneboxLink();
        onebox.mTitle = jsonObject.getString("title");
        onebox.mUrl = jsonObject.getString("url");
        return onebox;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getUrl() {
        return mUrl;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (((Object)this).getClass() != o.getClass()) return false;
        OneboxLink other = (OneboxLink)o;
        return (super.equals(o) &&
                mTitle.equals(other.mTitle) &&
                mUrl.equals(other.mUrl)
        );
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTitle);
        dest.writeString(mUrl);
    }

    public static final Parcelable.Creator<OneboxLink> CREATOR
            = new Parcelable.Creator<OneboxLink>() {
        public OneboxLink createFromParcel(Parcel in) {
            return new OneboxLink(in);
        }

        public OneboxLink[] newArray(int size) {
            return new OneboxLink[size];
        }
    };

    static public FullEvent parse(Bundle b) {
        return (FullEvent)b.getParcelable("EVENT");
    }

    public Bundle getBundle() {
        Bundle b = new Bundle();
        b.putParcelable("ONEBOX_LINK", this);
        return b;
    }

    @SuppressWarnings("unchecked")
    private OneboxLink(Parcel in) {
        mTitle = in.readString();
        mUrl = in.readString();
    }}