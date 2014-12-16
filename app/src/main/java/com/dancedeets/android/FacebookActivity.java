package com.dancedeets.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;

/**
 * Created by lambert on 2014/11/08.
 */
public class FacebookActivity extends Activity {
    private UiLifecycleHelper uiHelper;

    private static final String LOG_TAG = "FacebookActivity";
    private SessionState mLastSessionState;
    private String mLastSessionToken;

    protected void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if (state.isOpened()) {
            Log.i(LOG_TAG, "Activity " + this + " is logged in, with state: " + state);
        } else if (state.isClosed()) {
            Log.i(LOG_TAG, "Activity " + this + " is logged out, with state: " + state);

            // On logout, send them back to the login screen.
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setAction(Intent.ACTION_MAIN);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(this, new Session.StatusCallback() {
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                if (isSessionChanged(session)) {
                    mLastSessionState = session.getState();
                    mLastSessionToken = session.getAccessToken();
                    onSessionStateChange(session, state, exception);
                }
            }
        });
        uiHelper.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Facebook tells us to call onSessionStateChange, but this can result in double-calling,
        // so instead we check if it's different from the last session, and call it conditionally.
        Session session = Session.getActiveSession();
        if (session != null &&
                (session.isOpened() || session.isClosed()) ) {
            if (isSessionChanged(session)) {
                mLastSessionState = session.getState();
                mLastSessionToken = session.getAccessToken();
                onSessionStateChange(session, session.getState(), null);
            }
        }
        uiHelper.onResume();
    }


    private boolean isSessionChanged(Session session) {
        if (mLastSessionState == null && mLastSessionToken == null) {
            return true;
        }

        if (mLastSessionState != session.getState()) {
            return true;
        }

        if (!mLastSessionToken.equals(session.getAccessToken())) {
            return true;
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }
}
