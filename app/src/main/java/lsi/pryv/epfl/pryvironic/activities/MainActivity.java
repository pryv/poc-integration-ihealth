package lsi.pryv.epfl.pryvironic.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;

import lsi.pryv.epfl.pryvironic.utils.AccountManager;
import lsi.pryv.epfl.pryvironic.utils.Connector;
import lsi.pryv.epfl.pryvironic.R;
import lsi.pryv.epfl.pryvironic.structures.BloodSensor;

public class MainActivity extends AppCompatActivity {

    private static BloodSensor sensor = null;
    private ListView electrodesList;
    private String[] electrodes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Connector.initiateConnection();
        sensor = new BloodSensor();

        electrodesList = (ListView)findViewById(R.id.checkbox_list);
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

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Log out")
                .setMessage("You are about to log out. Continue?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AccountManager.resetCreditentials();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }

    public void startMeasurement(View view) {
        if(sensor.getActiveElectrode().size()>0) {
            Intent intent = new Intent(this, MonitoringActivity.class);
            intent.putExtra("electrodes",sensor.getActiveElectrode());
            startActivity(intent);
        } else {
            Toast.makeText(this,"No electrode to monitor!",Toast.LENGTH_SHORT).show();
        }
    }

    public BloodSensor getSensor() {
        return sensor;
    }

}