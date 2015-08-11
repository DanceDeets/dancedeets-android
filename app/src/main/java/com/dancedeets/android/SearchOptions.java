package com.dancedeets.android;

import java.io.Serializable;

/**
 * Created by lambert on 2014/10/05.
 */
public class SearchOptions implements Serializable {
    public String location;
    public String keywords;
    public TimePeriod timePeriod;

    public enum TimePeriod {
        PAST("PAST"),
        ONGOING("ONGOING"),
        UPCOMING("UPCOMING");

        private final String queryArg;

        TimePeriod(String queryArg) {
            this.queryArg = queryArg;
        }

        public String toString() {
            return queryArg;
        }
    };

    public SearchOptions() {
        location = "";
        keywords = "";
        timePeriod = TimePeriod.UPCOMING;
    }
}
