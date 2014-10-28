package com.dancedeets.android.robotests;

import android.os.Looper;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.a;

import java.util.concurrent.TimeUnit;

/**
 * Created by lambert on 2014/10/25.
 */
public class MockGoogleApiClient implements GoogleApiClient {

    com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks mCallbacks;

    public MockGoogleApiClient(com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    @Override
    public <A extends Api.a, T extends a.b<? extends Result, A>> T a(T t) {
        return null;
    }

    @Override
    public <A extends Api.a, T extends a.b<? extends Result, A>> T b(T t) {
        return null;
    }

    @Override
    public <C extends Api.a> C a(Api.c<C> cc) {
        return null;
    }

    @Override
    public Looper getLooper() {
        return null;
    }

    @Override
    public void connect() {
        mCallbacks.onConnected(null);
    }

    @Override
    public ConnectionResult blockingConnect() {
        return null;
    }

    @Override
    public ConnectionResult blockingConnect(long l, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public void disconnect() {

    }

    @Override
    public void reconnect() {

    }

    @Override
    public void stopAutoManage() {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isConnecting() {
        return false;
    }

    @Override
    public void registerConnectionCallbacks(ConnectionCallbacks connectionCallbacks) {

    }

    @Override
    public boolean isConnectionCallbacksRegistered(ConnectionCallbacks connectionCallbacks) {
        return false;
    }

    @Override
    public void unregisterConnectionCallbacks(ConnectionCallbacks connectionCallbacks) {

    }

    @Override
    public void registerConnectionFailedListener(OnConnectionFailedListener onConnectionFailedListener) {

    }

    @Override
    public boolean isConnectionFailedListenerRegistered(OnConnectionFailedListener onConnectionFailedListener) {
        return false;
    }

    @Override
    public void unregisterConnectionFailedListener(OnConnectionFailedListener onConnectionFailedListener) {

    }
}
