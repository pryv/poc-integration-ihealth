package lsi.pryv.epfl.pryvironic;

import android.util.Log;

import com.pryv.Connection;
import com.pryv.Pryv;
import com.pryv.api.EventsCallback;
import com.pryv.api.StreamsCallback;
import com.pryv.api.database.DBinitCallback;
import com.pryv.api.model.Event;
import com.pryv.api.model.Stream;

import java.util.Map;

/**
 * Created by Thieb on 26.02.2016.
 */
public class Connector {
    private static Connection connection = null;
    private static StreamsCallback streamsCallback = null;
    private static EventsCallback eventsCallback = null;

    public static void initiateConnection() {
        Pryv.deactivateCache();
        Pryv.deactivateSupervisor();
        connection = new Connection("tmodoux", "cikxq15dt0he41fyqu194bhre", new DBinitCallback() {
        });
        instanciateSCB();
        instanciateECB();
    }

    public static void saveEvent(Event event) {
        connection.createEvent(event, eventsCallback);
    }

    public static void saveStream(Stream stream) {
        connection.createStream(stream, streamsCallback);
    }

    private static void instanciateECB() {
        eventsCallback = new EventsCallback() {

            @Override
            public void onEventsRetrievalSuccess(Map<String, Event> events, Double serverTime) {
                Log.d("Pryv", "onEventsRetrievalSuccess");
            }

            @Override
            public void onEventsRetrievalError(String errorMessage, Double serverTime) {
                Log.d("Pryv", "onEventsRetrievalError");
            }

            @Override
            public void onEventsSuccess(String successMessage, Event event, Integer stoppedId,
                                        Double serverTime) {
                Log.d("Pryv", "onEventsSuccess");
            }

            @Override
            public void onEventsError(String errorMessage, Double serverTime) {
                Log.d("Pryv", "onEventsError");
            }

        };
    }

    private static void instanciateSCB() {
        streamsCallback = new StreamsCallback() {

            @Override
            public void onStreamsSuccess(String successMessage, Stream stream, Double serverTime) {
                Log.d("Pryv", "onStreamsSuccess");
            }

            @Override
            public void onStreamsRetrievalSuccess(Map<String, Stream> streams, Double serverTime) {
                Log.d("Pryv", "onStreamsRetrievalSuccess");
            }

            @Override
            public void onStreamsRetrievalError(String errorMessage, Double serverTime) {
                Log.d("Pryv", "onStreamsRetrievalError");
            }

            @Override
            public void onStreamError(String errorMessage, Double serverTime) {
                Log.d("Pryv", "onStreamError");
            }
        };
    }
}
