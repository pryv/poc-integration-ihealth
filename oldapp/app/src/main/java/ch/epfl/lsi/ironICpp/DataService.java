/**
 * This service handles all the background processing with data:
 * fetching, processing, displaying and saving it
 */
package ch.epfl.lsi.ironICpp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

/**
 * @author Francesca Stradolini (francesca.stradolini@epfl.ch)
 */

public class DataService extends Service {
	// Service messages
	ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients
	int mValue = 0; // Holds last value set by a client.
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_NEW_BT_STATE = 4;
	public static final int MSG_NEW_FILE = 5;
	public static final int MSG_NEW_DATA = 6;
	public static final int MSG_AVERAGE = 7;
	public static final int MSG_PARAMETERS = 8;

	// Action messages
	public static final String SVC_ACTION = "SVC_ACTION"; // Request an action from this service
	public static final String SVC_CONNECT_DEVICE = "CONNECT_DEVICE"; // Asks to launch DeviceList Activity.
	public static final String SVC_BT_GET_STATE = "SVC_BT_GET_STATE";
	public static final String SVC_BT_SEND_COMMAND = "SVC_BT_SEND_COMMAND";
	public static final String SVC_BT_CONNECT_TO_DEVICE = "SVC_BT_CONNECT_TO_DEVICE";
	public static final String SVC_BT_DISCONNECT = "SVC_BT_DISCONNECT";
	public static final String SVC_BT_DEVICE_MAC = "SVC_BT_DEVICE_MAC";
	public static final String SVC_FILE_LOAD = "SVC_FILE_LOAD";
	public static final String SVC_START_ACQUISITION = "SVC_START_ACQUISITION";
	public static final String SVC_ACQUISITION_FINISHED = "ACQUISITION_FINISHED";
	public static final String SVC_ACQUISITION_RUNNING = "ACQUISITION_RUNNING";
	public static final String SVC_QUIT_IF_POSSIBLE = "SVC_QUIT_IF_POSSIBLE";
	public static final String SVC_NEW_DATA = "SVC_NEW_DATA";
	public static final String SVC_AVERAGE = "SVC_AVERAGE";
	public static final String ELECTRODES_NAME = "SVC_ELECTRODES_NAME";
	public static final String SVC_BT_SEND_MESSAGE = "SVC_BT_SEND_MESSAGE";
	public static final String SVC_BT_SEND_ADDRESS = "SVC_BT_SEND_ADDRESS";


	// Communication flags with BluetoothService
	public static final int MESSAGE_BT_STATE_HAS_CHANGED = 1; // Bluetooth service status changed
	public static final int MESSAGE_READ_A_RESPONSE = 2; // New message read
	public static final int MESSAGE_WRITE_A_COMMAND = 3; // New message has been sent
	public static final int MESSAGE_DEVICE_NAME = 4; // Show connected device's name
	public static final int END_READING = 5;


	// Current state
	private static int bluetoothState = BluetoothService.STATE_NONE;
	private final Messenger mMessenger = new Messenger(new IncomingHandler(this)); // Target we publish for clients to send messages to IncomingHandler

	// IO Data
	private ArrayList<Byte> transmitBuffer = new ArrayList<Byte>();
	private static Vector<Integer> data_x = new Vector<Integer>();
	private static Vector<Integer> data_y = new Vector<Integer>();

	// Bluetooth related
	private static BluetoothAdapter mBluetoothAdapter = null; // Local device Bluetooth adapter
	private static BluetoothService mBluetoothService = null; // Object in charge of managing the Bluetooth connection
	private static BluetoothFakeService mFakeBluetoothService = null; // Fake bluetooth service
	private static Handler BTHandler; // Handler used to communicate with the BT

	// Measurements
	private static int currentSampleID=0;
	private static double currentSample = 0.0;
	private ArrayList<FileOutputStream> fileStream = new ArrayList<FileOutputStream>();
	private ArrayList<String> MonitoredElectrodes = new ArrayList<String>();
	private long time_start = -1;
	private int prev_time1 = -1;
	private int prev_time2 = -1;
	private int prev_time = -1;


	// Debug specific
	private static final String LOG_HEADER = "DataService"; // Class name
	private static final boolean D = false; // Debug flag

	//Calibration value
	static final String CALIBRATION_ON = "Calibration_on";
	static float m = 1, q = 0;  //inizialized with neutral value y = mx + q
	private Boolean calibrationOn = false;
	String fileName ;

	@Override
	public void onCreate()
	{
		super.onCreate();
		if(D) Log.d(LOG_HEADER, "onCreate");
		// Show notification in the notifications drawer
		Intent i = new Intent(this, StartActivity.class);
		PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_ONE_SHOT);
		Notification notification = new NotificationCompat.Builder(
				getApplicationContext())
				.setContentTitle(getString(R.string.notification_title))
				.setContentText(getString(R.string.notification_touch_to_open))
				.setSmallIcon(R.drawable.ic_drawer)
						// .setLargeIcon(get R.drawable.ic_launcher)
				.setUsesChronometer(true).setContentIntent(pi).build();
		startForeground(1337, notification);
	}

	// Handler of incoming messages from clients.
	static private class IncomingHandler extends Handler {

		private final WeakReference<DataService> mService;

		public IncomingHandler(DataService service)
		{
			mService = new WeakReference<DataService>(service);
		}

		@Override
		public void handleMessage(Message msg) {
			DataService service = mService.get();
			if(service != null)
			{
				switch (msg.what) {
					case MSG_REGISTER_CLIENT:
						service.mClients.add(msg.replyTo);
						service.sendMessageToUI_bluetoothState();
						break;
					case MSG_UNREGISTER_CLIENT:
						service.mClients.remove(msg.replyTo);

						break;
					default:
						super.handleMessage(msg);
				}
			}
		}
	}

	private void sendMessageToUI_setRegister(Byte address, Byte value) {
		// TODO Auto-generated method stub
		if(D) Log.d(LOG_HEADER,"address "+ address +" value "+value);
		Bundle b = new Bundle();
		b.putByte("Address", address);
		b.putByte("Parameters", value);
		Message msg = Message.obtain(null, MSG_PARAMETERS);
		msg.setData(b);

		for (int i=mClients.size()-1; i>=0; i--) {
			try {
				mClients.get(i).send(msg);
			} catch (RemoteException e) {
				// The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
				mClients.remove(i);
			}
		}
	}

	private void sendMessageToUI_bluetoothState() {
		Bundle b = new Bundle();
		b.putInt("btState", bluetoothState);
		Message msg = Message.obtain(null, MSG_NEW_BT_STATE);
		msg.setData(b);

		Log.e(LOG_HEADER, "Sending state "+ bluetoothState +" to clients");

		for (int i=mClients.size()-1; i>=0; i--) {
			try {
				mClients.get(i).send(msg);
				Log.e(LOG_HEADER, "Sent to client");
			} catch (RemoteException e) {
				// The client is dead. Remove it from the list; we are going through the
				// list from back to front so this is safe to do inside the loop.
				mClients.remove(i);
			}
		}
	}

	private void sendMessageToUI_newFile(String filename,String nameOfMetabolite) {
		Bundle b = new Bundle();
		b.putString("filename", filename);
		b.putString("metabolite", nameOfMetabolite);
		Message msg = Message.obtain(null, MSG_NEW_FILE);
		msg.setData(b);

		for (int i=mClients.size()-1; i>=0; i--) {
			try {
				mClients.get(i).send(msg);
			} catch (RemoteException e) {
				// The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
				mClients.remove(i);
			}
		}
	}

	private void sendMessageToUI_newData(int WEpos) {
		Bundle b = new Bundle();
		String filename = MonitoredElectrodes.get(WEpos);
		b.putString("filename", filename);
		Message msg = Message.obtain(null, MSG_NEW_DATA);
		msg.setData(b);

		for (int i=mClients.size()-1; i>=0; i--) {
			try {
				mClients.get(i).send(msg);
			} catch (RemoteException e) {
				// The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
				mClients.remove(i);
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the
		// job
		/*
		 * Message msg = mServiceHandler.obtainMessage(); msg.arg1 = startId;
		 * mServiceHandler.sendMessage(msg);
		 */

		// Process action
		Bundle bundle = intent.getExtras();
		if(bundle != null) {
			calibrationOn = bundle.getBoolean(CALIBRATION_ON, false);
			if(D) Log.i(LOG_HEADER, "onStartCommand, bundle is "+bundle.getStringArrayList(ELECTRODES_NAME));
			if(D) Log.i(LOG_HEADER, "with action "+bundle.getString(DataService.SVC_ACTION));
			if(bundle.getString(DataService.SVC_ACTION).equals(DataService.SVC_BT_SEND_COMMAND))
			{
				if(D) Log.d(LOG_HEADER,"action "+ bundle.getString(DataService.SVC_ACTION)+" with: "+bundle.getByte(DataService.SVC_BT_SEND_COMMAND));
				this.sendTestMessage(bundle.getByte(DataService.SVC_BT_SEND_COMMAND));
			}
			else if(bundle.getString(DataService.SVC_ACTION).equals(DataService.SVC_BT_SEND_MESSAGE))
			{
				if(D) Log.d(LOG_HEADER,"action "+ bundle.getString(DataService.SVC_ACTION)+" with "+bundle.getByte(DataService.SVC_BT_SEND_ADDRESS));
				this.sendTestMessage(bundle.getByte(DataService.SVC_BT_SEND_ADDRESS));
			}
			else if(bundle.getString(DataService.SVC_ACTION).equals(DataService.SVC_BT_CONNECT_TO_DEVICE))
			{
				// Get Bluetooth adapter, get remote device, create Bluetooth service, connect to device
				//in order to open as many files as the monitored metabolites.
				MonitoredElectrodes.clear();
				if(D) Log.d(LOG_HEADER,"Monitoring electrodes 0 "+MonitoredElectrodes.size());
				MonitoredElectrodes.addAll(bundle.getStringArrayList(ELECTRODES_NAME));
				if(D) Log.d(LOG_HEADER,"Monitoring electrodes 1 "+MonitoredElectrodes.size());
				String address = bundle.getString(DataService.SVC_BT_DEVICE_MAC);
				if(D) Log.d(LOG_HEADER, address);
				// Check if fake-debug device
				if(address.equals("00:00:00:00:00:00"))
				{
					if(D) Log.d(LOG_HEADER, "Connecting to fake device");
					mFakeBluetoothService = new BluetoothFakeService(this, mHandler);
				}
				else
				{
					mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
					BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
					mBluetoothService = new BluetoothService(this, mHandler);
					mBluetoothService.connect(device);
				}
			}
			else if(bundle.getString(DataService.SVC_ACTION).equals(DataService.SVC_BT_DISCONNECT))
			{
				time_start = -1;
				prev_time1 = -1;
				prev_time2 = -1;
				if(D) Log.d(LOG_HEADER,"Disconnecting");
				// Kill the son-threads
				if(mBluetoothService != null)
				{
					if(D) Log.d(LOG_HEADER, "Disconnecting bluetooth "+ mBluetoothService);
					if(D) Log.d(LOG_HEADER, "Kill BT threads");
					mBluetoothService.stop();
					mBluetoothService = null;
				}
				if(mFakeBluetoothService != null)
				{
					if(D) Log.d(LOG_HEADER, "Disconnecting bluetooth "+ mFakeBluetoothService);
					if(D) Log.d(LOG_HEADER, "Kill fake BT threads");
					mFakeBluetoothService.stop();
					mFakeBluetoothService = null;
				}
//				// Disable Bluetooth
//				if(mBluetoothService != null && mBluetoothAdapter.isEnabled())
//				{
//					mBluetoothAdapter.disable();
//				}
			}
			else if(bundle.getString(DataService.SVC_ACTION).equals(DataService.SVC_QUIT_IF_POSSIBLE))
			{
				if(D) Log.v(LOG_HEADER, "Stopping service? bluetoothConnected = " + bluetoothState);

				// Kill service if no acquisition is running
				if(mFakeBluetoothService == null && mBluetoothService == null)
				{
					if(D) Log.v(LOG_HEADER, "Stopping service");
					this.stopSelf();
				}
				else
				{
					if(D) Log.v(LOG_HEADER, "Bluetooth connection active, keep service!");
				}
			}
			else if(bundle.getString(DataService.SVC_ACTION).equals(DataService.SVC_NEW_DATA))
			{
				if(D) Log.d(LOG_HEADER,"new data received");
				// Create a new file to save data
				if(fileStream == null)
				{
					Toast.makeText(getApplicationContext(), R.string.no_file_open, Toast.LENGTH_SHORT).show();
				}
				else
				{
					Toast.makeText(getApplicationContext(), R.string.new_file_open, Toast.LENGTH_SHORT).show();
					try {
						if(fileStream != null)
						{
							for(int i = 0; i<fileStream.size(); i++){
								fileStream.get(i).close();
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					fileStream = null;

					// Discard eventually received data
					data_x.clear();
					data_y.clear();
					currentSampleID = 0;
					time_start = -1;
				}
			}
			else if(bundle.getString(DataService.SVC_ACTION).equals(DataService.SVC_BT_GET_STATE))
			{
				sendMessageToUI_bluetoothState();
			}
			else
			{
				if(D) Log.w(LOG_HEADER, "Bundle action not recognized");
				if(D) Log.w(LOG_HEADER, bundle.getString(DataService.SVC_ACTION));
			}
		}

		// If we get killed, after returning from here, restart
		//return START_STICKY;
		// Might avoid nullPointerException on new calls as in
		// http://stackoverflow.com/questions/23148677/service-onstartcommand-throwing-nullpointerexception
		return START_REDELIVER_INTENT;
	}

	public static void DisconnectBT(){
		// Kill the son-threads
		if(mBluetoothService != null)
		{
			if(D) Log.d(LOG_HEADER, "Disconnecting bluetooth "+ mBluetoothService);
			if(D) Log.d(LOG_HEADER, "Kill BT threads");
			mBluetoothService.stop();
			mBluetoothService = null;
		}
		if(mFakeBluetoothService != null)
		{
			if(D) Log.d(LOG_HEADER, "Disconnecting bluetooth "+ mFakeBluetoothService);
			if(D) Log.d(LOG_HEADER, "Kill fake BT threads");
			mFakeBluetoothService.stop();
			mFakeBluetoothService = null;
		}

	}

	/** Open handle to file on SD card 
	 * @param nameOfMetabolite */
	public boolean saveFileStart(String nameOfMetabolite) {
		/**
		 * This method stores the signal acquire into external storage.
		 * Every Android-compatible device supports a shared "external 
		 * storage" that can be used to save files. This can be a 
		 * removable storage media (such as an SD card) or an internal 
		 * (non-removable) storage. Files saved to the external storage
		 * are world-readable and can be modified by the user when 
		 * they enable USB mass storage to transfer files on a computer.
		 *
		 */

		// Checking media availability.
		String state = Environment.getExternalStorageState();
		if(!Environment.MEDIA_MOUNTED.equals(state))
		{
			Toast.makeText(getApplicationContext(), R.string.storage_unavailable, Toast.LENGTH_SHORT).show();
			return false;
		}


		// Create an application folder if not present.
		File AppFolder = new File(Environment.getExternalStorageDirectory() + "/IronicCells");
		if(!AppFolder.exists()) AppFolder.mkdir();

		// Create File.
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		fileName = nameOfMetabolite + "_" + sdf.format(new Date()) +".csv";
		File file = new File(AppFolder, fileName); // Create a file inside the root of application's file directory.

		// Open the file stream
		try {
			if(D) Log.d(LOG_HEADER,"fileOutputStream"+ fileStream);
			FileOutputStream fileToAdd = new FileOutputStream(file);
			if (!fileStream.contains(fileToAdd))
				fileStream.add(fileToAdd); // Output stream ready to write the file.
		} catch (IOException e) {
			if(D) Log.e("ExternalStorage", "Error writing " + file, e);
		}


		// Warn UI- thread
		if(D) Log.d(LOG_HEADER,"Metaboloite used "+ nameOfMetabolite);
		sendMessageToUI_newFile(fileName, nameOfMetabolite);

		return true;
	}

	/** Dumps the current data_x and data_y to SD card */
	public void saveDataToFile(int pos) {

		if(fileStream == null)
		{
			fileStream = new ArrayList<FileOutputStream>();
			for(int i=0;i<MonitoredElectrodes.size();i++){
				saveFileStart(MonitoredElectrodes.get(i));
			}
		}

		// Write the file.
		String newRow = new String(); // Row to be written into the file
		int len = data_x.size();
		for (int i=0;i<len ;i++){
			// Writing file one row at a time with zero-padding to have constant length thus easy seek in file
			newRow = String.format(Locale.ROOT, "%08d\t%05d\n", data_x.get(i), data_y.get(i));
			try {
				// Change size if newRow length is changed
				fileStream.get(pos).write(newRow.getBytes());
			} catch (IOException e) {
				Toast.makeText(getApplicationContext(), "Impossible to write recording", Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
		}
		data_x.clear();
		data_y.clear();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	@Override
	public void onDestroy() {
		// Kill the son-threads
		if(mBluetoothService != null)
		{
			mBluetoothService.stop();
			mBluetoothService = null;
		}
		if(mFakeBluetoothService != null)
		{
			mFakeBluetoothService.stop();
			mFakeBluetoothService = null;
		}

		// Disable Bluetooth
		if(mBluetoothService != null && mBluetoothAdapter.isEnabled())
		{
			mBluetoothAdapter.disable();
		}
	}

	private final Handler mHandler = new Handler() {
		/**
		 * Handler. Special member used to communicate with the connection thread.
		 */

		@SuppressLint("HandlerLeak") @SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

				case MESSAGE_BT_STATE_HAS_CHANGED:
					if(D) Log.i(LOG_HEADER, "MESSAGE_BT_STATE_HAS_CHANGED: " + msg.arg1);
					bluetoothState = msg.arg1;
					switch (msg.arg1) {
						case BluetoothService.STATE_CONNECTED:
							// Configure new chart
							data_x.clear();
							data_y.clear();
							currentSampleID = 0;
							break;
						case BluetoothService.STATE_CONNECTING:
						case BluetoothService.STATE_NONE:
						default:
							try {
								if(fileStream != null)
								{
									for(int i = 0 ; i<fileStream.size();i++){
										fileStream.get(i).close();
									}
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
							fileStream = null;
							break;
					}
					sendMessageToUI_bluetoothState();
					break;

				case MESSAGE_READ_A_RESPONSE:
					// TODO: send to Protocol class to handle proper decoding of the data
					int WEpos=0;
					ArrayList<Byte> data = (ArrayList<Byte>) msg.obj;

					if(data.size() == 0) return;

					if((0xFF & data.get(0)) <= 0x12 && data.size() == 2) {
						// Received DATA flag and the settings byte with the value
						//data.get(0) == register address
						//data.get(1) == value to set
						// do things with it: set labels in ChipParametersActivity
						sendMessageToUI_setRegister(data.get(0), data.get(1));
					}
					else
					{
						//2 Bytes but the first one is enough in order to understood if it is a data (range)
						for(int i=0; i<data.size()-1; i+=2)
						{
							//the first byte has 5 bits in order to understand its information is a data and from which electrode
							//so the correspondent file will be update:
							//0x13-Glucose 0x14-Lactate 0x15-Bilitubin 0x16-Sodium 0x17-Potassium 0x18-Temperature 0x19-PH
							//if(D) Log.d(LOG_HEADER,"data "+(data.get(i)&0xFF));
							if((((data.get(i) & 0xF8) >>3) == 0x13)||(((data.get(i) & 0xF8) >>3) == 0x14))
							{
								if(D) Log.d(LOG_HEADER, "Received valid data");
								//validation of the second byte: MSB is disparity bit
								if((data.get(i+1) & 0x80) == 0x00){

									if(((data.get(i) & 0xF8) >>3) == 0x13){
										WEpos=0;
										if(D) Log.d(LOG_HEADER,"first electrode");
									}
									else if(((data.get(i) & 0xF8) >>3) == 0x14){
										WEpos=1;
										if(D) Log.d(LOG_HEADER,"second electrode");
									}

									if(time_start==-1){
										time_start = System.currentTimeMillis();
									}

									// Measurement data received, process it
									currentSampleID++;
									currentSample = (int)
											(
													(
															//Compose the 2 Bytes data and cast it as int in order to plot it
															(
																	//0xFF & data Evil cast of the byte to an unsigned
																	(((0xFF & data.get(i)) << 8) // unsigned shifted MSBs
																			+ (0xFF & data.get(i+1) << 1) >>1) // unsigned LSB
															) & 0x03FF		// 0x03FF : Mask to discard the first 6 bites
													)
											);


									int time = (int) (System.currentTimeMillis() - time_start);

									switch (WEpos) {
										case 0:	 prev_time = prev_time1; break;
										case 1: prev_time = prev_time2; break;
									}

									if (prev_time != time){
										if((time - prev_time)>1){
											//if(D) Log.d(LOG_HEADER,"newX time"+prev_time);
											int diff = time-prev_time;
											for(int j = 1; j<diff; j++){
												data_x.add(prev_time+j);
												if(D) Log.d(LOG_HEADER,"newX time"+(prev_time+j));
												data_y.add((int) currentSample);
											}
										}
										data_x.add(time);
										//data_x.add((int) currentSampleID);
										//if(D) Log.d(LOG_HEADER,"current sample "+currentSample);
										data_y.add((int) currentSample);

										switch (WEpos) {
											case 0:	 prev_time1 = time; break;
											case 1: prev_time2 = time; break;
										}

										if(D) Log.d(LOG_HEADER,"newX "+ time +" incr "+currentSampleID+" value "+currentSample);
										if(calibrationOn){
											Log.d(LOG_HEADER,"calibration on");
											WEpos=0;
											saveDataToFile(WEpos);
											if(D) Log.d(LOG_HEADER,"calibrationON");

										}
										else{
											saveDataToFile(WEpos);
											sendMessageToUI_newData(WEpos);
										}
									}
								}
								else
								{
									// If data wrong, we might lost one byte, so
									// we increment by -1+2 = 1 instead of +2
									i--;
									if(D) Log.d(LOG_HEADER, "Received one byte less");
								}
							}
							else
							{
								// If data wrong, we might have byte swapping, so
								// we increment by -1+2 = 1 instead of +2
								i--;
								if(D) Log.d(LOG_HEADER, "Received invalid data");
							}

						}
						data.clear();
						data = null;
						System.gc();

						BTHandler.obtainMessage(END_READING, 1,
								-1).sendToTarget();
					}
					break;

				case MESSAGE_WRITE_A_COMMAND:
					if(D) Log.d(LOG_HEADER,"Message written");
					break;

				case MESSAGE_DEVICE_NAME:
					break;
			}
		}

	};

	private void sendTransmitBuffer(){
		/**
		 * Sends the Transmit Buffer Content.
		 */

		if(D) Log.d(LOG_HEADER, "send("+transmitBuffer.toString()+")");

		if(mBluetoothService == null)
		{
			Toast.makeText(getApplicationContext(), getString(R.string.bt_not_available_connect_to_fake_device), Toast.LENGTH_SHORT).show();
		}
		else
		{
			if(mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
				// Connection down
				Toast.makeText(getApplicationContext(), R.string.bt_not_connected, Toast.LENGTH_SHORT).show();
				return;
			}
			else
			{
				// Send the message and clear buffer
				mBluetoothService.write(transmitBuffer);
				transmitBuffer.clear();
			}
		}
	}

	public void sendTestMessage(Byte c){
		if(D) Log.i(LOG_HEADER, "sendMesage("+c+")");
		switch(c)
		{
			case 0x20: if(D) Log.i(LOG_HEADER, "stop data"); break;
			case 0x21: if(D) Log.i(LOG_HEADER, "start data"); break;
		}
		setTransmitBuffer(c);
		sendTransmitBuffer();
	}

	// This method sets the transmit buffer, converting into array of bytes
	private void setTransmitBuffer(int msg){
		ArrayList<Byte> decompose = new ArrayList<Byte>();
		this.transmitBuffer.clear();		//this

		// Decompose message to individual bytes
		do
		{
			decompose.add((byte) (msg & 0xFF));
			msg = msg >> 8;
		}while(msg != 0);

		// Fill-in the transmit buffer with the MSB first
		for(int i=0; i<decompose.size(); i++)
		{
			if(D) Log.d(LOG_HEADER, "transmit buffer "+ decompose.get(i));
			this.transmitBuffer.add(decompose.get(decompose.size()-i-1));
		}
		decompose.clear();
	}

	public static void setBTHandler(Handler bTHandler2) {
		// TODO Auto-generated method stub
		BTHandler = bTHandler2;
	}

}
