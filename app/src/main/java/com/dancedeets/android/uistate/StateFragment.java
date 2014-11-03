package com.dancedeets.android.uistate;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

/**
 * Created by lambert on 2014/10/23.
 */
public abstract class StateFragment<Bundled extends BundledState, Retained extends RetainedState> extends Fragment implements StateHolder<Bundled, Retained> {
    private Bundled mBundled;
    private Retained mRetained;

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
