package ch.epfl.lsi.ironICpp.electrodeFactory;

/**
 * Created by Thieb on 19.02.2016.
 */
public class MeasurementFactory {
    // Use this method to get the appropriate Electrode
    public Measurement getMeasurement(Byte measurementType, double value){

        if(measurementType == null){
            return null;
        }

        if(measurementType != null) {
            switch(measurementType) {
                case 0x13: return new GlucoseMeasure(value);
                case 0x14: return new LactateMeasure(value);
                case 0x15: return new BilirubinMeasure(value);
                case 0x16: return new PotassiumMeasure(value);
                case 0x17: return new TemperatureMeasure(value);
                case 0x18: return new PHMeasure(value);
            }
        }
        return null;

    }
}
