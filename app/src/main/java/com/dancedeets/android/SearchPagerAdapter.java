package com.dancedeets.android;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Resources;
import android.support.v13.app.FragmentPagerAdapter;

/**
 * Created by lambert on 2015/08/10.
 */
public class SearchPagerAdapter extends FragmentPagerAdapter {
    // Tab Titles
    private static final int tabTitles[] = new int[] { R.string.tab_upcoming_events};//, R.string.tab_ongoing_events, R.string.tab_past_events};
    private static final int PAGE_COUNT = tabTitles.length;

    private final Resources mResources;

    private boolean mTwoPane;

    public SearchPagerAdapter(FragmentManager fm, Resources resources, boolean twoPane) {
        super(fm);
        mResources = resources;
        mTwoPane = twoPane;
    }

    @Override
    public Fragment getItem(int position) {
        EventListFragment eventListFragment = null;
        switch (position) {
            case 0:
                eventListFragment = new EventListFragment();
                eventListFragment.setEventSearchType(SearchOptions.TimePeriod.UPCOMING);
                eventListFragment.setTwoPane(mTwoPane);
                return eventListFragment;

            case 1:
                eventListFragment = new EventListFragment();
                eventListFragment.setEventSearchType(SearchOptions.TimePeriod.ONGOING);
                eventListFragment.setTwoPane(mTwoPane);
                return eventListFragment;

            case 2:
                eventListFragment = new EventListFragment();
                eventListFragment.setEventSearchType(SearchOptions.TimePeriod.PAST);
                eventListFragment.setTwoPane(mTwoPane);
                return eventListFragment;
        }
        return null;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mResources.getString(tabTitles[position]);
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
}
