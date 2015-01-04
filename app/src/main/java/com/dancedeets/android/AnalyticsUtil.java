package com.dancedeets.android;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;

import com.dancedeets.android.models.FullEvent;
import com.facebook.model.GraphUser;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Created by lambert on 2015/01/03.
 */
public class AnalyticsUtil {

    private static final String LOG_TAG = "AnalyticsUtil";

    public static final String PROD_MIXPANEL_TOKEN = "f5d9d18ed1bbe3b190f9c7c7388df243";
    public static final String DEV_MIXPANEL_TOKEN = "668941ad91e251d2ae9408b1ea80f67b";
    private static MixpanelAPI mMixPanel;

    private static String getMixPanelToken() {
        if (BuildConfig.DEBUG) {
            return DEV_MIXPANEL_TOKEN;
        } else {
            return PROD_MIXPANEL_TOKEN;
        }
    }

    private AnalyticsUtil(Context context) {
    }

    public static void createInstance(Context context) {
        if (mMixPanel == null) {
            mMixPanel = MixpanelAPI.getInstance(context, getMixPanelToken());
        }
    }

    public static void logout() {
        mMixPanel.reset();
    }

    public static void flush() {
        mMixPanel.flush();
    }

    public static void login(GraphUser user) {
        mMixPanel.identify(user.getId());

        MixpanelAPI.People people = mMixPanel.getPeople();
        people.identify(user.getId());
        people.set("$first_name", user.getFirstName());
        people.set("$last_name", user.getLastName());
        people.set("FB Birthday", user.getBirthday());
        people.set("FB Gender", user.getProperty("gender"));
        people.set("FB Locale", user.getProperty("locale"));
        people.set("FB Timezone", user.getProperty("timezone"));
        people.set("$email", user.getProperty("email"));
        String today = DateFormat.format("yyyy-MM-dd'T'HH:mm:ss", Calendar.getInstance()).toString();
        people.set("Last Login", today);
        people.setOnce("$created", today);
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
    }

}
