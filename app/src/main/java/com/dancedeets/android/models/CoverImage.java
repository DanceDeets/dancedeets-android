package com.dancedeets.android.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
* Created by lambert on 2014/10/14.
*/
public class CoverImage implements Parcelable, Serializable {
    protected String mSourceUrl;
    protected int mWidth;
    protected int mHeight;

    protected CoverImage() {
    }

    static public CoverImage parse(JSONObject jsonObject) throws JSONException {
        CoverImage coverImage = new CoverImage();
        coverImage.mSourceUrl = jsonObject.getString("source");
        coverImage.mWidth = jsonObject.getInt("width");
        coverImage.mHeight = jsonObject.getInt("height");
        return coverImage;
    }

    public String getSourceUrl() {
        return mSourceUrl;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (((Object)this).getClass() != o.getClass()) return false;
        CoverImage other = (CoverImage)o;
        return (mSourceUrl.equals(other.mSourceUrl) &&
                mWidth == other.mWidth &&
                mHeight == other.mHeight
        );
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSourceUrl);
        dest.writeInt(mWidth);
        dest.writeInt(mHeight);
    }

    public static final Parcelable.Creator<CoverImage> CREATOR
            = new Parcelable.Creator<CoverImage>() {
        public CoverImage createFromParcel(Parcel in) {
            return new CoverImage(in);
        }

        public CoverImage[] newArray(int size) {
            return new CoverImage[size];
        }
    };


    @SuppressWarnings("unchecked")
    private CoverImage(Parcel in) {
        mSourceUrl = in.readString();
        mWidth = in.readInt();
        mHeight = in.readInt();
    }

}
