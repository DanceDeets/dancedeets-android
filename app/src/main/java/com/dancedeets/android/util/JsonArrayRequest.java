package com.dancedeets.android.util;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;

/**
 * Created by lambert on 2014/10/12.
 */
public class JsonArrayRequest extends com.android.volley.toolbox.JsonRequest<JSONArray> {
    public JsonArrayRequest(int method, String url, String body,
                            Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
        super(method, url, body, listener, errorListener);
    }

    @Override
    public String getBodyContentType() {
        return "application/x-www-form-urlencoded; charset=utf-8";
    }

    // Different POST bodies should invalidate our cache key...though in reality, we probably just shouldn't cache.
    public String getCacheKey() {
        return super.getCacheKey() + new String(getBody());
    }
    @Override
    protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString =
                    new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            return Response.success(new JSONArray(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }
}
