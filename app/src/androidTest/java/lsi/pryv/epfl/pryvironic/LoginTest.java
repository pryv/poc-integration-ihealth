package lsi.pryv.epfl.pryvironic;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import lsi.pryv.epfl.pryvironic.activities.LoginActivity;
import lsi.pryv.epfl.pryvironic.activities.MainActivity;

import static junit.framework.Assert.assertEquals;


/**
 * Created by Thieb on 01.03.2016.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginTest {

    private final static String USERNAME = "dummyuser";
    private final static String TOKEN = "dummytoken";

    @Rule
    public ActivityTestRule<LoginActivity> loginActivityRule = new ActivityTestRule(LoginActivity.class);

    @Test
    public void empty_creditentials_check() {
        assertEquals(LoginActivity.getUsername(),null);
        assertEquals(LoginActivity.getToken(),null);
    }

    @Test
    public void creditentials_storage_check() {
        LoginActivity.setCreditentials(USERNAME,TOKEN);
        assertEquals(LoginActivity.getUsername(),USERNAME);
        assertEquals(LoginActivity.getToken(),TOKEN);
    }

    @Test
    public void reset_creditentials_check() {
        LoginActivity.setCreditentials(USERNAME,TOKEN);
        LoginActivity.resetCreditentials();
        assertEquals(LoginActivity.getUsername(), null);
        assertEquals(LoginActivity.getToken(), null);
    }

}
