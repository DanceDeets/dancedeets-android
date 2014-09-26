package com.dancedeets.dancedeets;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class EventInfoFragment extends Fragment {

    static final String LOG_TAG = "EventInfoFragment";

    public EventInfoFragment() {
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_event_info,
                container, false);
        return rootView;
    }
}
