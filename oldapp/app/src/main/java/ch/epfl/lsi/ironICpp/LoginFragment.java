// Application package
package ch.epfl.lsi.ironICpp;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

public class LoginFragment extends DialogFragment {
	protected static final String LOG_HEADER = "LoginDialog";
	protected static final boolean D = true;
	protected String [] choise = {"Calibration", "Settings"};

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();

		// Inflate and set the layout for the dialog
		// Pass null as the parent view because its going in the dialog layout
		builder.setView(inflater.inflate(R.layout.login_popup, null))
				// Add action buttons
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						EditText password = (EditText)LoginFragment.this.getDialog().findViewById(R.id.password);

						if(password.getText().toString().equals("a")) {
							if(D) Log.d(LOG_HEADER,"Login success");
							//LoginFragment.this.getDialog().cancel();
							showOptionDialog();
						}
						else{
							Toast.makeText(getActivity(), "Wrong password, try again", Toast.LENGTH_LONG).show();
							LoginFragment.this.getDialog().show();
						}
					}
				})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						LoginFragment.this.getDialog().cancel();
					}
				});
		return builder.create();
	}

	public void showOptionDialog() {

//		final  Context context = getActivity();
//		Log.d(LOG_HEADER,"getActivity "+ getActivity());
//		Log.d(LOG_HEADER,"getActivity "+ context);
//		AlertDialog.Builder builder = new AlertDialog.Builder(LoginFragment.this.getActivity());
//	    builder.setTitle(R.string.adavanced_options)
//	           .setItems(choise, new DialogInterface.OnClickListener() {
//	               public void onClick(DialogInterface dialog, int which) {
//	               // The 'which' argument contains the index position
//	               // of the selected item
//	            	   if(which==0){
//	            		   //Toast.makeText(getActivity().getApplicationContext(), "Calibration", Toast.LENGTH_LONG).show();
//	            		   Log.d(LOG_HEADER,"getActivity "+ context);
//			           		Intent intent = new Intent(context, CalibrationActivity.class);
//			           		intent.putExtra("STEP",0);
//			           		startActivity(intent);
//			           		}
//	            	   else {
//	            		   Toast.makeText(getActivity(), "Settings", Toast.LENGTH_LONG).show();
//	            		   Intent intent = new Intent(getActivity(), ChipParametersActivity.class);
//			           	   startActivity(intent);
//	            	   }
//	           }
//	    });
//	    builder.create();
//	    builder.show();
		AdvancedOptionFragment OptionFrag = new AdvancedOptionFragment();
		OptionFrag.show(getFragmentManager(), "AdvancedOption");
	}

}