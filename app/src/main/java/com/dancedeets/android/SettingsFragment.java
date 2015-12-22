package com.dancedeets.android;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * A SettingsFragment for use in SettingsActivity.
 *
 * The documentation really pushes for using fragments versus activities, so we do that here.
 */

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
