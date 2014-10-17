package com.dancedeets.dancedeets;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.dancedeets.dancedeets.models.IdEvent;

import java.util.ArrayList;

/**
 * Created by lambert on 2014/10/16.
 */
public class EventInfoPagerAdapter extends FragmentStatePagerAdapter {

    private String LOG_TAG = "EventInfoPagerAdapter";

    protected String[] mEventList;

    // We have no way to grab the fragment for a given position, which makes it near-impossible to implement getItem().
    // So we need to override instantiateItem/destroyItem to keep track of fragments, so we can find a title.
    protected ArrayList<EventInfoFragment> mFragments;

    public EventInfoPagerAdapter(FragmentManager fm, String[] eventList) {
        super(fm);
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
}
