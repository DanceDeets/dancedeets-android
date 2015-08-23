package com.dancedeets.android.uistate;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;

/**
 * A ListFragment with Bundled and Retained state management.
 */
public abstract class StateListFragment<Bundled extends BundledState, Retained extends RetainedState> extends ListFragment implements StateHolder<Bundled, Retained> {
    protected Bundled mBundled;
    protected Retained mRetained;

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
