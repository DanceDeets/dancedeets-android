package com.dancedeets.android;

import java.io.Serializable;

/**
 * A representation for the search options we can query the API server with
 */
public class SearchOptions implements Serializable {
    public String location;
    public String keywords;
    public TimePeriod timePeriod;

    public enum TimePeriod {
        PAST("PAST"),
        ONGOING("ONGOING"),
        UPCOMING("UPCOMING"),
        UNSET(null);

        private final String queryArg;

        TimePeriod(String queryArg) {
            this.queryArg = queryArg;
        }

        public String toString() {
            return queryArg;
        }
    }

    public SearchOptions() {
        this.location = "";
        this.keywords = "";
        this.timePeriod = TimePeriod.UNSET;
    }

    public SearchOptions(String location) {
        this.location = location;
        this.keywords = "";
        this.timePeriod = TimePeriod.UNSET;
    }

    public SearchOptions(String location, String keywords) {
        this.location = location;
        this.keywords = keywords;
        this.timePeriod = TimePeriod.UNSET;
    }

    public boolean isEmpty() {
        return location.isEmpty() && keywords.isEmpty();
    }

    public String toString() {
        return "SearchOptions(" + this.location + ", " + this.keywords + ", " + this.timePeriod + ")";
    }
}