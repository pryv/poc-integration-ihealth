package ch.epfl.lsi.ironICpp.electrodeFactory;

/**
 * Created by Thieb on 19.02.2016.
 */
public interface Electrode {
    // This method initialize an Electrode by setting all its properties and storing them on Pryv's cloud
    abstract void create();
}