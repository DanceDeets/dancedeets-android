package com.dancedeets.android;

import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.widget.LoginButton;

/**
 * Created by lambert on 2014/11/11.
 */
public class LoginActivity extends FacebookActivity {

    private static final String LOG_TAG = "LoginActivity";

    public void clickedExplainWhyLogin(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("ResourceID", R.layout.login_explain_why);
        intent.setAction(Intent.ACTION_DEFAULT);
        startActivity(intent);
    }

    public void clickedUseWithoutFacebookLogin(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("ResourceID", R.layout.login_use_without_facebook_login);
        intent.setAction(Intent.ACTION_DEFAULT);
        startActivity(intent);
    }

    public void clickedUseWebsite(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.dancedeets.com/"));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private static class SendAuthRequest implements FetchLocation.AddressListener {
        private static final String LOG_TAG = "SendAuthRequest";

        private final Session mSession;
        private final FetchLocation mFetchLocation;

        SendAuthRequest(Session session, FetchLocation fetchLocation) {
            Log.i(LOG_TAG, "Received session " + session + ", with " + fetchLocation);
            mSession = session;
            mFetchLocation = fetchLocation;
        }

        @Override
        public void onAddressFound(Location location, Address address) {
            Log.i(LOG_TAG, "onAddressFound with location: " + location + ", address: " + address);

            String addressString = null;
            if (address != null) {
                addressString = address.getLocality() + ", " + address.getAdminArea() + ", " + address.getCountryCode();
            } else {
                Log.e(LOG_TAG, "Failed to get address from server, sending update with empty location.");
            }
            DanceDeetsApi.sendAuth(mSession, addressString);

            mFetchLocation.onStop();
        }
    }

    protected void onSessionStateChange(Session session, SessionState state, Exception exception) {
        // Don't call the super, since we don't want it sending us back to the LoginActivity when logged out
        // super.onSessionStateChange(session, state, exception);
        if (state.isOpened()) {
            Log.i(LOG_TAG, "Activity " + this + " is logged in, with state: " + state);

            FetchLocation fetchLocation = new FetchLocation();
            fetchLocation.onStart(this, new SendAuthRequest(session, fetchLocation));

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
        VolleySingleton.createInstance(getApplicationContext());
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        int layoutId = R.layout.login;
        if (intent != null) {
            if (intent.hasExtra("ResourceID")) {
                getActionBar().setDisplayHomeAsUpEnabled(true);
            }
            layoutId = intent.getIntExtra("ResourceID", R.layout.login);
        }
        setContentView(layoutId);

        TextView link1 = (TextView)findViewById(R.id.login_use_without_fblogin);
        if (link1 != null) {
            link1.setTextColor(link1.getLinkTextColors());
        }
        TextView link2 = (TextView)findViewById(R.id.login_explain_why_login);
        if (link2 != null) {
            link2.setTextColor(link1.getLinkTextColors());
        }

        LoginButton authButton = (LoginButton)findViewById(R.id.authButton);
        // We should ask for "rsvp_event" later, when needed to actually rsvp for the user? And implement that on the website, too?
        authButton.setReadPermissions("email", "public_profile", "user_events", "user_friends");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
