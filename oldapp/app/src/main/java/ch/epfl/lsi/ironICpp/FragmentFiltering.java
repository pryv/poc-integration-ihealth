package ch.epfl.lsi.ironICpp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


public class FragmentFiltering extends DialogFragment {
	static String PREFS_NAME = "Ironic1Prefs";
	static Activity activity;

	public interface SettingsChangedListener {
		// Used for giving back data to ActivityChart
		void onFinishSettingsChanged();
	}

	public static void setFilteringPrefsName(String name_settings){
		PREFS_NAME = name_settings;
	}

	public static void setFilteringActivity(Activity actualactivity){
		activity = actualactivity;
	}

	@SuppressLint("InflateParams") @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Handling settings
		final SharedPreferences settings = this.getActivity().getSharedPreferences(PREFS_NAME,0);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();

		// Inflate the layout
		final View ll = inflater.inflate(R.layout.fragment_filtering, null, false);

		// Widget storage
		final TextView dataScale = (TextView) ll.findViewById(R.id.editTextScale);
		final TextView dataOffset = (TextView) ll.findViewById(R.id.editTextOffset);
		final TextView dataMax = (TextView) ll.findViewById(R.id.editTextMax);
		final TextView dataMin = (TextView) ll.findViewById(R.id.editTextMin);
		final Spinner filterID = (Spinner) ll.findViewById(R.id.spinnerFilter);
		final SeekBar filterLength = (SeekBar) ll.findViewById(R.id.seekBarFilterLength);

		final ToggleButton plotIDEnabled = (ToggleButton) ll.findViewById(R.id.toggleButtonPlot);

		final NumberPicker plotID = (NumberPicker) ll.findViewById(R.id.numberPicker1);
		if(!FragmentFiltering.activity.toString().contains("ChipParametersActivity")){
			int dimension=0;
			MultitouchPlot.isPause = true;
			plotID.setMinValue(1);
			if(FragmentFiltering.activity.toString().contains("MonitoringActivity")){
				Log.d("Filtering","set the maximum value of the piker"+MonitoringActivity.arrayOfCheckedElectrodes.size());
				plotID.setMaxValue(MonitoringActivity.arrayOfCheckedElectrodes.size());
				dimension = MonitoringActivity.arrayOfCheckedElectrodes.size();
			}
			if(FragmentFiltering.activity.toString().contains("ActivityChart")){
				Log.d("Filtering","set the maximum value of the piker"+ActivityChart.arrayOfCheckedFiles.size());
				plotID.setMaxValue(ActivityChart.arrayOfCheckedFiles.size());
				dimension = ActivityChart.arrayOfCheckedFiles.size();
			}
			String[] nums = new String[dimension];
			for(int i=0; i<nums.length; i++)
				nums[i] = Integer.toString(i+1);
			plotID.setWrapSelectorWheel(false);
			plotID.setDisplayedValues(nums);
			// Set widget state according to settings in the application
			plotID.setValue(settings.getInt("plotID", 0));
			plotIDEnabled.setChecked(settings.getBoolean("plotIDEnabled", false));
		}
		else {
			Log.d("Filtering","chipParameters activity, only one plot available");
			plotID.setVisibility(View.INVISIBLE);
			plotIDEnabled.setVisibility(View.INVISIBLE);
		}

		final ToggleButton dataScaleEnabled = (ToggleButton) ll.findViewById(R.id.toggleButtonScale);
		final ToggleButton dataOffsetEnabled = (ToggleButton) ll.findViewById(R.id.toggleButtonOffset);
		final ToggleButton dataMaxEnabled = (ToggleButton) ll.findViewById(R.id.toggleButtonMax);
		final ToggleButton dataMinEnabled = (ToggleButton) ll.findViewById(R.id.toggleButtonMin);
		final ToggleButton filterIDEnabled = (ToggleButton) ll.findViewById(R.id.toggleButtonFilter);


		// Set widget state according to settings in the application
		dataScale.setText(String.valueOf(settings.getFloat("dataScale", 1)));
		dataOffset.setText(String.valueOf(settings.getFloat("dataOffset", 0)));
		dataMax.setText(String.valueOf(settings.getFloat("dataMax", 1024)));
		dataMin.setText(String.valueOf(settings.getFloat("dataMin", 0)));
		filterID.setSelection(settings.getInt("filterID", 0));
		filterLength.setProgress(settings.getInt("filterLength", 100));


		dataScaleEnabled.setChecked(settings.getBoolean("dataScaleEnabled", false));
		dataOffsetEnabled.setChecked(settings.getBoolean("dataOffsetEnabled", false));
		dataMaxEnabled.setChecked(settings.getBoolean("dataMaxEnabled", false));
		dataMinEnabled.setChecked(settings.getBoolean("dataMinEnabled", false));
		filterIDEnabled.setChecked(settings.getBoolean("filterIDEnabled", false));


		// Set the layout for the dialog
		builder.setView(ll)
				// Add action buttons
				.setPositiveButton(getResources().getString(R.string.filter_apply), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id)
					{
						// Save preferences
						SharedPreferences.Editor editor = settings.edit();

						editor.putFloat("dataScale", Float.parseFloat(dataScale.getText().toString()));
						editor.putFloat("dataOffset", Float.parseFloat(dataOffset.getText().toString()));
						editor.putFloat("dataMax", Float.parseFloat(dataMax.getText().toString()));
						editor.putFloat("dataMin", Float.parseFloat(dataMin.getText().toString()));
						editor.putInt("filterID", filterID.getSelectedItemPosition());
						editor.putInt("filterLength", filterLength.getProgress());
						editor.putInt("plotID", Integer.parseInt(String.valueOf(plotID.getValue())));

						editor.putBoolean("dataScaleEnabled", dataScaleEnabled.isChecked());
						editor.putBoolean("dataOffsetEnabled", dataOffsetEnabled.isChecked());
						editor.putBoolean("dataMaxEnabled", dataMaxEnabled.isChecked());
						editor.putBoolean("dataMinEnabled", dataMinEnabled.isChecked());
						editor.putBoolean("filterIDEnabled", filterIDEnabled.isChecked());
						editor.putBoolean("plotIDEnabled", plotIDEnabled.isChecked());

						editor.commit();
						MultitouchPlot.isPause = false;
						// Notify user and update display
						Toast.makeText(getActivity(), getResources().getString(R.string.filter_saved), Toast.LENGTH_SHORT).show();
						SettingsChangedListener activity = (SettingsChangedListener) getActivity();
						activity.onFinishSettingsChanged();
					}
				})
				.setNegativeButton(getResources().getString(R.string.filter_cancel), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						MultitouchPlot.isPause = false;
						// Notify user
						Toast.makeText(getActivity(), getResources().getString(R.string.filter_canceled), Toast.LENGTH_SHORT).show();

						FragmentFiltering.this.getDialog().cancel();
					}
				});
		return builder.create();
	}
}