package com.dancedeets.dancedeets;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;

/**
 * Shows zoomable/pannable event flyers, when clicked on from the event info page.
 */
public class ViewFlyerFragment extends Fragment {

    static final String LOG_TAG = "ViewFlyerFragment";

    //TODO: add a shareable action for sharing raw flyers
    // http://stackoverflow.com/questions/22795525/how-to-share-image-that-is-downloaded-and-cached-with-volley


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Event event = new Event(getArguments());
        Log.i(LOG_TAG, "Received Bundle: " + getArguments());
        final ImageViewTouch imageViewTouch = new ImageViewTouch(getActivity(), null);
        imageViewTouch.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);

        container.addView(imageViewTouch);

        ImageLoader photoLoader = VolleySingleton.getInstance().getPhotoLoader();
        photoLoader.get(event.getCoverUrl(), new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                imageViewTouch.setImageBitmap(response.getBitmap());
            }

            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        return null;
    }


    Bundle b = getArguments();
}
