package com.dancedeets.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;

import com.crashlytics.android.Crashlytics;
import com.dancedeets.android.eventlist.SearchListActivity;
import com.dancedeets.android.util.Hashing;
import com.facebook.AccessToken;

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

    public static void setupShareAppItem(Menu menu) {
        MenuItem shareItem = menu.findItem(R.id.action_share_app);

        ShareActionProvider shareActionProvider = (ShareActionProvider) shareItem.getActionProvider();
        // Track share item clicks
        shareActionProvider.setOnShareTargetSelectedListener(
                new ShareActionProvider.OnShareTargetSelectedListener() {
                    @Override
                    public boolean onShareTargetSelected(ShareActionProvider shareActionProvider,
                                                         Intent intent) {
                        AnalyticsUtil.track("Share DanceDeets");
                        return false;
                    }
                });

        // Set up ShareActionProvider shareIntent
        Intent intent = new Intent(Intent.ACTION_SEND);
        // We need to keep this as text/plain, not text/html, so we get the full set of apps to share to.
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Check out DanceDeets, with every street dance event!");
        intent.putExtra(Intent.EXTRA_TEXT,
                "DanceDeets is your ticket to finding street dance events near you:\n" +
                        "competitions, workshops, parties, and more!\n" +
                        "http://www.dancedeets.com/");
        shareActionProvider.setShareIntent(intent);

    }

    public static void openAddEvent(SearchListActivity activity) {
        AnalyticsUtil.track("Add Event");
        Uri.Builder builder = Uri.parse("http://www.dancedeets.com/events_add?hl=" + Locale.getDefault().getLanguage()).buildUpon();
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null) {
            String uid = accessToken.getUserId();
            String accessTokenMD5 = Hashing.md5(accessToken.getToken());
            builder.appendQueryParameter("uid", uid);
            builder.appendQueryParameter("access_token_md5", accessTokenMD5);
        }
        Uri url = builder.build();
        Crashlytics.log(Log.INFO, LOG_TAG, "Opening URL: " + url);

        Intent intent = new Intent(activity, WebViewActivity.class);
        intent.setData(url);
        activity.startActivity(intent);
    }
}
