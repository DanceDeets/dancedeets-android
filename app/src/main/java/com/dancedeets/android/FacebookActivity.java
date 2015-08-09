package com.dancedeets.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.appevents.AppEventsLogger;

/**
 * Created by lambert on 2014/11/08.
 */
public class FacebookActivity extends Activity {
    private static final String LOG_TAG = "FacebookActivity";

    private CallbackManager mCallbackManager;
    private AccessTokenTracker mAccessTokenTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCallbackManager = CallbackManager.Factory.create();
        mAccessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken newAccessToken) {
                Log.i(LOG_TAG, "onCurrentAccessTokenChanged: " + oldAccessToken + " -> " + newAccessToken);
                if (newAccessToken == null) {
                    logOut();
                } else if (oldAccessToken == null && newAccessToken != null) {
                    logIn(newAccessToken);
                }
            }
        };
    }

    protected void logOut() {
        // Reset the user id, now that they've logged out
        AnalyticsUtil.logout();

        // On logout, send them back to the login screen.
        Intent intent = new Intent(FacebookActivity.this, LoginActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        startActivity(intent);
    }

    protected void logIn(AccessToken accessToken) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        mAccessTokenTracker.stopTracking();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppEventsLogger.deactivateApp(this);
    }
}
