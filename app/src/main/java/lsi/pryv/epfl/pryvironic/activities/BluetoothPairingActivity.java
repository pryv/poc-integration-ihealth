package lsi.pryv.epfl.pryvironic.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import lsi.pryv.epfl.pryvironic.R;

public class BluetoothPairingActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> pairedDevices;
    private ListView devicesList;
    private final static int BLUETOOTH_ENABLED = 1;
    public final static String BLUETOOTH_ERROR = "BluetoothError";
    public final static String BLUETOOTH_NAME = "BluetoothName";
    private final static String ENABLED_ERROR = "Bluetooth is not enabled!";
    private final static String SUPPORTED_ERROR = "Bluetooth is not supported!";
    private final static String CANCEL_ERROR = "Bluetooth pairing canceled!";
    public static BluetoothDevice connectedDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_pairing);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            endWithError(SUPPORTED_ERROR);
        } else if (!bluetoothAdapter.isEnabled()) {
            // Bluetooth is not enable
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,BLUETOOTH_ENABLED);
        } else {
            updateDevicesList();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == BLUETOOTH_ENABLED) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                updateDevicesList();
            } else {
                endWithError(ENABLED_ERROR);
            }
        }
    }

    @Override
    public void onBackPressed() {
        endWithError(CANCEL_ERROR);
    }

    private void updateDevicesList() {
        pairedDevices = new ArrayList();
        pairedDevices.addAll(bluetoothAdapter.getBondedDevices());

        ArrayList<String> devices = new ArrayList();
        for(BluetoothDevice device: pairedDevices) {
            devices.add(device.getName()+"\n"+device.getAddress());
        }


        devicesList = (ListView)findViewById(R.id.devices_list);
        ArrayAdapter<String> adapter = new ArrayAdapter(this, R.layout.device_item, devices);
        devicesList.setAdapter(adapter);
        devicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                connectedDevice = pairedDevices.get(position);
                connect(connectedDevice);
            }
        });
    }

    private void connect(BluetoothDevice device) {
        Intent intent = new Intent();
        intent.putExtra(BLUETOOTH_NAME, device.getName());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private void endWithError(String error) {
        Intent intent = new Intent();
        intent.putExtra(BLUETOOTH_ERROR, error);
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
    }
}
