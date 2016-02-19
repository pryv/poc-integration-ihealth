package ch.epfl.lsi.ironICpp.electrodeFactory;

/**
 * Created by Thieb on 19.02.2016.
 */
public class FactoryDemo {
    public void main() {
        ElectrodeFactory eF = new ElectrodeFactory();
        Electrode e = eF.getElectrode("Glucose");
        e.create();
    }
}