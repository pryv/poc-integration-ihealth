package ch.epfl.lsi.ironICpp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Messenger;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;


public class CalibrationActivity extends Activity {

	//Debug flag
	protected static final String LOG_HEADER = "CalibrationActivity";
	private static final boolean D = true;

	// Application Level Communication protocol object
	private ProgressDialog progress;
	Handler myHandler;
	int Step = 0;
	int Iter = 0;
	String time_string =  "";
	private int progressStatus=0;
	private static int myProgress;
	static ArrayList<String> arrayOfCheckedElectrodes = new ArrayList<String>();
	static Context context;
	// Menu
	private static Menu menu;
	// Preferences settings
	private static final String PREFS_NAME = "CalibrationPrefs";
	public static final String STEP = "STEP";
	private static SharedPreferences settings;
	private static BluetoothConnector BTconnector ;
	// Bluetooth
	private static final int ENABLE_BLUETOOTH = 1; // Message code for bluetooth enable request
	private static final int CONNECT_DEVICE = 2; // Message code for bluetooth connection to device
	private static final int LINE_LENGTH = 15;

	Messenger mService = null;

	//Calibration variable
	int time=1;
	Intent i;
	private FileOutputStream fileStream;
	static String current_filename;
	//variables for the calibration line plot
	double [] x_values = {1, 2}; //Low and High concentration in mM
	private XYPlot mySimpleXYPlot = null;
	private SimpleXYSeries line_series = new SimpleXYSeries(" ");
	private SimpleXYSeries std_dev_low_series = new SimpleXYSeries(" ");
	private SimpleXYSeries std_dev_high_series = new SimpleXYSeries(" ");

	//variables for calculating the average 
	int actual_mean = 0;//count in which position fill the calculated average in all_the_means[9]
	static double final_means[] = new double[3];
	static double final_std_dev[] = new double[3];
	static double all_the_means[] = new double[9]; //all_the_means[0-2]=PBS1-3 all_the_means[3-5]=lowConcentration1-3 all_the_means[6-8]=highConcentration1-3
	ArrayList<ArrayList<Float>> columnsArray = new ArrayList<ArrayList<Float>>();
	ArrayList<Float> yValues = new ArrayList<Float>();
	static final int[] columns = {8, 5};
	long numberOfLines;
	float sum = 0;
	int WE = -1;
	boolean doStep = false;
	//double [] saved_value = null;
	String fileName;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calibration_activity);

		//initialization
		i = new Intent(this, DataService.class);
		// Settings
		settings = getSharedPreferences(PREFS_NAME, 0);

		getActionBar().setTitle("Calibration");
		savedInstanceState = getIntent().getExtras();
		context = getApplicationContext();

		myHandler = new Handler();
		current_filename = "Calibration";
		arrayOfCheckedElectrodes.add(current_filename);
		BTconnector = new BluetoothConnector(getApplicationContext(),arrayOfCheckedElectrodes,this);

		WE = -1;
		doStep = false;

		LinearLayout linear= (LinearLayout)findViewById(R.id.layout);
		linear.setVisibility(View.VISIBLE);
		linear= (LinearLayout)findViewById(R.id.layout0);
		linear.setVisibility(View.VISIBLE);
		linear= (LinearLayout)findViewById(R.id.layout1);
		linear.setVisibility(View.INVISIBLE);
		linear = (LinearLayout)findViewById(R.id.layout2);
		linear.setVisibility(View.INVISIBLE);
		linear = (LinearLayout)findViewById(R.id.layout3);
		linear.setVisibility(View.INVISIBLE);
	}

	@Override
	public synchronized void onResume() {
		/**
		 * Called when the activity will start interacting with the user.
		 * At this point the activity is at the top of the activity stack,
		 * with user input going to it.
		 */

		super.onResume();

		if(D) Log.d(LOG_HEADER, "onResume()");

		BTconnector.onResume(true);

	}

	@Override
	public void onPause() {
		/**
		 * Called as part of the activity lifecycle when an activity is
		 * going into the background, but has not (yet) been killed.
		 * The counterpart to onResume().
		 */

		super.onStop();

		if(D) Log.d(LOG_HEADER, "onPause()");

		BTconnector.onPause(true);
	}

	@Override
	public void onDestroy() {
		/**
		 * The final call you receive before your activity is destroyed. 
		 */

		super.onDestroy();

		if(D) Log.d(LOG_HEADER, "onDestroy()");

		BTconnector.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/**
		 * Initialize the contents of the Activity's standard options menu.
		 * @param menu  The options menu in which to place the items.
		 */


		if(D) Log.d(LOG_HEADER, "onCreateOptionsMenu method");

		// Setup Main Activity Options Menu.
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.calibration_menu, menu);

		CalibrationActivity.menu = menu;
		boolean btConnect = settings.getBoolean("btConnected", false);
		updateActionBar(btConnect);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/**
		 * This method is called whenever an item in the options menu is selected.
		 * @param item  The item selected in the options menu. 
		 */

		if(D) Log.d(LOG_HEADER, "onOptionsItemSelected method");

		switch (item.getItemId()) {
			case R.id.connect_bt:
				BTconnector.Connect(true);
				return true;
			case R.id.disconnect_bt:
				BTconnector.Disconnect();
				return true;
			case R.id.stop:
				if(D) Log.i(LOG_HEADER, "Chose 'stop'");
				//Intent i = new Intent(this, DataService.class);
				BTconnector.send_stop_command(i);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu){
		/**
		 * Prepare the Screen's standard options menu to be displayed.
		 * If the connection is started, the options menu must be not displayed.
		 * It the connection is starting, the setting item must be disabled.
		 */

		if(D) Log.d(LOG_HEADER, "onPrepareOptionsMenu");

		//switch(mBluetoothService.getState()){
		//case BluetoothService.STATE_CONNECTING: menu.getItem(1).setEnabled(false); return true;
		//case BluetoothService.STATE_NONE: menu.getItem(1).setEnabled(true); return true;
		//case BluetoothService.STATE_CONNECTED: return false;
		//}
		return true;
	}

	static void updateActionBar(boolean btConnected) {
		SharedPreferences.Editor settingsEditor = settings.edit();
		settingsEditor.putBoolean("btConnected", btConnected);
		settingsEditor.commit();

		if(CalibrationActivity.menu != null)
		{
			if(btConnected)
			{
				Log.d(LOG_HEADER,"connect modality");
				// Display only the relevant actions
				menu.findItem(R.id.connect_bt).setVisible(false);
				menu.findItem(R.id.disconnect_bt).setVisible(true);
				menu.findItem(R.id.stop).setVisible(true);
			}
			else
			{
				Log.d(LOG_HEADER,"disconnect modality");
				// Display only the relevant actions
				menu.findItem(R.id.connect_bt).setVisible(true);
				menu.findItem(R.id.disconnect_bt).setVisible(false);
				menu.findItem(R.id.stop).setVisible(false);
			}
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		/**
		 *  Called when an activity launched exits, giving back the requestCode It started it with,
		 *  the resultCode it returned, and any additional data from it.
		 *  The DataService class handles the results (TODO: filter first and handle some messages
		 *  directly here)
		 *  @param requestCode  The reason why an activity was launched by the main activity.
		 *  @param resultCode   The result returned by the activity launched when It exits.
		 *  @param data         The data attached by the activity launched when It exits.
		 */

		if(D) Log.d(LOG_HEADER, "onActivityResult method. ResultCode: " +resultCode+". RequestCode: "+requestCode+". ");

		switch (requestCode)
		{
			case ENABLE_BLUETOOTH:
				// When the request to enable Bluetooth returns.
				BTconnector.BT_Enabled(resultCode);
				break;
			case CONNECT_DEVICE:
				// When ActivityDeviceList returns with a device to connect to
				BTconnector.ConnectedToDevice(resultCode, data, true);
				break;
			default:
				if(D) Log.w(LOG_HEADER, String.valueOf(requestCode));
		}
	}


	public void doStep(View view){
		if(D) Log.d(LOG_HEADER,"index "+Step);
		if(Step == 0 ){
			RadioGroup radioButtonGroup = (RadioGroup) findViewById(R.id.radioButtonGroup);
			int radioButtonID = radioButtonGroup.getCheckedRadioButtonId();
			View radioButton = radioButtonGroup.findViewById(radioButtonID);
			WE = radioButtonGroup.indexOfChild(radioButton);
			//advise the microcontroller the calibrated electrode
			//BTconnector.set_electrode((byte) WE);
			if(D) Log.d(LOG_HEADER,"index "+WE);
			if(WE == -1){
				Toast.makeText(getApplicationContext(), "Error! Chose one WE!", Toast.LENGTH_LONG).show();
			}
			else {
				doStep = true;
				Byte value = null;
				if(WE==0) value=0x30;
				else if(WE==1) value=0x31;
				else if(WE==2) value=0x32;
				Log.d("Calibration", "welectrode "+value);
				if(value!=null)
					BTconnector.set_electrode(value);
			}
		}

		if(doStep){
			switch(Step){
				case 0:
					LinearLayout rl1 = (LinearLayout)view.getParent();
					EditText et = (EditText) rl1.findViewById(R.id.Time0);
					time_string = et.getText().toString();
					break;
				case 1:
					LinearLayout rl2 = (LinearLayout)view.getParent();
					EditText et1 = (EditText) rl2.findViewById(R.id.Time1);
					time_string = et1.getText().toString();

					InputMethodManager imm = (InputMethodManager)getSystemService(
							Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(et1.getWindowToken(), 0);
					break;

				case 2:
					LinearLayout rl3 = (LinearLayout)view.getParent();
					EditText et3 = (EditText) rl3.findViewById(R.id.Time2);
					time_string = et3.getText().toString();

					InputMethodManager imm1 = (InputMethodManager)getSystemService(
							Context.INPUT_METHOD_SERVICE);
					imm1.hideSoftInputFromWindow(et3.getWindowToken(), 0);
					break;
			}

			Log.d(LOG_HEADER,"time string: "+time_string);
			if(!time_string.equals(""))
				time = Integer.parseInt(time_string);
			else
				Toast.makeText(getApplicationContext(), "default time 6 sec", Toast.LENGTH_LONG).show();

			open(view,"Measurements", "Measuring...",this);

			switch (Iter){
				case 0:
					Log.d(LOG_HEADER,"iter "+Iter+" step "+Step);
					LinearLayout rl1 = (LinearLayout)view.getParent();
					rl1.setVisibility(View.INVISIBLE);
					setVisible(R.id.layout1);
					Step = 1;
					Iter = 1;
					break;

				case 1:
					Log.d(LOG_HEADER,"iter "+Iter+" step "+Step);
					LinearLayout rl2 = (LinearLayout)view.getParent();
					rl2.setVisibility(View.INVISIBLE);
					setVisible(R.id.layout2);
					Step = 2;
					Iter = 2;
					break;

				case 2:
					Log.d(LOG_HEADER,"iter "+Iter+"step "+Step);
					LinearLayout rl3 = (LinearLayout)view.getParent();
					rl3.setVisibility(View.INVISIBLE);
					rl3= (LinearLayout)findViewById(R.id.layout0);
					rl3.setVisibility(View.VISIBLE);
					rl3= (LinearLayout)findViewById(R.id.layout1);
					rl3.setVisibility(View.INVISIBLE);
					rl3 = (LinearLayout)findViewById(R.id.layout2);
					rl3.setVisibility(View.INVISIBLE);
					rl3 = (LinearLayout)findViewById(R.id.layout3);
					rl3.setVisibility(View.INVISIBLE);
					setVisible(R.id.sublayout0);
					Step = 0;
					Iter = 3;
					break;

				case 3:
					Log.d(LOG_HEADER,"iter "+Iter+"step "+Step);
					LinearLayout rl4 = (LinearLayout)view.getParent();
					rl4.setVisibility(View.INVISIBLE);
					setVisible(R.id.layout1);
					setVisible(R.id.sublayout1);
					Step = 1;
					Iter = 4;
					break;

				case 4:
					Log.d(LOG_HEADER,"iter "+Iter+"step "+Step);
					LinearLayout rl5 = (LinearLayout)view.getParent();
					rl5.setVisibility(View.INVISIBLE);
					setVisible(R.id.layout2);
					setVisible(R.id.sublayout2);
					Step = 3;
					Iter = 5;
					break;

				case 5:
					Log.d(LOG_HEADER,"iter "+Iter+"step "+Step);
					LinearLayout rl6 = (LinearLayout)view.getParent();
					rl6.setVisibility(View.INVISIBLE);
					rl6= (LinearLayout)findViewById(R.id.layout0);
					rl6.setVisibility(View.VISIBLE);
					rl6= (LinearLayout)findViewById(R.id.layout1);
					rl6.setVisibility(View.INVISIBLE);
					rl6 = (LinearLayout)findViewById(R.id.layout2);
					rl6.setVisibility(View.INVISIBLE);
					rl6 = (LinearLayout)findViewById(R.id.layout3);
					rl6.setVisibility(View.INVISIBLE);
					setVisible(R.id.sublayout0);
					Step = 0;
					Iter = 6;
					break;

				case 6:
					Log.d(LOG_HEADER,"iter "+Iter+"step "+Step);
					LinearLayout rl7 = (LinearLayout)view.getParent();
					rl7.setVisibility(View.INVISIBLE);
					setVisible(R.id.layout1);
					setVisible(R.id.sublayout1);
					Step = 1;
					Iter = 7;
					break;

				case 7:
					Log.d(LOG_HEADER,"iter "+Iter+"step "+Step);
					LinearLayout rl8 = (LinearLayout)view.getParent();
					rl8.setVisibility(View.INVISIBLE);
					setVisible(R.id.layout2);
					setVisible(R.id.sublayout2);
					Step = 2;
					Iter = 8;
					break;

				case 8:
					Log.d(LOG_HEADER,"iter "+Iter+"step "+Step);
					LinearLayout rl9 = (LinearLayout)view.getParent();
					rl9.setVisibility(View.INVISIBLE);
					Step = 0;
					Iter = 0;
					break;
			}
		}
	}

	public void setVisible(int id){
		LinearLayout linear = (LinearLayout) getWindow().findViewById(id);
		linear.setVisibility(View.VISIBLE);
	}

	public void open( View view, String Title, String Message, Context context){
		progress = new ProgressDialog(context);
		Log.d(LOG_HEADER,"CalibrationActivity "+CalibrationActivity.this);
		progress.setTitle(Title);
		progress.setMessage(Message);
		progress.setIndeterminate(true);
		progress.show();

		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(progressStatus<time)
				{
					progressStatus = performTask1();
				}
				myProgress=0;
				progressStatus=0;
				Log.d(LOG_HEADER,"start command send");
				//BTconnector.Connect(true);	//for fake
				BTconnector.request_new_file(i);
				BTconnector.send_start_command(i, true); 	//for real device


				while(progressStatus<time)
				{
					Log.d(LOG_HEADER,"Task2");
					progressStatus = performTask2();
				}
				/*Hides the Progress bar*/
				myHandler.post(new Runnable() {

					@Override
					public void run() {
						progress.cancel();
						Log.d(LOG_HEADER,"stop command send");
						//BTconnector.Disconnect();	//uncomment for fake calibration
						BTconnector.send_stop_command(i); //uncomment for real calibration
						Toast.makeText(getBaseContext(),"Task Completed...please wait until the average is done! "+ current_filename,Toast.LENGTH_LONG).show();
						progressStatus=0;
						doAverageofStoredData(current_filename);
					}
				});

			}
			/* Do some task*/
			private int performTask1()
			{
				if (D) Log.d(LOG_HEADER,"perform Task1");
				try {
					//java.lang.Thread.sleep(long milliseconds)
					Thread.sleep(8000);	//now it is 8 sec if you put 1 in the AlertDialog EditText
				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				return ++myProgress;
			}
			private int performTask2()
			{
				if (D) Log.d(LOG_HEADER,"perform Task2");
				try {
					//java.lang.Thread.sleep(long milliseconds)
					Thread.sleep(4000);	//now it is 4 sec if you put 1 in the AlertDialog EditText
				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				return ++myProgress;
			}
		}).start();
	}
	static File getFileHandle(String fileName) {
		// Checking media availability.
		String state = Environment.getExternalStorageState();
		if(!Environment.MEDIA_MOUNTED.equals(state))
		{
			Toast.makeText(context, R.string.storage_unavailable, Toast.LENGTH_SHORT).show();
			return null;
		}

		// Load data selected
		String path = Environment.getExternalStorageDirectory() + "/IronicCells/" + fileName;
		File fileHandle = new File(path);
		return fileHandle;
	}

	public void doAverageofStoredData(String current_filename){
		File fileHandle = getFileHandle(current_filename);
		numberOfLines = fileHandle.length()/LINE_LENGTH;
		Log.d(LOG_HEADER,"lines in the file "+current_filename+" are "+ numberOfLines );
		if(D) Log.v(LOG_HEADER, "file length: "+ fileHandle.length() + current_filename);
		if(fileHandle != null)
		{
			try {
				// Open file for random access reading
				RandomAccessFile file = new RandomAccessFile(fileHandle, "r");

				// Read last values (x)
				if(readDataFromLinesInFile(file, numberOfLines-1, 1).size()!=0){
					Float length_file = readDataFromLinesInFile(file, numberOfLines-1, 1).get(0).get(0);
					yValues.addAll(readDataFromLinesInFile(file, numberOfLines-1, (int) numberOfLines).get(1));
					if(D) Log.v(LOG_HEADER, "Number of samples (lines) detected in: "+ current_filename+" are "+ length_file);
					//do the average of the stored data.
					sum = 0 ;
					for(int i = 0; i < yValues.size(); i ++){
						if(D) Log.v(LOG_HEADER, "samples : "+yValues.get(i));
						sum = sum + yValues.get(i);
					}

					all_the_means[actual_mean]=sum/yValues.size();

					if(D) Log.d(LOG_HEADER,"mean at "+actual_mean+" is "+ all_the_means[actual_mean] );

					actual_mean ++;

					Toast.makeText(getApplicationContext(), "average at step "+ actual_mean +" is evaluated", Toast.LENGTH_LONG).show();

					if(actual_mean==9){
						//Calibration is finished!
						Toast.makeText(getApplicationContext(), "Finish calibration", Toast.LENGTH_LONG).show();
						//thinks to do:
						//calculate means of each measurement: PBS lowConcentration and highConcentration
						//calculate variances of each measurements
						//save low and high concentration means as first and second point of the plot of calibration line
						//calculate sensitivity S of the line (pendency)
						//calculate LOD as 3xdevstd_blank/S

						//all_the_means: i=0-3-6 blank measurement; i=1-4-7 low concentration measurement; i=2-5-8 high concentration measurement
						//average
						double numerator =0;
						for(int j = 0; j < 3 ; j ++){
							for (int i = j ; i < 9; i = i+3)
								numerator = numerator + all_the_means[i] ;
							final_means[j] = numerator / 3;	//j=0 blank j=1 low j=2 high
							numerator = 0;
						}
						//std deviation
						numerator = 0;
						for(int j = 0; j < 3 ; j ++){
							for (int i = j ; i < 9 ; i = i+3)
								numerator = numerator + Math.pow((all_the_means[i]-final_means[j]),2);
							final_std_dev[j]=Math.sqrt(numerator/ 3); //j=0 blank j=1 low j=2 high
							numerator = 0;
						}

						if(D){
							for (int i = 0; i < 3; i++){
								Log.v(LOG_HEADER, "final mean: "+ final_means[i]+" derived from: ");
								for (int j = i; j < 9; j = j+3)
									Log.v(LOG_HEADER, " "+ all_the_means[j]);
								Log.v(LOG_HEADER, "with std deviation: "+ final_std_dev[i]);
							}
							plotCalibrationLine();
						}
					}

					// Close file handle
					file.close();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private ArrayList<ArrayList<Float>> readDataFromLinesInFile(RandomAccessFile file, long lineNumber, int numberOfLinesToRead) {

		// Create an array of Float arrays (one per column) 
		if(lineNumber < 0 || numberOfLinesToRead <= 0 || lineNumber >= numberOfLines)
		{
			Log.e(LOG_HEADER,"wrong line to read");
		}
		else
		{
			columnsArray.clear();
		}

		// Read bytes from file in one shot
		byte[] buffer = new byte[LINE_LENGTH*numberOfLinesToRead];
		try {
			file.seek((lineNumber-(numberOfLinesToRead-1))*LINE_LENGTH);
			file.read(buffer);
		} catch (IOException e) {
			if(D) Log.w(LOG_HEADER, "Read data out of file bound. Line = " + lineNumber + ", number of lines asked = " + numberOfLinesToRead);
			return columnsArray;
		}

		// Convert line to fill-in arrays
		int offset = 0;
		for(int column=0; column<columns.length; column++)
		{
			// For each column, read corresponding data in buffer in ArrayList
			ArrayList<Float> col = new ArrayList<Float>();

			for(int line=0; line<numberOfLinesToRead; line++)
			{
				//Log.i(LOG_HEADER, "   Reading line "+line+" out of line "+(numberOfLinesToRead-1));

				// For each line, add value read in ArrayList
				float value = 0;
				for(int readByte=0; readByte<columns[column]; readByte++)
				{
					// For each byte, parse value
					value = value*10 + buffer[line*LINE_LENGTH+offset+readByte] - '0';
				}

				col.add(value);
			}

			columnsArray.add(col);

			// Set offset to next column plus separator char
			offset += columns[column]+1;
		}
		return columnsArray;
	}

	public void plotCalibrationLine(){
		//BTconnector.Disconnect();
		Log.d(LOG_HEADER,"plot calibration line");
		// initialize our XYPlot before visualize it  	
		LinearLayout l1=(LinearLayout)findViewById(R.id.layout3);
		mySimpleXYPlot = (XYPlot) l1.findViewById(R.id.mySimpleXYPlot);
		mySimpleXYPlot.setDomainBoundaries(0.45, 2.05, BoundaryMode.FIXED);
		line_series.addLast(x_values[0], final_means[1]);	//final mean of low concentration
		line_series.addLast(x_values[1], final_means[2]);	//final mean of high concentration
		Log.d(LOG_HEADER, "Series "+ line_series.getX(0)+" "+line_series.getY(0)+" "+line_series.getX(1)+" "+line_series.getY(1));


		mySimpleXYPlot.addSeries(line_series, new LineAndPointFormatter(
				//null,
				Color.HSVToColor(new float[]{348,81,90}),//red
				null,
				null, null));

		std_dev_low_series.addLast(x_values[0], final_means[1]+final_std_dev[1]);
		std_dev_low_series.addLast(x_values[0], final_means[1]-final_std_dev[1]);

		std_dev_high_series.addLast(x_values[1], final_means[2]+final_std_dev[2]);
		std_dev_high_series.addLast(x_values[1], final_means[2]-final_std_dev[2]);

		mySimpleXYPlot.addSeries(std_dev_low_series, new LineAndPointFormatter(
				//null,
				Color.HSVToColor(new float[]{180,100,100}),//light blue
				null,
				null, null));

		mySimpleXYPlot.addSeries(std_dev_high_series, new LineAndPointFormatter(
				//null,
				Color.HSVToColor(new float[]{180,100,100}),//light blue
				null,
				null, null));

		line_series = null;
		std_dev_low_series = null;
		std_dev_high_series = null;

		setVisible(R.id.layout3);

		calculate_m_q_LOD();
	}

	public void calculate_m_q_LOD(){
		// y = m*x + q   P1(x_values[0];low_high_mean.get(0)) P2(x_values[1];low_high_mean.get(1))
		//m = y2-y1 / x2-x1   it is the Sensitivity
		//q = - m*x1 + y1

		double m = (final_means[2] - final_means[1]) / (x_values[1] - x_values[0]);
		double q = final_means[2] - x_values[1] * m;
		double LOD = 3*final_std_dev[0]/m;

		if(D) Log.d(LOG_HEADER,"m "+m+" q "+q+" LOD "+LOD);

		// Checking media availability.
		String state = Environment.getExternalStorageState();
		if(!Environment.MEDIA_MOUNTED.equals(state))
		{
			Toast.makeText(getApplicationContext(), R.string.storage_unavailable, Toast.LENGTH_SHORT).show();
		}

		// Create an application folder if not present.
		File AppFolder = new File(Environment.getExternalStorageDirectory() + "/IronicCells");
		if(!AppFolder.exists()) AppFolder.mkdir();
		AppFolder = new File(Environment.getExternalStorageDirectory() + "/IronicCells/Calibration");
		if(!AppFolder.exists()) AppFolder.mkdir();

		// Create File.
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

		switch (WE) {
			case 0:
				fileName = "Calibration_Glucose_" + sdf.format(new Date()) +".csv";
				break;
			case 1:
				fileName = "Calibration_Lactate_" + sdf.format(new Date()) +".csv";
				break;
			case 2:
				fileName = "Calibration_Bilirubin_" + sdf.format(new Date()) +".csv";
				break;
			case 3:
				fileName = "Calibration_Sodium_" + sdf.format(new Date()) +".csv";
				break;
			case 4:
				fileName = "Calibration_Potassium_" + sdf.format(new Date()) +".csv";
				break;
			case 5:
				fileName = "Calibration_Ph_" + sdf.format(new Date()) +".csv";
				break;
			case 6:
				fileName = "Calibration_Temperature_" + sdf.format(new Date()) +".csv";
				break;
		}

		File file = new File(AppFolder, fileName); // Create a file inside the root of application's file directory.

		// Open the file stream
		try {
			Log.d(LOG_HEADER,"fileOutputStream "+ file);
			fileStream = new FileOutputStream(file); // Output stream ready to write the file.
		} catch (IOException e) {
			if(D) Log.e("ExternalStorage", "Error writing " + file, e);
		}

		// Write the file.
		String newRow = new String(); // Row to be written into the file
		for (int i=0;i<3 ;i++){
			// Writing file one row at a time with zero-padding to have constant length thus easy seek in file
			newRow = String.format(Locale.ROOT, "%08f\t%05f\n", final_means[i], final_std_dev[i]);
			write_in_the_file(newRow);
		}
		newRow = String.format(Locale.ROOT, "%08f\t%05f\n", x_values[0], x_values[1]);
		write_in_the_file(newRow);
		// Writing file one row at a time with zero-padding to have constant length thus easy seek in file
		newRow = String.format(Locale.ROOT, "%08f\t%05f\n", m, q);
		write_in_the_file(newRow);
		newRow = String.format(Locale.ROOT, "%08f\t%05f\n", LOD, 0.0);
		write_in_the_file(newRow);

		//a new calibration file is available for the user
		switch (WE) {
			case 0:
				StartActivity.loadSpinner("Glucose", StartActivity.spinner1,StartActivity.spinner_adapter1);
				break;

			case 1:
				StartActivity.loadSpinner("Lactate", StartActivity.spinner2,StartActivity.spinner_adapter2);
				break;

			case 2:
				StartActivity.loadSpinner("Bilirubin", StartActivity.spinner3,StartActivity.spinner_adapter3);
				break;

			case 3:
				StartActivity.loadSpinner("Sodium", StartActivity.spinner4,StartActivity.spinner_adapter4);
				break;

			case 4:
				StartActivity.loadSpinner("Potassium", StartActivity.spinner5,StartActivity.spinner_adapter5);
				break;

			case 5:
				StartActivity.loadSpinner("pH", StartActivity.spinner6,StartActivity.spinner_adapter6);
				break;

			case 6:
				StartActivity.loadSpinner("Temperature", StartActivity.spinner7,StartActivity.spinner_adapter7);
				break;
		}
		//remove useless file of calibration
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				if(filename.contains("Calibration_")&&(filename.endsWith(".txt") || filename.endsWith(".csv"))) return true;
				return false;
			}
		};
		// List files and sort by inverse alphabetical order (Default is the first one)
		File appFolder = new File(Environment.getExternalStorageDirectory() + "/IronicCells");
		if(!appFolder.exists()) appFolder.mkdir();
		String[] cal_files = appFolder.list(filter);
		File remove;
		for (int i =0; i<cal_files.length;i++){

			remove = new File(appFolder,cal_files[i]);//line2
			Log.d(LOG_HEADER,"cal file "+remove+" deleted");
			remove.delete();

		}
		cal_files=null;
		//ready for next calibration on another electrode
		WE = -1;
		doStep = false;
	}

	public void write_in_the_file(String newRow){
		try {
			// Change size if newRow length is changed
			fileStream.write(newRow.getBytes());
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), "Impossible to write recording", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}
}