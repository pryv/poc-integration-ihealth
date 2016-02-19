//Declaring the application package in which the class is included
package ch.epfl.lsi.ironICpp;

//Importing some Java useful objects.
import java.util.Set;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class ActivityDeviceList extends Activity {
	/**
	 * This Activity appears as a Dialog It lists any paired devices and
	 * devices detected in the area after discovery. When a device is chosen
	 * by the user, the MAC address of the device is sent back to the parent
	 * Activity in the result Intent.
	 */

	/**
	 * Class constants.
	 */
	private static final boolean D = true; // Debug flag used to marking down some informations in the Log register
	public static String EXTRA_DEVICE_ADDRESS = "device_address"; // Intent constant

	/**
	 * Class member variables
	 */
	private BluetoothAdapter mBtAdapter; // Local device Bluetooth adapter
	private ArrayAdapter<String> mDevicesArrayAdapter;// List of devices

	/**
	 * Class methods.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		/**
		 * Called when the activity is starting.
		 * @param savedInstanceState  Bundle containing the data it most recently supplied in onSaveInstanceState(Bundle). 
		 */

		super.onCreate(savedInstanceState);

		if(D) Log.d("ActivityDeviceList", "onCreate()");

		setTitle(R.string.title_paired_devices);

		// Setup the window layout
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_device_list);

		// Set result CANCELED in case the user backs out
		setResult(Activity.RESULT_CANCELED);

		// Initialize array adapters. One for already paired devices and one for newly discovered devices
		mDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.list_device);

		// Find and set up the ListView for devices
		ListView devicesListView = (ListView) findViewById(R.id.list_devices);
		devicesListView.setAdapter(mDevicesArrayAdapter);
		devicesListView.setOnItemClickListener(mDeviceClickListener);

		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND); // Instantiate intent filter for device found
		this.registerReceiver(mReceiver, filter); // Register the broadcast receiver for the event: device found.

		// Register for broadcasts when discovery has finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); // Instantiate intent filter for discovery ended
		this.registerReceiver(mReceiver, filter); // Register the broadcast receiver for the event: discovery ended.

		// Get the local device Bluetooth adapter
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();

		//Get a set of currently paired devices
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		// Debug using a fake device
		mDevicesArrayAdapter.add("Random generator\n 00:00:00:00:00:00");

		// If there are paired devices, add each one to the ArrayAdapter
		if(pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				mDevicesArrayAdapter.add(device.getName() + "\n " + device.getAddress());
			}
		} else {
			String noDevices = getResources().getText(R.string.none_paired).toString();
			mDevicesArrayAdapter.add(noDevices);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/**
		 * Initialize the contents of the Activity's standard options menu.
		 * @param menu  The options menu in which to place the items.
		 */

		// Setup Main Activity Options Menu
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_device_list_menu, menu);
		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/**
		 * This method is called whenever an item in the options menu is selected.
		 * @param item  The item selected in the options menu. 
		 */

		switch (item.getItemId()) {
			case R.id.button_scan:
				doDiscovery();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		/**
		 * Perform any final cleanup before an activity is destroyed.
		 */

		if(D) Log.d("ActivityDeviceList", "onDestroy()");

		super.onDestroy();

		// Cancel discovery
		if(mBtAdapter != null) {
			mBtAdapter.cancelDiscovery();
		}

		// Unregister broadcast listeners
		this.unregisterReceiver(mReceiver);
	}
	private void doDiscovery() {
		/**
		 * Start device discover with the BluetoothAdapter.
		 */

		if(D) Log.d("ActivityDeviceList", "doDiscovery()");

		// Indicate scanning status
		setProgressBarIndeterminateVisibility(true);
		findViewById(R.id.button_scan).setEnabled(false);

		// Turn on sub-title for new devices
		setTitle(R.string.title_scanned_devices);

		// Clear list
		mDevicesArrayAdapter.clear();

		// If we're already discovering, stop it
		if(mBtAdapter.isDiscovering()) {
			mBtAdapter.cancelDiscovery();
		}

		// Request discover from BluetoothAdapter
		mBtAdapter.startDiscovery();
	}

	/**
	 * Objects used by the members of the class.
	 */
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
		/**
		 * Event Listener called when the desired device is selected by the user.
		 */

		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
			/**
			 * Method to handle the event.
			 */

			if(D) Log.d("ActivityDeviceList", "onItemClick");

			// Cancel discovery because it's costly and we're about to connect
			mBtAdapter.cancelDiscovery();

			// Get the device MAC address, which is the last 17 chars in the View
			String info = ((TextView) v).getText().toString();
			String address = info.substring(info.length() - 17);

			// Create the result Intent and include the MAC address
			Intent intent = new Intent();
			intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

			// Set result and finish this Activity
			setResult(Activity.RESULT_OK, intent);
			finish();
		}
	};
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		/**
		 * Object used to capture the notification events reported by the operating system.
		 * It is able to receive notifications for two events: device found and end of discovery.
		 */

		@Override
		public void onReceive(Context context, Intent intent) {
			/**
			 * Method to handle the event.
			 */

			// Recovering the action having caused the event
			String action = intent.getAction();


			if(BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Device found event
				if(D) Log.d("ActivityDeviceList. BroadcastReceiver.","Found new device");
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// If it's already paired, skip it, because it's been listed already
				if(device.getBondState() != BluetoothDevice.BOND_BONDED) {
					mDevicesArrayAdapter.add(device.getName() + "\n " + device.getAddress());
				}
			} else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				// Discovery finished event
				if(D) Log.d("ActivityDeviceList. BroadcastReceiver.","Discovery ended");
				setProgressBarIndeterminateVisibility(false);
				findViewById(R.id.button_scan).setEnabled(true);
				if(mDevicesArrayAdapter.getCount() == 0) {
					String noDevices = getResources().getText(R.string.none_found).toString();
					mDevicesArrayAdapter.add(noDevices);
				}
			}
		}
	};
}
