package com.ihealth.devices;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.ihealth.Credentials;
import com.ihealth.R;
import com.ihealth.activities.LoginActivity;
import com.ihealth.communication.control.Bp5Control;
import com.ihealth.communication.control.BpProfile;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.pryv.Connection;
import com.pryv.Filter;
import com.pryv.database.DBinitCallback;
import com.pryv.interfaces.EventsCallback;
import com.pryv.interfaces.GetEventsCallback;
import com.pryv.interfaces.GetStreamsCallback;
import com.pryv.interfaces.StreamsCallback;
import com.pryv.model.Event;
import com.pryv.model.Stream;

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
	private Stream batteryLevelStream;
	private Stream highPressureStream;
	private Stream lowPressureStream;
	private Stream pulseStream;
	private Stream heartBeatStream;
	private Connection connection;
	private EventsCallback eventsCallback;
	private GetEventsCallback getEventsCallback;
	private StreamsCallback streamsCallback;
	private GetStreamsCallback getStreamsCallback;
	private Credentials credentials;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bp5);

		// Initiate new connection to Pryv with connected account
		credentials = new Credentials(this);
		if(credentials.hasCredentials()) {
			connection = new Connection(this, credentials.getUsername(), credentials.getToken(), LoginActivity.DOMAIN, true, new DBinitCallback());

			Stream onlineResultsStream = new Stream("BP5_onlineResults","BP5_onlineResults");
			batteryLevelStream = new Stream("BP5_batteryLevel","BP5_batteryLevel");
			highPressureStream = new Stream("BP5_highPressure","BP5_highPressure");
			lowPressureStream = new Stream("BP5_lowPressure","BP5_lowPressure");
			heartBeatStream = new Stream("BP5_heartBeat","BP5_heartBeat");
			pulseStream = new Stream("BP5_pulse","BP5_pulse");

			onlineResultsStream.addChildStream(lowPressureStream);
			onlineResultsStream.addChildStream(highPressureStream);
			onlineResultsStream.addChildStream(pulseStream);
			onlineResultsStream.addChildStream(heartBeatStream);

			Filter scope = new Filter();
			scope.addStream(onlineResultsStream);
			connection.setupCacheScope(scope);
			connection.streams.create(onlineResultsStream, streamsCallback);
			connection.streams.create(batteryLevelStream, streamsCallback);
			connection.streams.create(highPressureStream, streamsCallback);
			connection.streams.create(lowPressureStream, streamsCallback);
			connection.streams.create(heartBeatStream, streamsCallback);
			connection.streams.create(pulseStream, streamsCallback);
		}

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
					tv_return.setText("Battery level: " + battery);
					if(connection!=null) {
						connection.events.create(new Event(batteryLevelStream.getId(), "ratio/percent", battery), eventsCallback);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			} else if(BpProfile.ACTION_DISENABLE_OFFLINE_BP.equals(action)){
				tv_return.setText("Disable offline");
			} else if(BpProfile.ACTION_ENABLE_OFFLINE_BP.equals(action)){
				tv_return.setText("Enable offline");
			} else if(BpProfile.ACTION_ERROR_BP.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String num = info.getString(BpProfile.ERROR_NUM_BP);
					tv_return.setText("Error: " + num);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			} else if(BpProfile.ACTION_HISTORICAL_DATA_BP.equals(action)){
				//TODO: JSON
				try {
					JSONObject info = new JSONObject(message);
					if (info.has(BpProfile.HISTORICAL_DATA_BP)) {
			            JSONArray array = info.getJSONArray(BpProfile.HISTORICAL_DATA_BP);

						tv_return.setText("Saving "+array.length()+" historical data...");

			            for (int i = 0; i < array.length(); i++) {
			            	JSONObject obj = array.getJSONObject(i);
			            	String date          = obj.getString(BpProfile.MEASUREMENT_DATE_BP);
			            	String highPressure = obj.getString(BpProfile.HIGH_BLOOD_PRESSURE_BP);
			            	String lowPressure   = obj.getString(BpProfile.LOW_BLOOD_PRESSURE_BP);
			            	String pulseWave     = obj.getString(BpProfile.PULSEWAVE_BP);
			            	String ahr           = obj.getString(BpProfile.MEASUREMENT_AHR_BP);
			            	String hsd           = obj.getString(BpProfile.MEASUREMENT_HSD_BP);

							//connection.saveEvent(historicalDataStream.getId(), "note/txt", date);
							//connection.saveEvent(historicalDataStream.getId(),"pressure/mmhg",highPressure);
							//connection.saveEvent(historicalDataStream.getId(),"pressure/mmhg",lowPressure);
							//connection.saveEvent(historicalDataStream.getId(),"note/txt",ahr);
							//connection.saveEvent(historicalDataStream.getId(), "frequency/bpm", pulseWave);
							//connection.saveEvent(historicalDataStream.getId(), "note/txt", hsd);

						}
			        }
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			} else if(BpProfile.ACTION_HISTORICAL_NUM_BP.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String num = info.getString(BpProfile.HISTORICAL_NUM_BP);
					tv_return.setText("Historical num: " + num);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			} else if(BpProfile.ACTION_IS_ENABLE_OFFLINE.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String isEnableoffline =info.getString(BpProfile.IS_ENABLE_OFFLINE);
					tv_return.setText("Is enable offline? " + isEnableoffline);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			} else if(BpProfile.ACTION_ONLINE_PRESSURE_BP.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String pressure =info.getString(BpProfile.BLOOD_PRESSURE_BP);
					tv_return.setText("Pressure: " + pressure);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			} else if(BpProfile.ACTION_ONLINE_PULSEWAVE_BP.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String pressure =info.getString(BpProfile.BLOOD_PRESSURE_BP);
					String wave = info.getString(BpProfile.PULSEWAVE_BP);
					String heartbeat = info.getString(BpProfile.FLAG_HEARTBEAT_BP);
					String s = "Wave: "+wave+"\nHearthbeat: "+heartbeat+"\nPressure: "+pressure;
					tv_return.setText(s);
					if(connection!=null) {
						connection.events.create(new Event(heartBeatStream.getId(), "pressure/mmhg", pressure), eventsCallback);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			} else if(BpProfile.ACTION_ONLINE_RESULT_BP.equals(action)){
				try {
					JSONObject info = new JSONObject(message);
					String highPressure =info.getString(BpProfile.HIGH_BLOOD_PRESSURE_BP);
					String lowPressure =info.getString(BpProfile.LOW_BLOOD_PRESSURE_BP);
					String ahr =info.getString(BpProfile.MEASUREMENT_AHR_BP);
					String pulse =info.getString(BpProfile.PULSE_BP);
					String s = "HighPressure: "+highPressure+"\n LowPressure: "+lowPressure+"\n Ahr: "+ahr+"\n Pulse: "+pulse;

					tv_return.setText(s);

					if(connection!=null) {
						connection.events.create(new Event(highPressureStream.getId(), "pressure/mmhg", highPressure), eventsCallback);
						connection.events.create(new Event(lowPressureStream.getId(), "pressure/mmhg", lowPressure), eventsCallback);
						connection.events.create(new Event(pulseStream.getId(), "frequency/bpm", pulse), eventsCallback);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}else if(BpProfile.ACTION_ZOREING_BP.equals(action)){
				String obj = "Zoreing";
				tv_return.setText(obj);

			}else if(BpProfile.ACTION_ZOREOVER_BP.equals(action)){
				String obj = "Zoreover";
				tv_return.setText(obj);
			}
		}
	};

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
