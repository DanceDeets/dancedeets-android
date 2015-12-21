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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.dancedeets.android.DanceDeetsApi;
import com.dancedeets.android.R;
import com.dancedeets.android.models.FullEvent;
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
    };

    private static final String TAG = "ListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.i(TAG, "onMessageReceived");
        if (data.containsKey("mp_message")) {
            String mp_message = data.getString("mp_message");
            MixPanelReceiver receiver = new MixPanelReceiver();
            receiver.handleNotificationIntent(this, data);
        } else {
            Log.d(TAG, "From: " + from);

            if (from.startsWith("/topics/")) {
                // message received from some topic.
            } else {
                // normal downstream message.
            }

            // [START_EXCLUDE]
            /**
             * Production applications would usually process the message here.
             * Eg: - Syncing with server.
             *     - Store message in local database.
             *     - Update UI.
             */

            /**
             * In some cases it may be useful to show a notification indicating to the user
             * that a message was received.
             */
            switch(NotificationType.valueOf((String) data.get("notification_type"))) {
                case EVENT_REMINDER:
                    sendNotification(data);
                    break;
            }
            // [END_EXCLUDE]
        }
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param data GCM data received.
     */
    private void sendNotification(final Bundle data) {
        final String eventId = data.getString("event_id");
        if (eventId == null) {
            Log.e(LOG_TAG, "Got empty event_id from server.");
            return;
        }
        // Grab all the relevant Event information in a way that lets us use our OOP FullEvent accessors.
        DanceDeetsApi.getEvent(eventId, new DanceDeetsApi.OnEventReceivedListener() {

            @Override
            public void onEventReceived(final FullEvent event) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ListenerService.this);

                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(data.getString("url")));
                        PendingIntent pendingIntent = PendingIntent.getActivity(ListenerService.this, 0 /* Request code */, intent,
                                PendingIntent.FLAG_ONE_SHOT);

                        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                        String startTime = event.getStartTimeStringTimeOnly();
                        String location = event.getVenue().getName();
                        notificationBuilder.setSmallIcon(R.drawable.ic_penguin_head_outline)
                                .setContentTitle(event.getTitle())
                                .setContentText(startTime + ": " + location)
                                .setSubText(ListenerService.this.getString(R.string.open_event))
                                .setAutoCancel(true)
                                .setSound(defaultSoundUri)
                                .setCategory(NotificationCompat.CATEGORY_EVENT)
                                .setStyle(new NotificationCompat.BigTextStyle()
                                        .bigText(event.getDescription()))
                                .setContentIntent(pendingIntent);

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
                            Log.e(LOG_TAG, "" + bitmap);
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
                        int notificationId = eventId.hashCode();
                        notificationManager.notify(notificationId, notificationBuilder.build());
                    }
                }).start();
            }

            @Override
            public void onError(Exception exception) {
                Log.e(LOG_TAG, "Silently ignoring error retrieving event id " + eventId);
            }
        });
    }
}
