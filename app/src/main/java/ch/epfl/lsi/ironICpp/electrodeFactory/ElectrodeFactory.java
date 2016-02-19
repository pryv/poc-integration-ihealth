package ch.epfl.lsi.ironICpp.electrodeFactory;

/**
 * Created by Thieb on 19.02.2016.
 */
public class ElectrodeFactory {
    // Use this method to get the appropriate Electrode
    public Electrode getElectrode(Byte electrodeType){

        if(electrodeType == null){
            return null;
        }

        if(electrodeType == 0x13){
            return new Bilirubin();

        } else if(electrodeType == 0x13){
            return new Glucose();

        } else if(electrodeType == 0x14){
            return new Lactate();

        } else if(electrodeType == 0x15){
            return new Bilirubin();

        } else if(electrodeType == 0x16){
            return new Sodium();

        } else if(electrodeType == 0x17){
            return new Potassium();

        } else if(electrodeType == 0x18){
            return new Temperature();

        } else if(electrodeType == 0x19){
            return new PH();
        }

        return null;
    }
}
