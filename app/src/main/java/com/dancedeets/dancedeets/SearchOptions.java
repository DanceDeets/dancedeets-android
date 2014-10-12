package com.dancedeets.dancedeets;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by lambert on 2014/10/05.
 */
public class SearchOptions implements Parcelable {
    public String location;
    public String keywords;

    @Override
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(location);
        out.writeString(keywords);
    }

    public static final Parcelable.Creator<SearchOptions> CREATOR
            = new Parcelable.Creator<SearchOptions>() {
        public SearchOptions createFromParcel(Parcel in) {
            return new SearchOptions(in);
        }

        public SearchOptions[] newArray(int size) {
            return new SearchOptions[size];
        }
    };

    public SearchOptions() {
        location = "";
        keywords = "";
    }

    private SearchOptions(Parcel in) {
        location = in.readString();
        keywords = in.readString();
    }
}
