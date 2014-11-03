package com.dancedeets.android.uistate;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by lambert on 2014/10/23.
 */
public abstract class StateDialogFragment<Bundled extends BundledState, Retained extends RetainedState> extends DialogFragment implements StateHolder<Bundled, Retained> {
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
        Log.i("StateDialog", "onAttach to " + this + " with retained " + mRetained);
    }

    @Override
    public void onDestroy() {
        Log.i("StateDialog", "onDestroy to " + this + " with retained " + mRetained);
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
