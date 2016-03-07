package lsi.pryv.epfl.pryvironic.utils;

import lsi.pryv.epfl.pryvironic.activities.BluetoothPairingActivity;

/**
 * Created by Thieb on 29.02.2016.
 */
public class AccountManager {
    public static String userName;
    public static String token;
    public final static String DOMAIN = "pryv-switch.ch";
    public final static String APPID = "epfl-lsi-ironic";

    public static void setCreditentials(String user, String tk) {
        userName = user;
        token = tk;
    }

    public static void logout() {
        userName = null;
        token = null;
        BluetoothPairingActivity.connectedDevice = null;
    }
}