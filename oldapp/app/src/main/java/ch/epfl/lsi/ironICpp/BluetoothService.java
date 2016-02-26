//Declaring the application package in which the class is included.
package ch.epfl.lsi.ironICpp;

//Importing some Java useful objects.
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.ArrayList;

import android.annotation.SuppressLint;
//Importing Android Bluetooth objects.
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

//Importing  Android usual classes.
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothService {
	/**
	 * This class does all the work for setting up and managing Bluetooth
	 * connections with other Bluetooth devices. The software is designed so
	 * that it asks the connection to another device. Therefore this one is seen
	 * as a Bluetooth Remote Server and this class as a Bluetooth Client. It has
	 * a thread for requesting connection with a device and a thread for data
	 * transmission once connected. The data are sent and received as single
	 * bytes. This means every message is sent (received) writing (reading) only
	 * one byte at a time. The message size is set in the ChatActivity.
	 */

	/**
	 * Class constants.
	 */
	// Debug specific
	private static final String LOG_HEADER = "BluetoothService"; // Class name
	private static final boolean D = true; // Debug flag

	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // My UUID. It is the standard SPP UUID

	public static final int STATE_NONE = 0; // Connection state value: Nothing
	public static final int STATE_CONNECTING = 1; // Initiating a connection
	public static final int STATE_CONNECTED = 2; // Connected to a remote device

	/**
	 * Class member variables.
	 */
	// Member variables.
	private static BluetoothSocket mmSocket; // The connection Socket
	private static  InputStream mmInStream; // The connection input stream
	private static OutputStream mmOutStream; // The connection output stream

	private final BluetoothAdapter mAdapter; // Local device Bluetooth adapter
	private final Handler mHandler; // Handler used to communicate with the UI
	// thread.
	private ConnectThread mConnectThread; // Connecting Thread.
	public ConnectedThread mConnectedThread; // Connection Thread.
	private int mState; // Connection state.
	private boolean stopFlag; // Stop flag for stopping correctly the threads
	private boolean isAlive=true;
	// Response buffer
	ArrayList<Byte> buffer = new ArrayList<Byte>();

	/**
	 * Class methods.
	 */
	public BluetoothService(Context context, Handler handler) {
		/**
		 * Class Constructor.
		 *
		 * @param context
		 *            The main thread Context.
		 * @param handler
		 *            A Handler to send messages back to the main thread.
		 */

		if(D) Log.d(LOG_HEADER, "Constructor");

		// Get the local device Bluetooth adapter
		mAdapter = BluetoothAdapter.getDefaultAdapter();

		// The connection state is initialized to NONE
		mState = STATE_NONE;

		// Set the Handler received as argument from main thread
		mHandler = handler;

		DataService.setBTHandler(BTHandler);

		resetConnection();

	}

	/**
	 * Reset input and output streams and make sure socket is closed. 
	 * This method will be used during shutdown() to ensure that the connection is properly closed during a shutdown.  
	 * @return
	 */

	public static void resetConnection() {
		if (mmInStream != null) {
			try {mmInStream.close();} catch (Exception e) {}
			mmInStream = null;
		}

		if (mmOutStream != null) {
			try {mmOutStream.close();} catch (Exception e) {}
			mmOutStream = null;
		}

		if (mmSocket != null) {
			try {mmSocket.close();} catch (Exception e) {}
			mmSocket = null;
		}
	}

	public synchronized void setState(int state) {
		/**
		 * Set the current state of the connection
		 *
		 * @param state
		 *            An integer defining the current connection state
		 */

		if(D) Log.d(LOG_HEADER+".setState", "setState() " + mState + " -> "
				+ state);

		// Updating the connection state
		mState = state;

		// Give the new state to the main thread
		mHandler.obtainMessage(DataService.MESSAGE_BT_STATE_HAS_CHANGED, state,
				-1).sendToTarget();
	}

	public synchronized int getState() {
		/**
		 * Return the current connection state.
		 */

		return mState;
	}

	public synchronized void connect(BluetoothDevice device) {
		/**
		 * Start the ConnectThread in order to connect to a remote device.
		 *
		 * @param device
		 *            The BluetoothDevice to connect.
		 */

		if(D) Log.d(LOG_HEADER+".Connect method.", "Connecting to: "
				+ device);

		resetConnection();
//		// Cancel any thread attempting to make a connection
//		if (mConnectThread != null) {
//			mConnectThread.cancel();
//			mConnectThread = null;
//		}

//		// Cancel any thread currently running a connection
//		if (mConnectedThread != null) {
//			mConnectedThread.cancel();
//			mConnectedThread = null;
//		}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();

		// Update the connection state.
		setState(STATE_CONNECTING);
	}

	public synchronized void connected(BluetoothSocket socket,
									   BluetoothDevice device) {
		/**
		 * Start the ConnectedThread to manage the Bluetooth connection.
		 *
		 * @param socket
		 *            The BluetoothSocket on which the connection was made.
		 * @param device
		 *            The BluetoothDevice that has been connected.
		 */

		if(D) Log.d(LOG_HEADER+".Connected method.", "Connected to: "
				+ device);

//		// Cancel the thread that completed the connection
//		if (mConnectThread != null) {
//			mConnectThread.cancel();
//			mConnectThread = null;
//			mConnectedThread = null;
//		}

//		// Cancel any thread currently running a connection
//		if (mConnectedThread != null) {
//			mConnectedThread.cancel();
//			mConnectedThread = null;
//		}

		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket);
		setisAlive(true);
		mConnectedThread.start();

		// Send the name of the connected device back to the main thread
		mHandler.obtainMessage(DataService.MESSAGE_DEVICE_NAME,
				device.getName()).sendToTarget();

		// Update the connection state.
		setState(STATE_CONNECTED);
	}

	public synchronized void stop() {
		/**
		 * Stop all threads and set the connection status to NONE.
		 */

		// stranezze:
		// 1 si interrompe il socket prima di interrompere il thread da cui legge
		// 2 viene ripetuta 2 volte la cancellazione/stop del thread
		// 3 il mmSocket  

		if(D) Log.d(LOG_HEADER+".Stop method",
				"Stopping all BluetoothService threads.");

		//cose secondo me da fare:
		// mettere variabile isalive nel thread
		// mettere variabile QUI isAlive a false
		//poi fai la resetConnection();

		setisAlive(false);
		resetConnection();

		// Updating stopFlag
		stopFlag = true;

		// Cancel all threads.
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
			mConnectedThread = null;
		}

//		Log.d(LOG_HEADER,"mconnectedThread before "+mConnectedThread);
//		if (mConnectedThread != null) {
//			Log.d(LOG_HEADER,"mconnectedThread after "+mConnectedThread);
//			mConnectedThread.cancel();
//			mConnectedThread = null;
//		}

		// Updating connection state without advising the UI thread
		setState(STATE_NONE);
	}

	public void write(ArrayList<Byte> out) {
		/**
		 * Write to the ConnectedThread in an unsynchronized manner
		 *
		 * @param out
		 *            The byte to write
		 * @see ConnectedThread#write(byte)
		 */

		if(D) Log.d(LOG_HEADER, "Write unsynchronized method.");

		// Create temporary object
		ConnectedThread r;

		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			r = mConnectedThread;
		}

		// Perform the write unsynchronized
		r.write(out);
	}

	private void connectionFailed() {
		/**
		 * Indicate that the connection attempt failed and notify the main
		 * thread.
		 */

		if(D) Log.d(LOG_HEADER, "connectionFailed method");

		// Cancel all threads
		setisAlive(false);
		resetConnection();
		this.stop();

		// Send a failure message back to the main thread
//		mHandler.obtainMessage(DataService.MESSAGE_TOAST,
//				"Unable to connect device").sendToTarget();
	}

	private void connectionLost() {
		/**
		 * Indicate that the connection was lost and notify the main thread.
		 */

		if(D) Log.d(LOG_HEADER, "connectionLost method");

		// Cancel all threads
		setisAlive(false);
		resetConnection();
		this.stop();

		// Send a failure message back to the main thread
//		mHandler.obtainMessage(DataService.MESSAGE_TOAST, "Connection over")
//		.sendToTarget();
	}

	/**
	 * Nested Classes.
	 */
	private class ConnectThread extends Thread {
		/**
		 * Connecting Thread. This thread runs while attempting to make an
		 * outgoing connection with a device. It runs straight through; the
		 * connection either succeeds or fails.
		 */

		// Member variables
		private final BluetoothSocket mmSocket; // The connection Socket
		private final BluetoothDevice mmDevice; // The device to connect to

		// Class Methods
		public ConnectThread(BluetoothDevice device) {
			// Class constructor.

			if(D) Log.d(LOG_HEADER+".ConnectThread",
					"CREATE Connecting Thread");

			// Set priority. This thread is a I/O intensive thread because It
			// reads data stream from a socket.
			// Therefore must have higher priority than UI thread. UI thread has
			// standard priority by default.
			this.setPriority(MAX_PRIORITY);

			// Assign the device received as argument.
			mmDevice = device;

			// Create an RFCOMM BluetoothSocket ready to start a secure outgoing
			// connection to this remote device using SDP lookup of UUID.
			BluetoothSocket tmp = null;
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				if(D) Log.e(LOG_HEADER+".ConnectThread",
						"Rfcomm socket creation failed", e);
			}
			mmSocket = tmp;

			// Set the stop flag value
			stopFlag = false;
		}

		public void run() {
			// Run the thread

			if(D) Log.d(LOG_HEADER+".ConnectThread", "BEGIN mConnectThread");

			// Cancel discovery (It will slow down a connection)
			mAdapter.cancelDiscovery();

			// Connect to the remote device
			try {
				if(D) Log.d(LOG_HEADER+".ConnectThread",
						"mmSocket.connect()");
				mmSocket.connect();
			} catch (IOException e) {
				// Clear all threads and notify the failure to the main thread
				if (!stopFlag)
					connectionFailed();
				// End the thread
				return;
			}

			// Reset the connecting thread
			synchronized (BluetoothService.this) {
				mConnectThread = null;
			}

			// Start the connection thread
			connected(mmSocket, mmDevice);
		}

		public void cancel() {
			// Cancel the thread

			if(D) Log.d(LOG_HEADER+".ConnectThread",
					"CANCEL mConnectThread");

			// Close the socket
			try {
				mmSocket.close();
			} catch (IOException e) {
				if(D) Log.d(LOG_HEADER+".ConnectThread",
						"Unable to close socket", e);
			}
		}
	}

	private class ConnectedThread extends Thread {
		/**
		 * This thread runs during a connection with a remote device. It handles
		 * all incoming and outgoing transmissions.
		 */


		// Class Methods.

		public ConnectedThread(BluetoothSocket socket) {
			// Class Constructor

			if(D) Log.d(LOG_HEADER+".ConnectedThread",
					"CREATE Connected Thread");

			// Set priority. This thread is a I/O intensive thread because It
			// reads data stream from a socket.
			// Therefore must have higher priority than UI thread. UI thread has
			// standard priority by default.
			this.setPriority(MAX_PRIORITY);

			// Assign the socket received as argument.
			mmSocket = socket;

			// Get the BluetoothSocket input and output streams
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				if(D) Log.e(LOG_HEADER+".ConnectedThread",
						"Unable to get the input and output streams", e);
			}
			mmInStream = tmpIn;
			mmOutStream = tmpOut;

			// Set the stop flag value
			stopFlag = false;
		}

		public void run() {
			// Run the thread. It is an infinite reading loop

			if(D) Log.d(LOG_HEADER+".ConnectedThread",
					"BEGIN Connected Thread");

			// Infinite connection management loop. Reading the message from the
			// connected input stream.
			//the boolean variable isAlive interrupt the loop before Socket is closed during disconnection
			while (isAlive) {

				try {
					// Prevent too heavy CPU usage
					sleep(100);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				try {
					if (mmInStream.available() > 0) {
						// Message reading cycle. Reading one byte at a time
						// until the input stream is finished.
						if(D) Log.d(LOG_HEADER+".ConnectedThread",
								"Ready for a new message reading cycle");

						while (mmInStream.available() > 0) {
							buffer.add((byte) mmInStream.read());
							if(D) Log.v(LOG_HEADER,"BYTE: 0x"
									+ Integer.toHexString(0xFF & buffer.get(buffer.size() - 1))
									+ " = "
									+ Integer.toString(0xFF & buffer.get(buffer.size() - 1))
									+ " READ");

//							if (Integer.toString(0xFF & buffer.get(buffer.size() - 1)).equals("40"))
//								mHandler.obtainMessage(
//										DataService.RESPONSE_TO_B, -1, -1, buffer).sendToTarget();
						}


						if(D) Log.d(LOG_HEADER+".ConnectedThread,Message reading cycle ended",
								Long.toString(System.currentTimeMillis()));

						// Send the obtained message to the main thread
						mHandler.obtainMessage(
								DataService.MESSAGE_READ_A_RESPONSE, -1, -1, buffer).sendToTarget();

					}
				} catch (IOException e) {
					// Clear all threads and notify the problem to the main
					// thread.
					if (!stopFlag)
						connectionLost();
					// End the thread.
					break;
				} catch (Exception e) {
					Log.d(LOG_HEADER,"Problem with the BT Service: "+e);
					break;
				}

			}

		}

		public void write(ArrayList<Byte> message) {
			// Write the message to the connected output stream.

			try {
				// Message writing cycle. Writing one byte at a time until the
				// message is finished.
				for (int i = 0; i < message.size(); i++) {
					mmOutStream.write(message.get(i));
					if(D) Log.v("+++++++++BYTE " + Integer.toString(i) + ": 0x"
									+ Integer.toHexString(message.get(i))
									+ " WRITE+++++++++",
							Long.toString(System.currentTimeMillis()));
				}
				// Advise the UI Activity that the message is been sent.
				mHandler.obtainMessage(DataService.MESSAGE_WRITE_A_COMMAND)
						.sendToTarget();
			} catch (IOException e) {
				if(D) Log.e("BluetoothService. ConnectedThread",
						"Exception during write", e);
			}
		}

//		public void cancel() {
//			// Cancel the thread.
//
//			if(D) Log.d("BluetoothService. ConnectedThread",
//						"CANCEL Connected Thread");
//
//			// Close the socket.
//			try {
//				mmSocket.close();
//			} catch (IOException e) {
//				if(D) Log.e("BluetoothService. ConnectThread",
//						"Unable to close socket", e);
//			}
//		}
	}

	public boolean getisAlive(){
		return isAlive;
	}
	public void setisAlive(boolean value){
		isAlive = value;
	}

	private final Handler BTHandler = new Handler() {
		/**
		 * Handler. Special member used to communicate with the DataService.
		 */

		@SuppressLint("HandlerLeak") @Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

				case DataService.END_READING:
					// Clear buffer now it has been read
					buffer.clear();
					//buffer = new ArrayList<Byte>();
					//Log.d(LOG_HEADER, "end reading buffer");
					break;
			}
		}
	};
}
