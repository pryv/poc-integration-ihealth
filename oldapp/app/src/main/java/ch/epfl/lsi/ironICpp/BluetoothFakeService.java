//Declaring the application package in which the class is included.
package ch.epfl.lsi.ironICpp;

//Importing some Java useful objects.
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothFakeService {
	/**
	 * This class simulates an active Bluetooth service
	 */

	/**
	 * Class constants.
	 */
	// Debug specific
	private static final String LOG_HEADER = "FakeBluetoothService"; // Class name
	private static final boolean D = false; // Debug flag
	

	private final Handler mHandler; // Handler used to communicate with the UI
	public ConnectedThread mConnectedThread; // Connection Thread.
	private int mState; // Connection state.
	private static Timer timer = new Timer();
	ArrayList<Byte> buffer = new ArrayList<Byte>(); // Response buffer
	
	/**
	 * Class methods.
	 */
	public BluetoothFakeService(Context context, Handler handler) {
		/**
		 * Class Constructor.
		 * 
		 * @param context
		 *            The main thread Context.
		 * @param handler
		 *            A Handler to send messages back to the main thread.
		 */

		if(D){
			Log.d(LOG_HEADER, "Constructor");
		}

		// The connection state is initialized to NONE
		mState = BluetoothService.STATE_NONE;

		// Set the Handler received as argument from main thread
		mHandler = handler;
		DataService.setBTHandler(BTHandler);
		
		// Simulate proper connection
		setState(BluetoothService.STATE_CONNECTING);
		mHandler.obtainMessage(DataService.MESSAGE_DEVICE_NAME, "Random generator").sendToTarget();
		setState(BluetoothService.STATE_CONNECTED);

		// Start the thread to generate data
		mConnectedThread = new ConnectedThread();
		mConnectedThread.start();
	}

	private synchronized void setState(int state) {
		/**
		 * Set the current state of the connection
		 * 
		 * @param state
		 *            An integer defining the current connection state
		 */

		if(D){
			Log.d(LOG_HEADER+".setState", "setState() " + mState + " -> " + state);
		}

		// Updating the connection state
		mState = state;

		// Give the new state to the main thread
		mHandler.obtainMessage(DataService.MESSAGE_BT_STATE_HAS_CHANGED, state, -1).sendToTarget();
	}

	/**
	 * Reset input and output streams and make sure socket is closed. 
	 * This method will be used during shutdown() to ensure that the connection is properly closed during a shutdown.  
	 * @return
	 */
	
	public static void resetConnection() {
		Log.d(LOG_HEADER, "timer deleted");
		timer.cancel();
		timer.purge();
	}
	
	public synchronized int getState() {
		/**
		 * Return the current connection state.
		 */
		return mState;
	}

	/**
	 * Nested Classes.
	 */
	private class ConnectedThread extends Thread {
		/**
		 * This thread runs during a connection with a remote device. It handles
		 * all incoming and outgoing transmissions.
		 */

		// Member variables.

		// Class Methods.
		public ConnectedThread() {
			// Class Constructor
			if(D){
				Log.d(LOG_HEADER+".ConnectedThread", "CREATE Connected Thread");
			}

			// Set priority. This thread is a I/O intensive thread because It
			// reads data stream from a socket.
			// Therefore must have higher priority than UI thread. UI thread has
			// standard priority by default.
			setPriority(MAX_PRIORITY);
			
			// Start a timer task every 300ms to eventually update the UI
			timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask(){ public void run() {generateData();}}, 100L, 300L);
			}
		}
		
		public void generateData() {
			// Run the thread. It is an infinite reading loop
			if(D){
				Log.d(LOG_HEADER+".ConnectedThread", "BEGIN Connected Thread");
			}

			// Generating samples (200)
			for(float i=0; i<10; i++) {
				//buffer.add((byte) 0x0F);
				// buffer.add((byte)i); // Creates a ramp
				buffer.add((byte) 0x9B);
				// Generate noisy sine
				buffer.add((byte) (0x7F & (byte)((0.5+0.2*Math.sin(Math.PI*2/40*i)+(0.3*Math.random()))*255)));
			//	buffer.add((byte) (0xA0));
			//	buffer.add((byte) (0x17));
//				if(D) Log.v(LOG_HEADER, "BYTE: 0x"
//						+ Integer.toHexString(0xFF & buffer.get(buffer.size() - 1))
//						+ " = "
//						+ Integer.toString(0xFF & buffer.get(buffer.size() - 1))
//						+ " READ");
			}

			if(D){
				Log.d(LOG_HEADER+".ConnectedThread,Message reading cycle ended", Long.toString(System.currentTimeMillis()));
			}

			// Send the obtained message to the main thread
			mHandler.obtainMessage(DataService.MESSAGE_READ_A_RESPONSE, -1, -1, buffer).sendToTarget();
		}

	public void run(){
		// Empty
	}
	
	public synchronized void stop() {
		/**
		 * Stop all threads and set the connection status to NONE.
		 */

		if(D){
			Log.d(LOG_HEADER+".Stop method.", "Stopping all BluetoothService threads.");
		}
		
		timer.cancel();
		timer.purge();
		
		// Updating connection state without advising the UI thread
		setState(BluetoothService.STATE_NONE);
	}

	@SuppressLint("HandlerLeak")
	private final Handler BTHandler = new Handler() {
		/**
		 * Handler. Special member used to communicate with the DataService.
		 */

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case DataService.END_READING:
					buffer.clear();
					break;
			}
		}
	};
}


