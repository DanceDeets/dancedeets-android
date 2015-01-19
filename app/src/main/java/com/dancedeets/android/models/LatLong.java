package com.dancedeets.android.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by lambert on 2015/01/16.
 */

public class LatLong implements Parcelable, Serializable {
    protected double mLatitude;
    protected double mLongitude;

    public LatLong(double latitude, double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public boolean equals(Object o) {
        if (!(o instanceof LatLong)) {
            return false;
        }
        LatLong otherLatLong = (LatLong)o;
        return mLatitude == otherLatLong.mLatitude && mLongitude == otherLatLong.mLongitude;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(mLatitude);
        dest.writeDouble(mLongitude);
    }
/*
    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        Parcel dest = Parcel.obtain();
        writeToParcel(dest, 0);
        byte[] b = dest.marshall();
        out.write(b);
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException
    {
        byte[] b;
        in.read(b);
        Parcel p = Parcel.obtain().unmarshall();

        Par
        new Parcel()
    }*/
    public static final Parcelable.Creator<LatLong> CREATOR
            = new Parcelable.Creator<LatLong>() {
        public LatLong createFromParcel(Parcel in) {
            return new LatLong(in);
        }

        public LatLong[] newArray(int size) {
            return new LatLong[size];
        }
    };

    @SuppressWarnings("unchecked")
    private LatLong(Parcel in) {
        mLatitude = in.readDouble();
        mLongitude = in.readDouble();
    }
}
