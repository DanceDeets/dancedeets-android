package com.dancedeets.android;

import java.io.Serializable;

/**
 * Created by lambert on 2014/10/05.
 */
public class SearchOptions implements Serializable {
    public String location;
    public String keywords;

    public SearchOptions() {
        location = "";
        keywords = "";
    }
}
