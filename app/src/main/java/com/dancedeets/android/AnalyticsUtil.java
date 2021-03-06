package com.dancedeets.android;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.dancedeets.android.models.FullEvent;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by lambert on 2015/01/03.
 */
public class AnalyticsUtil {

    private static final String LOG_TAG = "AnalyticsUtil";

    private static final String PROD_MIXPANEL_TOKEN = "f5d9d18ed1bbe3b190f9c7c7388df243";
    private static final String DEV_MIXPANEL_TOKEN = "668941ad91e251d2ae9408b1ea80f67b";

    private static MixpanelAPI mMixPanel;
    private static GoogleAnalytics mGoogleAnalytics;
    private static Tracker mGoogleTracker;

    private static String getMixPanelToken() {
        if (BuildConfig.DEBUG) {
            return DEV_MIXPANEL_TOKEN;
        } else {
            return PROD_MIXPANEL_TOKEN;
        }
    }

    private AnalyticsUtil() {
    }

    public static void createInstance(Context context) {
        if (mMixPanel == null) {
            mMixPanel = MixpanelAPI.getInstance(context, getMixPanelToken());
        }
        if (mGoogleAnalytics == null) {
            mGoogleAnalytics = GoogleAnalytics.getInstance(context);
            //mGoogleAnalytics.setDryRun(false);
            //mGoogleAnalytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
            mGoogleTracker = mGoogleAnalytics.newTracker(R.xml.global_tracker);
        }
    }

    public static void flush() {
        mMixPanel.flush();
        mGoogleAnalytics.dispatchLocalHits();
    }

    // We must call identify *first*, before calling setDeviceToken() or login().
    // This ensures the latter functions operate against the correct user.
    public static void identify(String id) {
        mMixPanel.identify(id);
    }

    public static void setDeviceToken(String deviceToken) {
        MixpanelAPI.People people = mMixPanel.getPeople();
        people.setPushRegistrationId(deviceToken);
    }

    public static void login(JSONObject user) throws JSONException {
        // Register for notifications
        MixpanelAPI.People people = mMixPanel.getPeople();
        // Instead of this, we now call setPushRegistrationId in setDeviceToken
        //mMixPanel.getPeople().initPushHandling(getString(R.string.gcm_defaultSenderId));

        Crashlytics.log(Log.INFO, LOG_TAG, "User " + user.getString("id") + ": " + user.getString("name"));

        people.identify(user.getString("id"));
        people.set("$first_name", user.getString("first_name"));
        people.set("$last_name", user.getString("last_name"));
        people.set("FB Gender", user.getString("gender"));
        people.set("FB Locale", user.getString("locale"));
        people.set("FB Timezone", user.getString("timezone"));
        people.set("$email", user.getString("email"));
        // Use SimpleDateFormat instead of DateFormat,
        // since older APIs' DateFormat doesn't support HH (just a hacked kk).
        SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        String today = df.format(Calendar.getInstance().getTime());
        people.set("Last Login", today);
        people.setOnce("$created", today);

        // Google Analytics doesn't do per-user tracking
    }

    public static void logout() {
        mMixPanel.reset();
        //Google Analytics doesn't do any per-user tracking, so there's no per-user logout either
    }

    public static void setGlobalProperties(JSONObject props) {
        mMixPanel.registerSuperProperties(props);
    }

    public static void track(String eventName, String... keyValuePairs) {
        mMixPanel.getPeople().increment("Event: " + eventName, 1);

        Log.d(LOG_TAG, "Track(" + eventName + ")");
        try {
            JSONObject props = new JSONObject();
            for (int i=0; i<keyValuePairs.length; i+=2) {
                props.put(keyValuePairs[i], keyValuePairs[i+1]);
            }
            mMixPanel.track(eventName, props);
        } catch (JSONException e) {
        }

        mGoogleTracker.send(new HitBuilders.EventBuilder()
                .setCategory(eventName)
                .build());
    }

    public static void trackEvent(String eventName, FullEvent event, String... keyValuePairs) {
        try {
            JSONObject props = new JSONObject();
            props.put("Event ID", event.getId());
            props.put("Event City", event.getVenue().getCityStateCountry());
            props.put("Event Country", event.getVenue().getCountry());
            for (int i=0; i<keyValuePairs.length; i+=2) {
                props.put(keyValuePairs[i], keyValuePairs[i+1]);
            }
            mMixPanel.track(eventName, props);
        } catch (JSONException e) {
        }

        mGoogleTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Event")
                .setAction(eventName)
                .setLabel(event.getVenue().getCityStateCountry())
                .build());
    }

}
