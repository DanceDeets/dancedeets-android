package com.dancedeets.dancedeets.models;

import android.os.Bundle;

import java.io.Serializable;

/**
 * Created by lambert on 2014/10/13.
 */
public class IdEvent implements Serializable {

    protected String mId;

    protected IdEvent() {}

    public IdEvent(String id) {
        mId = id;
    }

    static public IdEvent parse(Bundle b) {
        IdEvent event = (IdEvent)b.getSerializable("EVENT");
        return event;
    }

    public Bundle getBundle() {
        Bundle b = new Bundle();
        b.putSerializable("EVENT", this);
        return b;
    }

    public String getId() {
        return mId;
    }

    public String getUrl() {
        return "http://www.dancedeets.com/events/" + getId() + "/";
    }

    public String getFacebookUrl() {
        return "http://www.facebook.com/events/" + getId() + "/";
    }

    public String getApiDataUrl() {
        return "http://www.dancedeets.com/api/events/" + getId();
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (((Object)this).getClass() != o.getClass()) return false;
        IdEvent other = (IdEvent)o;
        return mId.equals(other.mId);
    }

}
