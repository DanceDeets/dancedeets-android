package com.dancedeets.android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.crashlytics.android.Crashlytics;
import com.dancedeets.android.models.FullEvent;
import com.dancedeets.android.uistate.BundledState;
import com.dancedeets.android.uistate.RetainedState;
import com.dancedeets.android.uistate.StateFragment;
import com.dancedeets.android.util.VolleySingleton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;

/**
 * Shows zoomable/pannable event flyers, when clicked on from the event info page.
 */
public class ViewFlyerFragment extends StateFragment<
        ViewFlyerFragment.MyBundledState,
        RetainedState> {

    static protected class MyBundledState extends BundledState {
        protected FullEvent mEvent;
    }
    protected ShareActionProvider mShareActionProvider;
    protected ImageViewTouch mImageViewTouch;
    protected Bitmap mBitmap;

    private static final String LOG_TAG = "ViewFlyerFragment";

    @Override
    public MyBundledState buildBundledState() {
        return new MyBundledState();
    }

    @Override
    public RetainedState buildRetainedState() {
        return new RetainedState();
    }

    @Override
    public String getUniqueTag() {
        return LOG_TAG;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.view_flyer_menu, menu);

        MenuItem shareItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) shareItem.getActionProvider();
        // Track share item clicks
        mShareActionProvider.setOnShareTargetSelectedListener(
                new ShareActionProvider.OnShareTargetSelectedListener() {
                    @Override
                    public boolean onShareTargetSelected(ShareActionProvider shareActionProvider,
                                                         Intent intent) {
                        AnalyticsUtil.trackEvent("Share Flyer", mBundled.mEvent);
                        return false;
                    }
                });

        // We need to set a different history file name, as Android only stores one ShareIntent per ShareHistoryFileName.
        // This means the EventInfoFragment ShareIntent is overwritten by the ViewFlyerFragment ShareIntent,
        // and when the user navigates up/back, it uses the wrong ShareIntent.
        // We could re-set the ShareIntent manually, but it seems cleaner to just use two history files.
        // We also need call setShareHistoryFilename before calling setShareIntent, so we do that immediately.
        mShareActionProvider.setShareHistoryFileName("flyer_" + ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);

        // Sometimes the event gets loaded before the share menu is set up,
        // so this check handles that possibility and ensures the share intent is set.
        if (mBitmap != null) {
            setupShareIntent();
        }
    }

    static private void loadPhoto(String imageUrl, final RetainedState retainedState) {
        ImageLoader photoLoader = VolleySingleton.getInstance().getPhotoLoader();
        photoLoader.get(imageUrl, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                ViewFlyerFragment viewFlyerFragment = (ViewFlyerFragment) retainedState.getTargetFragment();
                viewFlyerFragment.mBitmap = response.getBitmap();
                viewFlyerFragment.mImageViewTouch.setImageBitmap(viewFlyerFragment.mBitmap);
                if (viewFlyerFragment.mShareActionProvider != null) {
                    viewFlyerFragment.setupShareIntent();
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Failed to load flyer image for display: " + error);
                Toast.makeText(retainedState.getActivity().getBaseContext(), "Failed to load flyer! " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Nullable
    private String getEvent() {
        if (mBundled.mEvent != null) {
            return mBundled.mEvent.getId();
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBundled.mEvent = FullEvent.parse(getArguments());
        Crashlytics.log("ViewFlyerFragment.onCreateView: Event " + getEvent());
        Log.i(LOG_TAG, "Received Bundle: " + getArguments());
        mImageViewTouch = new ImageViewTouch(getActivity(), null);
        mImageViewTouch.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);

        mImageViewTouch.setBackgroundColor(Color.BLACK);

        loadPhoto(mBundled.mEvent.getCoverUrl(), mRetained);

        AnalyticsUtil.trackEvent("View Flyer", mBundled.mEvent);

        return mImageViewTouch;
    }

    public void setupShareIntent() {
        Crashlytics.log("setupShareIntent: Event " + getEvent());
        // Save off flyers to the Pictures/DanceDeets/ directory (they'll show up in Gallery too)
        File localImageUriDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DanceDeets");
        localImageUriDir.mkdirs();
        // Name the files using the sortable event title and date
        String dateString = new SimpleDateFormat("yyyyMMdd").format(mBundled.mEvent.getStartTimeLong());
        String sanitizedTitle = mBundled.mEvent.getTitle().replace("/", "-");
        File localImageUri = new File(localImageUriDir, dateString + " - " + sanitizedTitle + ".jpg");
        Log.i(LOG_TAG, "Saving flyer to " + localImageUri);
        try {
            OutputStream out = new FileOutputStream(localImageUri);
            boolean success = mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
            // Update metadata so file shows up in other directory listings correctly
            MediaScannerConnection.scanFile(getActivity(), new String[]{localImageUri.getAbsolutePath()}, null, null);

            if (!success) {
                Log.e(LOG_TAG, "Failed to write flyer to disk for sharing: " + localImageUri);
            } else {
                // Create the share Intent
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(localImageUri));
                intent.putExtra(Intent.EXTRA_SUBJECT, EventSharing.getTitle(mBundled.mEvent));
                intent.putExtra(Intent.EXTRA_TEXT, EventSharing.getBodyText(mBundled.mEvent));
                mShareActionProvider.setShareIntent(intent);
            }

        } catch (IOException e) {
            Crashlytics.log(Log.ERROR, LOG_TAG, "Failed to write flyer to disk for sharing: " + e);
            Crashlytics.logException(e);
            e.printStackTrace();
        }
    }
}
