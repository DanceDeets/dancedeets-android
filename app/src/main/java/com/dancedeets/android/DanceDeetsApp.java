package com.dancedeets.android;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.facebook.FacebookSdk;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

import io.fabric.sdk.android.Fabric;

/**
 * Created by lambert on 2014/10/05.
 */
public class DanceDeetsApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        initializeGoogle();
        AnalyticsUtil.createInstance(getApplicationContext());
        FacebookSdk.sdkInitialize(getApplicationContext());
    }

    protected void initializeGoogle() {
        GoogleAnalytics.getInstance(this).setDryRun(false);
        GoogleAnalytics.getInstance(this).getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
        GoogleAnalytics.getInstance(this).enableAutoActivityReports(this);
        Tracker gaTracker = GoogleAnalytics.getInstance(this).newTracker(R.xml.activity_tracker);
        gaTracker.enableAutoActivityTracking(true);
    }

}
