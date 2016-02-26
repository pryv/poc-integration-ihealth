package ch.epfl.lsi.ironICpp;
import java.text.DecimalFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import ch.epfl.lsi.ironICpp.FragmentFiltering.SettingsChangedListener;

import com.androidplot.xy.XYPlot;

public class ChipParametersActivity extends Activity implements SettingsChangedListener {
	private static final String LOG_HEADER = "ChipParameters";
	private static final boolean D = true;
	private PickerFragment PicFrag = new PickerFragment();
	static ArrayList<String> arrayOfCheckedElectrodes = new ArrayList<String>();
	// Menu
	private static Menu menu;
	private static SharedPreferences settings;
	private static final String PREFS_NAME = "ChipParamPrefs";
	private static BluetoothConnector BTconnector ;

	//private static ChipParametersActivity mParentInstance = null;
	private static final int ENABLE_BLUETOOTH = 1; // Message code for bluetooth enable request
	private static final int CONNECT_DEVICE = 2; // Message code for bluetooth connection to device
	private static ChipParametersActivity mParentInstance = null;
	static Byte address;
	static Byte value0x10;
	static Byte value0x11;
	static Byte value0x12;
	static MultitouchPlot signalPlot;
	static Byte working_electrode;
	static String filename = null;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		/**
		 * Called when the activity is starting.
		 * @param savedInstanceState  Bundle containing the data it most recently supplied in onSaveInstanceState(Bundle). 
		 */

		if(D) Log.d(LOG_HEADER, "onCreate()");

		// Calling the superior class method.
		super.onCreate(savedInstanceState);

		// Set layout content.
		setContentView(R.layout.paramenters_option_activity);

		//mParentInstance = ChipParametersActivity.this; 

		// Settings
		settings = getSharedPreferences(PREFS_NAME, 0);
		FragmentFiltering.setFilteringPrefsName(PREFS_NAME);
		MultitouchPlot.setFilteringPrefsName(PREFS_NAME);
		FragmentFiltering.setFilteringActivity(this);
		Log.d(LOG_HEADER, "prefs name "+ MultitouchPlot.PREFS_NAME.toString()+" "+ FragmentFiltering.PREFS_NAME.toString()+" "+PREFS_NAME);

		arrayOfCheckedElectrodes.add("ChipParamenters");
		BTconnector = new BluetoothConnector(getApplicationContext(),arrayOfCheckedElectrodes,this);
		mParentInstance = ChipParametersActivity.this;
		getActionBar().setTitle("Chip Parameters");

		//Plot
		setupPlot();
		signalPlot.setTitle("Plot of ChipParamenters" );
		//signalPlot.setRangeBoundaries(0, 1024, BoundaryMode.FIXED);
		if(filename!=null)
			signalPlot.loadFile(filename);
		// Decimal format of the value on y axes if using digital value
		signalPlot.setRangeValueFormat(new DecimalFormat("#"));

		//Status Register
		LinearLayout l1 = (LinearLayout)findViewById(R.id.layout1);
		final CheckBox cb_status = (CheckBox)l1.findViewById(R.id.status_register);
		cb_status.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (cb_status.isChecked())
					Toast.makeText(getApplicationContext(), "Ready", Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(getApplicationContext(), "Not Ready", Toast.LENGTH_SHORT).show();
			}
		});

		//Protection Register
		l1 = (LinearLayout)findViewById(R.id.layout1);
		final CheckBox cb_lock = (CheckBox)l1.findViewById(R.id.protection_register);
		//default
		cb_lock.setChecked(true);
		cb_lock.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (cb_lock.isChecked())
					Toast.makeText(getApplicationContext(), "Registers 0x10, 0x11 in read only mode", Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(getApplicationContext(), "Registers 0x10, 0x11 in write mode", Toast.LENGTH_SHORT).show();
			}
		});

		//Control Register
		value0x10 = 0x03;	//default
		l1 = (LinearLayout)findViewById(R.id.layout2);
		Button bt_gain = (Button) l1.findViewById(R.id.gain_button);
		bt_gain.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//Address ControlRegister 0x10
				address = 0x10;
				BTconnector.send_address(address);
				// Perform action on click
				String[] nums = {"External", "2.75 k½","3.5 k½","7 k½","14 k½","35 k½","120 k½","350 k½"};
				PicFrag.setValueInPicker(nums,0,R.id.layout2, R.id.valueGain,1, mParentInstance );
				showPickerAlert();
			}
		});
		Button bt_load = (Button) l1.findViewById(R.id.Rload_button);
		bt_load.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//Address ControlRegister 0x10
				address = 0x10;
				BTconnector.send_address(address);
				// Perform action on click
				String[] nums = {"10 ½","33 ½","50 ½","100 ½"};
				PicFrag.setValueInPicker(nums,nums.length,R.id.layout2,R.id.valueLOAD,2, mParentInstance);
				showPickerAlert();
			}
		});

		//Reference Control Register
		value0x11 = 0x20;	//default
		l1 = (LinearLayout)findViewById(R.id.layout3);
		final CheckBox cb_ref = (CheckBox)l1.findViewById(R.id.ref_source_checkbox);
		cb_ref.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (cb_ref.isChecked())
					Toast.makeText(getApplicationContext(), "External", Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(getApplicationContext(), "Internal", Toast.LENGTH_SHORT).show();
			}
		});

		final CheckBox cb_bias = (CheckBox)l1.findViewById(R.id.bias_sign_checkbox);
		cb_bias.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				address = 0x11;
				BTconnector.send_address(address);
				if (cb_bias.isChecked()){
					//set bit [4] to 1
					value0x11 = (byte) ((value0x11 & 0xEF) | 0x10);
					BTconnector.send_value(value0x11);
					Toast.makeText(getApplicationContext(), "Positive (Vwe - Vre) > 0 V", Toast.LENGTH_SHORT).show();
				}
				else{
					//set bit [4] to 0
					value0x11 = (byte) ((value0x11 & 0xEF) | 0x00);
					BTconnector.send_value(value0x11);
					Toast.makeText(getApplicationContext(), "Negative (Vwe - Vre) < 0 V", Toast.LENGTH_SHORT).show();
				}
			}
		});

		l1 = (LinearLayout)findViewById(R.id.layout4);
		Button bt_int_z = (Button) l1.findViewById(R.id.int_z_button);
		bt_int_z.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Perform action on click
				address = 0x11;
				BTconnector.send_address(address);
				String[] nums = {"20 %","50 %","67 %","Internal"};
				PicFrag.setValueInPicker(nums, 1, R.id.layout4, R.id.valueINT_Z,1,mParentInstance);
				showPickerAlert();
			}
		});

		Button bt_bias = (Button) l1.findViewById(R.id.bias_button);
		bt_bias.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				address = 0x11;
				BTconnector.send_address(address);
				// Perform action on click
				String[] nums = new String [14];
				nums[0] = "0 %";
				nums[1] = "1 %";
				int pos = 2;
				for (int i = 2; i<= 24 ; i=i+2){
					nums [pos] = Integer.toString(i) + " %";
					pos++;
				}
				PicFrag.setValueInPicker(nums, 0, R.id.layout4, R.id.valueBIAS,2,mParentInstance);
				showPickerAlert();
			}
		});

		//Mode Control Register
		value0x10 = 0x0F;	//default
		l1 = (LinearLayout)findViewById(R.id.layout5);
		final CheckBox cb_fet = (CheckBox)l1.findViewById(R.id.fet_short_checkbox);
		cb_fet.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (cb_fet.isChecked())
					Toast.makeText(getApplicationContext(), "Enabled", Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(getApplicationContext(), "Disabled", Toast.LENGTH_SHORT).show();
			}
		});

		Button btn_op_mode = (Button) l1.findViewById(R.id.op_mode_button);
		btn_op_mode.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Perform action on click
				String[] nums = {"Deep Sleep","2-lead ground referred galvanic cell","Standby","3-lead amperometric cell","T measurement - TIA OFF", "T measurement - TIA ON"};
				PicFrag.setValueInPicker(nums, 0 , R.id.layout5, R.id.valueOPMode,1,mParentInstance);
				showPickerAlert();
			}
		});

	}

	private void setupPlot(){
		/**
		 * This method set the chart graphic settings.
		 */

		if(D) Log.d(LOG_HEADER, "setupPlot");

		// Getting the XYPlot object.
		signalPlot = (MultitouchPlot) findViewById(R.id.multitouchPlot);

		// Set the appearance
		signalPlot.setBorderStyle(XYPlot.BorderStyle.SQUARE, null, null);
	}

	public static void SaveFileName(String filename_saved){
		filename = filename_saved;
	}

	public static void selected_value(String value, int which_parameter) {
		// TODO Auto-generated method stub`
		//value0x10: 000-xxx-xx
		Log.d(LOG_HEADER,"want to set the register "+ address + " on the "+ which_parameter+" parameter with "+value);
		switch (address) {
			case 0x10:
				if(which_parameter==1){
					//mask in order to modify only [4:2] bits is with & 0xE3
					value0x10 = (byte) (value0x10 & 0xE3);
					Log.d(LOG_HEADER,"value actual "+ value0x10);
					if (value.equals("External")){
						//000 :
						value0x10 = (byte) (value0x10 | 0x00);
						signalPlot.setRgain((float)(1.0));
					}
					else if ( value.equals("2.75 k½")){
						//001
						value0x10 = (byte) (value0x10 | 0x04);
						signalPlot.setRgain((float)(2.75*1000));
					}
					else if ( value.equals("3.5 k½")){
						//010
						value0x10 = (byte) (value0x10 | 0x08);
						signalPlot.setRgain((float)(3.5*1000));
					}
					else if ( value.equals("7 k½")){
						//011
						value0x10 = (byte) (value0x10 | 0x0C);
						signalPlot.setRgain((float)(7*1000));
					}
					else if ( value.equals("14 k½")){
						//100
						value0x10 = (byte) (value0x10 | 0x10);
						signalPlot.setRgain((float)(14*1000));
					}
					else if ( value.equals("35 k½")){
						//101
						value0x10 = (byte) (value0x10 | 0x14);
						signalPlot.setRgain((float)(35*1000));
					}
					else if ( value.equals("120 k½")){
						//110
						value0x10 = (byte) (value0x10 | 0x18);
						signalPlot.setRgain((float)(120*1000));
					}
					else if ( value.equals("350 k½")){
						//111
						value0x10 = (byte) (value0x10 | 0x1C);
						signalPlot.setRgain((float)(350*1000));
					}
					Log.d(LOG_HEADER,"value new "+ value0x10);
					//signalPlot.setRangeBoundaries(0*signalPlot.delta_step/signalPlot.R_gain, 1024*signalPlot.delta_step/signalPlot.R_gain, BoundaryMode.FIXED);
					BTconnector.send_value(value0x10);

				}
				else if(which_parameter==2){
					//mask in order to modify only [0:1] bits is with & 0xFC
					value0x10 = (byte) (value0x10 & 0xFC);
					if (value.equals("10 ½")){
						//000
						value0x10 = (byte) (value0x10 | 0x00);
					}
					else if ( value.equals("33 ½")){
						//001
						value0x10 = (byte) (value0x10 | 0x01);
					}
					else if ( value.equals("50 k½")){
						//010
						value0x10 = (byte) (value0x10 | 0x02);
					}
					else if ( value.equals("100 ½")){
						//011
						value0x10 = (byte) (value0x10 | 0x03);
					}
					BTconnector.send_value(value0x10);
				}

				break;
			case 0x11:
				if(which_parameter==1){
					//mask in order to modify only [6:5] bits is with & 0x9F
					value0x11 = (byte) (value0x11 & 0x9F);
					Log.d(LOG_HEADER,"value actual "+ value0x11);
					if (value.equals("20 %")){
						//00 :
						value0x11 = (byte) (value0x11 | 0x00);
					}
					else if ( value.equals("50 %")){
						//01
						value0x11 = (byte) (value0x11 | 0x20);
					}
					else if ( value.equals("67 %")){
						//10
						value0x11 = (byte) (value0x11 | 0x40);
					}
					else if ( value.equals("Internal")){
						//11
						value0x11 = (byte) (value0x11 | 0x60);
					}

					Log.d(LOG_HEADER,"value new "+ value0x11);
					BTconnector.send_value(value0x11);

				}
				else if(which_parameter==2){
					//mask in order to modify only [0:3] bits is with & 0xF0
					value0x11 = (byte) (value0x11 & 0xF0);
					if (value.equals("0 %")){
						//0000
						value0x11 = (byte) (value0x11 | 0x00);
					}
					else if ( value.equals("1 %")){
						//0001
						value0x11 = (byte) (value0x11 | 0x01);
					}
					else if ( value.equals("2 %")){
						//0010
						value0x11 = (byte) (value0x11 | 0x02);
					}
					else if ( value.equals("4 %")){
						//0011
						value0x11 = (byte) (value0x11 | 0x03);
					}
					else if ( value.equals("6 %")){
						//0100
						value0x11 = (byte) (value0x11 | 0x04);
					}
					else if ( value.equals("8 %")){
						//0101
						value0x11 = (byte) (value0x11 | 0x05);
					}
					else if ( value.equals("10 %")){
						//0110
						value0x11 = (byte) (value0x11 | 0x06);
					}
					else if ( value.equals("12 %")){
						//0111
						value0x11 = (byte) (value0x11 | 0x07);
					}
					else if ( value.equals("14 %")){
						//1000
						value0x11 = (byte) (value0x11 | 0x08);
					}
					else if ( value.equals("16 %")){
						//1001
						value0x11 = (byte) (value0x11 | 0x09);
					}
					else if ( value.equals("18 %")){
						//1010
						value0x11 = (byte) (value0x11 | 0xA);
					}
					else if ( value.equals("20 %")){
						//1011
						value0x11 = (byte) (value0x11 | 0x0B);
					}
					else if ( value.equals("22 %")){
						//1100
						value0x11 = (byte) (value0x11 | 0x0C);
					}
					else if ( value.equals("24 %")){
						//1101
						value0x11 = (byte) (value0x11 | 0x0D);
					}
					BTconnector.send_value(value0x11);
				}

				break;
			case 0x12:
				if(which_parameter==1){
					//mask in order to modify only [2:0] bits is with & 0xF8
					value0x12 = (byte) (value0x12 & 0xF8);
					Log.d(LOG_HEADER,"value actual "+ value0x12);
					if (value.equals("Deep Sleep")){
						//000 :
						value0x12 = (byte) (value0x12 | 0x00);
					}
					else if ( value.equals("2-lead ground referred galvanic cell")){
						//001
						value0x12 = (byte) (value0x12 | 0x01);
					}
					else if ( value.equals("Standby")){
						//010
						value0x12 = (byte) (value0x12 | 0x02);
					}
					else if ( value.equals("3-lead amperometric cell")){
						//011
						value0x12 = (byte) (value0x12 | 0x03);
					}
					else if ( value.equals("T measurement - TIA OFF")){
						//110
						value0x12 = (byte) (value0x12 | 0x06);
					}
					else if ( value.equals("T measurement - TIA ON")){
						//111
						value0x12 = (byte) (value0x12 | 0x07);
					}

					Log.d(LOG_HEADER,"value new "+ value0x12);
					BTconnector.send_value(value0x12);

				}

			default:
				break;
		}
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

		BTconnector.onResume(false);

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

		BTconnector.onPause(false);
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
		inflater.inflate(R.menu.settings_menu, menu);

		ChipParametersActivity.menu = menu;
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

		// Manage ActionBar events
		Intent i = new Intent(this, DataService.class);
		switch (item.getItemId()) {
			case R.id.connect_bt:
				BTconnector.Connect(false);
				return true;
			case R.id.disconnect_bt:
				if(D) Log.i(LOG_HEADER, "Chose 'disconnect_bt'");
				BTconnector.Disconnect();
				return true;
			case R.id.start_stream:
				if(D) Log.i(LOG_HEADER, "Chose 'start'");
				// Perform action on click
				PickerFragment.setWEwith(true);
				Log.d("Picker", "setWEwith "+PickerFragment.setWE);
				String[] nums = {"WE1=0x30", "WE2=0x31","WE3=0x32"};
				PicFrag.setValueInPicker(nums, this );
				PicFrag.show(getFragmentManager(), "pickerFragment");
				Log.d("Picker", "welectrode1 "+ working_electrode);

				BTconnector.send_start_command(i,false);
				return true;
			case R.id.stop:
				if(D) Log.i(LOG_HEADER, "Chose 'stop'");
				BTconnector.send_stop_command(i);
				return true;
			case R.id.filtering:
				DialogFragment filteringSettingsFragment = new FragmentFiltering();
				filteringSettingsFragment.show(getFragmentManager(), "filtering");
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public static void set_WE(Byte value){
		working_electrode = value;
		BTconnector.set_electrode(working_electrode);
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
		//menu.findItem(R.id.start).setTitle("Start Simulation");

		if(ChipParametersActivity.menu != null)
		{
			if(btConnected)
			{
				Log.d(LOG_HEADER,"connect modality");
				// Display only the relevant actions
				menu.findItem(R.id.connect_bt).setVisible(false);
				menu.findItem(R.id.disconnect_bt).setVisible(true);
				menu.findItem(R.id.start_stream).setVisible(true);
				menu.findItem(R.id.stop).setVisible(true);
				menu.findItem(R.id.filtering).setVisible(true);
			}
			else
			{
				Log.d(LOG_HEADER,"disconnect modality");
				// Display only the relevant actions
				menu.findItem(R.id.connect_bt).setVisible(true);
				menu.findItem(R.id.disconnect_bt).setVisible(false);
				menu.findItem(R.id.start_stream).setVisible(false);
				menu.findItem(R.id.stop).setVisible(false);
				menu.findItem(R.id.filtering).setVisible(false);
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
				BTconnector.ConnectedToDevice(resultCode, data, false);
				break;
			default:
				if(D) Log.w(LOG_HEADER, String.valueOf(requestCode));
		}
	}


	public void showPickerAlert(){
		// when a button is clicked, Picker is shown
		PicFrag.show(getFragmentManager(), "pickerFragment");
	}

	public static void SetRegisterLabel(Byte address, Byte parameters) {
		// TODO Auto-generated method stub
		LinearLayout l1;
		final CheckBox cb_lock;
		switch (address) {
			case 0x00:	//Status Register
				Log.d(LOG_HEADER,"Status Register riceved");
				Toast.makeText(mParentInstance.getApplicationContext(), "set address 0x00 "+address+" with "+parameters, Toast.LENGTH_LONG).show();
				l1 = (LinearLayout)mParentInstance.findViewById(R.id.layout1);
				cb_lock = (CheckBox)l1.findViewById(R.id.status_register);
				//I have to extract the bit 0 and if it is 1 the Status is true
				cb_lock.setChecked(((parameters & 0xff) & 0x01) == 0x01);
				break;
			case 0x01:	//Protection Register
				Toast.makeText(mParentInstance.getApplicationContext(), "set address 0x01 "+address+" with "+parameters, Toast.LENGTH_LONG).show();
				Log.d(LOG_HEADER,"Protection Register riceved");
				l1 = (LinearLayout)mParentInstance.findViewById(R.id.layout1);
				cb_lock = (CheckBox)l1.findViewById(R.id.protection_register);
				//I have to extract the bit 0 and if it is 1 the Lock is true
				cb_lock.setChecked(((parameters & 0xff) & 0x01) == 0x01);
				break;	//Control Register
			case 0x10:
				Toast.makeText(mParentInstance.getApplicationContext(), "set address 0x10 "+address+" with "+parameters, Toast.LENGTH_LONG).show();
				break;	//Reference Control Register
			case 0x11:
				Toast.makeText(mParentInstance.getApplicationContext(), "set address 0x11 "+address+" with "+parameters, Toast.LENGTH_LONG).show();
				break;	//Mode Control Register
			case 0x12:
				Toast.makeText(mParentInstance.getApplicationContext(), "set address 0x12 "+address+" with "+parameters, Toast.LENGTH_LONG).show();
				break;
			default:
				break;
		}
	}

	public static BluetoothConnector getBTconnector() {
		return BTconnector;
	}

	public static void setBTconnector(BluetoothConnector bTconnector) {
		BTconnector = bTconnector;
	}

	@Override
	public void onFinishSettingsChanged() {
		// TODO Auto-generated method stub
		Log.d(LOG_HEADER, "onFinishSettingsChanged "+getApplicationContext());
		signalPlot.reloadFilteringPrefs(getApplicationContext());
	}
}