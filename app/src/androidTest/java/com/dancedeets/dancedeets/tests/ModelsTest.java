package com.dancedeets.dancedeets.tests;

import android.test.InstrumentationTestCase;

import com.dancedeets.dancedeets.models.FullEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by lambert on 2014/10/19.
 */
public class ModelsTest extends InstrumentationTestCase {

    private static final String LOG_TAG = "ModelsTest";

    private String readTextFile(InputStream inputStream) throws IOException {
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } finally {
            inputStream.close();
        }

        String jsonString = writer.toString();
        return jsonString;
    }

    public void testSomething() throws JSONException, IOException {
        // We need to grab from the resources of the test file
        InputStream inputStream = getInstrumentation().getContext().getResources().openRawResource(R.raw.fullevent_example_json);
        String jsonEvent = readTextFile(inputStream);
        FullEvent event = FullEvent.parse(new JSONObject(jsonEvent));

        assertEquals(event.getTitle(), "ROOTSNYC OCTOBER 2014");
    }
}
