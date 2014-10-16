package com.dancedeets.dancedeets;

import android.app.Fragment;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ShareActionProvider;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.dancedeets.dancedeets.models.FullEvent;

import java.io.IOException;
import java.io.OutputStream;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;

/**
 * Shows zoomable/pannable event flyers, when clicked on from the event info page.
 */
public class ViewFlyerFragment extends Fragment {

    static final String LOG_TAG = "ViewFlyerFragment";

    protected FullEvent mEvent;
    protected Bitmap mBitmap;
    protected ShareActionProvider mShareActionProvider;

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
        // Sometimes the event gets loaded before the share menu is set up,
        // so this check handles that possibility and ensures the share intent is set.
        if (mBitmap != null) {
            setupShareIntent();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mEvent = FullEvent.parse(getArguments());
        Log.i(LOG_TAG, "Received Bundle: " + getArguments());
        final ImageViewTouch imageViewTouch = new ImageViewTouch(getActivity(), null);
        imageViewTouch.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);

        imageViewTouch.setBackgroundColor(Color.BLACK);

        ImageLoader photoLoader = VolleySingleton.getInstance().getPhotoLoader();
        photoLoader.get(mEvent.getCoverUrl(), new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                mBitmap = response.getBitmap();
                imageViewTouch.setImageBitmap(mBitmap);
                if (mShareActionProvider != null) {
                    setupShareIntent();
                }

            }

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Failed to load flyer image for display: " + error);
            }
        });
        return imageViewTouch;
    }

    public void setupShareIntent() {
        ContentValues image = new ContentValues();
        image.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
        Uri localImageUri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, image);
        try {
            OutputStream out = getActivity().getContentResolver().openOutputStream(localImageUri);
            boolean success = mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
            if (!success) {
                Log.e(LOG_TAG, "Failed to write flyer to disk for sharing: " + localImageUri);
            } else {
                // Create the share Intent
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/jpg");
                intent.putExtra(Intent.EXTRA_STREAM, localImageUri);
                // Share the title
                intent.putExtra(Intent.EXTRA_TEXT, mEvent.getTitle());
                mShareActionProvider.setShareIntent(intent);
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to write flyer to disk for sharing: " + localImageUri + ": " + e);
        }
    }


}
