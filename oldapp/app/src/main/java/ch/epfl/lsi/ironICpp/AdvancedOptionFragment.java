package ch.epfl.lsi.ironICpp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class AdvancedOptionFragment extends DialogFragment  {
	protected static final String LOG_HEADER = "AdvancedOption";
	protected String [] choise = {"Calibration", "Settings"};
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.adavanced_options)
				.setItems(choise, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// The 'which' argument contains the index position
						// of the selected item
						if(which==0){
							Toast.makeText(getActivity(), "Calibration", Toast.LENGTH_LONG).show();
							Intent intent = new Intent(getActivity(), CalibrationActivity.class);
							//Log.d(LOG_HEADER,"getActivity "+ getActivity());
							intent.putExtra("STEP",0);
							startActivity(intent);
						}
						else {
							Toast.makeText(getActivity(), "Settings", Toast.LENGTH_LONG).show();
							Intent intent = new Intent(getActivity(), ChipParametersActivity.class);
							startActivity(intent);
						}
					}
				});
		return builder.create();
	}
}