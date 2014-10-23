package com.dancedeets.android.uistate;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by lambert on 2014/10/23.
 */
public abstract class StateFragment<Bundled extends BundledState, Derived extends DerivedState, Retained extends RetainedState> extends Fragment {
    private Derived mDerived;
    private Bundled mBundled;
    private Retained mRetained;

    protected abstract Bundled buildBundledState();
    protected abstract Derived buildDerivedState();
    protected abstract Retained buildRetainedState();
    // Must be unique
    public abstract String getUniqueTag();


    public StateFragment() {
        mDerived = buildDerivedState();
    }

    protected Bundled getBundledState() {
        return mBundled;
    }
    protected Derived getDerivedState() {
        return mDerived;
    }
    protected Retained getRetainedState() {
        return mRetained;
    }


    @Override
    public void onAttach(Activity activity) {
        String fragmentTag = "Fragment" + getUniqueTag();

        // Call this from the earliest possible moment that we can
        Fragment fragment = getFragmentManager().findFragmentByTag(fragmentTag);
        mRetained = (Retained)fragment;
        if (mRetained == null) {
            mRetained = buildRetainedState();
            mRetained.setRetainInstance(true);
            activity.getFragmentManager().beginTransaction()
                    .add(mRetained, fragmentTag)
                    .commit();
        }
        Log.i("TEST", "onAttach fragment " + this + ", adding new retained state " + mRetained);
        mRetained.setTargetFragment(this, 0);

        super.onAttach(activity);
    }

    @Override
    public void onDestroy() {
        if (!getActivity().isChangingConfigurations()) {
            getActivity().getFragmentManager().beginTransaction().remove(mRetained);
        }
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDerived.onActivityCreated(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String tag = getUniqueTag();
        if (savedInstanceState != null) {
            mBundled = (Bundled) savedInstanceState.getSerializable(tag);
        } else {
            mBundled = buildBundledState();
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(getUniqueTag(), mBundled);
    }
}