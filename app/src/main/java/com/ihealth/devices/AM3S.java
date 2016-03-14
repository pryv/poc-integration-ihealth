package com.ihealth.devices;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.ihealth.R;
import com.ihealth.communication.control.Am3sControl;
import com.ihealth.communication.control.AmProfile;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.utils.Connector;
import com.pryv.api.model.Stream;

import org.json.JSONException;
import org.json.JSONObject;

public class AM3S extends Activity {
	private Am3sControl am3sControl;
	private String mac;
	private int clientId;
	private TextView tv_return;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_am3_s);

		Connector.initiateConnection();

		clientId = iHealthDevicesManager.getInstance().registerClientCallback(iHealthDevicesCallback);
		
		iHealthDevicesManager.getInstance().addCallbackFilterForDeviceType(clientId,
                iHealthDevicesManager.TYPE_AM3S);
		
		Intent intent = getIntent();
		this.mac = intent.getStringExtra("mac");
		
		am3sControl = iHealthDevicesManager.getInstance().getAm3sControl(this.mac);

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
		if(am3sControl != null)
			am3sControl.disconnect();
		iHealthDevicesManager.getInstance().unRegisterClientCallback(clientId);
	}


	private iHealthDevicesCallback iHealthDevicesCallback = new iHealthDevicesCallback() {

		@Override
		public void onScanDevice(String mac, String deviceType) {}

		@Override
		public void onDeviceConnectionStateChange(String mac, String deviceType, int status) {}

		@Override
		public void onDeviceNotify(String mac, String deviceType, String action, String message) {

			Stream s = Connector.saveStream(action,"AM3S "+action);
			Connector.saveEvent(s.getId(), "note/txt", message);

			switch (action) {
			case AmProfile.ACTION_QUERY_STATE_AM:
				try {
					JSONObject info = new JSONObject(message);
					String battery =info.getString(AmProfile.QUERY_BATTERY_AM);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;

			case AmProfile.ACTION_USERID_AM:
				try {
					JSONObject info = new JSONObject(message);
					String id =info.getString(AmProfile.USERID_AM);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;
			case AmProfile.ACTION_GET_ALARMNUM_AM:
				try {
					JSONObject info = new JSONObject(message);
					String alarm_num =info.getString(AmProfile.GET_ALARMNUM_AM);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;
			case AmProfile.ACTION_SYNC_STAGE_DATA_AM:
				try {
					JSONObject info = new JSONObject(message);
					String stage_info =info.getString(AmProfile.SYNC_STAGE_DATA_AM);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;
			case AmProfile.ACTION_SYNC_SLEEP_DATA_AM:
				try {
					JSONObject info = new JSONObject(message);
					String stage_info =info.getString(AmProfile.SYNC_SLEEP_DATA_AM);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;
			case AmProfile.ACTION_SYNC_ACTIVITY_DATA_AM:
				try {
					JSONObject info = new JSONObject(message);
					String activity_info =info.getString(AmProfile.SYNC_ACTIVITY_DATA_AM);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;
			case AmProfile.ACTION_SYNC_REAL_DATA_AM:
				try {
					JSONObject info = new JSONObject(message);
					String real_info =info.getString(AmProfile.SYNC_REAL_STEP_AM);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;
			case AmProfile.ACTION_GET_USERINFO_AM:
				try {
					JSONObject info = new JSONObject(message);
					String user_info =info.getString(AmProfile.GET_USER_AGE_AM);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;
			case AmProfile.ACTION_GET_ALARMINFO_AM:
				try {
					JSONObject info = new JSONObject(message);
					String alarm_id =info.getString(AmProfile.GET_ALARM_ID_AM);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;
			case AmProfile.ACTION_SET_USERID_SUCCESS_AM:
				String obj = "Set ID success";
				break;
			case AmProfile.ACTION_GET_RANDOM_AM:
				try {
					JSONObject info = new JSONObject(message);
					String random =info.getString(AmProfile.GET_RANDOM_AM);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;
			default:
				break;
			}
		}
	};

	public void getBattery(View v) {
		if (am3sControl != null) {
			am3sControl.queryAMState();
		}else
			Toast.makeText(this, "am3sControl == null", Toast.LENGTH_LONG).show();
	}

	public void getUserId(View v) {
		if (am3sControl != null) {
			am3sControl.getUserId();
		}else
			Toast.makeText(this, "am3sControl == null", Toast.LENGTH_LONG).show();
	}

	public void getAlarmNum(View v) {
		if (am3sControl != null) {
			am3sControl.getAlarmClockNum();
		}else
			Toast.makeText(this, "am3sControl == null", Toast.LENGTH_LONG).show();
	}

	public void syncStage(View v) {
		if (am3sControl != null) {
			am3sControl.syncStageReprotData();
		}else
			Toast.makeText(this, "am3sControl == null", Toast.LENGTH_LONG).show();
	}

	public void syncSleep(View v) {
		if (am3sControl != null) {
			am3sControl.syncSleepData();
		}else
			Toast.makeText(this, "am3sControl == null", Toast.LENGTH_LONG).show();
	}

	public void syncActivity(View v) {
		if (am3sControl != null) {
			am3sControl.syncActivityData();
		}else
			Toast.makeText(this, "am3sControl == null", Toast.LENGTH_LONG).show();
	}

	public void syncReal(View v) {
		if (am3sControl != null) {
			am3sControl.syncRealData();
		}else
			Toast.makeText(this, "am3sControl == null", Toast.LENGTH_LONG).show();
	}

	public void getUserInfo(View v) {
		if (am3sControl != null) {
			am3sControl.getUserInfo();
		}else
			Toast.makeText(this, "am3sControl == null", Toast.LENGTH_LONG).show();
	}

	public void getAlarmInfo(View v) {
		if (am3sControl != null) {
			am3sControl.checkAlarmClock(1);
		}else
			Toast.makeText(this, "am3sControl == null", Toast.LENGTH_LONG).show();
	}

	public void setUserId(View v) {
		if (am3sControl != null) {
			am3sControl.setUserId(1);
		}else
			Toast.makeText(this, "am3sControl == null", Toast.LENGTH_LONG).show();
	}

	public void sendRandom(View v) {
		if (am3sControl != null) {
			am3sControl.sendRandom();
		}else
			Toast.makeText(this, "am3sControl == null", Toast.LENGTH_LONG).show();
	}

}
