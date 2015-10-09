package com.dancedeets.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.facebook.AccessToken;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Created by lambert on 2015/01/21.
 */
public class HelpSystem {
    private static final String LOG_TAG = "HelpSystem";

    public static void openHelp(Activity activity) {
        AnalyticsUtil.track("Open Help");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.dancedeets.com/help?hl=" + Locale.getDefault().getLanguage()));
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        }
    }

    public static void openAddEvent(SearchListActivity activity) {
        AnalyticsUtil.track("Add Event");
        String url = "http://www.dancedeets.com/events_add?hl=" + Locale.getDefault().getLanguage();
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null) {
            String uid = accessToken.getUserId();
            String accessTokenMD5 = Hashing.md5(accessToken.getToken());
            url += "&uid=" + uid + "&access_token_md5=" + accessTokenMD5;
        }
        Crashlytics.log(Log.INFO, LOG_TAG, "Opening URL: " + url);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        }
    }
}
