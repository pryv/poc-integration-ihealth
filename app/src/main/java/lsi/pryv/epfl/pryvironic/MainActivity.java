package lsi.pryv.epfl.pryvironic;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;

import com.pryv.Connection;
import com.pryv.Pryv;
import com.pryv.api.EventsCallback;
import com.pryv.api.StreamsCallback;
import com.pryv.api.database.DBinitCallback;
import com.pryv.api.model.Event;
import com.pryv.api.model.Stream;

import java.util.Map;

import lsi.pryv.epfl.pryvironic.structures.Electrode;
import lsi.pryv.epfl.pryvironic.structures.Sensor;

public class MainActivity extends AppCompatActivity {

    private static Sensor sensor = null;
    private ListView electrodesList;
    private String[] electrodes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Connector.initiateConnection();
        sensor = new Sensor();

        electrodesList = (ListView)findViewById(R.id.listView1);
        electrodes = sensor.getElectrodes().keySet().toArray(new String[sensor.getElectrodes().keySet().size()]);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.list_item, electrodes);
        electrodesList.setAdapter(adapter);
        electrodesList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        electrodesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckedTextView checkBox = (CheckedTextView) view;
                sensor.getElectrodes().get(checkBox.getText()).setActive(checkBox.isChecked());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startMeasurement(View view) {
        for(String electrode: electrodes) {
            Electrode currentElectrode = sensor.getElectrodeFromStringID(electrode);
            if(currentElectrode.isActive()) {
                currentElectrode.save();
            }
        }
    }

}
