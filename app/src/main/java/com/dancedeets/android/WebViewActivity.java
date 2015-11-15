package com.dancedeets.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.crashlytics.android.Crashlytics;

/**
 * Created by lambert on 2015/10/27.
 */
public class WebViewActivity extends Activity {

    private static final String LOG_TAG = "WebViewActivity";

    private WebView mWebView;

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.webview_activity);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.i(LOG_TAG, url);
                if (url.contains("webview=1")) {
                    view.loadUrl(url);
                    return true;
                } else {
                    return false;
                }
            }
        });

        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);

        Intent intent = getIntent();
        Uri url = intent.getData();
        Crashlytics.log(Log.INFO, LOG_TAG, "Viewing URL: " + url);

        Uri newUrl = url.buildUpon().appendQueryParameter("webview", "1").build();
        Log.i(LOG_TAG, newUrl.toString());
        mWebView.loadUrl(newUrl.toString());
    }

}
