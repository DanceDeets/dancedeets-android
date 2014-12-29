package com.dancedeets.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;

import com.facebook.model.GraphUser;

/**
 * Created by lambert on 2014/12/20.
 */
public class SendFeedback {
    public static void sendFeedback(Activity activity, GraphUser user) {
        Intent intent = buildIntent(activity, user);
        activity.startActivity(Intent.createChooser(intent, "Send feedback..."));
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        }
    }

    private static Intent buildIntent(Activity activity, GraphUser user) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setType("text/plain");
        String subject = "Feedback for Android DanceDeets " + getAppVersion(activity);
        String body =
                "\n____________________" +
                        "\nDevice Info:" +
                        getDeviceInformation(activity) +
                        //TODO(lambert): re-enable user information, when we can grab a GraphUser
                        //"\nUser Info:" +
                        //getUserInformation(user) +
                        "\n____________________" +
                        "\n" + activity.getResources().getString(R.string.enter_feedback_here) +
                        "\n";
        String uriText = "mailto:" + Uri.encode("feedback@dancedeets.com") +
                "?subject=" + Uri.encode(subject) +
                "&body=" + Uri.encode(body);
        Uri uri = Uri.parse(uriText);
        intent.setData(uri);
        return intent;
    }

    private static String getUserInformation(GraphUser user) {
        StringBuilder userInfoBuilder = new StringBuilder();
        userInfoBuilder.append("\nUID: ").append(user.getId());
        userInfoBuilder.append("\nName: ").append(user.getName());
        userInfoBuilder.append("\n");
        return userInfoBuilder.toString();

    }


    private static String getAppVersion(Context context) {
        try {
            return "v" + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "vUnknown";
        }
    }

    private static String getDeviceInformation(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        StringBuilder handsetInfoBuilder = new StringBuilder();
        handsetInfoBuilder.append("\nBrand: ").append(Build.BRAND);
        handsetInfoBuilder.append("\nManufacturer: ").append(Build.MANUFACTURER);
        handsetInfoBuilder.append("\nModel: ").append(Build.MODEL);
        handsetInfoBuilder.append("\nDevice: ").append(Build.DEVICE);
        handsetInfoBuilder.append("\nScreen Density: ").append(metrics.density);
        handsetInfoBuilder.append("\nScreen Size: ").append(metrics.widthPixels).append("x").append(metrics.heightPixels);
        handsetInfoBuilder.append("\nVersion SDK: ").append(Build.VERSION.SDK_INT);
        handsetInfoBuilder.append("\nVersion Codename: ").append(Build.VERSION.CODENAME);
        handsetInfoBuilder.append("\nVersion Incremental: ").append(Build.VERSION.INCREMENTAL);
        handsetInfoBuilder.append("\n");
        return handsetInfoBuilder.toString();
    }
}
