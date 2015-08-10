package com.dancedeets.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import java.util.Locale;

/**
 * Created by lambert on 2015/01/21.
 */
public class HelpSystem {
    public static void openHelp(Activity activity) {
        AnalyticsUtil.track("Open Help");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.dancedeets.com/help?hl=" + Locale.getDefault().getLanguage()));
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        }
    }

    public static void openAddEvent(SearchListActivity activity) {
        AnalyticsUtil.track("Add Event");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.dancedeets.com/events_add?hl=" + Locale.getDefault().getLanguage()));
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        }
    }
}
