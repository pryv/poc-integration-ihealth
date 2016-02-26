package ch.epfl.lsi.ironICpp;

//Android object definitions.
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

public class MultitouchPlot extends XYPlot implements OnTouchListener{
	/**
	 * MultitouchPlot is a subclass of the XYPlot class belonging to AndroidPlot library.
	 * This class implements a plot with the same characteristics of the XYPlot class, but 
	 * moreover is able to manage touch event such as single touch and multiple touches.
	 * Therefore Scroll and Zoom operations are possible.
	 *
	 */

	/**
	 * Class constants.
	 */

	// Debug flags.
	private static final boolean D = true;
	private static final String LOG_HEADER = "MultitouchPlot";

	// Preferences file
	static String PREFS_NAME ;
	private SharedPreferences settings;

	// File reading values
	private static int CHART_MAX_SAMPLES_DISPLAYED = 800;
	private static final int LINE_LENGTH = 15;

	/**
	 * Class members.
	 */
	// Plot extreme values
	private float minXValueOfPlot = Float.NaN;
	private float maxXValueOfPlot = Float.NaN;
	private boolean freeZoomAndScroll = true;

	// Touch-related variables
	private float scrollingShiftX = 0;
	private float zoomingScale = 1;
	private boolean lockTouch = false;

	// Data file
	static final int[] columns = {8, 5};
	private static final long Nan = 0;
	private String currentFile;
	private long numberOfLines;
	private float minXValueOfSeries;
	private float minXValueOfSeriesNoFilter;
	private float maxXValueOfSeries;
	private static Filter filter;

	// CPU usage limitation
	private boolean chartUpdateAllowed = true;

	// Caching settings to speed up reading
	private boolean filterIDEnabled;
	private int filterLength;
	private boolean dataScaleEnabled;
	private boolean dataOffsetEnabled;
	private float dataScale;
	private float dataOffset;
	private boolean dataMinEnabled;
	private boolean dataMaxEnabled;
	private float dataMin;
	private float dataMax;
	public float R_gain = 350000;	//350 KOhm
	public float delta_step = (float) (3.7/1024); //1024 digital values. and the maximum voltage is 3,3V
	public float offset = (float)205; //1024;	// set with the minimum y value in the next code// 20% 3.7V
	// Pseudo-caching for data read on disk
	ArrayList<ArrayList<Float>> columnsArray = new ArrayList<ArrayList<Float>>();
	static RandomAccessFile oldFile;
	static long oldLineNumber;
	static int oldNumberOfLinesToRead;
	private static SimpleXYSeries series;
	static Context context;
	static Activity activity;
	static boolean isPause = false;
	RandomAccessFile file;

	/**
	 * Class methods.
	 */
	// Class constructor type 1.
	@SuppressLint("ClickableViewAccessibility") public MultitouchPlot(Context context, AttributeSet attributeset) {
		super(context, attributeset);
		this.setOnTouchListener(this);
		reloadFilteringPrefs(context);
	}
	// Class constructor type 2.
	@SuppressLint("ClickableViewAccessibility") public MultitouchPlot(Context context, String title) {
		super(context, title);
		this.setOnTouchListener(this);
		reloadFilteringPrefs(context);
	}
	// Class constructor type 3.
	@SuppressLint("ClickableViewAccessibility") public MultitouchPlot(Context context, AttributeSet attributeset, int defStyle) {
		super(context, attributeset, defStyle);
		this.setOnTouchListener(this);
		reloadFilteringPrefs(context);
	}

	public static void setFilteringPrefsName(String setting_name){
		PREFS_NAME = setting_name;
	}

	public void reloadFilteringPrefs(Context context) {
		settings = context.getSharedPreferences(PREFS_NAME, 0);
		Log.d(LOG_HEADER, "prefs name "+ PREFS_NAME);
		filter = new Filter();
		filter.setFilter(
				settings.getInt("filterID", Filter.FILTER_MOVING_AVERAGE),
				settings.getFloat("filterStrength", 0.1F));

		// Store locally settings to speed up access during file reading
		filterIDEnabled = settings.getBoolean("filterIDEnabled", false);
		filterLength = settings.getInt("filterLength", 100);
		dataScaleEnabled = settings.getBoolean("dataScaleEnabled", false);
		dataOffsetEnabled = settings.getBoolean("dataOffsetEnabled", false);
		dataScale = settings.getFloat("dataScale", 1);
		dataOffset = settings.getFloat("dataOffset", 0);
		dataMinEnabled = settings.getBoolean("dataMinEnabled", false);
		dataMaxEnabled = settings.getBoolean("dataMaxEnabled", false);
		dataMin = settings.getFloat("dataMin", 0);
		dataMax = settings.getFloat("dataMax", 0);

		if(dataMinEnabled)
		{
			setRangeLowerBoundary(dataMin, BoundaryMode.FIXED);
			Log.d("MultitouchPlot", "fixed min value "+dataMin);
		}
		else
		{
			setRangeLowerBoundary(dataMin, BoundaryMode.AUTO);
		}
		if(dataMaxEnabled)
		{
			setRangeUpperBoundary(dataMax, BoundaryMode.FIXED);
			Log.d("MultitouchPlot", "fixed MAX value "+dataMax);
		}
		else
		{
			setRangeUpperBoundary(dataMax, BoundaryMode.AUTO);
		}

		// Reload the file to have the display-able bounds matching the
		// filtering length if we have already an opened file
		if(currentFile != null)
		{
			Log.w(LOG_HEADER, "Load file called from reloadFilteringPrefs");
			loadFile(currentFile);
		}
	}

	private void startRefreshTimer()
	{
		Timer timer = new Timer();
		timer.schedule(new TimerTask()
		{

			@Override
			public void run() {
				chartUpdateAllowed = true;
			}
		}, 300);
	}

	// Touch event manager.
	@SuppressLint("ClickableViewAccessibility") @Override
	public boolean onTouch(View v, MotionEvent event) {
		// Disable touch event theft from scrolling list
		v.getParent().requestDisallowInterceptTouchEvent(true);
		switch(event.getActionMasked()){
			case MotionEvent.ACTION_MOVE:
				if(event.getPointerCount() == 1 && event.getHistorySize() > 1){
					// Computation of scrolling shift.
					scrollingShiftX = event.getHistoricalX(0) - event.getX();
					float scrollingShiftY = event.getHistoricalY(0) - event.getY();
					if(!lockTouch && Math.abs(scrollingShiftY) > Math.abs(scrollingShiftX))
					{
						lockTouch = false;
						// Release touch lock
						v.getParent().requestDisallowInterceptTouchEvent(false);
						return true;
					}
					else
					{
						lockTouch = true;
						updatePlotBounds();
						if(D) Log.i(LOG_HEADER, "scrollingShiftX: "+scrollingShiftX);
					}
				}
				else if(event.getPointerCount() == 2 && event.getHistorySize() > 1)
				{
					// Computation of zooming scale factor on x-direction.
					zoomingScale = Math.abs((event.getHistoricalX(0, 0) - event.getHistoricalX(1, 0)) / (event.getX(0) - event.getX(1)));
					updatePlotBounds();
					if(D) Log.i(LOG_HEADER, "zoomingScale: "+zoomingScale);
				}
				else if(event.getPointerCount() == 3)
				{
					Toast.makeText(getContext(), R.string.zoom_reset, Toast.LENGTH_SHORT).show();
					minXValueOfPlot = minXValueOfSeries;
					maxXValueOfPlot = maxXValueOfSeries;
					setDomainBoundaries(minXValueOfPlot/1000, maxXValueOfPlot/1000, BoundaryMode.FIXED);
				}

				// Plot redrawing
				if(!filterIDEnabled)
				{
					// Live update of data only when filtering inactive
					// otherwise, too much work is done
					reloadSeriesAndRedraw();
				}
				break;

			case MotionEvent.ACTION_UP: // Last finger released
				if(D) Log.i(LOG_HEADER, "scrollingShiftX");
				reloadSeriesAndRedraw();
				// Release touch lock
				lockTouch = false;
				v.getParent().requestDisallowInterceptTouchEvent(false);
				break;
		}

		return true;
	}

	// Display update onTouch event
	private void updatePlotBounds(){
		if(D) Log.d(LOG_HEADER, "Old Plot bounds = " + minXValueOfPlot + ", " + maxXValueOfPlot);

		float newXMin = minXValueOfPlot;
		float newXMax = maxXValueOfPlot;

		if(D) Log.w(LOG_HEADER, "Temp plot bounds (init.) = " + newXMin/1000 + ", " + newXMax/1000);

		// Scroll
		if(scrollingShiftX != 0)
		{
			// Scroll factor
			float scrollingFactor = 2*(maxXValueOfPlot-minXValueOfPlot)/this.getWidth();
			// Scrolling factor depends on zoom with a slight acceleration
			scrollingShiftX *= scrollingFactor;
			newXMin += scrollingShiftX;
			newXMax += scrollingShiftX;
			if(D) Log.w(LOG_HEADER, "Temp plot bounds (scroll) = " + newXMin/1000 + ", " + newXMax/1000);
			scrollingShiftX = 0;
		}

		// Zoom
		if(zoomingScale != 1)
		{
			float midPoint = (minXValueOfPlot+maxXValueOfPlot)/2;
			newXMin = (minXValueOfPlot-midPoint)*zoomingScale+midPoint;
			newXMax = (maxXValueOfPlot-midPoint)*zoomingScale+midPoint;
			if(D) Log.w(LOG_HEADER, "Temp plot bounds (zoom) = " + newXMin/1000 + ", " + newXMax/1000);
			zoomingScale = 1;
		}

		// Apply constraints if needed
		if(!freeZoomAndScroll)
		{
			newXMin = Math.max(newXMin, Math.max(minXValueOfPlot, minXValueOfSeries));
			newXMax = Math.min(newXMax, Math.min(maxXValueOfPlot, maxXValueOfSeries));
		}

		if(D) Log.w(LOG_HEADER, "Temp plot bounds (cnstrnd) = " + newXMin/1000 + ", " + newXMax/1000);

		minXValueOfPlot = newXMin;
		maxXValueOfPlot = newXMax;

		if(D) Log.d(LOG_HEADER, "New Plot bounds = " + minXValueOfPlot + ", " + maxXValueOfPlot);

		// Apply modifications
		setDomainBoundaries(minXValueOfPlot/1000, maxXValueOfPlot/1000, BoundaryMode.FIXED);
		redraw();
	}

	/** Loads metadata of the file: min x-axis, max x-axis, file size */
	public void reloadFile() {
		if(D) Log.w(LOG_HEADER, "Reload in progress");
		// Reloading file, automatic zoom if unzoomed (useful for live view)
		if(chartUpdateAllowed)
		{
			if(D) Log.i(LOG_HEADER, "Reload allowed");
			Log.w(LOG_HEADER, "Load file called from reloadFile");
			loadFile(currentFile);
		}
		else
		{
			if(D) Log.w(LOG_HEADER, "Reload denied");
		}
		chartUpdateAllowed = false;
		startRefreshTimer();
	}

	public void removeFile(String fileName){
		if(currentFile.equals(fileName)){
			currentFile = null;
		}
		clear();
		redraw();
	}

	/** Loads metadata of the file: min x-axis, max x-axis */
	public void loadFile(String filename) {
		Log.e(LOG_HEADER, "Load file "+ filename);
		// Set current file
		currentFile = filename;
		File fileHandle = getFileHandle(currentFile);

		// Remember view parameters
		float oldMaxXValueOfSeries = maxXValueOfSeries;

		if(fileHandle != null)
		{
			// Guess how much data we do have: A = fileSizeInBytes / 15 bytesPerLine
			numberOfLines = fileHandle.length()/LINE_LENGTH;

			//if(D) Log.v(LOG_HEADER, "Number of samples (lines) detected: "+ numberOfLines);
			//if(D) Log.v(LOG_HEADER, "file length: "+ fileHandle.length());

			try {
				// Open file for random access reading
				file = new RandomAccessFile(fileHandle, "r");

				// Read first and last values (x-axis scaling)
				minXValueOfSeries = minXValueOfSeriesNoFilter = readDataFromLinesInFile(file, 0, 1).get(0).get(0);
				maxXValueOfSeries = readDataFromLinesInFile(file, numberOfLines-1, 1).get(0).get(0);

				if(D) Log.i(LOG_HEADER, "Raw series range (x-axis) " + minXValueOfSeries + " to " + maxXValueOfSeries);

				// Adapt the bounds to the filter (if filter is 100 measures long, the first 100 values can NEVER be shown!)
				if(filterIDEnabled)
				{
					// Filter is enabled, restriction of the domain has to be set
					minXValueOfSeries += (filterLength-1)*((maxXValueOfSeries-minXValueOfSeries)/(numberOfLines-1));
				}

				if(D) Log.i(LOG_HEADER, "Filtered series range (x-axis) " + minXValueOfSeries + " to " + maxXValueOfSeries);

				// Set plot bounds if they are not
				if(Float.isNaN(minXValueOfPlot))
				{
					minXValueOfPlot = minXValueOfSeries;
				}
				if(Float.isNaN(maxXValueOfPlot))
				{
					maxXValueOfPlot = maxXValueOfSeries;
				}

				// Scroll display is data previously in screen goes out (previous data in view, new is out)
				if(oldMaxXValueOfSeries < maxXValueOfPlot && maxXValueOfSeries > maxXValueOfPlot)
				{
					if(D) Log.e(LOG_HEADER, "Moving display");
					float totalScrolling = 0.9F*(maxXValueOfPlot-minXValueOfPlot);
					minXValueOfPlot += totalScrolling;
					maxXValueOfPlot += totalScrolling;
					if(D) Log.e(LOG_HEADER, "Finished scolling");
					setDomainBoundaries(minXValueOfPlot/1000, maxXValueOfPlot/1000, BoundaryMode.FIXED);
				}

				// Close file handle
				//file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}finally {
				if(file!= null){
					try {
						file.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				System.gc();
			}
			if(!isPause)
				reloadSeriesAndRedraw();
		}
	}

	private void reloadSeriesAndRedraw() {

		File fileHandle = getFileHandle(currentFile);
		removeSeries(series);
		clear();
		series = null;
		System.gc();
		series = new SimpleXYSeries(currentFile);

		try {
			// Open file for random access reading
			RandomAccessFile file = new RandomAccessFile(fileHandle, "r");

			// Random-access read the file
			if(D) Log.i(LOG_HEADER, "Plot    Min, max = " + minXValueOfPlot/1000 + " " + maxXValueOfPlot/1000);
			if(D) Log.i(LOG_HEADER, "Series  Min, max = " + minXValueOfSeries/1000 + " " + maxXValueOfSeries/1000);
			if(D) Log.v(LOG_HEADER,
					"filter = "+getResources().getStringArray(R.array.filters_array)[filter.getFilterID()]
							+"   getFilterEnabled = "
							+filterIDEnabled
							+"   filterLength = "+filterLength);
			float min, max;
//			if(false) // TRUE for adaptative resolution, false for constant resolution 
//			{
//				min = Math.max(minXValueOfPlot, minXValueOfSeries);
//				max = Math.min(maxXValueOfPlot, maxXValueOfSeries);
//			}
//			else
//			{
			min = minXValueOfPlot;
			max = maxXValueOfPlot;
//			}
			for(int i=0; i < CHART_MAX_SAMPLES_DISPLAYED; i++)
			{
				float xValueApproximate = (max-min)*i/(CHART_MAX_SAMPLES_DISPLAYED-1) + min;
				float xValue = getColumnValueAroundXPosition(file, xValueApproximate, 0);
				float yValue = getColumnValueAroundXPosition(file, xValueApproximate, 1);

//				if (yValue<offset)
//					offset=yValue;

				if(!Float.isNaN(xValue) && !Float.isNaN(yValue))
				{
					Log.d(LOG_HEADER,"y value "+ yValue+ " " +(yValue-offset)*delta_step/R_gain);
					series.addLast(xValue/1000, ((yValue-offset)*delta_step/R_gain)*1000000000);
					// if you receive a digital value, you have to use this conversion: ((yValue-offset)*delta_step/R_gain)*1000000000 nA
				}
			}

			// Close file handle
			//file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			if(file!= null){
				try {
					file.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			System.gc();
		}

		addSeries(series, new LineAndPointFormatter(
				Color.HSVToColor(new float[]{348,81,90}),//Color.HSVToColor(new float[]{85,255,50}),   {85,255,128}
				null,
				null, null));

		// Allow new display
		redraw();
		chartUpdateAllowed = true;
	}

	// Converts an axis value to the corresponding line number
	private long getLineNumberFromAxisValue(float xValue) {
		//if(D) Log.v(LOG_HEADER, "Want xValue = "+ xValue +" "+ minXValueOfSeriesNoFilter+" "+ maxXValueOfSeries+" "+minXValueOfSeriesNoFilter);
		if (maxXValueOfSeries-minXValueOfSeriesNoFilter != 0){
			long lineNumber =
					(long)((numberOfLines*xValue - minXValueOfSeriesNoFilter)/(maxXValueOfSeries-minXValueOfSeriesNoFilter));
			return lineNumber;
		}
		else
			return Nan;

	}

	// Get X or Y value around a given approximate X value
	private float getColumnValueAroundXPosition(RandomAccessFile file, float xValueApproximate, int column) {

		long seekLine = getLineNumberFromAxisValue(xValueApproximate);
		if(seekLine == Nan){
			return Float.NaN;
		}
		else{
			int numberOfLinesToRead;
			if(filterIDEnabled)
			{
				numberOfLinesToRead = filterLength;
			}
			else
			{
				numberOfLinesToRead = 1;
			}

			// Read the data from the file
			ArrayList<ArrayList<Float>> dataRead = readDataFromLinesInFile(file, seekLine, numberOfLinesToRead);
			if(dataRead.size() > 0)
			{
				if(column != 0 && filterIDEnabled)
				{
					return filter.filter(dataRead.get(column));
				}
				else
				{
					return dataRead.get(column).get(0);
				}
			}
			else
			{
				return Float.NaN;
			}
		}
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

	// Reads data from lines in file, with the number 'm' of lines going backwards (lines n-m to line n)
	// Otherwise, return Float.NaN
	private ArrayList<ArrayList<Float>> readDataFromLinesInFile(RandomAccessFile file, long lineNumber, int numberOfLinesToRead) {
		//if(D) Log.v(LOG_HEADER, "Asked for line "+lineNumber);

		// Create an array of Float arrays (one per column) 
		if(lineNumber < 0 || numberOfLinesToRead <= 0 || lineNumber >= numberOfLines)
		{
			// We know we're out of bounds
			//if(D) Log.w(LOG_HEADER, "Exceeded file bounds: line = "+(lineNumber-(numberOfLinesToRead-1))+". Should be in [0, "+(numberOfLines-1)+"], "+numberOfLinesToRead+" lines to read.");
			ArrayList<ArrayList<Float>> columns = new ArrayList<ArrayList<Float>>();
			ArrayList<Float> temp = new ArrayList<Float>();
			temp.add(Float.NaN);
			for(int i=0; i<MultitouchPlot.columns.length; i++)
			{
				columns.add(temp);
			}
			return columns;
		}

		// If data already read previously, return previous result
		if(file == oldFile && lineNumber == oldLineNumber && numberOfLinesToRead == oldNumberOfLinesToRead)
		{
			//if(false) Log.i(LOG_HEADER, "Data already in memory, fast return ("+lineNumber+", "+numberOfLinesToRead+")");
			return columnsArray;
		}
		else
		{
			for(int i=0;i<columnsArray.size();i++){
				columnsArray.get(i).clear() ;
			}
			columnsArray.clear();
			oldFile = file;
			oldLineNumber = lineNumber;
			oldNumberOfLinesToRead = numberOfLinesToRead;
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
			//Log.i(LOG_HEADER, "Reading column "+column+" out of "+(columns.length-1));

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

				// Validate data
				if(column > 0)
				{
					// Applying scale (if needed)
					if(dataScaleEnabled)
					{
						value *= dataScale;
					}

					// Applying offset (if needed)
					if(dataOffsetEnabled)
					{
						value += dataOffset;
					}

					// Add NaN is data if out of bounds
					if(
							(dataMinEnabled && value < dataMin)
									||
									(dataMaxEnabled && value > dataMax)
							)
					{
						value = Float.NaN;
					}
				}

				col.add(value);
			}

			//Log.i(LOG_HEADER, "Add column to array: "+col);

			columnsArray.add(col);

			// Set offset to next column plus separator char
			offset += columns[column]+1;
		}
		return columnsArray;
	}
	public void setRgain(float value) {
		// TODO Auto-generated method stub
		R_gain=value;
	}
	public void setOffset(float i) {
		// TODO Auto-generated method stub
		offset = i;
	}

	public String getFileName(){
		return currentFile;
	}


}

