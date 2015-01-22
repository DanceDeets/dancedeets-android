package com.dancedeets.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

/**
 * Created by lambert on 2015/01/21.
 */
public class HelpSystem {
    public static void openHelp(Activity activity) {
        AnalyticsUtil.track("Open Help");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.dancedeets.com/help"));
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        }
    }

    public static void openAddEvent(EventListActivity activity) {
        AnalyticsUtil.track("Add Event");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.dancedeets.com/events_add"));
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        }
    }
}
