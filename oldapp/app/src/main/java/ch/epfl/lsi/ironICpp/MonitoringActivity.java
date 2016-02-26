package ch.epfl.lsi.ironICpp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
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

import com.androidplot.xy.XYPlot;

/**
 * @author Francesca Sradolini (francesca.stadolini@epfl.ch)
 */

public class MonitoringActivity extends Activity implements SettingsChangedListener {
    /**
     * This activity displays the data as it's being acquired. It is fetched by the
     * DataProvider class. Data acquisition is triggered by the ActionBar buttons,
     * with the relevant actions "forwarded" to the correct class thanks to the
     * DataProvider.
     * TODO: Move that documentation elsewhere in the correct class:
     s	 * connection, another one in charge of managing the connection. Therefore this part of the software is
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
    private static final String LOG_HEADER = "MonitoringActivity"; // Debug flag used to identify caller
    private static final String EXTRA_MESSAGE = "Electrodes";
    static List<String> listOfMetabolites = new ArrayList<String>();

    // Preferences file
    private static final String PREFS_NAME = "Ironic1Prefs";
    private static SharedPreferences settings;

    // Software parameters.
    public static final int M = 90; // Moving average filter window.

    // Menu
    private static Menu menu;

    // Left drawer
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private MyElectrodesListAdapter listOfMetabolitesAdapter = null;

    // Data service provider
    Messenger mService = null;
    boolean mIsBound;
    static String electrodesToStore = "";
    //all the checked files are shown in a different MultiplotTouch
    static ArrayList<String> arrayOfCheckedElectrodes = new ArrayList<String>();
    static ArrayList<String> arrayOfMonitoringFiles = new ArrayList<String>();

    // Bluetooth
    private static BluetoothConnector BTconnector ;
    private static final int ENABLE_BLUETOOTH = 1; // Message code for bluetooth enable request
    private static final int CONNECT_DEVICE = 2; // Message code for bluetooth connection to device

    //list of plots
    static MultitouchPlot signalPlot1;
    static MultitouchPlot signalPlot2;
    static MultitouchPlot signalPlot3;
    static MultitouchPlot signalPlot4;
    static MultitouchPlot signalPlot5;
    static MultitouchPlot signalPlot6;
    static MultitouchPlot signalPlot7;
    boolean[] electrodes = new boolean [7];

    //Calibration values
    static float m1=1, m2=1, m3=1, m4=1, m5=1, m6=1, m7=1;
    static float q1=0, q2=0, q3=0, q4=0, q5=0, q6=0, q7=0;
    static Byte working_electrode;
    PickerFragment PicFrag = new PickerFragment();
    static int numberofplot;
    static int minutes;

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

        // Take the data from the activity caller
        if(savedInstanceState!=null)
        {
            numberofplot = savedInstanceState.getInt("numberofplot");
            arrayOfMonitoringFiles = savedInstanceState.getStringArrayList("files");
        }

        else
            savedInstanceState = getIntent().getExtras();

        minutes = getIntent().getExtras().getInt("minutes") ;
        if(D) Log.d(LOG_HEADER,"minutes "+minutes);

        if(D) Log.d(LOG_HEADER,"number of plots "+numberofplot);
        // Set layout content.
        switch(numberofplot){
            case 0:
                setContentView(R.layout.monitoring_activity_0);
                break;
            case 1:
                setContentView(R.layout.monitoring_activity_1);
                break;
            case 2:
                setContentView(R.layout.monitoring_activity_2);
                break;
            case 3:
                setContentView(R.layout.monitoring_activity_3);
                break;
            case 4:
                setContentView(R.layout.monitoring_activity_4);
                break;
            case 5:
                setContentView(R.layout.monitoring_activity_5);
                break;
            case 6:
                setContentView(R.layout.monitoring_activity_6);
                break;
            case 7:
                setContentView(R.layout.monitoring_activity_7);
                break;
        }

        getActionBar().setTitle("Monitoring");

        // prefsname
        settings = getSharedPreferences(PREFS_NAME, 0);
        FragmentFiltering.setFilteringPrefsName(PREFS_NAME);
        MultitouchPlot.setFilteringPrefsName(PREFS_NAME);
        FragmentFiltering.setFilteringActivity(this);
        Log.d(LOG_HEADER, "fragment filtering activity "+ FragmentFiltering.activity);

        if(listOfMetabolites==null)
            listOfMetabolites=new ArrayList<String>();

        if (!listOfMetabolites.contains("Glucose"))
            listOfMetabolites.add(new String("Glucose"));
        if (!listOfMetabolites.contains("Lactate"))
            listOfMetabolites.add(new String("Lactate"));
        if (!listOfMetabolites.contains("Bilirubin"))
            listOfMetabolites.add(new String("Bilirubin"));
        if (!listOfMetabolites.contains("Sodium"))
            listOfMetabolites.add(new String("Sodium"));
        if (!listOfMetabolites.contains("Potassium"))
            listOfMetabolites.add(new String("Potassium"));
        if (!listOfMetabolites.contains("Temperature"))
            listOfMetabolites.add(new String("Temperature"));
        if (!listOfMetabolites.contains("PH"))
            listOfMetabolites.add(new String("PH"));

        if(savedInstanceState!=null)
            electrodes = savedInstanceState.getBooleanArray("electrodes");

        if(arrayOfCheckedElectrodes==null)
            arrayOfCheckedElectrodes=new ArrayList<String>();
        if(arrayOfMonitoringFiles==null)
            arrayOfMonitoringFiles=new ArrayList<String>();

        Log.d(LOG_HEADER,"list "+arrayOfCheckedElectrodes);
        for(int i=0; i<electrodes.length; i++){
            if(electrodes[i]){
                Log.d(LOG_HEADER,"status electrodes:"+listOfMetabolites.get(i));
                if (!arrayOfCheckedElectrodes.contains(listOfMetabolites.get(i))){
                    arrayOfCheckedElectrodes.add(listOfMetabolites.get(i));
                }
            }
        }

        BTconnector = new BluetoothConnector(getApplicationContext(),arrayOfCheckedElectrodes,this);

        // Populate the drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.right_drawer);

        listOfMetabolitesAdapter= new MyElectrodesListAdapter(getApplicationContext(), R.layout.list_inner_view, listOfMetabolites, arrayOfCheckedElectrodes,arrayOfMonitoringFiles, electrodesToStore, settings);

        mDrawerList.setAdapter(
                listOfMetabolitesAdapter
        );

        switch(numberofplot){
            case 7:
                signalPlot7 = (MultitouchPlot) findViewById(R.id.multitouchPlot7);
                signalPlot7.setTitle("Plot of "+arrayOfCheckedElectrodes.get(6) );
                signalPlot7.setBorderStyle(XYPlot.BorderStyle.SQUARE, null, null);
                if(arrayOfMonitoringFiles.size()==7){
                    signalPlot7.loadFile(arrayOfMonitoringFiles.get(6));
                }
            case 6:
                signalPlot6 = (MultitouchPlot) findViewById(R.id.multitouchPlot6);
                signalPlot6.setTitle("Plot of "+arrayOfCheckedElectrodes.get(5) );
                signalPlot6.setBorderStyle(XYPlot.BorderStyle.SQUARE, null, null);
                if(arrayOfMonitoringFiles.size()==6){
                    signalPlot6.loadFile(arrayOfMonitoringFiles.get(5));
                }
            case 5:
                signalPlot5 = (MultitouchPlot) findViewById(R.id.multitouchPlot5);
                signalPlot5.setTitle("Plot of "+arrayOfCheckedElectrodes.get(4) );
                signalPlot5.setBorderStyle(XYPlot.BorderStyle.SQUARE, null, null);
                if(arrayOfMonitoringFiles.size()==5){
                    signalPlot5.loadFile(arrayOfMonitoringFiles.get(4));
                }
            case 4:
                signalPlot4 = (MultitouchPlot) findViewById(R.id.multitouchPlot4);
                signalPlot4.setTitle("Plot of "+arrayOfCheckedElectrodes.get(3) );
                signalPlot4.setBorderStyle(XYPlot.BorderStyle.SQUARE, null, null);
                if(arrayOfMonitoringFiles.size()==4){
                    signalPlot4.loadFile(arrayOfMonitoringFiles.get(3));
                }
            case 3:
                signalPlot3 = (MultitouchPlot) findViewById(R.id.multitouchPlot3);
                signalPlot3.setTitle("Plot of "+arrayOfCheckedElectrodes.get(2) );
                signalPlot3.setBorderStyle(XYPlot.BorderStyle.SQUARE, null, null);
                if(arrayOfMonitoringFiles.size()==3){
                    signalPlot3.loadFile(arrayOfMonitoringFiles.get(2));
                }
            case 2:
                signalPlot2 = (MultitouchPlot) findViewById(R.id.multitouchPlot2);
                signalPlot2.setTitle("Plot of "+arrayOfCheckedElectrodes.get(1) );
                signalPlot2.setBorderStyle(XYPlot.BorderStyle.SQUARE, null, null);
                if(arrayOfMonitoringFiles.size()==2){
                    signalPlot2.loadFile(arrayOfMonitoringFiles.get(1));
                }
            case 1:
                signalPlot1 = (MultitouchPlot) findViewById(R.id.multitouchPlot1);
                signalPlot1.setTitle("Plot of "+arrayOfCheckedElectrodes.get(0) );
                signalPlot1.setBorderStyle(XYPlot.BorderStyle.SQUARE, null, null);
                signalPlot1.setDomainValueFormat(new DecimalFormat("0.000"));
                if(arrayOfMonitoringFiles.size()==1){
                    signalPlot1.loadFile(arrayOfMonitoringFiles.get(0));
                }
                break;
        }

        // Checking media availability.
        String state = Environment.getExternalStorageState();
        if(!Environment.MEDIA_MOUNTED.equals(state))
        {
            Toast.makeText(getApplicationContext(), R.string.storage_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        //save actual state
        savedInstanceState.putInt("numberofplot", numberofplot);
        savedInstanceState.putBooleanArray("electrodes", electrodes);
        arrayOfMonitoringFiles.clear();
        switch (numberofplot) {
            case 1:
                arrayOfMonitoringFiles.add(signalPlot1.getFileName());
                if(numberofplot==1)	break;
            case 2:
                arrayOfMonitoringFiles.add(signalPlot2.getFileName());
                if(numberofplot==2)	break;
            case 3:
                arrayOfMonitoringFiles.add(signalPlot3.getFileName());
                if(numberofplot==3)	break;
            case 4:
                arrayOfMonitoringFiles.add(signalPlot4.getFileName());
                if(numberofplot==4)	break;
            case 5:
                arrayOfMonitoringFiles.add(signalPlot5.getFileName());
                if(numberofplot==5)	break;
            case 6:
                arrayOfMonitoringFiles.add(signalPlot6.getFileName());
                if(numberofplot==6)	break;
            case 7:
                arrayOfMonitoringFiles.add(signalPlot7.getFileName());
                break;
        }
        savedInstanceState.putStringArrayList("files", arrayOfMonitoringFiles);
        super.onSaveInstanceState(savedInstanceState);
    }
    public static void ReloadThePlotList(String filename){
        //refresh the plot
        Log.d(LOG_HEADER,"filename "+filename);
        if(filename!=null){
            switch(numberofplot){
                case 7:
                    if (filename.contains(arrayOfCheckedElectrodes.get(6))){
                        signalPlot7.reloadFile();
                        break;
                    }
                case 6:
                    if (filename.contains(arrayOfCheckedElectrodes.get(5))){
                        signalPlot6.reloadFile();
                        break;
                    }
                case 5:
                    if (filename.contains(arrayOfCheckedElectrodes.get(4))){
                        signalPlot5.reloadFile();
                        break;
                    }
                case 4:
                    if (filename.contains(arrayOfCheckedElectrodes.get(3))){
                        signalPlot4.reloadFile();
                        break;
                    }
                case 3:
                    if (filename.contains(arrayOfCheckedElectrodes.get(2))){
                        signalPlot3.reloadFile();
                        break;
                    }
                case 2:
                    if (filename.contains(arrayOfCheckedElectrodes.get(1))){
                        signalPlot2.reloadFile();
                        break;
                    }
                case 1:
                    if (filename.contains(arrayOfCheckedElectrodes.get(0))){
                        signalPlot1.reloadFile();
                        break;
                    }
            }
        }
    }

    public static void LoadThePlotList(String filename){
        //reload the plot file
        Log.d(LOG_HEADER,"filename "+filename);
        if(filename!=null){
            switch(numberofplot){
                case 7:
                    if (filename.contains(arrayOfCheckedElectrodes.get(6))){
                        signalPlot7.loadFile(filename);
                        break;
                    }
                case 6:
                    if (filename.contains(arrayOfCheckedElectrodes.get(5))){
                        signalPlot6.loadFile(filename);
                        break;
                    }
                case 5:
                    if (filename.contains(arrayOfCheckedElectrodes.get(4))){
                        signalPlot5.loadFile(filename);
                        break;
                    }
                case 4:
                    if (filename.contains(arrayOfCheckedElectrodes.get(3))){
                        signalPlot4.loadFile(filename);
                        break;
                    }
                case 3:
                    if (filename.contains(arrayOfCheckedElectrodes.get(2))){
                        signalPlot3.loadFile(filename);
                        break;
                    }
                case 2:
                    if (filename.contains(arrayOfCheckedElectrodes.get(1))){
                        signalPlot2.loadFile(filename);
                        break;
                    }
                case 1:
                    if (filename.contains(arrayOfCheckedElectrodes.get(0))){
                        signalPlot1.loadFile(filename);
                        break;
                    }
            }
        }
    }

    public static void set_WE(Byte value){
        //message for set the WE on the platform
        working_electrode = value;
        BTconnector.set_electrode(working_electrode);
    }

    public static void set_m_q(String cal_file, int we){
        List<String> line1 = getFileHandle(cal_file);
        switch(we){
            case 1:
                m1 = Float.parseFloat(line1.get(line1.size()-4));
                q1 = Float.parseFloat(line1.get(line1.size()-3));
                if(D) Log.d(LOG_HEADER,"for WE "+ we +" calculated m = "+ m1 +" and q = "+ q1 +" by: "+ cal_file);
                break;
            case 2:
                m2 = Float.parseFloat(line1.get(line1.size()-4));
                q2 = Float.parseFloat(line1.get(line1.size()-3));
                if(D) Log.d(LOG_HEADER,"for WE "+ we +"calculated m = "+ m2 +" and q = "+q2 +" by: "+ cal_file);
                break;
            case 3:
                m3 = Float.parseFloat(line1.get(line1.size()-4));
                q3 = Float.parseFloat(line1.get(line1.size()-3));
                if(D) Log.d(LOG_HEADER,"for WE "+ we +"calculated m = "+ m3 +" and q = "+q3 +" by: "+ cal_file);
                break;
            case 4:
                m4 = Float.parseFloat(line1.get(line1.size()-4));
                q4 = Float.parseFloat(line1.get(line1.size()-3));
                if(D) Log.d(LOG_HEADER,"for WE "+ we +"calculated m = "+ m4 +" and q = "+q4 +" by: "+ cal_file);
                break;
            case 5:
                m5 = Float.parseFloat(line1.get(line1.size()-4));
                q5 = Float.parseFloat(line1.get(line1.size()-3));
                if(D) Log.d(LOG_HEADER,"for WE "+ we +"calculated m = "+ m5 +" and q = "+q5 +" by: "+ cal_file);
                break;
            case 6:
                m6 = Float.parseFloat(line1.get(line1.size()-4));
                q6 = Float.parseFloat(line1.get(line1.size()-3));
                if(D) Log.d(LOG_HEADER,"for WE "+ we +"calculated m = "+ m6 +" and q = "+q6 +" by: "+ cal_file);
                break;
            case 7:
                m7 = Float.parseFloat(line1.get(line1.size()-4));
                q7 = Float.parseFloat(line1.get(line1.size()-3));
                if(D) Log.d(LOG_HEADER,"for WE "+ we +"calculated m = "+ m7 +" and q = "+q7 +" by: "+ cal_file);
                break;
        }
    }

    private static List<String> getFileHandle(String fileName) {
        // Checking media availability.
        String state = Environment.getExternalStorageState();
        if(!Environment.MEDIA_MOUNTED.equals(state))
        {
            Log.e(LOG_HEADER,"Not available external memory!");
        }
        // Load Calibration file
        String path = Environment.getExternalStorageDirectory() + "/IronicCells/"+ fileName ;
        Log.d(LOG_HEADER,"path"+ path);
        try
        {
            FileInputStream instream = new FileInputStream(new File(path));
            Log.d(LOG_HEADER,"path"+ path);
            InputStreamReader inputreader = new InputStreamReader(instream);
            @SuppressWarnings("resource")
            BufferedReader buffreader = new BufferedReader(inputreader);
            List<String> line1 = new  ArrayList<String>() ;
            String line;
            try
            {
                while ((line = buffreader.readLine()) != null)
                {
                    String[] splitStr = line.split("\\s+");
                    line1.add(splitStr[0]);
                    line1.add(splitStr[1]);
                }
                return line1;
            }catch (Exception e)
            {
                e.printStackTrace();
                return null;
            }

        }catch (Exception e)
        {
            e.printStackTrace();
            return null;
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
        if( BTconnector!=null)
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
        if(BTconnector!=null)
            BTconnector.onPause(false);
    }

    @Override
    public void onDestroy() {
        /**
         * The final call you receive before your activity is destroyed.
         */

        super.onDestroy();

        if(D) Log.d(LOG_HEADER, "onDestroy()");
        listOfMetabolites = null;
        arrayOfCheckedElectrodes = null;
        arrayOfMonitoringFiles = null;
        if(BTconnector!=null)
            BTconnector.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            // do something on back.
            Intent intent = new Intent(this, StartActivity.class);

            intent.putStringArrayListExtra(EXTRA_MESSAGE, arrayOfCheckedElectrodes);

            startActivity(intent);
            return true;
        }

        return super.onKeyDown(keyCode, event);
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
        inflater.inflate(R.menu.activity_monitoring_menu, menu);

        MonitoringActivity.menu = menu;
        boolean btConnect = settings.getBoolean("btConnected", false);
        updateActionBar(btConnect);

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
        if(mDrawerLayout!=null)
            mDrawerLayout.closeDrawer(drawerGravity);

        // Manage ActionBar events
        Intent i = new Intent(this, DataService.class);
        switch (item.getItemId()) {
            case R.id.filtering:
                DialogFragment filteringSettingsFragment = new FragmentFiltering();
                filteringSettingsFragment.show(getFragmentManager(), "filtering");
                return true;
            case R.id.connect_bt:
                BTconnector.Connect(false);
                return true;
            case R.id.disconnect_bt:
                BTconnector.Disconnect();
                return true;
            case R.id.b:
                BTconnector.send_start_command(i,false);
                return true;
            case R.id.newfile:
                if(D) Log.i(LOG_HEADER, "Chose 'newfile'");
                Intent intent = new Intent(this, DataService.class);
                BTconnector.request_new_file(intent);
                return true;
            case R.id.start_stream:
                if(D) Log.i(LOG_HEADER, "Chose 'start'");

//			// Perform action on click
//			String[] nums = {"WE1=0x30", "WE2=0x31","WE3=0x32"};		            	
//			PicFrag.setValueInPicker(nums, this );
//			PicFrag.show(getFragmentManager(), "pickerFragment");
//			Log.d("Picker", "welectrode1 "+ working_electrode);

                //0x50 message for informing the microcontroller that next message is the minutes for each measuring
                BTconnector.send_value((byte) 0x50);
                BTconnector.send_value((byte) minutes);
                //0x51 message for informing the microcontroller that next message is the number and the name of wanted metabolite
                BTconnector.send_value((byte) 0x51);
                BTconnector.send_value((byte) numberofplot);
                for(int j = 0; j<numberofplot; j++){
                    switch(Electrodes.valueOf(arrayOfCheckedElectrodes.get(j))){
                        case Glucose:
                            BTconnector.send_value((byte) 0x13);
                            break;
                        case Lactate:
                            BTconnector.send_value((byte)0x14);
                            break;
                        case Bilirubin:
                            BTconnector.send_value((byte)0x15);
                            break;
                        case Sodium:
                            BTconnector.send_value((byte)0x16);
                            break;
                        case Potassium:
                            BTconnector.send_value((byte)0x17);
                            break;
                        case PH:
                            BTconnector.send_value((byte)0x18);
                            break;
                        case Temperature:
                            BTconnector.send_value((byte)0x19);
                            break;
                    }
                }

                BTconnector.send_start_command(i,false);
                return true;
            case R.id.stop:
                if(D) Log.i(LOG_HEADER, "Chose 'stop'");
                BTconnector.send_stop_command(i);
                return true;
            case R.id.AddMeasurements:
                if(!mDrawerLayout.isDrawerOpen(drawerGravity)&&(mDrawerLayout!=null))
                {
                    mDrawerLayout.openDrawer(drawerGravity);
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

    public static void Save_in_Settings(ArrayList<String> arrayOfMonitoringFiles) {
        // method to store the visualized data.
        //a single string is created and all the file name are stored toghether separated by \t char
        String FileToStore="";
        if(arrayOfMonitoringFiles.size()!=0){
            for (int i =0; i<arrayOfMonitoringFiles.size();i++)
            {
                if(!FileToStore.equals("")){
                    FileToStore = FileToStore + "\t" + arrayOfMonitoringFiles.get(i);
                }
                else
                    FileToStore =  arrayOfMonitoringFiles.get(i);
            }

        }
        Log.d(LOG_HEADER,"settings "+settings);
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.putString("files", FileToStore);
        settingsEditor.commit();
    }

    static void updateActionBar(boolean btConnected) {
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.putBoolean("btConnected", btConnected);
        settingsEditor.commit();

        if(MonitoringActivity.menu != null)
        {
            if(btConnected)
            {
                Log.d(LOG_HEADER,"connect modality");
                // Display only the relevant actions
                menu.findItem(R.id.connect_bt).setVisible(false);
                menu.findItem(R.id.AddMeasurements).setVisible(false);
                menu.findItem(R.id.disconnect_bt).setVisible(true);
                menu.findItem(R.id.newfile).setVisible(true);
                menu.findItem(R.id.b).setVisible(true);
                menu.findItem(R.id.start_stream).setVisible(true);
                menu.findItem(R.id.stop).setVisible(true);
            }
            else
            {
                Log.d(LOG_HEADER,"disconnect modality");
                // Display only the relevant actions
                menu.findItem(R.id.connect_bt).setVisible(true);
                menu.findItem(R.id.AddMeasurements).setVisible(true);
                menu.findItem(R.id.disconnect_bt).setVisible(false);
                menu.findItem(R.id.newfile).setVisible(false);
                menu.findItem(R.id.b).setVisible(false);
                menu.findItem(R.id.start_stream).setVisible(false);
                menu.findItem(R.id.stop).setVisible(false);
            }
        }
    }

    @Override
    public void onFinishSettingsChanged() {
        // TODO refresh of filtering prefs
        int plotID = settings.getInt("plotID", 1);
        Boolean plotIDEnabled=settings.getBoolean("plotIDEnabled", false);
        Log.d(LOG_HEADER, "prefs name "+ PREFS_NAME+" "+plotID+" "+plotIDEnabled);
        if(plotIDEnabled){
            //only one plot has to modify the filtering prefs
            switch(numberofplot){
                case 7:
                    if (plotID==7){
                        signalPlot7.reloadFilteringPrefs(getApplicationContext());
                        break;
                    }
                case 6:
                    if (plotID==6){
                        signalPlot6.reloadFilteringPrefs(getApplicationContext());
                        break;
                    }
                case 5:
                    if (plotID==5){
                        signalPlot5.reloadFilteringPrefs(getApplicationContext());
                        break;
                    }
                case 4:
                    if (plotID==4){
                        signalPlot4.reloadFilteringPrefs(getApplicationContext());
                        break;
                    }
                case 3:
                    if (plotID==3){
                        signalPlot3.reloadFilteringPrefs(getApplicationContext());
                        break;
                    }
                case 2:
                    if (plotID==2){
                        signalPlot2.reloadFilteringPrefs(getApplicationContext());
                        break;
                    }
                case 1:
                    if (plotID==1){
                        signalPlot1.reloadFilteringPrefs(getApplicationContext());
                        break;
                    }
            }
        }

        else {
            //all the plot have to reload filtering prefs
            switch(numberofplot){
                case 7:
                    signalPlot7.reloadFilteringPrefs(getApplicationContext());
                case 6:
                    signalPlot6.reloadFilteringPrefs(getApplicationContext());
                case 5:
                    signalPlot5.reloadFilteringPrefs(getApplicationContext());
                case 4:
                    signalPlot4.reloadFilteringPrefs(getApplicationContext());
                case 3:
                    signalPlot3.reloadFilteringPrefs(getApplicationContext());
                case 2:
                    signalPlot2.reloadFilteringPrefs(getApplicationContext());
                case 1:
                    signalPlot1.reloadFilteringPrefs(getApplicationContext());
                    break;
            }
        }
    }

}
