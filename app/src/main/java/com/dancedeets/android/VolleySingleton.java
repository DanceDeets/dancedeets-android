package com.dancedeets.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

public class VolleySingleton {

    private static VolleySingleton instance;
    private RequestQueue requestQueue;
    private ImageLoader thumbnailLoader;
    private ImageLoader photoLoader;

    class ImageCache implements ImageLoader.ImageCache {
        // LRU of 100 images
        private final LruCache<String, Bitmap> cache;

        public ImageCache(int size) {
            cache = new LruCache<String, Bitmap>(size);
        }


        @Override
        public Bitmap getBitmap(String url) {
            return cache.get(url);
        }

        @Override
        public void putBitmap(String url, Bitmap bitmap) {
            cache.put(url, bitmap);
        }
    }

    private VolleySingleton(Context context) {
        requestQueue = Volley.newRequestQueue(context);

        // Can cache many small thumbnails
        thumbnailLoader = new ImageLoader(requestQueue, new ImageCache(100));

        // But can only cache a few big photos
        photoLoader = new ImageLoader(requestQueue, new ImageCache(3));
    }

    private VolleySingleton(RequestQueue newRequestQueue) {
        requestQueue = newRequestQueue;

        // Can cache many small thumbnails
        thumbnailLoader = new ImageLoader(requestQueue, new ImageCache(100));

        // But can only cache a few big photos
        photoLoader = new ImageLoader(requestQueue, new ImageCache(3));
    }

    public static VolleySingleton getInstance() {
        return instance;
    }

    public static VolleySingleton createInstance(Context context) {
        if (instance == null) {
            instance = new VolleySingleton(context);
        }
        return instance;
    }

    // Only used for testing
    static VolleySingleton createInstance(RequestQueue requestQueue) {
        instance = new VolleySingleton(requestQueue);
        return instance;
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public ImageLoader getThumbnailLoader() {
        return thumbnailLoader;
    }

    public ImageLoader getPhotoLoader() {
        return photoLoader;
    }

    ImageLoader.ImageListener mDummyListener = new ImageLoader.ImageListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
        }

        @Override
        public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
        }
    };

    public void prefetchThumbnail(String url) {
        thumbnailLoader.get(url, mDummyListener);
    }

    public void prefetchPhoto(String url) {
        photoLoader.get(url, mDummyListener);
    }
}