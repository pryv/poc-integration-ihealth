package ch.epfl.lsi.ironICpp;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/**
 * @author Francesca Sradolini (francesca.stadolini@epfl.ch)
 */

class MyMultitouchPlotAdapter extends ArrayAdapter<String> {
	protected static final String LOG_HEADER = null;
	private static final boolean D = true;
	List<String> filenames;
	Context context;
	ArrayList<MultitouchPlot> ListOfMultitouchPlot ;
	boolean firstStart=true;


	public MyMultitouchPlotAdapter(Context context, int resId, List<String> filenames) {
		super(context, resId, filenames);
		this.context=context;
		this.ListOfMultitouchPlot = new ArrayList<MultitouchPlot>();
		this.filenames = new ArrayList<String>();
		if(filenames != null)
			this.filenames.addAll(filenames);
	}

	@Override
	public int getCount() {
		return filenames.size();
	}

	@Override
	public View getView(int pos, View convertView, ViewGroup parent) {
		LayoutInflater inf = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View v = convertView;
		if (v == null) {
			v = inf.inflate(R.layout.listview_example_item, parent, false);
		}

		//Creation of a new Multitouchplot with the data and title
		MultitouchPlot p = (MultitouchPlot) v.findViewById(R.id.multitouchPlot);
		p.setTitle("Plot of " + filenames.get(pos));

		// Decimal format of the value on y axes
		p.setRangeValueFormat(new DecimalFormat("#"));
		//p.setOffset(1024);
		//evita che si loaddi se non esiste ancora il file
		if(filenames.get(pos)!=MonitoringActivity.listOfMetabolites.get(0)&&!firstStart){
			p.loadFile(filenames.get(pos));
			Log.d(LOG_HEADER, "plot"+p);
			if(!ListOfMultitouchPlot.contains(p))
				ListOfMultitouchPlot.add(p);
			Log.d(LOG_HEADER, " size of list "+ ListOfMultitouchPlot.size());
		}
		else
			firstStart=false;

		return v;
	}

	public void reloadAllFilteringPrefs(Context context){
		// if you want to filter all the plot in the same way
		for(int i = 0;i<ListOfMultitouchPlot.size();i++){
			if(D) Log.d(LOG_HEADER,"listOfMultitouchPlot "+ListOfMultitouchPlot.size());
			ListOfMultitouchPlot.get(i).reloadFilteringPrefs(context);
		}
	}
	public void reloadFilteringPrefs(Context applicationContext, int plotID) {
		// TODO new filter parameter are setted only for one plot
		Log.d(LOG_HEADER, "reload filter prefs about "+ plotID);
		ListOfMultitouchPlot.get(plotID).reloadFilteringPrefs(context);
	}

	public void notifyDataSetChanged(ArrayList<String> arrayOfCheckedFiles) {
		// TODO two possibilities: you have add or remove one filename to visualize, so you have to update the list of the adapter
		if(D) Log.d(LOG_HEADER,"DataSetChanged");
		if (filenames.size()!= arrayOfCheckedFiles.size() ){
			ReloadThePlotList(arrayOfCheckedFiles);
		}
		//or the data inside the file continue to change (ex Fakebluetooth)
		else super.notifyDataSetChanged();
	}

	public void ReloadThePlotList(ArrayList<String> arrayOfCheckedFiles) {
		// TODO two possibilities: you have add or remove one filename to visualize, so you have to update the list of the adapter

		filenames.clear();
		filenames.addAll(arrayOfCheckedFiles);
//			ListOfMultitouchPlot.clear();
//			ListOfMultitouchPlot=null;
//			ListOfMultitouchPlot=new ArrayList<MultitouchPlot>();
		super.notifyDataSetChanged();

	}

}

