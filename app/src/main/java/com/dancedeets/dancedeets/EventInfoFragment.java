package com.dancedeets.dancedeets;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

public class EventInfoFragment extends Fragment {

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    static final String LOG_TAG = "EventInfoFragment";

    public EventInfoFragment() {
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ImageLoader imageLoader = VolleySingleton.getInstance(null).getImageLoader();

        View rootView = inflater.inflate(R.layout.fragment_event_info,
                container, false);

        Bundle b = getArguments();

        NetworkImageView cover = (NetworkImageView)rootView.findViewById(R.id.cover);
        Log.i(LOG_TAG, "Received Bundle: " + b);
        cover.setImageUrl(b.getString("cover"), imageLoader);

        TextView title = (TextView)rootView.findViewById(R.id.title);
        title.setText(b.getString("title"));
        return rootView;
    }
}
