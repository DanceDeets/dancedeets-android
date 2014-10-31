package com.dancedeets.android;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpStack;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Created by lambert on 2014/10/29.
 */
public class VolleyDiskBasedHttpStack implements HttpStack {

    private static final String LOG_TAG = "VolleyDiskBasedHttpStack";

    private boolean mBlockResponses;

    public VolleyDiskBasedHttpStack() {
        setBlockResponses(false);
    }

    public boolean isBlockResponses() {
        return mBlockResponses;
    }

    public void setBlockResponses(boolean blockResponses) {
        mBlockResponses = blockResponses;
    }

    protected HttpResponse constructHttpResponseForStream(InputStream stream) throws IOException {
        StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 0), 200, "OK");
        HttpResponse httpResponse = new BasicHttpResponse(statusLine);
        BasicHttpEntity httpEntity = new BasicHttpEntity();
        httpEntity.setContent(stream);
        //httpEntity.setContentLength(stream.available());
        httpResponse.setEntity(httpEntity);
        return httpResponse;
    }

    static String getFilenameForUrl(String urlString) {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        String[] pathComponents = url.getPath().split("/");
        return pathComponents[pathComponents.length - 1];
    }

    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
        String url = request.getUrl();
        Log.i(LOG_TAG, "Loading URL: " + url);
        String path = getFilenameForUrl(url);
        try {
            while (isBlockResponses()) {
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Sleeping due to isBlockResponses interrupted: " + e);
        }

        InputStream stream = getClass().getResourceAsStream("local_volley/" + path);
        if (stream != null) {
            return constructHttpResponseForStream(stream);
        } else {
            Log.e(LOG_TAG, "Could not find stream for resource: " + path);
            // Now return 404 not found
            StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 0), 404, "NOT FOUND");
            return new BasicHttpResponse(statusLine);
        }
    }
}
