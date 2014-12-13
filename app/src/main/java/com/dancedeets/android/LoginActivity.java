package com.dancedeets.android;

import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request.Method;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by lambert on 2014/11/11.
 */
public class LoginActivity extends FacebookActivity {

    private static final String LOG_TAG = "LoginActivity";

    static DateFormat isoDateTimeFormatWithTZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

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

            String addressString = address.getLocality() + ", " + address.getAdminArea() + ", " + address.getCountryCode();
            updateServerSessionAndLocation(mSession, addressString);

            mFetchLocation.onStop();
        }

        void updateServerSessionAndLocation(Session session, String location) {
            Log.i(LOG_TAG, "updateServerSessionAndLocation with location: " + location);
            JSONObject jsonPayload = new JSONObject();
            try {
                jsonPayload.put("access_token", session.getAccessToken());
                jsonPayload.put("access_token_expires", isoDateTimeFormatWithTZ.format(session.getExpirationDate()));
                jsonPayload.put("location", location);
                jsonPayload.put("client", "android");
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error constructing request: " + e);
                return;
            }
            JsonObjectRequest request = new JsonObjectRequest(
                    Method.POST,
                    "http://www.dancedeets.com/api/auth",
                    jsonPayload,
                    new com.android.volley.Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.i(LOG_TAG, "Successfully called /api/auth: " + response);
                        }
                    }, new com.android.volley.Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(LOG_TAG, "Error calling /api/auth: " + error);
                }
            });
            VolleySingleton.getInstance().getRequestQueue().add(request);
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
        setContentView(R.layout.login);

        LoginButton authButton = (LoginButton)findViewById(R.id.authButton);
        // We should ask for "rsvp_event" later, when needed to actually rsvp for the user? And implement that on the website, too?
        authButton.setReadPermissions("email", "public_profile", "user_events", "user_friends");
    }
}
