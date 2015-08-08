package com.dancedeets.android;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Address;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginActivity extends FacebookActivity {

    private static final String LOG_TAG = "LoginActivity";
    private boolean mClickedLogin;

    public void clickedExplainWhyLogin(View view) {
        AnalyticsUtil.track("Login - Explain Why");
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("ResourceID", R.layout.login_explain_why);
        intent.setAction(Intent.ACTION_DEFAULT);
        startActivity(intent);
    }

    public void clickedUseWithoutFacebookLogin(View view) {
        AnalyticsUtil.track("Login - Without Facebook");
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("ResourceID", R.layout.login_use_without_facebook_login);
        intent.setAction(Intent.ACTION_DEFAULT);
        startActivity(intent);
    }

    public void clickedUseWebsite(View view) {
        AnalyticsUtil.track("Login - Use Website");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.dancedeets.com/"));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private static class SendAuthRequest implements FetchLocation.AddressListener {
        private static final String LOG_TAG = "SendAuthRequest";

        private final AccessToken mAccessToken;
        private final FetchLocation mFetchLocation;

        SendAuthRequest(AccessToken accessToken, FetchLocation fetchLocation) {
            Log.i(LOG_TAG, "Received access token " + accessToken + ", with " + fetchLocation);
            mAccessToken = accessToken;
            mFetchLocation = fetchLocation;
        }

        @Override
        public void onAddressFound(Location location, Address address) {
            // We special-case this because the address.toString() omits SubLocality for some reason, and it's useful to us.
            String optionalSubLocality = (address != null) ? " (with SubLocality " + address.getSubLocality() + ")" : "";
            Log.i(LOG_TAG, "onAddressFound with location: " + location + ", address: " + address + optionalSubLocality);

            String addressString = null;
            if (address != null) {
                addressString = FetchLocation.formatAddress(address);

                // Only do this if we have an address, so future events get tagged with the user's location
                try {
                    JSONObject props = new JSONObject();
                    props.put("GPS City", addressString);
                    props.put("GPS Country", address.getCountryName());
                    AnalyticsUtil.setGlobalProperties(props);
                } catch (JSONException e) {
                }
            } else {
                Log.e(LOG_TAG, "Failed to get address from server, sending update with empty location.");
            }
            DanceDeetsApi.sendAuth(mAccessToken, addressString);

            mFetchLocation.onStop();
        }
    }

    // Static context so we don't retain a reference to the possibly-destroyed Activity
    static class MeCompleted implements GraphRequest.GraphJSONObjectCallback {

        MeCompleted() {}

        @Override
        public void onCompleted(
                JSONObject object,
                GraphResponse response) {
            if (object != null) {
                // When we log-in (auto or manual), identify as this uid,
                // so all future events (across website and ios/android)
                // can be correlated with each other.
                try {
                    AnalyticsUtil.login(object);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error sending user data to MixPanel: " + e);
                }
            }
        }
    }

    public void handleLogin(AccessToken accessToken) {
        // Set the access token using
        // currentAccessToken when it's loaded or set.
        Log.i(LOG_TAG, "Activity " + this + " is logged in: " + accessToken);

        GraphRequest request = GraphRequest.newMeRequest(
                accessToken, new MeCompleted());
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,first_name,last_name,gender,locale,timezone,email,link");
        request.setParameters(parameters);
        request.executeAsync();

        FetchLocation fetchLocation = new FetchLocation();
        fetchLocation.onStart(LoginActivity.this, new SendAuthRequest(accessToken, fetchLocation));

        Intent intent = new Intent(LoginActivity.this, EventListActivity.class);
        intent.setAction(Intent.ACTION_DEFAULT);
        // We can't use the noHistory option on this activity as we need state retained:
        // Facebook login navigate to a sub-activity expecting a response back to this one.
        // So any navigation away must manually ensure there is no history stack retained.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    protected void logIn(AccessToken newAccessToken) {
        Log.i(LOG_TAG, "onCurrentAccessTokenChanged: " + newAccessToken);
        // Only post Complete! events for people who clicked login (no autologin!)
        if (mClickedLogin) {
            AnalyticsUtil.track("Login - Completed");
        }
        handleLogin(newAccessToken);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        VolleySingleton.createInstance(getApplicationContext());
        super.onCreate(savedInstanceState);

        AccessToken currentAccessToken = AccessToken.getCurrentAccessToken();
        Log.i(LOG_TAG, "currentAccessToken is " + currentAccessToken);
        if (currentAccessToken == null) {
            AnalyticsUtil.track("Login - Not Logged In");
        } else {
            handleLogin(currentAccessToken);
        }
        // Set (DEBUG) title
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            //The .debug specified in gradle
            if (pInfo.packageName.equals("com.dancedeets.android.debug")) {
                setTitle(getTitle() + " (DEBUG)");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // Print out the KeyHashes used.
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d(LOG_TAG, "KeyHash: " + Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }

        Intent intent = getIntent();
        int layoutId = R.layout.login;
        if (intent != null) {
            if (intent.hasExtra("ResourceID") && getActionBar() != null) {
                getActionBar().setDisplayHomeAsUpEnabled(true);
            }
            layoutId = intent.getIntExtra("ResourceID", R.layout.login);
        }
        setContentView(layoutId);

        TextView link1 = (TextView) findViewById(R.id.login_use_without_fblogin);
        if (link1 != null) {
            link1.setTextColor(link1.getLinkTextColors());
        }
        TextView link2 = (TextView) findViewById(R.id.login_explain_why_login);
        if (link2 != null) {
            link2.setTextColor(link2.getLinkTextColors());
        }

        LoginButton authButton = (LoginButton) findViewById(R.id.authButton);
        authButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClickedLogin = true;
                AnalyticsUtil.track("Login - FBLogin Button Pressed");
            }
        });
        // We should ask for "rsvp_event" later, when needed to actually rsvp for the user? And implement that on the website, too?
        authButton.setReadPermissions("email", "public_profile", "user_events", "user_friends");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.login_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_help:
                HelpSystem.openHelp(this);
                return true;
            case R.id.action_feedback:
                SendFeedback.sendFeedback(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
