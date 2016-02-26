package lsi.pryv.epfl.pryvironic.structures;

import java.util.HashMap;

/**
 * Created by Thieb on 19.02.2016.
 */
public class Sensor {
    private HashMap<String, Electrode> electrodes;
    private GlucoseElectrode glucose;
    private LactateElectrode lactate;
    private BilirubinElectrode bilirubin;
    private PotassiumElectrode potassium;
    private TemperatureElectrode temperature;
    private PHElectrode ph;

    public Sensor() {
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

    public Electrode getElectrodeFromStringID(String stringID) {

        if (stringID == null) {
            return null;

        } else {
            switch (stringID) {
                case "Glucose":
                    return glucose;
                case "Lactate":
                    return lactate;
                case "Bilirubin":
                    return bilirubin;
                case "Potassium":
                    return potassium;
                case "Temperature":
                    return temperature;
                case "PH":
                    return ph;
            }
        }

        return null;
    }

    public HashMap<String, Electrode> getElectrodes() {
        return electrodes;
    }

}
