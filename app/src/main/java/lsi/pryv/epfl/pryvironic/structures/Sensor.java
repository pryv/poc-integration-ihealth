package lsi.pryv.epfl.pryvironic.structures;

import java.util.HashMap;

/**
 * Created by Thieb on 03.03.2016.
 */
public interface Sensor {

    Electrode getElectrodeFromByteID(Byte byteID);
    HashMap<String, Electrode> getElectrodes();
    HashMap<String, Electrode> getActiveElectrode();

    }
