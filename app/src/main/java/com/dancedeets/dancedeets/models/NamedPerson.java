package com.dancedeets.dancedeets.models;

import java.io.Serializable;

/**
 * Created by lambert on 2014/10/15.
 */
public class NamedPerson implements Serializable {

    protected String mId;
    protected String mName;

    public NamedPerson(String id, String name) {
        mId = id;
        mName = name;
    }

    public String toString() {
        return mId + ": " + mName;
    }

}
