// Application package
package ch.epfl.lsi.ironICpp;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;


public class PickerFragment extends DialogFragment {
	private String[] nums ;
	NumberPicker np;
	int initialPos;
	int layout ;
	int textView ;
	private int wich_parameter;
	private static Activity activity;
	static boolean setWE = false;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the Builder class for convenient dialog construction

		np = new NumberPicker(getActivity());
		np.setMinValue(0);
		np.setMaxValue(nums.length-1);
		np.setWrapSelectorWheel(false);
		np.setDisplayedValues(nums);
		np.setValue(initialPos);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage("Select the value:")
				.setView(np)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// send message to micro-controller in order to set the value of the chip at the wanted value
						Toast.makeText(getActivity(), "The value chosen is: " + nums[np.getValue()] , Toast.LENGTH_LONG).show();
						if(activity.toString().contains("ChipParametersActivity")&&(setWE != true)){
							LinearLayout l1 = (LinearLayout) getActivity().findViewById(layout);
							TextView text = (TextView) l1.findViewById(textView);
							text.setText(nums[np.getValue()]);
							ChipParametersActivity.selected_value(nums[np.getValue()],wich_parameter);
						}
						Log.d("Picker", "welectrode "+activity.toString());
						if(activity.toString().contains("MonitoringActivity")){
							Byte value=0;
							Log.d("Picker", "welectrode "+np.getValue());
							if(np.getValue()==0) value=0x30;
							else if(np.getValue()==1) value=0x31;
							else if(np.getValue()==2) value=0x32;
							Log.d("Picker", "welectrode "+value);
							MonitoringActivity.set_WE(value);
						}
						if(activity.toString().contains("ChipParametersActivity")&&(setWE == true)){
							Byte value=0;
							Log.d("Picker", "welectrode "+np.getValue());
							if(np.getValue()==0) value=0x30;
							else if(np.getValue()==1) value=0x31;
							else if(np.getValue()==2) value=0x32;
							Log.d("Picker", "welectrode "+value);
							ChipParametersActivity.set_WE(value);
							setWEwith(false);
							Log.d("Picker", "setWEwith "+setWE);
						}
					}
				})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// go back without do anything
						PickerFragment.this.getDialog().cancel();
					}
				});
		// Create the AlertDialog object and return it
		return builder.create();
	}
	public static void setWEwith(boolean value){
		Log.d("Picker", "setWEwith "+setWE);
		setWE = value;
	}
	public void setValueInPicker(String [] value, int pos, int layout, int id, int parameter,  Activity activity){
		this.layout = layout;
		this.textView = id;
		nums = new String [value.length];
		nums = value;
		initialPos = pos;
		wich_parameter = parameter;
		PickerFragment.activity = activity;
	}
	public void setValueInPicker(String [] value, Activity activity){
		nums = new String [value.length];
		nums = value;
		PickerFragment.activity = activity;
	}
}