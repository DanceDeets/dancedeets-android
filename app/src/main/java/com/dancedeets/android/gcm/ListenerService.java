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
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.dancedeets.android.DanceDeetsApi;
import com.dancedeets.android.R;
import com.dancedeets.android.SettingsActivity;
import com.dancedeets.android.models.FullEvent;
import com.dancedeets.android.util.VolleySingleton;
import com.google.android.gms.gcm.GcmListenerService;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class ListenerService extends GcmListenerService {

    private static String LOG_TAG = "ListenerService";

    public enum NotificationType {
        EVENT_REMINDER("EVENT_REMINDER"),
        EVENT_ADDED("EVENT_ADDED");

        private String value;
        NotificationType(String value) {
            this.value = value;
        }
        String getValue() {
            return value;
        }
    }

    private static int EVENT_ADDED_NOTIFICATION_ID = -1;

    // A list of event titles. Can be updated/read in many places, so need to synchronize.
    // We use an ArrayList to make it easier to sort down below.
    private static ArrayList<FullEvent> addedEventTitles = new ArrayList<>();


    public abstract class OnEventLoadedListener implements DanceDeetsApi.OnEventReceivedListener {
        @Override
        public void onError(Exception exception) {
            // Never runs...
        }
    }
    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.i(LOG_TAG, "onMessageReceived");
        if (data.containsKey("mp_message")) {
            MixPanelReceiver receiver = new MixPanelReceiver();
            receiver.handleNotificationIntent(this, data);
        } else {
            Log.d(LOG_TAG, "From: " + from);

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            boolean notificationsEnabled = sharedPref.getBoolean(SettingsActivity.Notifications.GLOBAL, true);
            boolean notificationsUpcomingEventsEnabled = sharedPref.getBoolean(SettingsActivity.Notifications.UPCOMING_EVENTS, true);
            boolean notificationsAddedEventsEnabled = sharedPref.getBoolean(SettingsActivity.Notifications.ADDED_EVENTS, true);

            //if (from.startsWith("/topics/")) {
            //}

            try {
                switch (NotificationType.valueOf((String)data.get("notification_type"))) {
                    case EVENT_REMINDER:
                        if (notificationsEnabled && notificationsUpcomingEventsEnabled) {
                            loadEvent(data.getString("event_id"), new OnEventLoadedListener() {
                                @Override
                                public void onEventReceived(FullEvent event) {
                                    ListenerService.this.sendUpcomingEventReminder(event);
                                }
                            });
                        }
                        break;
                    case EVENT_ADDED:
                        if (notificationsEnabled && notificationsAddedEventsEnabled) {
                            loadEvent(data.getString("event_id"), new OnEventLoadedListener() {
                                @Override
                                public void onEventReceived(FullEvent event) {
                                    ListenerService.this.sendAddedEventReminder(event);
                                }
                            });
                        }
                        break;
                }
            } catch (Exception e) {
                Crashlytics.log(Log.ERROR, LOG_TAG, "Unknown notification_type: " + data.get("notification_type"));
            }
        }
    }

    private void loadEvent(final String eventId, final DanceDeetsApi.OnEventReceivedListener eventReceivedListener) {
        if (eventId == null) {
            Log.e(LOG_TAG, "Got empty event_id from server.");
            return;
        }
        // Ensure we initialize the singleton, as this code may run before the UI ever initializes...
        VolleySingleton.createInstance(this);
        // Grab all the relevant Event information in a way that lets us use our OOP FullEvent accessors.
        DanceDeetsApi.getEvent(eventId, new DanceDeetsApi.OnEventReceivedListener() {

            @Override
            public void onEventReceived(final FullEvent event) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        eventReceivedListener.onEventReceived(event);
                    }
                }).start();
            }

            @Override
            public void onError(Exception exception) {
                Crashlytics.log(Log.ERROR, LOG_TAG, "Silently ignoring error retrieving event: " + eventId);
                Crashlytics.logException(exception);
            }
        });
    }

    protected Bitmap getBitmap(FullEvent event) {
        // Sadly, most flyers are not amenable to viewing in the 2:1 ratio wide image views
        // used for BigImageStyle notifications.
        // So instead, we load a small cover image for use for notification thumbnails.
        String imageUrl = event.getThumbnailUrl();
        Bitmap bitmap = null;
        if (imageUrl != null) {
            try {
                // Download Image from URL
                InputStream input = new java.net.URL(imageUrl).openStream();
                // Decode Bitmap
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    public void sendUpcomingEventReminder(FullEvent event) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ListenerService.this);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.getUrl()));
        // Ensure we open this URL using the DanceDeets app
        intent.setPackage(getPackageName());

        PendingIntent pendingIntent = PendingIntent.getActivity(ListenerService.this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String startTime = event.getStartTimeStringTimeOnly();
        String location = event.getVenue().getName();
        notificationBuilder
                .setSmallIcon(R.drawable.ic_penguin_head_outline)
                .setContentTitle(event.getTitle())
                .setContentText(startTime + ": " + location)
                .setSubText(getString(R.string.open_event))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(event.getDescription()))
                .setContentIntent(pendingIntent);

        // This is a blocking operation
        notificationBuilder.setLargeIcon(getBitmap(event));

        if (sharedPref.getBoolean(SettingsActivity.Notifications.SOUND, true)) {
            Uri defaultSoundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.happening);
            notificationBuilder.setSound(defaultSoundUri);
        }
        boolean hasVibratePermission = ContextCompat.checkSelfPermission(this, "android.permission.VIBRATE") == PackageManager.PERMISSION_GRANTED;
        if (hasVibratePermission && sharedPref.getBoolean(SettingsActivity.Notifications.VIBRATE, true)) {
            notificationBuilder.setVibrate(new long[]{0, 250, 250, 250});
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Uri mapUrl = event.getOpenMapUrl();
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUrl);
        PendingIntent mapPendingIntent = PendingIntent.getActivity(ListenerService.this, 0 /* Request code */, mapIntent,
                PendingIntent.FLAG_ONE_SHOT);
        notificationBuilder.addAction(R.drawable.ic_menu_map, getString(R.string.menu_view_map), mapPendingIntent);
        // The notificationId is used for overwriting existing notifications,
        // or ensuring separate notifications for separate events.
        int notificationId = event.getId().hashCode();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    // We synchronize access to edit/view the addedEventTitles list.
    public static void clearAddedEventTitles() {
        synchronized (addedEventTitles) {
            addedEventTitles.clear();
        }
    }

    //
    public static DateFormat getMonthDayFormatter() {
        SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM);
        sdf.applyPattern(sdf.toPattern().replaceAll("[^\\p{Alpha}'年]*y+[^\\p{Alpha}'年]*", ""));
        return sdf;
    }

    public synchronized void sendAddedEventReminder(FullEvent event) {
        synchronized (addedEventTitles) {
            innerSendAddedEventReminder(event);
        }
    }

    // Only call this from synchrnoized methods, we separated it out to make the synchronization less indented
    private void innerSendAddedEventReminder(FullEvent event) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        addedEventTitles.add(event);
        Collections.sort(addedEventTitles, new Comparator<FullEvent>() {
            @Override
            public int compare(FullEvent a, FullEvent b) {
                // Would Long.compare, but it's not available until API 19, so just inline it here:
                long lhs = a.getStartTimeLong();
                long rhs = b.getStartTimeLong();
                return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
            }
        });

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ListenerService.this);

        Intent intent;
        if (addedEventTitles.size() > 1) {
            // We have multiple events added, so just drop them on the main search page
            // But we don't use ACTION_SEARCH since we have nothing to specifically search for.
            intent = new Intent(Intent.ACTION_MAIN);
            intent.setPackage(getPackageName());
        } else {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.getUrl()));
            intent.setPackage(getPackageName());
        }
        // Ensure we open this URL using the DanceDeets app
        intent.setPackage(getPackageName());

        PendingIntent pendingIntent = PendingIntent.getActivity(ListenerService.this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        notificationBuilder
                .setSmallIcon(R.drawable.ic_penguin_head_outline)
                .setContentTitle(getString(R.string.event_added))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_EMAIL)
                .setContentIntent(pendingIntent);

        if (addedEventTitles.size() > 1) {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle()
                    .setBigContentTitle(getString(R.string.event_added));
            inboxStyle.setSummaryText(getString(R.string.see_all_events));
            for (FullEvent eachEvent: addedEventTitles) {
                Date eventDate = eachEvent.getStartTime();
                DateFormat format = getMonthDayFormatter();
                inboxStyle.addLine(format.format(eventDate) + ": " + eachEvent.getTitle());
            }
            notificationBuilder
                    .setSubText(getString(R.string.see_all_events))
                    .setContentText(String.format(getResources().getQuantityString(R.plurals.n_events, addedEventTitles.size(), addedEventTitles.size())))
                    .setStyle(inboxStyle);
        } else {
            notificationBuilder
                    .setSubText(getString(R.string.open_event))
                    .setContentText(event.getTitle())
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(event.getDescription()));

            // This is a blocking operation
            notificationBuilder.setLargeIcon(getBitmap(event));
        }

        // TODO: Maybe add an "RSVP as Going" action?

        // The notificationId is used for overwriting existing notifications,
        // or ensuring separate notifications for separate events.
        int notificationId = EVENT_ADDED_NOTIFICATION_ID;
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}
