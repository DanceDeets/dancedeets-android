package com.dancedeets.android;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.parse.Parse;
import com.parse.ParseInstallation;

/**
 * Created by lambert on 2014/10/05.
 */
public class DanceDeetsApp extends Application {

    public static final String SAVED_DATA_FILENAME = "SAVED_DATA";

    public void onCreate() {
        initializeGoogle();
        initializeParse();
    }

    protected void initializeGoogle() {
        GoogleAnalytics.getInstance(this).setDryRun(false);
        GoogleAnalytics.getInstance(this).getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
        GoogleAnalytics.getInstance(this).enableAutoActivityReports(this);
        Tracker gaTracker = GoogleAnalytics.getInstance(this).newTracker(R.xml.activity_tracker);
        gaTracker.enableAutoActivityTracking(true);
    }

    protected void initializeParse() {
        Parse.initialize(this, "pTFDiCGQv0TJ3z3TwBaMdnEIhXYCa9bD9g6GPNNH", "gIYvPu8KgIizCu19a9UW7QrugJlXzBGQPpyxIHyC");
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }

}
