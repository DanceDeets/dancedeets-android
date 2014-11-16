package com.dancedeets.android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.dancedeets.dancedeets.R;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;

/**
 * Created by lambert on 2014/11/11.
 */
public class LoginActivity extends FacebookActivity {

    private static final String LOG_TAG = "LoginActivity";

    public static class MeCallback implements Request.GraphUserCallback {

        Session mSession;

        MeCallback(Session session) {
            mSession = session;
        }
        @Override
        public void onCompleted(GraphUser user, Response response) {
            // If the response is successful
            if (mSession == Session.getActiveSession()) {
                if (user != null) {
                    // TODO: send this ID and Session to the server.
                    //session.getAccessToken();
                    //session.getPermissions();
                    //session.getAuthorizationBundle();
                    Log.i(LOG_TAG, "ID: " + user.getId());
                    Log.i(LOG_TAG, "Name: " + user.getName());
                }
            }
        }
    }
    protected void onSessionStateChange(Session session, SessionState state, Exception exception) {
        // Don't call the super, since we don't want it sending us back to the LoginActivity when logged out
        // super.onSessionStateChange(session, state, exception);
        if (state.isOpened()) {
            Log.i(LOG_TAG, "Activity " + this + " is logged in, with state: " + state);

            Request request = Request.newMeRequest(session, new MeCallback(session));
            Request.executeBatchAsync(request);

            Intent intent = new Intent(this, EventListActivity.class);
            intent.setAction(Intent.ACTION_DEFAULT);
            startActivity(intent);
            // Finish this activity, so it is no longer on the back stack.
            // We can't use the noHistory option, as Facebook login does navigate off this activity,
            // and so we do need this activity back state retained for that navigation.
            finish();
        } else if (state.isClosed()) {
            Log.i(LOG_TAG, "Activity " + this + " is logged out, with state: " + state);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        LoginButton authButton = (LoginButton)findViewById(R.id.authButton);
        // We should ask for "rsvp_event" later, when needed to actually rsvp for the user? And implement that on the website, too?
        authButton.setReadPermissions("email", "public_profile", "user_events", "user_friends");
    }

}
