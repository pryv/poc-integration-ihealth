// Application package
package ch.epfl.lsi.ironICpp;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Messenger;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;
import ch.epfl.lsi.ironICpp.FragmentFiltering.SettingsChangedListener;

/**
 * @author Francesca Sradolini (francesca.stadolini@epfl.ch)
 */

public class ActivityChart extends Activity implements SettingsChangedListener {
	/**
	 * This activity displays the data as it's being acquired. It is fetched by the
	 * DataProvider class. Data acquisition is triggered by the ActionBar buttons,
	 * with the relevant actions "forwarded" to the correct class thanks to the
	 * DataProvider.
	 * TODO: Move that documentation elsewhere in the correct class:
	 * It is the main thread and it generates two son thread mutually exclusive: one in charge of requesting the 
	 * connection, another one in charge of managing the connection. Therefore this part of the software is 
	 * composed by two threads: the main thread and one of two connection thread. They communicate each other 
	 * through handler object, passed by the main thread to the son. Once the thread in charge of managing the 
	 * connection passed the datum to the main thread, this last draw it on the chart.
	 *
	 */


	/**
	 * Class constants and parameters.
	 */
	// Debug flags.
	private static final boolean D = true; // Debug flag used to marking down some informations in the Log register
	private static final String LOG_HEADER = "ActivityChart"; // Debug flag used to identify caller

	// Preferences file
	private static final String PREFS_NAME = "IronicChartPrefs";
	private static SharedPreferences settings;

	// Software parameters.
	public static final int M = 90; // Moving average filter window.

	// Menu
	static Menu menu;

	// Left drawer
	private DrawerLayout myDrawerLayout = null;
	private ListView myDrawerList = null;
	private String[] files = null;
	static MyListFileAdapter myfileAdapter = null;

	// Data service provider
	Messenger mService = null;
	boolean mIsBound;
	static String filenamesToStore = "";
	//all the checked files are shown in a different MultiplotTouch
	static ArrayList<String> arrayOfCheckedFiles = new ArrayList<String>();
	static String metaboliteName = "";
	static ArrayList<String> arrayOfCheckedElectrodes = new ArrayList<String>();

	//listView of plots
	private ListView mPlotList;
	private MyMultitouchPlotAdapter myMultitouchPlotAdapter = null;

	/**
	 * Class methods.
	 */
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
		setContentView(R.layout.activity_chart);

		// Load checked files
		settings = getSharedPreferences(PREFS_NAME, 0);
		FragmentFiltering.setFilteringPrefsName(PREFS_NAME);
		MultitouchPlot.setFilteringPrefsName(PREFS_NAME);
		FragmentFiltering.setFilteringActivity(this);
		Log.d(LOG_HEADER, "prefs name "+ MultitouchPlot.PREFS_NAME.toString()+" "+ FragmentFiltering.PREFS_NAME.toString()+" "+PREFS_NAME);
		// Take the data from the activity caller
		savedInstanceState = getIntent().getExtras();

		if(savedInstanceState!=null){
			metaboliteName = savedInstanceState.getString("metabolite");
			arrayOfCheckedElectrodes.add(metaboliteName);
		}


		getActionBar().setTitle("Stored files about: " + metaboliteName);
		//getActionBar().setTitle("IronIC++");
		// Load stored files in the settings 
		String filenames = settings.getString("filenames of "+ metaboliteName, "");
		String[] filenamesArray = filenames.split("\t");

		for(int i=0; i<filenamesArray.length; i++){
			if(!filenamesArray[i].equals("") && filenamesArray[i].contains(metaboliteName)){
				if (!arrayOfCheckedFiles.contains(filenamesArray[i])){
					arrayOfCheckedFiles.add(filenamesArray[i]);
				}
			}
		}

		mPlotList = (ListView) findViewById(R.id.listView2);
		myMultitouchPlotAdapter = new MyMultitouchPlotAdapter(getApplicationContext(),R.layout.listview_example_item, arrayOfCheckedFiles);
		Log.d(LOG_HEADER,"myMultitouchplotAdapter "+ myMultitouchPlotAdapter);
		mPlotList.setAdapter(myMultitouchPlotAdapter);


		// Populate the drawer
		myDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		myDrawerList = (ListView) findViewById(R.id.right_drawer);
		listRecordings();
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

	}


	@Override
	public void onDestroy() {
		/**
		 * The final call you receive before your activity is destroyed. 
		 */

		super.onDestroy();

		if(D) Log.d(LOG_HEADER, "onDestroy()");

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
		inflater.inflate(R.menu.activity_chart_menu, menu);

		ActivityChart.menu = menu;

		return true;
	}

	@SuppressLint("InlinedApi") @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/**
		 * This method is called whenever an item in the options menu is selected.
		 * @param item  The item selected in the options menu. 
		 */

		if(D) Log.d(LOG_HEADER, "onOptionsItemSelected method");

		// In every case, close the eventually opened drawer
		int drawerGravity = Gravity.END;
		myDrawerLayout.closeDrawer(drawerGravity);

		// Manage ActionBar events
		switch (item.getItemId()) {
			case R.id.filtering:
				DialogFragment filteringSettingsFragment = new FragmentFiltering();
				filteringSettingsFragment.show(getFragmentManager(), "filtering");
				return true;
			case R.id.load:
				if(!myDrawerLayout.isDrawerOpen(drawerGravity))
				{
					myDrawerLayout.openDrawer(drawerGravity);
				}
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

		return true;
	}

	private void listRecordings(){
		/**
		 * This method lists the existing recordings from a 
		 * removable storage media (such as an SD card) or an internal 
		 * (non-removable) storage. Files saved to the external storage
		 * are world-readable and can be modified by the user when 
		 * they enable USB mass storage to transfer files on a computer.
		 *
		 */

		// Clear existing list
		files = new String[0];

		// Checking media availability.
		String state = Environment.getExternalStorageState();
		if(!Environment.MEDIA_MOUNTED.equals(state))
		{
			Toast.makeText(getApplicationContext(), R.string.storage_unavailable, Toast.LENGTH_LONG).show();
		}

		// Create an application folder if not present.
		File appFolder = new File(Environment.getExternalStorageDirectory() + "/IronicCells");
		if(!appFolder.exists()) appFolder.mkdir();

		// Read files.
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				//if((filename.contains("ChipParamenters")||filename.contains(metaboliteName))&&(filename.endsWith(".txt") || filename.endsWith(".csv"))) return true;
				//if(filename.contains("Glucose")||filename.contains("Lactate")) return true;
				if(filename.contains("ChipParamenters")||filename.contains(metaboliteName)) return true;
				return false;
			}
		};

		// List files and sort by inverse alphabetical order
		files = appFolder.list(filter);
		if(files == null)
		{
			return;
		}
		List<String> listFiles = Arrays.asList(files);
		Collections.sort(listFiles);
		Collections.reverse(listFiles);
		files = (String[]) listFiles.toArray();

		List<String> list = new ArrayList<String>();

		for(int i=0;i<files.length;i++){
			list.add(new String(files[i]));
		}
		//Log.d(LOG_HEADER,"myMultitouchplotAdapter "+ myMultitouchPlotAdapter);
		myfileAdapter=new MyListFileAdapter(getApplicationContext(), R.layout.list_files_inner_view, list, arrayOfCheckedFiles, myMultitouchPlotAdapter,filenamesToStore, settings, this);
		myDrawerList.setAdapter(
				myfileAdapter
		);
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if ((keyCode == KeyEvent.KEYCODE_BACK))
		{
			if(D) Log.d(LOG_HEADER,"Quit");
			Intent i= new Intent(this,MonitoringActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
			arrayOfCheckedFiles.clear();
			startActivity(i);
			finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onFinishSettingsChanged() {
		// TODO Auto-generated method stub
		Log.d(LOG_HEADER, "prefs name "+ PREFS_NAME);
		int plotID = settings.getInt("plotID",-1);
		if(plotID==-1){
			Log.d(LOG_HEADER,"errore");
		}
		Boolean plotIDEnabled=settings.getBoolean("plotIDEnabled", false);
		if(plotIDEnabled)
			myMultitouchPlotAdapter.reloadFilteringPrefs(getApplicationContext(), plotID-1);
		else
			myMultitouchPlotAdapter.reloadAllFilteringPrefs(getApplicationContext());
	}
}
