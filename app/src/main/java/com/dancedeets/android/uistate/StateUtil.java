package com.dancedeets.android.uistate;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

/**
 * A series of functions for managing Bundled (serialized) and Retained (saved in memory) state
 * during UI tear-down/construction
 */
public class StateUtil {

    private static String BUNDLED_STATE = "BUNDLED_STATE";

    public static<Retained extends RetainedState> Retained createRetained(StateHolder<?, Retained> stateHolder, Activity activity) {
        String fragmentTag = "Fragment" + stateHolder.getUniqueTag();

        // Call this from the earliest possible moment that we can
        Fragment fragment = activity.getFragmentManager().findFragmentByTag(fragmentTag);
        Retained retained = (Retained) fragment;
        if (retained == null) {
            retained = stateHolder.buildRetainedState();
            if (retained != null) {
                retained.setRetainInstance(true);
                activity.getFragmentManager().beginTransaction()
                        .add(retained, fragmentTag)
                        .commit();
            }
        }
        return retained;
    }

    public static<Retained extends RetainedState> Retained createRetained(StateHolder<?, Retained> stateHolder, Fragment fragment) {
        Retained retained = createRetained(stateHolder, fragment.getActivity());
        if (retained != null) {
            retained.setTargetFragment(fragment, 0);
        }
        return retained;
    }

    public static<Retained extends RetainedState> void destroyRetained(Retained retained, Fragment fragment) {
        if (retained != null && !fragment.getActivity().isChangingConfigurations()) {
            if (fragment.isRemoving()) {
                fragment.getActivity().getFragmentManager().beginTransaction().remove(retained).commitAllowingStateLoss();
            }
        }
    }

    public static<Retained extends RetainedState> void destroyRetained(Retained retained, Activity activity) {
        if (retained != null && !activity.isChangingConfigurations()) {
            activity.getFragmentManager().beginTransaction().remove(retained).commitAllowingStateLoss();
        }
    }


    public static<Bundled extends BundledState> Bundled createBundled(StateHolder<Bundled, ?> stateHolder, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            return (Bundled) savedInstanceState.getSerializable(BUNDLED_STATE);
        } else {
            return stateHolder.buildBundledState();
        }
    }

    public static<Bundled extends BundledState> void saveBundled(StateHolder<Bundled, ?> stateHolder, Bundled mBundled, Bundle outState) {
        outState.putSerializable(BUNDLED_STATE, mBundled);
    }


}
