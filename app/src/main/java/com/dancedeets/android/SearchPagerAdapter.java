package com.dancedeets.android;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v13.app.FragmentCompat;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * A PagerAdapter for tracking the various search tabs we support.
 */
public class SearchPagerAdapter extends PagerAdapter {
    private static final boolean DEBUG = true;

    // Tab Titles
    private static final int tabTitles[] = new int[] { R.string.tab_upcoming_events, R.string.tab_ongoing_events, R.string.tab_past_events};
    private static final String LOG_TAG = "SearchPagerAdapter";
    private static final int PAGE_COUNT = tabTitles.length;

    private FragmentManager mFragmentManager;
    private FragmentTransaction mCurTransaction = null;
    private Fragment mCurrentPrimaryItem = null;

    private final Resources mResources;
    private final SearchOptionsManager mSearchOptionsManager;

    private boolean mTwoPane;
    private SearchOptions mSearchOptions;

    public interface SearchOptionsManager {
        SearchOptions getSearchOptions();
    }

    public SearchPagerAdapter(FragmentManager fm, SearchOptionsManager searchOptionsManager, Resources resources, boolean twoPane) {
        super();
        mFragmentManager = fm;
        mSearchOptionsManager = searchOptionsManager;
        mResources = resources;
        mTwoPane = twoPane;
    }

    public Fragment getExistingItem(ViewGroup container, int position) {
        String name = makeFragmentName(container.getId(), position);
        return mFragmentManager.findFragmentByTag(name);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = getExistingItem(container, position);
        if (fragment != null) {
            if (DEBUG) Log.i(LOG_TAG, "Attaching item #" + position + ": f=" + fragment);
            getCurrentTransaction().attach(fragment);
        } else {
            fragment = instantiateItem(position);
            if (DEBUG) Log.i(LOG_TAG, "Adding item #" + position + ": f=" + fragment);
            String name = makeFragmentName(container.getId(), position);
            getCurrentTransaction().add(container.getId(), fragment, name);
        }
        if (fragment != mCurrentPrimaryItem) {
            FragmentCompat.setMenuVisibility(fragment, false);
            FragmentCompat.setUserVisibleHint(fragment, false);
        }

        return fragment;
    }

    private Fragment instantiateItem(int position) {
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
        if (DEBUG) Log.i(LOG_TAG, "Detaching item #" + position + ": f=" + object
                + " v=" + ((Fragment)object).getView());
        getCurrentTransaction().remove((Fragment)object);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment)object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                FragmentCompat.setMenuVisibility(mCurrentPrimaryItem, false);
                FragmentCompat.setUserVisibleHint(mCurrentPrimaryItem, false);
            }
            if (fragment != null) {
                FragmentCompat.setMenuVisibility(fragment, true);
                FragmentCompat.setUserVisibleHint(fragment, true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }


    private FragmentTransaction getCurrentTransaction() {
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        return mCurTransaction;
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment)object).getView() == view;
    }

    private static String STATE_SUPERCLASS = "STATE_SUPERCLASS";
    private static String STATE_SEARCH_OPTIONS = "STATE_SEARCH_OPTIONS";

    @Override
    public Parcelable saveState() {
        Parcelable p = super.saveState();
        Bundle bundle = new Bundle();
        bundle.putParcelable(STATE_SUPERCLASS, p);
        bundle.putSerializable(STATE_SEARCH_OPTIONS, mSearchOptions);
        return bundle;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        Bundle bundle = (Bundle)state;
        if (bundle != null) {
            super.restoreState(bundle.getParcelable(STATE_SUPERCLASS), loader);
            mSearchOptions = (SearchOptions)bundle.getSerializable(STATE_SEARCH_OPTIONS);
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

    private static String makeFragmentName(int viewId, long id) {
        return "android:switcher:" + viewId + ":" + id;
    }
}
