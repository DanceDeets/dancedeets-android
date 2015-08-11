package com.dancedeets.android;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

/**
 * Created by lambert on 2015/08/10.
 */
public class SearchPagerAdapter extends FragmentPagerAdapter {
    // Tab Titles
    private static final String tabTitles[] = new String[] { "Upcoming Events" };//, "Ongoing Events" };
    final static int PAGE_COUNT = tabTitles.length;

    private boolean mTwoPane;

    public SearchPagerAdapter(FragmentManager fm, boolean twoPane) {
        super(fm);
        mTwoPane = twoPane;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                EventListFragment upcoming_fragment = new EventListFragment();
                upcoming_fragment.setEventSearchType(EventListFragment.EventSearchType.UPCOMING);
                upcoming_fragment.setTwoPane(mTwoPane);
                return upcoming_fragment;

            case 1:
                EventListFragment ongoing_fragment = new EventListFragment();
                ongoing_fragment.setEventSearchType(EventListFragment.EventSearchType.ONGOING);
                ongoing_fragment.setTwoPane(mTwoPane);
                return ongoing_fragment;
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
