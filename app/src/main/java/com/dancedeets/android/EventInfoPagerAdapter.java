package com.dancedeets.android;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v13.app.FragmentCompat;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.ViewGroup;

import com.dancedeets.android.models.IdEvent;

import java.util.ArrayList;

/**
 * Created by lambert on 2014/10/16.
 */
public class EventInfoPagerAdapter extends FragmentStatePagerAdapter {

    private static final String LOG_TAG = "EventInfoPagerAdapter";

    protected String[] mEventList;
    protected EventInfoFragment.OnEventReceivedListener mOnEventReceivedListener;

    // We have no way to grab the fragment for a given position, which makes it near-impossible to implement getItem().
    // So we need to override instantiateItem/destroyItem to keep track of fragments, so we can find a title.
    protected ArrayList<EventInfoFragment> mFragments;
    protected FragmentManager mFragmentManager;

    public EventInfoPagerAdapter(FragmentManager fm, EventInfoFragment.OnEventReceivedListener listener, String[] eventList) {
        super(fm);
        mOnEventReceivedListener = listener;
        mFragmentManager = fm;
        mEventList = eventList;
        mFragments = new ArrayList<EventInfoFragment>();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
        // Make sure to clean out our fragments, so we don't leak memory
        mFragments.set(position, null);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        EventInfoFragment fragment = (EventInfoFragment)super.instantiateItem(container, position);
        fragment.setOnEventReceivedListener(mOnEventReceivedListener);
        // Grow the array as necessary, like super.instantiateItem does
        while (mFragments.size() <= position) {
            mFragments.add(null);
        }
        mFragments.set(position, fragment);
        return fragment;
    }

    public EventInfoFragment getExistingItem(int i) {
        if (i < mFragments.size()) {
            return mFragments.get(i);
        } else {
            return null;
        }
    }

    @Override
    public Fragment getItem(int i) {
        IdEvent event = new IdEvent(mEventList[i]);
        EventInfoFragment eventInfoFragment = new EventInfoFragment();
        eventInfoFragment.setArguments(event.getBundle());
        return eventInfoFragment;
    }

    @Override
    public int getCount() {
        return mEventList.length;
    }

    private static String STATE_SUPERCLASS = "STATE_SUPERCLASS";
    private static String STATE_FRAGMENT_IDS = "STATE_FRAGMENT_IDS";

    @Override
    public Parcelable saveState() {
        Parcelable p = super.saveState();
        Bundle bundle = new Bundle();
        bundle.putParcelable(STATE_SUPERCLASS, p);

        Bundle fragmentsBundle = new Bundle();
        for (int i=0; i<mFragments.size(); i++) {
            Fragment f = mFragments.get(i);
            if (f != null) {
                mFragmentManager.putFragment(fragmentsBundle, Integer.toString(i), f);
            }
        }
        bundle.putBundle(STATE_FRAGMENT_IDS, fragmentsBundle);
        return bundle;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        Bundle bundle = (Bundle)state;
        if (bundle != null) {
            super.restoreState(bundle.getParcelable(STATE_SUPERCLASS), loader);
            Bundle fragmentsBundle = bundle.getBundle(STATE_FRAGMENT_IDS);
            mFragments.clear();
            mFragments.ensureCapacity(fragmentsBundle.size());
            Iterable<String> keys = fragmentsBundle.keySet();
            for (String key: keys) {
                int index = Integer.parseInt(key);
                Fragment f = mFragmentManager.getFragment(fragmentsBundle, key);
                if (f != null) {
                    while (mFragments.size() <= index) {
                        mFragments.add(null);
                    }
                    FragmentCompat.setMenuVisibility(f, false);
                    mFragments.set(index, (EventInfoFragment)f);
                } else {
                    Log.w(LOG_TAG, "Bad fragment at key " + key);
                }

            }
        }
    }
}
