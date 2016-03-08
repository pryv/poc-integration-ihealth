package lsi.pryv.epfl.pryvironic.structures;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by Thieb on 19.02.2016.
 */
public class SensorImpl implements Serializable, Sensor{
    private HashMap<String, Electrode> electrodes;
    private Electrode glucose;
    private Electrode lactate;
    private Electrode bilirubin;
    private Electrode potassium;
    private Electrode temperature;
    private Electrode ph;

    public SensorImpl() {

    }

    // Use this method to get the appropriate Electrode
    @Override
    public Electrode getElectrodeFromByteID(Byte byteID) {

        if (byteID == null) {
            return null;

        } else {
            switch (byteID) {
                case 0x13:
                    return glucose;
                case 0x14:
                    return lactate;
                case 0x15:
                    return bilirubin;
                case 0x16:
                    return potassium;
                case 0x17:
                    return temperature;
                case 0x18:
                    return ph;
            }
        }

        return null;
    }

    @Override
    public HashMap<String, Electrode> getElectrodes() {
        return electrodes;
    }

    @Override
    public HashMap<String, Electrode> getActiveElectrode() {
        HashMap<String, Electrode> activeElectrodes = new HashMap ();
        for(Electrode e: electrodes.values()) {
            if(e.isActive()) {
                activeElectrodes.put(e.getName(),e);
            }
        }
        return activeElectrodes;
    }

}
