package ch.epfl.lsi.ironICpp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MyListFileAdapter extends ArrayAdapter<String>
{
	private static final boolean D = true; // Debug flag used to marking down some informations in the Log register
	private static final String LOG_HEADER = "AdapterFileList"; // Debug flag used to identify caller
	static final int MSG_REMOVE_CONFIRMATION = 7;
	static ArrayList<String> arrayOfCheckedFiles = new ArrayList<String>();
	static Context context;
	List<String> taskList=new ArrayList<String>();
	private MyMultitouchPlotAdapter mMultitouchPlotAdapter = null;
	static String filenamesToStore = "";
	private SharedPreferences settings;
	private Activity activity;


	public MyListFileAdapter(Context context, int layoutResourceId,
							 List<String> objects, ArrayList<String> arrayOfCheckedFiles,MyMultitouchPlotAdapter mMultitouchPlotAdapter,String filenamesToStore, SharedPreferences settings, Activity activity  ) {
		super(context,layoutResourceId, objects);
		this.taskList=objects;
		MyListFileAdapter.context = context;
		MyListFileAdapter.arrayOfCheckedFiles = arrayOfCheckedFiles;
		this.mMultitouchPlotAdapter = mMultitouchPlotAdapter;
		MyListFileAdapter.filenamesToStore = filenamesToStore;
		this.settings = settings;
		this.activity = activity;


	}

	/**This method will DEFINe what the view inside the list view will finally look like
	 * Here we are going to code that the checkbox state is the status of task and
	 * check box text is the task name
	 */

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		CheckBox chk = null;
		ImageButton btn_remove = null;

		if (convertView == null) {

			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.list_files_inner_view,
					parent, false);
			chk = (CheckBox) convertView.findViewById(R.id.checkBox2);
			btn_remove = (ImageButton) convertView.findViewById(R.id.btn_remove);
			convertView.setTag(chk);
			btn_remove.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View convertView) {
					// TODO Auto-generated method stub			
					LinearLayout rl = (LinearLayout)convertView.getParent();
					CheckBox chbTemp = (CheckBox)rl.findViewById(R.id.checkBox2);

					// Showing Alert Message
					String fileRemove= taskList.get(position);
					showAlert(activity,"Do you really want to remove: "+chbTemp.getText().toString()+"?",fileRemove,chbTemp);

				}
			});

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
						String file2Add = cb.getText().toString();
						arrayOfCheckedFiles = addfileName(file2Add);
					}
					else {
						if(D) Log.d(LOG_HEADER,"remove file");
						String file2Remove = cb.getText().toString();
						arrayOfCheckedFiles = removeFileName(file2Remove);
					}

					if(D) Log.d(LOG_HEADER,"Multiplotadapter "+ mMultitouchPlotAdapter );
					if(mMultitouchPlotAdapter!=null)
						mMultitouchPlotAdapter.notifyDataSetChanged(arrayOfCheckedFiles);
				}
			});
		} else {
			chk = (CheckBox) convertView.getTag();
		}

		String current = taskList.get(position);
		chk.setText(current);

		chk.setChecked(getStatus(current,arrayOfCheckedFiles));
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

	public ArrayList<String> removeFileName(String fileToRemove){

		arrayOfCheckedFiles.remove(fileToRemove);

		filenamesToStore="";
		if(arrayOfCheckedFiles.size()!=0){
			if(D) Log.d(LOG_HEADER, "filenames: "+ arrayOfCheckedFiles);
			for (int i =0; i<arrayOfCheckedFiles.size();i++)
			{
				if(!filenamesToStore.equals("")){
					filenamesToStore = filenamesToStore + "\t" + arrayOfCheckedFiles.get(i);
				}
				else
					filenamesToStore =  arrayOfCheckedFiles.get(i);
			}

		}
		Log.d(LOG_HEADER,"settings "+settings);
		SharedPreferences.Editor settingsEditor = settings.edit();
		settingsEditor.putString("filenames of "+ ActivityChart.metaboliteName, filenamesToStore);
		settingsEditor.commit();
		return arrayOfCheckedFiles;
	}

	public ArrayList<String> addfileName(String fileToAdd){
		if(D) Log.d(LOG_HEADER,"add file"+ fileToAdd);

		if (!arrayOfCheckedFiles.contains(fileToAdd))
		{
			arrayOfCheckedFiles.add(fileToAdd);
		}

		filenamesToStore="";

		for (int i =0; i<arrayOfCheckedFiles.size();i++)
		{
			if(!filenamesToStore.equals("")){
				filenamesToStore = filenamesToStore + "\t" + arrayOfCheckedFiles.get(i);
			}
			else{
				filenamesToStore = arrayOfCheckedFiles.get(i);
			}
		}

		SharedPreferences.Editor settingsEditor = settings.edit();
		settingsEditor.putString("filenames of "+ ActivityChart.metaboliteName, filenamesToStore);
		settingsEditor.commit();
		return arrayOfCheckedFiles;
	}

	public void showAlert(final Activity activity, String message, final String filetoRemove, final CheckBox chbTemp) {

		TextView title = new TextView(activity);
		title.setText("Title");
		title.setPadding(10, 10, 10, 10);
		title.setGravity(Gravity.CENTER);
		title.setTextColor(Color.WHITE);
		title.setTextSize(20);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setIcon(R.drawable.trash_bin_icon)
				.setTitle("Remove")
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// ((FragmentAlertDialog)getActivity()).doPositiveClick();
								Log.d(LOG_HEADER,"yes");

								//Remove File from external storage
								String sdcard = Environment.getExternalStorageState();
								if(!Environment.MEDIA_MOUNTED.equals(sdcard))
								{
									Toast.makeText(context, R.string.storage_unavailable, Toast.LENGTH_SHORT).show();
								}
								File AppFolder = new File(Environment.getExternalStorageDirectory() + "/IronicCells");
								if(AppFolder.exists()){
									File remove = new File(AppFolder,filetoRemove);//line2
									remove.delete();

									Log.d(LOG_HEADER,"delete one file from the DrawerList " + remove.getPath());
									//Remove File from the list in the Drawer
									taskList.remove(filetoRemove);
									notifyDataSetChanged();

									if(arrayOfCheckedFiles.contains(chbTemp.getText().toString())){
										arrayOfCheckedFiles = removeFileName(chbTemp.getText().toString());
										if(mMultitouchPlotAdapter!=null){
											Log.d(LOG_HEADER,"delete one plot from the PlotList ");
											mMultitouchPlotAdapter.notifyDataSetChanged(arrayOfCheckedFiles);
										}
									}
								}

							}
						}
				)
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								Log.d(LOG_HEADER,"no");
								dialog.cancel();
							}
						}
				).create();

		builder.setMessage(message);

		builder.setCancelable(true);


		AlertDialog alert = builder.create();
		alert.show();
	}
}



