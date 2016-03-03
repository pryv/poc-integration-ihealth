package lsi.pryv.epfl.pryvironic.structures;

import com.pryv.api.model.Event;
import com.pryv.api.model.Stream;

import java.io.Serializable;

import lsi.pryv.epfl.pryvironic.utils.Connector;

/**
 * Created by Thieb on 19.02.2016.
 */
public abstract class Electrode implements Serializable{
    private Boolean active = false;

    public abstract String getName();
    public abstract void save();

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean isActive() {
        return active;
    }

    // TODO: Check if stream exists
    public void save(String streamID, String type, String content) {
        Stream stream = new Stream();
        stream.setId(streamID);
        stream.setName(streamID);
        Connector.saveStream(stream);

        Event event = new Event();
        event.setStreamId(streamID);
        event.setType(type);
        event.setContent(content);
        Connector.saveEvent(event);
    }

}