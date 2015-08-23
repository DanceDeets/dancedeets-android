package com.dancedeets.android;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v13.app.FragmentCompat;
import android.support.v13.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.ViewGroup;

/**
 * A PagerAdapter for tracking the various search tabs we support.
 */
public class SearchPagerAdapter extends FragmentPagerAdapter {
    // Tab Titles
    private static final int tabTitles[] = new int[] { R.string.tab_upcoming_events, R.string.tab_ongoing_events, R.string.tab_past_events};
    private static final String LOG_TAG = "SearchPagerAdapter";
    private Fragment[] mFragments = new Fragment[tabTitles.length];
    private static final int PAGE_COUNT = tabTitles.length;

    private FragmentManager mFragmentManager;
    private final Resources mResources;
    private final SearchOptionsManager mSearchOptionsManager;

    private boolean mTwoPane;
    private SearchOptions mSearchOptions;

    public interface SearchOptionsManager {
        SearchOptions getSearchOptions();
    }

    public SearchPagerAdapter(FragmentManager fm, SearchOptionsManager searchOptionsManager, Resources resources, boolean twoPane) {
        super(fm);
        mFragmentManager = fm;
        mSearchOptionsManager = searchOptionsManager;
        mResources = resources;
        mTwoPane = twoPane;
    }

    public SearchOptions getSearchOptions() {
        return mSearchOptions;
    }

    public SearchTarget getSearchTarget(int position) {
        return (SearchTarget)mFragments[position];
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment f = (Fragment)super.instantiateItem(container, position);
        mFragments[position] = f;
        Log.i(LOG_TAG, "instantiateItem(" + position + ") returns " + mFragments[position]);
        return f;
    }

    @Override
    public Fragment getItem(int position) {
        EventListFragment eventListFragment = null;
        switch (position) {
            case 0:
                eventListFragment = new EventListFragment();
                eventListFragment.setEventSearchType(SearchOptions.TimePeriod.UPCOMING);
                break;

            case 1:
                eventListFragment = new EventListFragment();
                eventListFragment.setEventSearchType(SearchOptions.TimePeriod.ONGOING);
                break;

            case 2:
                eventListFragment = new EventListFragment();
                eventListFragment.setEventSearchType(SearchOptions.TimePeriod.PAST);
                break;
        }
        if (eventListFragment != null) {
            eventListFragment.setTwoPane(mTwoPane);
            eventListFragment.prepareForSearchOptions(mSearchOptionsManager.getSearchOptions());
        }
        return eventListFragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
        Log.i(LOG_TAG, "destroyItem(" + position + ") returns " + mFragments[position]);
        mFragments[position] = null;
    }

    private static String STATE_SUPERCLASS = "STATE_SUPERCLASS";
    private static String STATE_FRAGMENT_IDS = "STATE_FRAGMENT_IDS";
    private static String STATE_SEARCH_OPTIONS = "STATE_SEARCH_OPTIONS";

    @Override
    public Parcelable saveState() {
        Parcelable p = super.saveState();
        Bundle bundle = new Bundle();
        bundle.putParcelable(STATE_SUPERCLASS, p);

        Bundle fragmentsBundle = new Bundle();
        for (int i=0; i<mFragments.length; i++) {
            Fragment f = mFragments[i];
            if (f != null) {
                mFragmentManager.putFragment(fragmentsBundle, Integer.toString(i), f);
            }
        }
        bundle.putBundle(STATE_FRAGMENT_IDS, fragmentsBundle);
        bundle.putSerializable(STATE_SEARCH_OPTIONS, mSearchOptions);
        return bundle;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        Bundle bundle = (Bundle)state;
        if (bundle != null) {
            super.restoreState(bundle.getParcelable(STATE_SUPERCLASS), loader);
            mSearchOptions = (SearchOptions)bundle.getSerializable(STATE_SEARCH_OPTIONS);
            Bundle fragmentsBundle = bundle.getBundle(STATE_FRAGMENT_IDS);
            Iterable<String> keys = fragmentsBundle.keySet();
            for (String key: keys) {
                int index = Integer.parseInt(key);
                Fragment f = mFragmentManager.getFragment(fragmentsBundle, key);
                if (f != null) {
                    FragmentCompat.setMenuVisibility(f, false);
                    mFragments[index] = f;
                } else {
                    mFragments[index] = null;
                    Log.w(LOG_TAG, "Bad fragment at key " + key);
                }

            }
        }
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
