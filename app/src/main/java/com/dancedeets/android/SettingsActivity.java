package com.dancedeets.android;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by lambert on 2015/12/22.
 */
public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}