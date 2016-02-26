package ch.epfl.lsi.ironICpp.electrodeFactory;

/**
 * Created by Thieb on 19.02.2016.
 */
public class FactoryDemo {
    public void main() {
        MeasurementFactory eF = new MeasurementFactory();
        Measurement e = eF.getMeasurement(new Byte("0x13"), 0.3);
        e.save();
    }
}