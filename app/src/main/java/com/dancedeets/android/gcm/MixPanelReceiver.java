package com.dancedeets.android.gcm;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.mixpanel.android.mpmetrics.MPConfig;
import com.mixpanel.android.mpmetrics.ResourceIds;
import com.mixpanel.android.mpmetrics.ResourceReader;

/**
 * This is mostly a copy of MixPanel's GCMReceiver, modified to be callable from our ListenerService for MixPanel notifications.
 * https://github.com/mixpanel/mixpanel-android/blob/master/src/main/java/com/mixpanel/android/mpmetrics/GCMReceiver.java
 */
public class MixPanelReceiver {

    private static String LOG_TAG = "MixPanelReceiver";

    /*
     * Package scope for testing only, do not call outside of GCMReceiver.
     */

    /* package */ static class NotificationData {
        private NotificationData(int anIcon, CharSequence aTitle, String aMessage, Intent anIntent) {
            icon = anIcon;
            title = aTitle;
            message = aMessage;
            intent = anIntent;
        }

        public final int icon;
        public final CharSequence title;
        public final String message;
        public final Intent intent;
    }

    /* package */ Intent getDefaultIntent(Context context) {
        final PackageManager manager = context.getPackageManager();
        return manager.getLaunchIntentForPackage(context.getPackageName());
    }

    /* package */ NotificationData readInboundIntent(Context context, Bundle inboundBundle, ResourceIds iconIds) {
        final PackageManager manager = context.getPackageManager();

        final String message = inboundBundle.getString("mp_message");
        final String iconName = inboundBundle.getString("mp_icnm");
        final String uriString = inboundBundle.getString("mp_cta");
        CharSequence notificationTitle = inboundBundle.getString("mp_title");

        if (message == null) {
            return null;
        }

        int notificationIcon = -1;
        if (null != iconName) {
            if (iconIds.knownIdName(iconName)) {
                notificationIcon = iconIds.idFromName(iconName);
            }
        }

        ApplicationInfo appInfo;
        try {
            appInfo = manager.getApplicationInfo(context.getPackageName(), 0);
        } catch (final PackageManager.NameNotFoundException e) {
            appInfo = null;
        }

        if (notificationIcon == -1 && null != appInfo) {
            notificationIcon = appInfo.icon;
        }

        if (notificationIcon == -1) {
            notificationIcon = android.R.drawable.sym_def_app_icon;
        }

        if (null == notificationTitle && null != appInfo) {
            notificationTitle = manager.getApplicationLabel(appInfo);
        }

        if (null == notificationTitle) {
            notificationTitle = "A message for you";
        }

        final Intent notificationIntent = buildNotificationIntent(context, uriString);

        return new NotificationData(notificationIcon, notificationTitle, message, notificationIntent);
    }

    private Intent buildNotificationIntent(Context context, String uriString) {
        Uri uri = null;
        if (null != uriString) {
            uri = Uri.parse(uriString);
        }

        final Intent ret;
        if (null == uri) {
            ret = getDefaultIntent(context);
        } else {
            ret = new Intent(Intent.ACTION_VIEW, uri);
        }

        return ret;
    }

    private Notification buildNotification(Context context, Bundle inboundBundle, ResourceIds iconIds) {
        final NotificationData notificationData = readInboundIntent(context, inboundBundle, iconIds);
        if (null == notificationData) {
            return null;
        }

        if (MPConfig.DEBUG) Log.d(LOG_TAG, "MP GCM notification received: " + notificationData.message);
        final PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                notificationData.intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        final Notification notification;
        if (Build.VERSION.SDK_INT >= 16) {
            notification = makeNotificationSDK16OrHigher(context, contentIntent, notificationData);
        } else {
            notification = makeNotificationSDK11OrHigher(context, contentIntent, notificationData);
        }

        return notification;
    }

    // We define this class here, because the ResourceReader.Drawables has a protected constructor.
    static class ConstructableDrawables extends ResourceReader.Drawables {
        protected ConstructableDrawables(String resourcePackageName, Context context) {
            super(resourcePackageName, context);
        }
    }

    public void handleNotificationIntent(Context context, Bundle bundle) {
        final MPConfig config = MPConfig.getInstance(context);
        String resourcePackage = config.getResourcePackageName();
        if (null == resourcePackage) {
            resourcePackage = context.getPackageName();
        }

        final ResourceIds drawableIds = new ConstructableDrawables(resourcePackage, context);
        final Context applicationContext = context.getApplicationContext();
        final Notification notification = buildNotification(applicationContext, bundle, drawableIds);

        if (null != notification) {
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(0, notification);
        }
    }

    @SuppressWarnings("deprecation")
    @TargetApi(11)
    private Notification makeNotificationSDK11OrHigher(Context context, PendingIntent intent, NotificationData notificationData) {
        final Notification.Builder builder = new Notification.Builder(context).
                setSmallIcon(notificationData.icon).
                setTicker(notificationData.message).
                setWhen(System.currentTimeMillis()).
                setContentTitle(notificationData.title).
                setContentText(notificationData.message).
                setContentIntent(intent);

        final Notification n = builder.getNotification();
        n.flags |= Notification.FLAG_AUTO_CANCEL;
        return n;
    }

    @SuppressLint("NewApi")
    @TargetApi(16)
    private Notification makeNotificationSDK16OrHigher(Context context, PendingIntent intent, NotificationData notificationData) {
        final Notification.Builder builder = new Notification.Builder(context).
                setSmallIcon(notificationData.icon).
                setTicker(notificationData.message).
                setWhen(System.currentTimeMillis()).
                setContentTitle(notificationData.title).
                setContentText(notificationData.message).
                setContentIntent(intent).
                setStyle(new Notification.BigTextStyle().bigText(notificationData.message));

        final Notification n = builder.build();
        n.flags |= Notification.FLAG_AUTO_CANCEL;
        return n;
    }
}
