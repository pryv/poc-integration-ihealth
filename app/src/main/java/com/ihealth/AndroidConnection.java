package com.ihealth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.ihealth.activities.LoginActivity;
import com.pryv.Connection;
import com.pryv.Filter;
import com.pryv.api.OnlineEventsAndStreamsManager;
import com.pryv.database.DBinitCallback;
import com.pryv.interfaces.EventsCallback;
import com.pryv.interfaces.GetEventsCallback;
import com.pryv.interfaces.StreamsCallback;
import com.pryv.model.Event;
import com.pryv.model.Stream;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class that handles all communications with Pryv by:
 * initiating connection, setting up callbacks, creating/retrieving events/streams
 * and notifying UI through handlers
 */
public class AndroidConnection {
    private Connection connection;
    private EventsCallback eventsCallback;
    private StreamsCallback streamsCallback;

    public AndroidConnection (String username, String token) {
        setCallbacks();

        // Initiate new connection to Pryv with connected account
        connection = new Connection(username, token, LoginActivity.DOMAIN, false, new DBinitCallback());
    }

    /**
     * Save a new event to Pryv
     * @param streamId: id of the stream containing the new event
     * @param type: type of the new event
     * @param content: content of the new event
     */
    public void saveEvent(String streamId, String type, String content) {
        connection.events.create(new Event(streamId, null, type, content), eventsCallback);
    }

    public void saveEvent(String streamId, String type, String content, DateTime date) {
        Event event = new Event();
        event.setStreamId(streamId);
        event.setType(type);
        event.setContent(content);
        event.setDate(date);
        connection.events.create(event, eventsCallback);
    }

    /**
     * Save a new stream to Pryv
     * @param streamId: id of the new stream
     * @param streamName: name of the new stream
     */
    public Stream saveStream(String streamId, String streamName) {
        Stream stream = new Stream();
        stream.setId(streamId);
        stream.setName(streamName);
        connection.streams.create(stream, streamsCallback);
        return stream;
    }

    /**
     * Initiate custom callbacks
     */
    private void setCallbacks() {

        //Called when action related to events creation/modification complete
        eventsCallback = new EventsCallback() {

            @Override
            public void onApiSuccess(String s, Event event, String s1, Double aDouble) {
            }

            @Override
            public void onApiError(String s, Double aDouble) {
            }

            @Override
            public void onCacheSuccess(String s, Event event) {
            }

            @Override
            public void onCacheError(String s) {
            }
        };

        //Called when action related to streams complete
        streamsCallback = new StreamsCallback() {

            @Override
            public void onApiSuccess(String s, Stream stream, Double aDouble) {
                Log.d("Pryv",s);
            }

            @Override
            public void onApiError(String s, Double aDouble) {
                Log.d("Pryv", s);
            }

            @Override
            public void onCacheSuccess(String s, Stream stream) {
                Log.d("Pryv", s);
            }

            @Override
            public void onCacheError(String s) {
                Log.d("Pryv", s);
            }

        };

    }

}