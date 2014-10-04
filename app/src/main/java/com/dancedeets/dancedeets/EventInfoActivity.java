package com.dancedeets.dancedeets;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.MenuItem;


public class EventInfoActivity extends Activity {

    private static String LOG_TAG = "EventInfoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        VolleySingleton.getInstance(getApplicationContext());

        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_event_info);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            setTitle(b.getString("title"));
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
            Fragment f = new EventInfoFragment();
            f.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(android.R.id.content, f).commit();

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {

            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                String parentName = NavUtils.getParentActivityName(this);
                final ComponentName target = new ComponentName(this, parentName);
                Intent upIntent = new Intent().setComponent(target);
                Log.i(LOG_TAG, "Home Selected: Navigating Home with Intent: " + upIntent);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    Log.d(LOG_TAG, "Home Selected: Should recreate task");
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                                    // Navigate up to the closest parent
                            .startActivities();
                } else {
                    Log.i(LOG_TAG, "Home Selected: Navigating up to previous task");
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
