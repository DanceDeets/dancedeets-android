package com.dancedeets.android.uistate;

/**
 * Created by lambert on 2014/11/02.
 */
public interface StateHolder<Bundled, Retained> {
    public Bundled buildBundledState();
    public Retained buildRetainedState();

    // Must be unique
    public String getUniqueTag();
}
