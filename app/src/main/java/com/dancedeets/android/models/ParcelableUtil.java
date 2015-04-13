package com.dancedeets.android.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by lambert on 2015/04/13.
 */
public class ParcelableUtil {

    public static<T extends Parcelable> byte[] marshallList(List<T> parcelableList) {
        Parcel parcel = Parcel.obtain();
        parcel.writeTypedList(parcelableList);
        byte[] bytes = parcel.marshall();
        parcel.recycle(); // not sure if needed or a good idea
        return bytes;
    }

    public static byte[] readBytes(File f) throws IOException {
        byte[] data = new byte[(int)f.length()];
        DataInputStream is = new DataInputStream(new FileInputStream(f));
        is.readFully(data);
        is.close();
        return data;
    }

    public static Parcel unmarshall(byte[] bytes) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0); // this is extremely important!
        return parcel;
    }

    public static <T extends Parcelable> void unmarshallList(byte[] bytes, List<T> list, Parcelable.Creator<T> creator) {
        Parcel parcel = unmarshall(bytes);
        parcel.readTypedList(list, creator);
    }

}