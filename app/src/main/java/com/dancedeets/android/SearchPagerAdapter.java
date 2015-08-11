package com.dancedeets.android;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

/**
 * Created by lambert on 2015/08/10.
 */
public class SearchPagerAdapter extends FragmentPagerAdapter {
    // Tab Titles
    private static final String tabTitles[] = new String[] { "Upcoming Events", "Ongoing Events", "Past Events" };
    final static int PAGE_COUNT = tabTitles.length;

    private boolean mTwoPane;

    public SearchPagerAdapter(FragmentManager fm, boolean twoPane) {
        super(fm);
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
        return tabTitles[position];
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
}
