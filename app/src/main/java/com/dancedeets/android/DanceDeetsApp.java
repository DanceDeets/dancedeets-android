package com.dancedeets.android;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.dancedeets.android.models.FullEvent;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.parse.Parse;
import com.parse.ParseInstallation;

import org.json.JSONException;
import org.json.JSONObject;

import io.fabric.sdk.android.Fabric;

/**
 * Created by lambert on 2014/10/05.
 */
public class DanceDeetsApp extends Application {

    public static final String SAVED_DATA_FILENAME = "SAVED_DATA";
    public static final String PROD_MIXPANEL_TOKEN = "f5d9d18ed1bbe3b190f9c7c7388df243";
    public static final String DEV_MIXPANEL_TOKEN = "668941ad91e251d2ae9408b1ea80f67b";
    private MixpanelAPI mMixPanel;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        initializeGoogle();
        initializeParse();
        mMixPanel = MixpanelAPI.getInstance(this, getMixPanelToken());
    }

    private String getMixPanelToken() {
        if (BuildConfig.DEBUG) {
            return DEV_MIXPANEL_TOKEN;
        } else {
            return PROD_MIXPANEL_TOKEN;
        }
    }

    public MixpanelAPI getMixPanel() {
        return mMixPanel;
    }

    public void track(String eventName, String... keyValuePairs) {
        try {
            JSONObject props = new JSONObject();
            for (int i=0; i<keyValuePairs.length; i+=2) {
                props.put(keyValuePairs[i], keyValuePairs[i+1]);
            }
            mMixPanel.track(eventName, props);
        } catch (JSONException e) {
        }
    }

    public void trackEvent(String eventName, FullEvent event, String... keyValuePairs) {
        try {
            JSONObject props = new JSONObject();
            props.put("Event", event.getId());
            props.put("City", event.getVenue().getCityStateCountry());
            props.put("Country", event.getVenue().getCountry());
            for (int i=0; i<keyValuePairs.length; i+=2) {
                props.put(keyValuePairs[i], keyValuePairs[i+1]);
            }
            mMixPanel.track(eventName, props);
        } catch (JSONException e) {
        }
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
