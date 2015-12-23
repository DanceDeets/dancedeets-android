package com.dancedeets.android;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * A basic SettingsActivity that manages the SettingsFragment.
 */
public class SettingsActivity extends PreferenceActivity {

    public static class Notifications {
        public final static String GLOBAL = "notifications_global";
        public final static String UPCOMING_EVENTS = "notifications_upcomingEvents";
        public final static String ADDED_EVENTS = "notifications_addedEvents";
        public final static String SOUND = "notifications_sound";
        public final static String VIBRATE = "notifications_vibrate";
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}