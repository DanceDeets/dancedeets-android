package com.dancedeets.android.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
* Created by lambert on 2014/10/14.
*/
public class CoverData implements Serializable {
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

    //TODO: implement more mCover accessors as needed, and optimize as needed

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
}
