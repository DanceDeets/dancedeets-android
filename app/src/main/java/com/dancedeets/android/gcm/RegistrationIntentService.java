/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dancedeets.android.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.dancedeets.android.AnalyticsUtil;
import com.dancedeets.android.DanceDeetsApi;
import com.dancedeets.android.R;
import com.facebook.AccessToken;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";

    public static final String FB_ACCESS_TOKEN = "fbAccessToken";

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences.getBoolean(SENT_TOKEN_TO_SERVER, false)) {
            // Disabled, as we currently don't verify that the servers received our tokens.
            // (Both MixPanel and DanceDeets need to receive and store the token.)
            // return;
        }

        AccessToken accessToken = intent.getParcelableExtra(FB_ACCESS_TOKEN);

        String deviceToken = null;
        int numTries = 0;
        int maxTries = 3;
        Exception finalException = null;

        // Occasionally we get an IOException(SERVICE_NOT_AVAILABLE),
        // so let's retry, per the advice on stack overflow:
        // http://stackoverflow.com/questions/25129611/gcm-register-service-not-available
        while (deviceToken == null && numTries < maxTries) {
            numTries++;
            try {
                // Initially this call goes out to the network to retrieve the token, subsequent calls
                // are local. It is blocking, which is why we run this in a separate Service.
                // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
                // See https://developers.google.com/cloud-messaging/android/start for details on this file.
                InstanceID instanceID = InstanceID.getInstance(this);
                deviceToken = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            } catch (Exception e) {
                Crashlytics.log(Log.ERROR, TAG, "Failed to complete token refresh");
                finalException = e;
            }
        }
        if (deviceToken != null) {
            sendRegistrationToServer(accessToken, deviceToken);
        } else if (finalException != null) {
            Crashlytics.logException(finalException);
        }
    }

    protected void sendRegistrationToServer(AccessToken accessToken, String deviceToken) {
        AnalyticsUtil.setDeviceToken(deviceToken);
        DanceDeetsApi.sendDeviceToken(accessToken, deviceToken);
    }
}
