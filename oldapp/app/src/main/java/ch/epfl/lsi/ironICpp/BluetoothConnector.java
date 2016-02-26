package ch.epfl.lsi.ironICpp;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class BluetoothConnector {

	private static final String LOG_HEADER = "BluetoothConnector";
	private static final boolean D = true;
	static ArrayList<String> arrayOfCheckedElectrodes = new ArrayList<String>();
	//Bluetooth
	private static final int ENABLE_BLUETOOTH = 1; // Message code for bluetooth enable request
	private static final int CONNECT_DEVICE = 2; // Message code for bluetooth connection to device
	private BluetoothAdapter myBluetoothAdapter = null; // Local device Bluetooth adapter.
	final Messenger myMessenger = new Messenger(new IncomingHandler(this));
	private static int lastBluetoothStatus = -1;
	static double actual_mean = 0;
	public static Context context;
	Messenger mService = null;
	private static Activity activity;
	static Byte address ;
	static Byte parameters;
	private static boolean plot = false;
	boolean mIsBound;



	// Constructor
	public BluetoothConnector(Context context, ArrayList<String> arrayOfCheckedElectrodes, Activity activity){
		BluetoothConnector.context = context;
		BluetoothConnector.arrayOfCheckedElectrodes = arrayOfCheckedElectrodes;
		BluetoothConnector.activity = activity;
	}

	public synchronized void onResume(boolean calibration_on) {

		CheckIfServiceIsRunning();
		doBindService();

		// Get BT state for actionbar
		Log.e(LOG_HEADER, "Pinging actionbar for menu");
		Intent i = new Intent(context, DataService.class);
		i.putExtra(DataService.SVC_ACTION, DataService.SVC_BT_GET_STATE);
		//i.putExtra(DataService.ELECTRODES_NAME, arrayOfCheckedElectrodes);
		i.putExtra(DataService.CALIBRATION_ON, calibration_on);
		context.startService(i);

	}

	void CheckIfServiceIsRunning() {
		Intent i = new Intent(context, DataService.class);
		context.startService(i);
	}


	public void onPause(boolean calibration_on) {
		// Quit service if safe
		Intent i = new Intent(context, DataService.class);
		i.putExtra(DataService.SVC_ACTION, DataService.SVC_QUIT_IF_POSSIBLE);
		//i.putExtra(DataService.ELECTRODES_NAME, arrayOfCheckedElectrodes);
		i.putExtra(DataService.CALIBRATION_ON, calibration_on);
		context.startService(i);
	}


	public void onDestroy() {
		try {
			doUnbindService();
		} catch (Throwable t) {
			if(D) Log.e(LOG_HEADER, "Failed to unbind from the service", t);
		}
	}

	public boolean Connect(boolean calibration_on){
		Log.d(LOG_HEADER,"Chose 'connect button'");
		MultitouchPlot.isPause=true;
		// Check if Bluetooth is active
		Intent i = new Intent(context,DataService.class);
		myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// If the adapter is null, then Bluetooth is not supported.
		if(myBluetoothAdapter == null)

		{
			if(D) Log.i(LOG_HEADER, "mBluetoothAdapter is null");
			Toast.makeText(context,
					R.string.bt_not_available_connect_to_fake_device, Toast.LENGTH_LONG)
					.show();
			// Connecting to fake bluetooth device
			//Intent i = new Intent(context, DataService.class);
			String address = "00:00:00:00:00:00";
			i.putExtra(DataService.SVC_ACTION, DataService.SVC_BT_CONNECT_TO_DEVICE);
			i.putExtra(DataService.SVC_BT_DEVICE_MAC, address);
			i.putExtra(DataService.ELECTRODES_NAME, arrayOfCheckedElectrodes);//
			i.putExtra(DataService.CALIBRATION_ON, calibration_on);
			context.startService(i);
		}
		else if(!myBluetoothAdapter.isEnabled())
		{
			if(D) Log.i(LOG_HEADER, "mBluetoothAdapter exists but BT disabled");
			// If Bluetooth is not on, request that it be enabled.
			// setup() will then be called during onActivityResult.
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			activity.startActivityForResult(enableIntent, ENABLE_BLUETOOTH);
		}
		else
		{
			// Bluetooth exists and is active, start DeviceList activity
			if(D) Log.d(LOG_HEADER, "Bluetooth OK");
			Intent pairIntent = new Intent(context, ActivityDeviceList.class);
			activity.startActivityForResult(pairIntent, CONNECT_DEVICE);
		}
		return true;
	}

	public void Disconnect(){
		if(D) Log.i(LOG_HEADER, "Chose 'disconnect_bt'");
		Intent i = new Intent(context, DataService.class);
		i.putExtra(DataService.SVC_ACTION, DataService.SVC_BT_DISCONNECT);
		//i.putExtra(DataService.ELECTRODES_NAME, arrayOfCheckedElectrodes);
		i.putExtra(DataService.CALIBRATION_ON, false);
		context.startService(i);
	}

	public void BT_Enabled(int resultCode){
		if(resultCode == Activity.RESULT_OK)
		{
			Log.d(LOG_HEADER,"BT OK");
			Intent pairIntent = new Intent(context, ActivityDeviceList.class);
			activity.startActivityForResult(pairIntent, CONNECT_DEVICE);
		}
		else
		{
			// User did not enable it or an error occurred.
			if(D) Log.d(LOG_HEADER, "activityResult method. Bluetooth not enabled");
			Toast.makeText(context, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
		}
	}

	public void ConnectedToDevice(int resultCode, Intent data,boolean calibration_on){
		Intent i = new Intent (context, DataService.class);
		if (resultCode == Activity.RESULT_OK) {
			String address = data.getExtras().getString(ActivityDeviceList.EXTRA_DEVICE_ADDRESS);
			i = new Intent(context, DataService.class);
			i.putExtra(DataService.SVC_ACTION, DataService.SVC_BT_CONNECT_TO_DEVICE);
			i.putExtra(DataService.ELECTRODES_NAME, arrayOfCheckedElectrodes); //
			i.putExtra(DataService.SVC_BT_DEVICE_MAC, address);
			i.putExtra(DataService.CALIBRATION_ON, calibration_on);
			context.startService(i);
		}
	}

	public void doBindService() {
		context.bindService(new Intent(context, DataService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	public void doUnbindService() {
		if (mIsBound) {
			if (mService != null) {
				try {
					Message msg = Message.obtain(null, DataService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = myMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service has crashed.
				}
			}
			// Detach our existing connection.
			mIsBound = false;
			context.unbindService(mConnection);
		}
	}
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			Toast.makeText(context, R.string.service_connected, Toast.LENGTH_SHORT).show();
			try {
				Message msg = Message.obtain(null, DataService.MSG_REGISTER_CLIENT);
				msg.replyTo = myMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even do anything with it
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been unexpectedly disconnected - process crashed.
			Toast.makeText(context, R.string.service_disconnected, Toast.LENGTH_SHORT).show();
			Disconnect();
			Toast.makeText(context, R.string.bt_disconnected, Toast.LENGTH_SHORT).show();
			updateActionBar(false);
			mService = null;
		}
	};

	/** Service handling methods */
	static class IncomingHandler extends Handler {
		private final WeakReference<BluetoothConnector> mActivity;

		public IncomingHandler(BluetoothConnector bluetoothConnector)
		{
			mActivity = new WeakReference<BluetoothConnector>(bluetoothConnector);
		}

		@Override
		public void handleMessage(Message msg) {
			BluetoothConnector activity = mActivity.get();
			if (activity != null) {
				switch (msg.what) {
					case DataService.MSG_NEW_BT_STATE:
						Log.e(LOG_HEADER, "Recieved new bluetooth state");
						activity.updateActionBar(msg.getData().getInt("btState") == BluetoothService.STATE_CONNECTED);
						Log.e(LOG_HEADER, "BT state received is "+msg.getData().getInt("btState")+" and previously was "+lastBluetoothStatus);
						if(lastBluetoothStatus != -1 && lastBluetoothStatus != msg.getData().getInt("btState"))
						{
							// If really new state (not simply refresh of display, show toast)
							switch(msg.getData().getInt("btState"))
							{
								case BluetoothService.STATE_CONNECTED:
									Toast.makeText(BluetoothConnector.context, R.string.bt_connected, Toast.LENGTH_SHORT).show();
									break;
								case BluetoothService.STATE_CONNECTING:
									//Toast.makeText(BluetoothConnector.context, R.string.bt_connecting, Toast.LENGTH_SHORT).show();
									MultitouchPlot.isPause=true;
									break;
								case BluetoothService.STATE_NONE:

									Toast.makeText(BluetoothConnector.context, R.string.bt_not_connected, Toast.LENGTH_SHORT).show();
									break;
								default:
									break;
							}

						}
						lastBluetoothStatus = msg.getData().getInt("btState");
						break;
					case DataService.MSG_NEW_FILE:
						//activity.listRecordings();
						if(BluetoothConnector.activity.toString().contains("ChipParametersActivity")){
							String filename = msg.getData().getString("filename");
							Log.d(LOG_HEADER, "file "+ filename);
							ChipParametersActivity.signalPlot.loadFile(filename);
							ChipParametersActivity.SaveFileName(filename);
							Toast.makeText(BluetoothConnector.context, "Saving to "+msg.getData().getString("filename"), Toast.LENGTH_SHORT).show();
							MultitouchPlot.isPause=false;
						}
						else if(BluetoothConnector.activity.toString().contains("MonitoringActivity")){
							Toast.makeText(BluetoothConnector.context, "Saving to "+msg.getData().getString("filename"), Toast.LENGTH_SHORT).show();
							String filename = msg.getData().getString("filename");
							MonitoringActivity.LoadThePlotList(filename);
							MultitouchPlot.isPause=false;
						}
						else if(BluetoothConnector.activity.toString().contains("CalibrationActivity")){
							CalibrationActivity.current_filename = msg.getData().getString("filename");
							Log.d(LOG_HEADER,"current filename "+ CalibrationActivity.current_filename);
							MultitouchPlot.isPause=false;
						}
						break;
					case DataService.MSG_NEW_DATA:
						if(BluetoothConnector.activity.toString().contains("ChipParametersActivity")){
							ChipParametersActivity.signalPlot.reloadFile();
						}
						if(BluetoothConnector.activity.toString().contains("MonitoringActivity")){
							String filename = msg.getData().getString("filename");
							MonitoringActivity.ReloadThePlotList(filename);
						}
						break;
					case DataService.MSG_PARAMETERS:
						if(BluetoothConnector.activity.toString().contains("ChipParametersActivity")){
							parameters = msg.getData().getByte("Parameters");
							address =  msg.getData().getByte("Address");
							Log.d(LOG_HEADER,"parameters and address received");
							Toast.makeText(BluetoothConnector.context, "set the register: " + address +" with: "+parameters,Toast.LENGTH_LONG).show();
							ChipParametersActivity.SetRegisterLabel(address,parameters);
						}
						break;
					default:
						super.handleMessage(msg);
				}
			}
		}
	}

	public void updateActionBar(boolean b) {
		// TODO update of the Action Bar of the actual Activity (each one have a particular set of options so it is important to destinguish)
		if(activity.toString().contains("ChipParametersActivity"))
			ChipParametersActivity.updateActionBar(b);
		if(activity.toString().contains("CalibrationActivity"))
			CalibrationActivity.updateActionBar(b);
		if(activity.toString().contains("MonitoringActivity"))
			MonitoringActivity.updateActionBar(b);
	}

	public void send_address( Byte address ){
		Log.d(LOG_HEADER,"send register address "+ address);
		Intent i = new Intent(context, DataService.class);
		send_address_Register(i,address);
	}
	public  void send_value( Byte value ){
		Log.d(LOG_HEADER,"send value "+ value);
		Intent i = new Intent(context, DataService.class);
		set_value_ControlRegister(i,value);
	}
	public void request_new_file(Intent intent ){
		intent.putExtra(DataService.SVC_ACTION, DataService.SVC_NEW_DATA);
		//intent.putExtra(DataService.ELECTRODES_NAME, arrayOfCheckedElectrodes);
		context.startService(intent);
	}

	public void send_stop_command(Intent i){
		if(D) Log.i(LOG_HEADER, "Send '0x20'");
		i.putExtra(DataService.SVC_ACTION, DataService.SVC_BT_SEND_COMMAND);
		//i.putExtra(DataService.ELECTRODES_NAME, arrayOfCheckedElectrodes);
		Byte stop = 0x20;
		i.putExtra(DataService.SVC_BT_SEND_COMMAND, stop);
		i.putExtra(DataService.CALIBRATION_ON, false);
		context.startService(i);
	}
	public void send_start_command(Intent i, boolean calibration_on){
		if(D) Log.i(LOG_HEADER, "Send '0x21'");
		i.putExtra(DataService.SVC_ACTION, DataService.SVC_BT_SEND_COMMAND);
		//i.putExtra(DataService.ELECTRODES_NAME, arrayOfCheckedElectrodes);
		Byte start = 0x21;
		i.putExtra(DataService.SVC_BT_SEND_COMMAND, start);
		i.putExtra(DataService.CALIBRATION_ON, calibration_on);
		context.startService(i);
	}

	void set_electrode(Byte nr_electrode){
		if(D) Log.i(LOG_HEADER, "Set electrode "+ nr_electrode);
		Intent i = new Intent(context, DataService.class);
		i.putExtra(DataService.SVC_ACTION, DataService.SVC_BT_SEND_COMMAND);
		//i.putExtra(DataService.ELECTRODES_NAME, arrayOfCheckedElectrodes);
		i.putExtra(DataService.SVC_BT_SEND_COMMAND, nr_electrode);
		i.putExtra(DataService.CALIBRATION_ON, false);
		context.startService(i);
	}

	void send_address_Register(Intent i, Byte address){
		if(D) Log.i(LOG_HEADER, "Send "+ address);
		i.putExtra(DataService.SVC_ACTION, DataService.SVC_BT_SEND_MESSAGE);
		//i.putExtra(DataService.ELECTRODES_NAME, arrayOfCheckedElectrodes);
		i.putExtra(DataService.SVC_BT_SEND_ADDRESS, address);
		i.putExtra(DataService.CALIBRATION_ON, false);
		context.startService(i);
	}

	void set_value_ControlRegister(Intent i, Byte new_value){
		if(D) Log.i(LOG_HEADER, "Send "+ new_value);
		i.putExtra(DataService.SVC_ACTION, DataService.SVC_BT_SEND_MESSAGE);
		//i.putExtra(DataService.ELECTRODES_NAME, arrayOfCheckedElectrodes);
		i.putExtra(DataService.SVC_BT_SEND_ADDRESS, new_value);
		i.putExtra(DataService.CALIBRATION_ON, false);
		context.startService(i);
	}

	void ask_parameters(Intent i){
		if(D) Log.i(LOG_HEADER, "Send 'b'");
		i.putExtra(DataService.SVC_ACTION, DataService.SVC_BT_SEND_COMMAND);
		//i.putExtra(DataService.ELECTRODES_NAME, arrayOfCheckedElectrodes);
		i.putExtra(DataService.SVC_BT_SEND_COMMAND, 'b');
		i.putExtra(DataService.CALIBRATION_ON, false);
		context.startService(i);
	}

	public boolean getPlot( ) {
		return plot;
	}

	public void setPlot(boolean plot) {
		BluetoothConnector.plot = plot;
	}

}