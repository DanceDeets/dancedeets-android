package com.dancedeets.dancedeets.tests.javatests;

import com.dancedeets.dancedeets.models.CoverData;
import com.dancedeets.dancedeets.models.FullEvent;
import com.dancedeets.dancedeets.models.Venue;
import com.dancedeets.dancedeets.tests.robotests.RobolectricGradleTestRunner;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Created by lambert on 2014/10/19.
 */
@RunWith(RobolectricGradleTestRunner.class)
public class ModelsTest {

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

        return writer.toString();
    }

    private JSONObject getJsonObjectFromResource(String path) {
        InputStream inputStream = getClass().getResourceAsStream(path);
        try {
            String jsonEvent = readTextFile(inputStream);
            return new JSONObject(jsonEvent);
        } catch (JSONException e) {
            fail("JSONException when loading resource: " + e);
        } catch (IOException e) {
            fail("IOException when loading resource: " + e);
        }
        return null;
    }

    @Test
    public void testVenueEverything() throws JSONException {
        Venue venue = Venue.parse(getJsonObjectFromResource("venue_everything"));

        assertEquals("Venue Name", venue.getName());
        assertEquals(41.0, venue.getLatLong().getLatitude(), 0.01);
        assertEquals(-74.0, venue.getLatLong().getLongitude(), 0.01);
    }

    @Test
    public void testVenueEverythingExceptZip() throws JSONException {
        Venue venue = Venue.parse(getJsonObjectFromResource("venue_everything_except_zip"));

        assertEquals("Venue Name", venue.getName());
        assertEquals(41.0, venue.getLatLong().getLatitude(), 0.01);
        assertEquals(-74.0, venue.getLatLong().getLongitude(), 0.01);
    }

    @Test
    public void testVenueNameIdGeocodeOnly() throws JSONException {
        Venue venue = Venue.parse(getJsonObjectFromResource("venue_name_id_geocode_only"));

        assertEquals("Venue Name", venue.getName());
        assertEquals(41.0, venue.getLatLong().getLatitude(), 0.01);
        assertEquals(-74.0, venue.getLatLong().getLongitude(), 0.01);
    }

    @Test
    public void testVenueNameOnly() throws JSONException {
        Venue venue = Venue.parse(getJsonObjectFromResource("venue_name_only"));

        assertEquals("Venue Name", venue.getName());
        assertNull(venue.getLatLong());
    }

    @Test
    public void testCoverUnsorted() throws JSONException {
        CoverData coverData = CoverData.parse(getJsonObjectFromResource("cover_unsorted"));

        assertEquals("Cover ID", coverData.getId());
        assertEquals(2048, coverData.getLargestCover().getHeight());
        assertEquals(1365, coverData.getLargestCover().getWidth());
    }

    @Test
    public void testEventStartAndEndTime() throws JSONException {
        FullEvent event = FullEvent.parse(getJsonObjectFromResource("fullevent_start_and_end_time"));
        assertEquals("Oct 15, 2014 10:00 PM", event.getStartTimeString(Locale.US));
        assertEquals("Oct 16, 2014 4:00 AM", event.getEndTimeString(Locale.US));
    }

    @Test
    public void testEventStartTimeOnly() throws JSONException {
        FullEvent event = FullEvent.parse(getJsonObjectFromResource("fullevent_no_end_time"));
        assertEquals("Oct 15, 2014 10:00 PM", event.getStartTimeString(Locale.US));
        assertNull(event.getEndTimeString(Locale.US));
        assertEquals(0, event.getEndTimeLong());
    }

    @Test
    public void testEventAllDay() throws JSONException {
        FullEvent event = FullEvent.parse(getJsonObjectFromResource("fullevent_allday"));
        assertEquals("Oct 15, 2014", event.getStartTimeString(Locale.US));
        assertNull(event.getEndTimeString(Locale.US));
    }

    @Test
    public void testFullEventSerialization() throws JSONException, ClassNotFoundException, IOException {
        FullEvent event = FullEvent.parse(getJsonObjectFromResource("fullevent_example_json"));

        ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
        ObjectOutputStream ooStream = new ObjectOutputStream(baoStream);
        ooStream.writeObject(event);
        byte[] bytes = baoStream.toByteArray();
        ByteArrayInputStream baiStream = new ByteArrayInputStream(bytes);
        ObjectInputStream oiStream = new ObjectInputStream(baiStream);
        FullEvent roundTripEvent = (FullEvent)oiStream.readObject();
        assertEquals(event, roundTripEvent);
    }
}
