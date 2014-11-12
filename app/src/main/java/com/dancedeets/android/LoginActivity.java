package com.dancedeets.android;

import android.content.Intent;
import android.os.Bundle;

import com.dancedeets.dancedeets.R;
import com.facebook.Session;
import com.facebook.SessionState;

/**
 * Created by lambert on 2014/11/11.
 */
public class LoginActivity extends FacebookActivity {

    private static final String LOG_TAG = "LoginActivity";

    protected void onSessionStateChange(Session session, SessionState state, Exception exception) {
        super.onSessionStateChange(session, state, exception);
        if (state.isOpened()) {
            Bundle bundle = new Bundle();
            Intent intent = new Intent(this, EventListActivity.class);
            intent.putExtras(bundle);
            intent.setAction(Intent.ACTION_DEFAULT);
            startActivity(intent);
        } else if (state.isClosed()) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
    }

}
