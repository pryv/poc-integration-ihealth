package lsi.pryv.epfl.pryvironic.structures;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by Thieb on 19.02.2016.
 */
public class BloodSensor implements Serializable, Sensor{
    private HashMap<String, Electrode> electrodes;
    private GlucoseElectrode glucose;
    private LactateElectrode lactate;
    private BilirubinElectrode bilirubin;
    private PotassiumElectrode potassium;
    private TemperatureElectrode temperature;
    private PHElectrode ph;

    public BloodSensor() {
        electrodes = new HashMap<>();

        glucose = new GlucoseElectrode();
        electrodes.put(glucose.getName(), glucose);

        lactate = new LactateElectrode();
        electrodes.put(lactate.getName(), lactate);

        bilirubin = new BilirubinElectrode();
        electrodes.put(bilirubin.getName(), bilirubin);

        potassium = new PotassiumElectrode();
        electrodes.put(potassium.getName(), potassium);

        temperature = new TemperatureElectrode();
        electrodes.put(temperature.getName(), temperature);

        ph = new PHElectrode();
        electrodes.put(ph.getName(), ph);
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
