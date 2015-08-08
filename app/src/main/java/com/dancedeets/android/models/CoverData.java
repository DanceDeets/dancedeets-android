package com.dancedeets.android.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
* Created by lambert on 2014/10/14.
*/
public class CoverData implements Parcelable, Serializable {
    protected String mId;

    protected List<CoverImage> mCovers;

    protected CoverData() {
    }

    static public CoverData parse(JSONObject jsonObject) throws JSONException {
        CoverData coverData = new CoverData();
        coverData.mId = jsonObject.getString("cover_id");
        JSONArray jsonCoverImages = jsonObject.getJSONArray("images");
        coverData.mCovers = new ArrayList<CoverImage>(jsonCoverImages.length());
        for (int i = 0; i < jsonCoverImages.length(); i++) {
            coverData.mCovers.add(CoverImage.parse(jsonCoverImages.getJSONObject(i)));
        }
        return coverData;
    }

    public String getId() {
        return mId;
    }

    public CoverImage getSmallestCoverLargerThan(int width, int height) {
        CoverImage bestCover = null;
        for (CoverImage cover : mCovers) {
            if (bestCover == null || (
                    (cover.getWidth() > width && cover.getHeight() > height)
                            &&
                    (cover.getWidth() < bestCover.getWidth() || cover.getHeight() < bestCover.getHeight())
            ))
            {
                bestCover = cover;
            }
        }
        return bestCover;
    }

    public CoverImage getLargestCover() {
        CoverImage largestCover = null;
        for (CoverImage cover : mCovers) {
            if (largestCover == null || cover.getWidth() > largestCover.getWidth()) {
                largestCover = cover;
            }
        }
        return largestCover;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (((Object)this).getClass() != o.getClass()) return false;
        CoverData other = (CoverData)o;
        return (mId.equals(other.mId) &&
                mCovers.equals(other.mCovers)
        );
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeList(mCovers);
    }

    public static final Parcelable.Creator<CoverData> CREATOR
            = new Parcelable.Creator<CoverData>() {
        public CoverData createFromParcel(Parcel in) {
            return new CoverData(in);
        }

        public CoverData[] newArray(int size) {
            return new CoverData[size];
        }
    };


    @SuppressWarnings("unchecked")
    private CoverData(Parcel in) {
        mId = in.readString();
        mCovers = in.readArrayList(CoverImage.class.getClassLoader());
    }
}
