package com.dancedeets.android.uistate;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.dancedeets.android.FacebookActivity;

/**
 * A FacebookActivity subclass with Bundled and Retained state
 */
public abstract class StateFacebookActivity<Bundled extends BundledState, Retained extends RetainedState> extends FacebookActivity implements StateHolder<Bundled, Retained> {
    protected Bundled mBundled;
    protected Retained mRetained;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBundled = StateUtil.createBundled(this, savedInstanceState);
        mRetained = StateUtil.createRetained(this, this);
    }

    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        StateUtil.saveBundled(this, mBundled, outState);
    }

    @Override
    public void onDestroy() {
        StateUtil.destroyRetained(mRetained, this);
        super.onDestroy();
    }
}
