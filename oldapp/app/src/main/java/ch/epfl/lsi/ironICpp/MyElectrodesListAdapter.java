package ch.epfl.lsi.ironICpp;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MyElectrodesListAdapter extends ArrayAdapter<String>
{
	private static final boolean D = true; // Debug flag used to marking down some informations in the Log register
	private static final String LOG_HEADER = "AdapterElectrodesList"; // Debug flag used to identify caller
	static ArrayList<String> arrayOfCheckedElectrodes = new ArrayList<String>();
	static ArrayList<String> arrayOfMonitoringFiles = new ArrayList<String>();
	Context context;
	List<String> taskList=new ArrayList<String>();
	//private MyMultitouchPlotAdapter mMultitouchPlotAdapter = null;
	static String ElectrodesToStore = "";
	static String FilesToStore = "";
	private SharedPreferences settings;


	public MyElectrodesListAdapter(Context context, int layoutResourceId,
								   List<String> objects, ArrayList<String> arrayOfCheckedElectrodes, ArrayList<String> arrayOfFiles,String electrodesToStore, SharedPreferences settings  ) {
		super(context,layoutResourceId, objects);
		//	this.layoutResourceId = layoutResourceId;
		this.taskList=objects;
		this.context=context;
		MyElectrodesListAdapter.arrayOfCheckedElectrodes = arrayOfCheckedElectrodes;
		MyElectrodesListAdapter.arrayOfMonitoringFiles = arrayOfFiles;
		MyElectrodesListAdapter.ElectrodesToStore = electrodesToStore;
		this.settings = settings;
	}


	/**This method will DEFINe what the view inside the list view will finally look like
	 * Here we are going to code that the checkbox state is the status of task and
	 * check box text is the task name
	 */

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		CheckBox chk = null;
		ImageButton btn = null;
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.list_inner_view,
					parent, false);
			chk = (CheckBox) convertView.findViewById(R.id.checkBox1);
			btn = (ImageButton) convertView.findViewById(R.id.btn_rec);

			if(btn!=null){
				btn.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// Start details: Activity Chart in order to access to stored files of the chosen metabolite.
						LinearLayout rl = (LinearLayout)v.getParent();
						CheckBox chbTemp = (CheckBox)rl.findViewById(R.id.checkBox1);
						String detailsOfMetabolite = chbTemp.getText().toString();

						Intent startIntent = new Intent(getContext(), ActivityChart.class);
						startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startIntent.putExtra("metabolite", detailsOfMetabolite);
						getContext().startActivity(startIntent);
						Toast.makeText(getContext(), "Recordings of "+detailsOfMetabolite, Toast.LENGTH_LONG).show();
					}
				});
			}
			convertView.setTag(chk);
			chk.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					CheckBox cb = (CheckBox) v;

					Toast.makeText(
							context,
							"Clicked on Checkbox: " + cb.getText() + " is "
									+ cb.isChecked(), Toast.LENGTH_SHORT)
							.show();
					if(cb.isChecked()){
						String electrodes2Add = cb.getText().toString();
						arrayOfCheckedElectrodes=addElectrodes(electrodes2Add);
					}
					else {
						if(D) Log.d(LOG_HEADER,"remove file");
						String electrodes2Remove = cb.getText().toString();
						arrayOfCheckedElectrodes=removeElectrodes(electrodes2Remove);
					}
				}
			});
		} else {
			chk = (CheckBox) convertView.getTag();
		}

		String current = taskList.get(position);
		chk.setText(current);

		chk.setChecked(getStatus(current,arrayOfCheckedElectrodes));
		chk.setTag(current);
		if(D) Log.d("listener", String.valueOf(current));

		return convertView;
	}

	private boolean getStatus(String current,ArrayList<String> array) {
		// TODO Auto-generated method stub

		if(array.size()!=0){
			if(D) Log.d(LOG_HEADER, "there is strings in settings"+ array);
			if(array.contains(current) ){
				return true;
			}
		}
		return false;
	}

	public ArrayList<String> removeElectrodes(String electrodesToRemove){

		arrayOfCheckedElectrodes.remove(electrodesToRemove);
		for (int i = 0; i<arrayOfMonitoringFiles.size(); i++){
			if(arrayOfMonitoringFiles.get(i).contains(electrodesToRemove))
				arrayOfMonitoringFiles.remove(i);
		}
		FilesToStore="";
		ElectrodesToStore="";
		if(arrayOfCheckedElectrodes.size()!=0){
			if(D) Log.d(LOG_HEADER, "electrodes: "+ arrayOfCheckedElectrodes);
			for (int i =0; i<arrayOfCheckedElectrodes.size(); i++)
			{
				if(!ElectrodesToStore.equals("")){
					ElectrodesToStore = ElectrodesToStore + "\t" + arrayOfCheckedElectrodes.get(i);
					if(i<arrayOfMonitoringFiles.size())
						FilesToStore = FilesToStore + "\t" + arrayOfMonitoringFiles.get(i);
				}
				else{
					ElectrodesToStore =  arrayOfCheckedElectrodes.get(i);
					if(i<arrayOfMonitoringFiles.size())
						FilesToStore = arrayOfMonitoringFiles.get(i);
				}
			}

		}
		Log.d(LOG_HEADER,"settings "+settings);
		SharedPreferences.Editor settingsEditor = settings.edit();
		settingsEditor.putString("electrodes", ElectrodesToStore);
		settingsEditor.putString("files", FilesToStore);
		settingsEditor.commit();
		return arrayOfCheckedElectrodes;
	}

	public ArrayList<String> addElectrodes(String electrodesToAdd){
		if(D) Log.d(LOG_HEADER,"add electrodes"+ electrodesToAdd);

		if (!arrayOfCheckedElectrodes.contains(electrodesToAdd))
		{
			arrayOfCheckedElectrodes.add(electrodesToAdd);
		}

		ElectrodesToStore="";

		for (int i =0; i<arrayOfCheckedElectrodes.size();i++)
		{
			if(!ElectrodesToStore.equals("")){
				ElectrodesToStore = ElectrodesToStore + "\t" + arrayOfCheckedElectrodes.get(i);
			}
			else{
				ElectrodesToStore = arrayOfCheckedElectrodes.get(i);
			}
		}

		SharedPreferences.Editor settingsEditor = settings.edit();
		settingsEditor.putString("electrodes", ElectrodesToStore);
		settingsEditor.commit();
		return arrayOfCheckedElectrodes;
	}

}

