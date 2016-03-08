package lsi.pryv.epfl.pryvironic;

import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.widget.ListView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import lsi.pryv.epfl.pryvironic.activities.MainActivity;
import lsi.pryv.epfl.pryvironic.structures.Electrode;
import lsi.pryv.epfl.pryvironic.structures.SensorImpl;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;


/**
 * Created by Thieb on 01.03.2016.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

    private SensorImpl sensor;
    private int size;

    @Rule
    public ActivityTestRule<MainActivity> mainActivityRule = new ActivityTestRule<MainActivity>(MainActivity.class);

    @Before
    public void initSensor(){
        sensor = mainActivityRule.getActivity().getSensor();
        size = sensor.getElectrodes().size();
    }

    @Test
    public void checkBox_size() {
        onView(withId(R.id.checkbox_list)).check(ViewAssertions.matches(Matchers.withListSize(size)));
    }

    @Test
    public void checkBox_uncheck() {
        for(Electrode e: sensor.getElectrodes().values()) {
            assertFalse(e.isActive());
        }
        assertEquals(0, sensor.getActiveElectrode().size());
    }

    @Test
    public void checkBox_checked() {
        int i = 0;
        for(Electrode e: sensor.getElectrodes().values()) {
            onData(anything()).inAdapterView(withId(R.id.checkbox_list)).atPosition(i).perform(click());
            assertTrue(e.isActive());
            i++;
            assertEquals(i,sensor.getActiveElectrode().size());
        }
    }

    @Test
    public void checkBox_doubleCheck() {
        onData(anything()).inAdapterView(withId(R.id.checkbox_list)).atPosition(0).perform(click());
        onData(anything()).inAdapterView(withId(R.id.checkbox_list)).atPosition(0).perform(click());
        assertFalse(sensor.getElectrodes().values().iterator().next().isActive());
    }

}

class Matchers {
    public static Matcher<View> withListSize (final int size) {
        return new TypeSafeMatcher<View>() {
            @Override public boolean matchesSafely (final View view) {
                return ((ListView) view).getChildCount () == size;
            }

            @Override public void describeTo (final Description description) {
                description.appendText ("ListView should have " + size + " items");
            }
        };
    }
}
