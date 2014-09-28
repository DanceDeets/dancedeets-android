package com.dancedeets.dancedeets;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

/**
 * Created by lambert on 2014/09/28.
 */
public class ViewFlyerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        VolleySingleton.getInstance(getApplicationContext());

        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            setTitle("Flyer for " + b.getString("title"));
        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Fragment f = new ViewFlyerFragment();
            f.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(android.R.id.content, f).commit();
        }
    }}
