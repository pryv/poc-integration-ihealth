package ch.epfl.lsi.ironICpp;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

/**
 * @author Francesca Sradolini (francesca.stadolini@epfl.ch)
 */

public class StartActivity extends Activity {

	// Debug flags.
	private static final boolean D = true; // Debug flag used to marking down some informations in the Log register
	private static final String LOG_HEADER = "StartActivity"; // Debug flag used to identify caller

	private static final String EXTRA_MESSAGE = "Electrodes";
	private CheckBox [] metabolites = new CheckBox[7];
	static String[] cal_files=new String[0];
	static ArrayAdapter<String> spinner_adapter1;
	static ArrayAdapter<String> spinner_adapter2;
	static ArrayAdapter<String> spinner_adapter3;
	static ArrayAdapter<String> spinner_adapter4;
	static ArrayAdapter<String> spinner_adapter5;
	static ArrayAdapter<String> spinner_adapter6;
	static ArrayAdapter<String> spinner_adapter7;
	static Spinner spinner1;
	static Spinner spinner2;
	static Spinner spinner3;
	static Spinner spinner4;
	static Spinner spinner5;
	static Spinner spinner6;
	static Spinner spinner7;
	NumberPicker numberPicker;
	static Context ctx;
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_activity);

		//Set background image
		View view = getWindow().getDecorView();

		int orientation = getResources().getConfiguration().orientation;
		if (Configuration.ORIENTATION_LANDSCAPE == orientation) {
			view.setBackgroundResource (R.drawable.bg_board1);
		} else {
			view.setBackgroundResource (R.drawable.bg_board);
		}

		metabolites[0]= (CheckBox) findViewById(R.id.checkbox_metab1);
		metabolites[1]= (CheckBox) findViewById(R.id.checkbox_metab2);
		metabolites[2]= (CheckBox) findViewById(R.id.checkbox_metab3);
		metabolites[3]= (CheckBox) findViewById(R.id.checkbox_metab4);
		metabolites[4]= (CheckBox) findViewById(R.id.checkbox_metab5);
		metabolites[5]= (CheckBox) findViewById(R.id.checkbox_metab6);
		metabolites[6]= (CheckBox) findViewById(R.id.checkbox_metab7);

		MonitoringActivity.numberofplot = 0;
		ctx = getApplicationContext();

		Intent intent=getIntent();
		ArrayList<String> checkedElectrodes = null;
		checkedElectrodes = intent.getStringArrayListExtra(EXTRA_MESSAGE);

		if(checkedElectrodes!=null)
			setStartCheck(checkedElectrodes);

		numberPicker = (NumberPicker) findViewById(R.id.numberPicker1);
		numberPicker.setMinValue(1);
		numberPicker.setMaxValue(10);
		numberPicker.setWrapSelectorWheel(true);


		spinner1 = (Spinner) findViewById(R.id.spinner0);
		loadSpinner("Glucose",spinner1, spinner_adapter1);
		spinner1.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> adapter, View view,int pos, long id) {
				String selected = (String)adapter.getItemAtPosition(pos);
				Toast.makeText(
						getApplicationContext(),
						"Calibration file is "+selected,
						Toast.LENGTH_LONG
				).show();
				MonitoringActivity.set_m_q("Calibration/"+selected, 1);
			}
			public void onNothingSelected(AdapterView<?> arg0) {
				MonitoringActivity.set_m_q("Calibration/Default_Glucose.csv", 1);
			}
		});

		spinner2 = (Spinner) findViewById(R.id.spinner1);
		loadSpinner("Lactate",spinner2, spinner_adapter2);
		spinner2.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> adapter, View view,int pos, long id) {
				String selected = (String)adapter.getItemAtPosition(pos);
				Toast.makeText(
						getApplicationContext(),
						"Calibration file is "+selected,
						Toast.LENGTH_LONG
				).show();
				MonitoringActivity.set_m_q("/Calibration/"+selected, 2);
			}
			public void onNothingSelected(AdapterView<?> arg0) {
				MonitoringActivity.set_m_q("/Calibration/Default_Lactate.csv", 2);
			}
		});

		spinner3 = (Spinner) findViewById(R.id.spinner2);
		loadSpinner("Bilirubin",spinner3,spinner_adapter3);
		spinner3.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> adapter, View view,int pos, long id) {
				String selected = (String)adapter.getItemAtPosition(pos);
				Toast.makeText(
						getApplicationContext(),
						"Calibration file is "+selected,
						Toast.LENGTH_LONG
				).show();
				MonitoringActivity.set_m_q("/Calibration/"+selected, 3);
			}
			public void onNothingSelected(AdapterView<?> arg0) {
				//	MonitoringActivity.set_m_q("/Calibration/Default_Bilirubin.csv", 3);
			}
		});

		spinner4 = (Spinner) findViewById(R.id.spinner3);
		loadSpinner("Sodium",spinner4,spinner_adapter4);
		spinner4.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> adapter, View view,int pos, long id) {
				String selected = (String)adapter.getItemAtPosition(pos);
				Toast.makeText(
						getApplicationContext(),
						"Calibration file is "+selected,
						Toast.LENGTH_LONG
				).show();
				MonitoringActivity.set_m_q("/Calibration/"+selected, 4);
			}
			public void onNothingSelected(AdapterView<?> arg0) {
				//	MonitoringActivity.set_m_q("/Calibration/Default_Sodium.csv", 4);
			}
		});

		spinner5 = (Spinner) findViewById(R.id.spinner4);
		loadSpinner("Potassium",spinner5,spinner_adapter5);
		spinner5.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> adapter, View view,int pos, long id) {
				String selected = (String)adapter.getItemAtPosition(pos);
				Toast.makeText(
						getApplicationContext(),
						"Calibration file is "+selected,
						Toast.LENGTH_LONG
				).show();
				MonitoringActivity.set_m_q("/Calibration/"+selected, 5);
			}
			public void onNothingSelected(AdapterView<?> arg0) {
				//MonitoringActivity.set_m_q("/Calibration/Default_Potassium.csv", 5);
			}
		});
		spinner6 = (Spinner) findViewById(R.id.spinner5);
		loadSpinner("pH",spinner6,spinner_adapter6);
		spinner6.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> adapter, View view,int pos, long id) {
				String selected = (String)adapter.getItemAtPosition(pos);
				Toast.makeText(
						getApplicationContext(),
						"Calibration file is "+selected,
						Toast.LENGTH_LONG
				).show();
				MonitoringActivity.set_m_q("/Calibration/"+selected, 6);
			}
			public void onNothingSelected(AdapterView<?> arg0) {
				//	MonitoringActivity.set_m_q("/Calibration/Default_pH.csv", 6);
			}
		});
		spinner7 = (Spinner) findViewById(R.id.spinner6);
		loadSpinner("Temperature",spinner7,spinner_adapter7);
		spinner7.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> adapter, View view,int pos, long id) {
				String selected = (String)adapter.getItemAtPosition(pos);
				Toast.makeText(
						getApplicationContext(),
						"Calibration file is "+selected,
						Toast.LENGTH_LONG
				).show();
				MonitoringActivity.set_m_q("/Calibration/"+selected, 7);
			}
			public void onNothingSelected(AdapterView<?> arg0) {
				//	MonitoringActivity.set_m_q("/Calibration/Default_Temperature.csv", 7);
			}
		});
	}


	private void setStartCheck(ArrayList<String> checkedElectrodes) {
		// TODO 
		for(int i=0;i<7;i++){
			for(int j=0;j<checkedElectrodes.size();j++){
				if(metabolites[i].getText().equals(checkedElectrodes.get(j))){
					metabolites[i].setChecked(true);
				}
			}
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

		loadSpinner("Glucose",spinner1, spinner_adapter1);
		loadSpinner("Lactate",spinner2, spinner_adapter2);
		loadSpinner("Bilirubin",spinner3, spinner_adapter3);
		loadSpinner("Sodium",spinner4, spinner_adapter4);
		loadSpinner("Potassium",spinner5, spinner_adapter5);
		loadSpinner("pH",spinner6, spinner_adapter6);
		loadSpinner("Temperature",spinner7, spinner_adapter7);

	}

	public static void loadSpinner(final String we, Spinner spinner, SpinnerAdapter sa){
		// Read files.
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				if(filename.contains(we)&&(filename.endsWith(".txt") || filename.endsWith(".csv"))) return true;
				return false;
			}
		};

		// List files and sort by inverse alphabetical order (Default is the first one)
		File appFolder = new File(Environment.getExternalStorageDirectory() + "/IronicCells/Calibration");
		if(!appFolder.exists()) appFolder.mkdir();
		cal_files = appFolder.list(filter);
		if(cal_files == null)
		{
			return;
		}
		List<String> listFiles = Arrays.asList(cal_files);
		Collections.sort(listFiles);
		Collections.reverse(listFiles);
		cal_files = (String[]) listFiles.toArray();
		if(cal_files.length==0){
			spinner.setVisibility(View.INVISIBLE);
		}
		else{
			spinner.setVisibility(View.VISIBLE);
			// Create an ArrayAdapter using the string array and a default spinner layout
			sa = new ArrayAdapter<String>(
					ctx,
					R.layout.my_spinner_item,
					cal_files
			);
			// Apply the adapter to the spinner
			spinner.setAdapter(sa);
		}
	}

	/** Called when the user touches the button */
	public void runActivityChart(View view) {

		// when the button Start is clicked, ActivityChart starts
		Intent intent = new Intent(this, MonitoringActivity.class);
		boolean [] electrodes = new boolean [7];
		for(int i=metabolites.length-1;i>=0;i--){
			Log.d(LOG_HEADER,"metabolites "+metabolites[i]);
			electrodes[i]=metabolites[i].isChecked();
			if(metabolites[i].isChecked()==true)
				MonitoringActivity.numberofplot++;

		}
		intent.putExtra("electrodes", electrodes);
		intent.putExtra("minutes", numberPicker.getValue());

		if(D) Log.d(LOG_HEADER,"Start MonitoringActivity with these electrodes:"+ electrodes[0]+electrodes[1]+electrodes[2]+electrodes[3]+electrodes[4]+electrodes[5]+electrodes[6]);

		startActivity(intent);
	}

	public void showLoginDialog(View view) {
		// when the button Start is clicked, ActivityChart starts
		LoginFragment logFrag = new LoginFragment();
		logFrag.show(getFragmentManager(), "login");
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		// when the back arrow is clicked, home screen is launched but the service still runs in order not to lose data
		if ((keyCode == KeyEvent.KEYCODE_BACK))
		{
			if(D) Log.d(LOG_HEADER,"Quit");
			Intent homeIntent= new Intent(Intent.ACTION_MAIN);
			homeIntent.addCategory(Intent.CATEGORY_HOME);
			homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
			startActivity(homeIntent);
			finish();
		}
		return super.onKeyDown(keyCode, event);
	}


}

