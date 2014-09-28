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
 * Created by lambert on 2014/09/28.
 */
public class ViewFlyerFragment extends Fragment {

    static final String LOG_TAG = "ViewFlyerFragment";

    //TODO: add a shareable action for sharing raw flyers

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*
            new ImageViewTouch(getActivity(), null);
            new TouchImageView(getActivity());
         */
        Bundle b = getArguments();
        Log.i(LOG_TAG, "Received Bundle: " + b);
        final ImageViewTouch imageViewTouch = new ImageViewTouch(getActivity(), null);
        imageViewTouch.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);

        //ViewGroup.LayoutParams params = new FrameLayout.LayoutParams()
        //getActivity().getBaseContext());
        container.addView(imageViewTouch);

        ImageLoader imageLoader = VolleySingleton.getInstance(null).getImageLoader();
        imageLoader.get(b.getString("cover"), new ImageLoader.ImageListener() {
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
