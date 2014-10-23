package com.dancedeets.android.uistate;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

/**
 * Created by lambert on 2014/10/23.
 */
public abstract class StateFragment<Bundled extends BundledState, Derived extends DerivedState, Retained extends RetainedState> extends Fragment {
    protected Bundled mBundled;
    protected Derived mDerived;
    protected Retained mRetained;

    public abstract void buildBundleDerived();
    public abstract Retained buildRetained();

    // Must be unique
    public abstract String getRetainedFragmentTag();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBundled.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        buildBundleDerived();
        // Call this from the earliest possible moment that we can
        Fragment fragment = getFragmentManager().findFragmentByTag(getRetainedFragmentTag());
        mRetained = (Retained)fragment;
        if (mRetained == null) {
            mRetained = buildRetained();
            mRetained.setRetainInstance(true);
            activity.getFragmentManager().beginTransaction()
                    .add(mRetained, getRetainedFragmentTag())
                    .commit();
        }
        mRetained.setTargetFragment(this, 0);

        super.onAttach(activity);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDerived.onActivityCreated(this);
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mBundled.onSaveInstanceState(outState);
    }
}
