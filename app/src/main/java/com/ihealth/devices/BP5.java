package com.ihealth.devices;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ihealth.R;
import com.ihealth.communication.control.Bp5Control;
import com.ihealth.communication.control.BpProfile;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.utils.Connector;
import com.pryv.api.model.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Activity for testing Bp5 device. 
 */
public class BP5 extends Activity {
	private Bp5Control bp5Control;
	private String deviceMac;
	private int clientCallbackId;
	private TextView tv_return;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bp5);

		Connector.initiateConnection();

		Intent intent = getIntent();
		deviceMac = intent.getStringExtra("mac");
		
		clientCallbackId = iHealthDevicesManager.getInstance().registerClientCallback(iHealthDevicesCallback);
		/* Limited wants to receive notification specified device */
		iHealthDevicesManager.getInstance().addCallbackFilterForDeviceType(clientCallbackId, iHealthDevicesManager.TYPE_BP5);
		/* Get bp5 controller */
		bp5Control = iHealthDevicesManager.getInstance().getBp5Control(deviceMac);

		tv_return = (TextView)findViewById(R.id.tv_return);
	}

	@Override
	protected void onDestroy() {
		reset();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		reset();
		super.onBackPressed();
	}

	private void reset() {
		if(bp5Control != null)
			bp5Control.disconnect();
		iHealthDevicesManager.getInstance().unRegisterClientCallback(clientCallbackId);
	}

	private iHealthDevicesCallback iHealthDevicesCallback = new iHealthDevicesCallback() {

		@Override
		public void onScanDevice(String mac, String deviceType) {}

		@Override
		public void onDeviceConnectionStateChange(String mac, String deviceType, int status) {}

		@Override
		public void onUserStatus(String username, int userStatus) {}

		@Override
		public void onDeviceNotify(String mac, String deviceType, String action, String message) {

			Stream s = Connector.saveStream(action,"BP5 "+action);
			Connector.saveEvent(s.getId(), "note/txt", message);
			
			if(BpProfile.ACTION_BATTERY_BP.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String battery = info.getString(BpProfile.BATTERY_BP);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_DISENABLE_OFFLINE_BP.equals(action)){

			}else if(BpProfile.ACTION_ENABLE_OFFLINE_BP.equals(action)){

			}else if(BpProfile.ACTION_ERROR_BP.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String num = info.getString(BpProfile.ERROR_NUM_BP);

				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_HISTORICAL_DATA_BP.equals(action)){
				String str = "";
				try {
					JSONObject info = new JSONObject(message);
					if (info.has(BpProfile.HISTORICAL_DATA_BP)) {
			            JSONArray array = info.getJSONArray(BpProfile.HISTORICAL_DATA_BP);
			            for (int i = 0; i < array.length(); i++) {
			            	JSONObject obj = array.getJSONObject(i);
			            	String date          = obj.getString(BpProfile.MEASUREMENT_DATE_BP);
			            	String hightPressure = obj.getString(BpProfile.HIGH_BLOOD_PRESSURE_BP);
			            	String lowPressure   = obj.getString(BpProfile.LOW_BLOOD_PRESSURE_BP);
			            	String pulseWave     = obj.getString(BpProfile.PULSEWAVE_BP);
			            	String ahr           = obj.getString(BpProfile.MEASUREMENT_AHR_BP);
			            	String hsd           = obj.getString(BpProfile.MEASUREMENT_HSD_BP);
			            	str = "date:" + date
			            			+ "hightPressure:" + hightPressure + "\n"
			            			+ "lowPressure:" + lowPressure + "\n"
			            			+ "pulseWave" + pulseWave + "\n"
			            			+ "ahr:" + ahr + "\n"
			            			+ "hsd:" + hsd + "\n";
			            }
			        }
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_HISTORICAL_NUM_BP.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String num = info.getString(BpProfile.HISTORICAL_NUM_BP);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_IS_ENABLE_OFFLINE.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String isEnableoffline =info.getString(BpProfile.IS_ENABLE_OFFLINE);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_ONLINE_PRESSURE_BP.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String pressure =info.getString(BpProfile.BLOOD_PRESSURE_BP);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_ONLINE_PULSEWAVE_BP.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String pressure =info.getString(BpProfile.BLOOD_PRESSURE_BP);
					String wave = info.getString(BpProfile.PULSEWAVE_BP);
					String heartbeat = info.getString(BpProfile.FLAG_HEARTBEAT_BP);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_ONLINE_RESULT_BP.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String highPressure =info.getString(BpProfile.HIGH_BLOOD_PRESSURE_BP);
					String lowPressure =info.getString(BpProfile.LOW_BLOOD_PRESSURE_BP);
					String ahr =info.getString(BpProfile.MEASUREMENT_AHR_BP);
					String pulse =info.getString(BpProfile.PULSE_BP);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_ZOREING_BP.equals(action)){
				String obj = "zoreing";
				
			}else if(BpProfile.ACTION_ZOREOVER_BP.equals(action)){
				String obj = "zoreover";

			}
		}
	};

	public void getBattery(View v) {
		if(bp5Control != null)
			bp5Control.getBattery();
		else
			Toast.makeText(BP5.this, "bp5Control == null", Toast.LENGTH_LONG).show();
	}

	public void isOfflineMeasure(View v) {
		if (bp5Control != null)
			bp5Control.isEnableOffline();
		else
			Toast.makeText(BP5.this, "bp5Control == null", Toast.LENGTH_LONG).show();
	}

	public void enableOfflineMeasure(View v) {
		if(bp5Control != null)
			bp5Control.enbleOffline();
		else
			Toast.makeText(BP5.this, "bp5Control == null", Toast.LENGTH_LONG).show();
	}

	public void disableOfflineMeasure(View v) {
		if (bp5Control != null)
			bp5Control.disableOffline();
		else
			Toast.makeText(BP5.this, "bp5Control == null", Toast.LENGTH_LONG).show();
	}

	public void startMeasure(View v) {
		if(bp5Control != null)
			bp5Control.startMeasure();
		else
			Toast.makeText(BP5.this, "bp5Control == null", Toast.LENGTH_LONG).show();
	}

	public void stopMeasure(View v) {
		if(bp5Control != null)
			bp5Control.interruptMeasure();
		else
			Toast.makeText(BP5.this, "bp5Control == null", Toast.LENGTH_LONG).show();
	}
}
