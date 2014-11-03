package com.dancedeets.android.uistate;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;

/**
 * Created by lambert on 2014/10/23.
 */
public abstract class StateListFragment<Bundled extends BundledState, Retained extends RetainedState> extends ListFragment implements StateHolder<Bundled, Retained> {
    private Bundled mBundled;
    private Retained mRetained;


    public static interface StateFragmentHolder<Bundled, Retained> extends StateHolder<Bundled, Retained> {
        public Bundled buildBundledState();
        public Retained buildRetainedState();

        // Must be unique
        public String getUniqueTag();
    }

    protected Bundled getBundledState() {
        return mBundled;
    }
    protected Retained getRetainedState() {
        return mRetained;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mRetained = StateUtil.createRetained(this, this);
    }

    @Override
    public void onDestroy() {
        StateUtil.destroyRetained(mRetained, this);
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBundled = StateUtil.createBundled(this, savedInstanceState);
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        StateUtil.saveBundled(this, mBundled, outState);
    }
}
