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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.dancedeets.android.DanceDeetsApi;
import com.dancedeets.android.R;
import com.dancedeets.android.SettingsActivity;
import com.dancedeets.android.models.FullEvent;
import com.dancedeets.android.util.VolleySingleton;
import com.google.android.gms.gcm.GcmListenerService;

import java.io.InputStream;

public class ListenerService extends GcmListenerService {

    private static String LOG_TAG = "ListenerService";

    public enum NotificationType {
        EVENT_REMINDER("EVENT_REMINDER");

        private String value;
        NotificationType(String value) {
            this.value = value;
        }
        String getValue() {
            return value;
        }
    }

    private static final String TAG = "ListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.i(TAG, "onMessageReceived");
        if (data.containsKey("mp_message")) {
            MixPanelReceiver receiver = new MixPanelReceiver();
            receiver.handleNotificationIntent(this, data);
        } else {
            Log.d(TAG, "From: " + from);

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            boolean notificationsEnabled = sharedPref.getBoolean(SettingsActivity.Notifications.GLOBAL, true);
            boolean notificationsEventsEnabled = sharedPref.getBoolean(SettingsActivity.Notifications.UPCOMING_EVENTS, true);

            //if (from.startsWith("/topics/")) {
            //}

            switch(NotificationType.valueOf((String) data.get("notification_type"))) {
                case EVENT_REMINDER:
                    if (notificationsEnabled && notificationsEventsEnabled) {
                        sendEventReminder(data);
                    }
                    break;
            }
        }
    }

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param data GCM data received.
     */
    private void sendEventReminder(final Bundle data) {
        final String eventId = data.getString("event_id");
        if (eventId == null) {
            Log.e(LOG_TAG, "Got empty event_id from server.");
            return;
        }
        // Grab all the relevant Event information in a way that lets us use our OOP FullEvent accessors.
        VolleySingleton.createInstance(this);
        DanceDeetsApi.getEvent(eventId, new DanceDeetsApi.OnEventReceivedListener() {

            @Override
            public void onEventReceived(final FullEvent event) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ListenerService.this.onEventReceived(event);
                    }
                }).start();
            }

            @Override
            public void onError(Exception exception) {
                Log.e(LOG_TAG, "Silently ignoring error retrieving event id " + eventId);
            }
        });
    }


    public void onEventReceived(FullEvent event) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ListenerService.this);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.getUrl()));
        // Ensure we open this URL using the DanceDeets app
        intent.setPackage(getPackageName());

        PendingIntent pendingIntent = PendingIntent.getActivity(ListenerService.this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String startTime = event.getStartTimeStringTimeOnly();
        String location = event.getVenue().getName();
        notificationBuilder.setSmallIcon(R.drawable.ic_penguin_head_outline)
                .setContentTitle(event.getTitle())
                .setContentText(startTime + ": " + location)
                .setSubText(ListenerService.this.getString(R.string.open_event))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(event.getDescription()))
                .setContentIntent(pendingIntent);

        if (sharedPref.getBoolean(SettingsActivity.Notifications.SOUND, true)) {
            Uri defaultSoundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.happening);
            notificationBuilder.setSound(defaultSoundUri);
        }
        if (sharedPref.getBoolean(SettingsActivity.Notifications.VIBRATE, true)) {
            notificationBuilder.setVibrate(new long[]{0, 250, 250, 250});
        }

        // Sadly, most flyers are not amenable to viewing in the 2:1 ratio wide image views
        // used for BigImageStyle notifications.
        // So instead, we load a small cover image for use for notification thumbnails.
        String imageUrl = event.getThumbnailUrl();
        if (imageUrl != null) {
            Bitmap bitmap = null;
            try {
                // Download Image from URL
                InputStream input = new java.net.URL(imageUrl).openStream();
                // Decode Bitmap
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            notificationBuilder.setLargeIcon(bitmap);
        }


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Uri mapUrl = event.getOpenMapUrl();
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUrl);
        PendingIntent mapPendingIntent = PendingIntent.getActivity(ListenerService.this, 0 /* Request code */, mapIntent,
                PendingIntent.FLAG_ONE_SHOT);
        notificationBuilder.addAction(R.drawable.ic_menu_map, "Get Directions", mapPendingIntent);
        // The notificationId is used for overwriting existing notifications,
        // or ensuring separate notifications for separate events.
        int notificationId = event.getId().hashCode();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

}
