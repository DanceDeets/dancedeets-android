package com.dancedeets.android.uistate;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

/**
 * Created by lambert on 2014/11/02.
 */
public class StateUtil {

    public static<Retained extends RetainedState> Retained createRetained(StateHolder<?, Retained> stateHolder, Activity activity) {
        String fragmentTag = "Fragment" + stateHolder.getUniqueTag();

        // Call this from the earliest possible moment that we can
        Fragment fragment = activity.getFragmentManager().findFragmentByTag(fragmentTag);
        Retained retained = (Retained) fragment;
        if (retained == null) {
            retained = stateHolder.buildRetainedState();
            retained.setRetainInstance(true);
            activity.getFragmentManager().beginTransaction()
                    .add(retained, fragmentTag)
                    .commit();
        }
        return retained;
    }

    public static<Retained extends RetainedState> Retained createRetained(StateHolder<?, Retained> stateHolder, Fragment fragment) {
        Retained retained = createRetained(stateHolder, fragment.getActivity());
        retained.setTargetFragment(fragment, 0);
        return retained;
    }

    public static<Retained extends RetainedState> void destroyRetained(Retained retained, Fragment fragment) {
        if (!fragment.getActivity().isChangingConfigurations()) {
            if (fragment.isRemoving()) {
                fragment.getActivity().getFragmentManager().beginTransaction().remove(retained).commitAllowingStateLoss();
            }
        }
    }


    public static<Bundled extends BundledState> Bundled createBundled(StateHolder<Bundled, ?> stateHolder, Bundle savedInstanceState) {
        String tag = stateHolder.getUniqueTag();
        if (savedInstanceState != null) {
            return (Bundled) savedInstanceState.getSerializable(tag);
        } else {
            return stateHolder.buildBundledState();
        }
    }

    public static<Bundled extends BundledState> void saveBundled(StateHolder<Bundled, ?> stateHolder, Bundled mBundled, Bundle outState) {
        outState.putSerializable(stateHolder.getUniqueTag(), mBundled);
    }


}
