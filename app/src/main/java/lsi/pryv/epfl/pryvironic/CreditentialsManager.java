package lsi.pryv.epfl.pryvironic;

/**
 * Created by Thieb on 29.02.2016.
 */
public class CreditentialsManager {
    private static String userName;
    private static String token;

    public static void setCreditentials(String user, String tk) {
        userName = user;
        token = tk;
    }
}
