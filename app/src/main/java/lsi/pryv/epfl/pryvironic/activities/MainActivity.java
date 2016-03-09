package lsi.pryv.epfl.pryvironic.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import lsi.pryv.epfl.pryvironic.R;
import lsi.pryv.epfl.pryvironic.utils.Connector;

public class MainActivity extends AppCompatActivity {
    private final static int BLUETOOTH_PAIRING = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Connector.initiateConnection();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.bluetooth_devices:
                Intent intent = new Intent(this,BluetoothPairingActivity.class);
                startActivityForResult(intent, BLUETOOTH_PAIRING);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Log out")
                .setMessage("You are about to log out. Continue?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        logout();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void logout() {
        LoginActivity.resetCreditentials();
        BluetoothPairingActivity.connectedDevice = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == BLUETOOTH_PAIRING) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Connected to : " + data.getExtras().getString(BluetoothPairingActivity.BLUETOOTH_NAME), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, data.getExtras().getString(BluetoothPairingActivity.BLUETOOTH_ERROR), Toast.LENGTH_SHORT).show();
            }
        }
    }

}