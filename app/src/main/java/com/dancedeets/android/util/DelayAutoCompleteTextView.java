package com.dancedeets.android.util;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

// Inspired by http://stackoverflow.com/questions/13193990/android-howto-send-requests-to-the-google-api-when-a-user-pauses-typing
public class DelayAutoCompleteTextView extends AutoCompleteTextView {
    public DelayAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private static int DELAYED_MESSAGE = 0;
    private static int DELAY = 500;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            DelayAutoCompleteTextView.super.performFiltering((CharSequence) msg.obj, msg.arg1);
        }
    };

    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        mHandler.removeMessages(DELAYED_MESSAGE);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(DELAYED_MESSAGE, keyCode, 0, text), DELAY);
    }
}