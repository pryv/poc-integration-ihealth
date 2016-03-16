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
import com.pryv.api.model.Event;
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
		public void onDeviceConnectionStateChange(String mac, String deviceType, int status) {
			if(status==2) {
				Intent intent = new Intent();
				setResult(Activity.RESULT_CANCELED, intent);
				reset();
				finish();
			}
		}

		@Override
		public void onUserStatus(String username, int userStatus) {}

		@Override
		public void onDeviceNotify(String mac, String deviceType, String action, String message) {
			
			if(BpProfile.ACTION_BATTERY_BP.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String battery = info.getString(BpProfile.BATTERY_BP);
					saveAction("batteryLevel","ratio/percent",battery);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_DISENABLE_OFFLINE_BP.equals(action)){
				saveAction("offlineState", "note/txt", "Disable offline");
			}else if(BpProfile.ACTION_ENABLE_OFFLINE_BP.equals(action)){
				saveAction("offlineState","note/txt","Enable offline");
			}else if(BpProfile.ACTION_ERROR_BP.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String num = info.getString(BpProfile.ERROR_NUM_BP);
					saveAction("errorNum", "count/generic", num);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_HISTORICAL_DATA_BP.equals(action)){
				//TODO: JSON
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
							saveAction("measurement","note/txt",str);
			            }
			        }
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_HISTORICAL_NUM_BP.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String num = info.getString(BpProfile.HISTORICAL_NUM_BP);
					saveAction("historicalNum", "count/generic", num);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_IS_ENABLE_OFFLINE.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String isEnableoffline =info.getString(BpProfile.IS_ENABLE_OFFLINE);
					saveAction("iEnableOffline", "count/generic", isEnableoffline);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_ONLINE_PRESSURE_BP.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String pressure =info.getString(BpProfile.BLOOD_PRESSURE_BP);
					saveAction("pressure", "pressure/mmhg", pressure);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_ONLINE_PULSEWAVE_BP.equals(action)){
				//TODO: JSON
				try {
					JSONObject info = new JSONObject(message);
					String pressure =info.getString(BpProfile.BLOOD_PRESSURE_BP);
					String wave = info.getString(BpProfile.PULSEWAVE_BP);
					String heartbeat = info.getString(BpProfile.FLAG_HEARTBEAT_BP);
					String s = "Wave: "+wave+"\n Hearthbeat: "+heartbeat+"\n Pressure: "+pressure;
					saveAction("onlinePulsewave","note/txt",s);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_ONLINE_RESULT_BP.equals(action)){
				//TODO: JSON
				try {
					JSONObject info = new JSONObject(message);
					String highPressure =info.getString(BpProfile.HIGH_BLOOD_PRESSURE_BP);
					String lowPressure =info.getString(BpProfile.LOW_BLOOD_PRESSURE_BP);
					String ahr =info.getString(BpProfile.MEASUREMENT_AHR_BP);
					String pulse =info.getString(BpProfile.PULSE_BP);
					String s = "HighPressure: "+highPressure+"\n LowPressure: "+lowPressure+"\n Ahr: "+ahr+"\n Pulse: "+pulse;
					saveAction("onlineResult","note/txt",s);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_ZOREING_BP.equals(action)){
				String obj = "zoreing";
				saveAction("Zero", "note/txt", obj);

			}else if(BpProfile.ACTION_ZOREOVER_BP.equals(action)){
				String obj = "zoreover";
				saveAction("Zero", "note/txt", obj);
			}
		}
	};

	private void saveAction(String action, String type, String content) {
		tv_return.setText(content);
		String streamID = "BP5_"+action;
		Stream s = Connector.saveStream(streamID,streamID);
		Connector.saveEvent(s.getId(), type, content);
	}

	public void getBattery(View v) {
		bp5Control.getBattery();
	}

	public void isOfflineMeasure(View v) {
		bp5Control.isEnableOffline();
	}

	public void enableOfflineMeasure(View v) {
		bp5Control.enbleOffline();
	}

	public void disableOfflineMeasure(View v) {
		bp5Control.disableOffline();
	}

	public void startMeasure(View v) {
		bp5Control.startMeasure();
	}

	public void stopMeasure(View v) {
		bp5Control.interruptMeasure();
	}
}
