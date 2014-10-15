package com.dancedeets.dancedeets.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
* Created by lambert on 2014/10/14.
*/
public class CoverImage implements Serializable {
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
}
