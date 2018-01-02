package com.sec.android.recordscreen;

import java.util.ArrayList;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DisplayInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private View rootView;
	private NotificationManager mNotificationManager;
	private Notification mNotification;
	TextView time;
	final String TAG = "svrMainActivity";
	// final RemoteViews remoteViews = null;
	RemoteViews remoteViews;
	private static ArrayList<String> videosToMerge;
	private static AsyncTask<String, Integer, String> mergeVideos;
	private static String workingPath;
	Context mContext;
	int kMinBitRate = 100000; // 0.1Mbps
	int kMidiumBitRate = 10 * 1000000; // 10Mbps
	int kHighBitRate = 50 * 1000000; // 50 Mbps
	int kMaxBitRate = 100 * 1000000; // 100Mbps
	int bitRate, duration;
	AlertDialog alertDialog = null;
	ImageView settingButton;
	int SHOW_TOUCHES = 0;
	int SHOW_RENAME_DIALOG = 1;
	int GET_LOGCAT = 2;
	public static boolean isShowTouches;
	public static boolean isShowRenameDialog;
	public static boolean isGetDumpstate;
	public static boolean isGetApCpDump;
	static int WIDTH, HEIGHT;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.resolution_chosen);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		mContext = getApplicationContext();
		sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
		if (isMyServiceRunning()) {
			Toast.makeText(mContext, "Recording service is running",
					Toast.LENGTH_LONG).show();
			finish();
		} else {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
			showDialog();
			implementSettingOptions();
		}
	}

	public void showDialog() {
		final CharSequence[] items = { "Very High (5 minutes)",
				"High (7 minutes)", "Medium (9 minutes)", "Low (12 minutes)" };
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		int densityDpi = displaymetrics.densityDpi;
		Log.d(TAG, "densityDpi  = " + densityDpi);
		WIDTH = displaymetrics.widthPixels;
		HEIGHT = displaymetrics.heightPixels;
		/*
		 * Log.d(TAG,"height = "+displaymetrics.heightPixels);
		 */Log.d(TAG, "width = " + displaymetrics.widthPixels);
		DisplayInfo displayInfo = new DisplayInfo();

		switch (densityDpi) {
		case DisplayMetrics.DENSITY_LOW: // 120dpi
			kMinBitRate = 1 * 1000000;// 1Mbps
			kMidiumBitRate = 2 * 1000000; // 2Mbps
			kHighBitRate = 3 * 1000000;
			kMaxBitRate = 4 * 1000000; // 3Mbps
			break;
		case DisplayMetrics.DENSITY_MEDIUM:// 160
			kMinBitRate = 3 * 1000000;// 3Mbps
			kMidiumBitRate = 4 * 1000000; // 4Mbps
			kHighBitRate = 5 * 1000000;
			kMaxBitRate = 6 * 1000000; // 6Mbps
			break;
		case DisplayMetrics.DENSITY_HIGH:// 240
			kMinBitRate = 4 * 1000000;
			kMidiumBitRate = 8 * 1000000;
			kHighBitRate = 10 * 1000000;
			kMaxBitRate = 12 * 1000000;

		case DisplayMetrics.DENSITY_XHIGH:// 320
			kMinBitRate = 5 * 1000000;// 5Mbps
			kMidiumBitRate = 8 * 1000000; // 10Mbps
			kHighBitRate = 14 * 1000000;
			kMaxBitRate = 17 * 1000000; // 20Mbps
			break;
		case DisplayMetrics.DENSITY_XXHIGH:// 480
			kMinBitRate = 6 * 1000000;// 15Mbps
			kMidiumBitRate = 10 * 1000000; // 15Mbps
			kHighBitRate = 15 * 1000000;
			kMaxBitRate = 20 * 1000000; // 25Mbps
			break;
		case DisplayMetrics.DENSITY_XXXHIGH:
			kMinBitRate = 15 * 1000000;// 15Mbps
			kMidiumBitRate = 20 * 1000000; // 25Mbps
			kHighBitRate = 25 * 1000000;
			kMaxBitRate = 30 * 1000000; // 30Mbps
			break;

		case DisplayMetrics.DENSITY_TV:// 213
			kMinBitRate = 4 * 1000000;
			kMidiumBitRate = 7 * 1000000;
			kHighBitRate = 13 * 1000000;
			kMaxBitRate = 16 * 1000000;
			break;
		default:
			kMinBitRate = 4 * 1000000;
			kMidiumBitRate = 6 * 1000000;
			kHighBitRate = 8 * 1000000;
			kMaxBitRate = 10 * 1000000;
			break;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		View titleLayout = inflater.inflate(R.layout.dialog_custom_title,
				(ViewGroup) findViewById(R.id.custom_title_layout));
		builder.setCustomTitle(titleLayout);

		builder.setOnKeyListener(new DialogInterface.OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				// TODO Auto-generated method stub
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					Log.d(TAG, "onKeyBack");
				}
				finish();
				return true;
			}
		});
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {

				switch (item) {
				case 0:
					bitRate = kMaxBitRate;
					duration = 5 * 60;
					break;
				case 1:
					bitRate = kHighBitRate;
					duration = 7 * 60;
					break;
				case 2:
					bitRate = kMidiumBitRate;
					duration = 9 * 60;
					break;
				case 3:
					bitRate = kMinBitRate;
					duration = 12 * 60;
					break;
				default:
					break;
				}

				Bundle bundle = new Bundle();
				bundle.putInt("Duration", duration);
				bundle.putInt("Resolution", bitRate);
				bundle.putBoolean("SHOW_RENAME_DIALOG", isShowRenameDialog);
				bundle.putBoolean("GET_DUMPSTATE", isGetDumpstate);
				bundle.putBoolean("GET_DUMPSTATE_CP", isGetApCpDump);
				bundle.putInt("device_width", WIDTH);
				bundle.putInt("device_height", HEIGHT);

				Intent homeIntent = new Intent(Intent.ACTION_MAIN);
				homeIntent.addCategory(Intent.CATEGORY_HOME);
				homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

				Intent recordService = new Intent(mContext,
						SRNotificationManagerService.class);
				recordService.putExtras(bundle);
				startActivity(homeIntent);
				startService(recordService);
				settingButton.setEnabled(true);

				finish();
			}
		});
		alertDialog = builder.create();
		alertDialog.show();

		settingButton = (ImageView) alertDialog
				.findViewById(R.id.settings_button);

		settingButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplication(),
						SettingActivity.class);
				startActivityForResult(intent, 1);
			}
		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				for (int i = 0; i < 4; i++) {
					// String result = data.getStringExtra(Integer.toString(i));
					boolean result = data.getExtras().getBoolean(
							Integer.toString(i));
					switch (i) {
					case 0:
						isShowTouches = result;
						saveState("SHOW_TOUCHES", result);
						break;
					case 1:
						isShowRenameDialog = result;
						saveState("SHOW_RENAME_DIALOG", result);
						break;
					case 2:
						isGetDumpstate = result;
						saveState("GET_DUMPSTATE", result);
						break;
					case 3:
						isGetApCpDump = result;
						saveState("GET_DUMPSTATE_CP", result);
						break;
					default:
						break;

					}
				}
			}
		}
		implementSettingOptions();

	}

	public void implementSettingOptions() {
		if (getState("SHOW_TOUCHES")) {
			Settings.System.putInt(this.getContentResolver(),
					Settings.System.SHOW_TOUCHES, 1);
		} else
			Settings.System.putInt(this.getContentResolver(),
					Settings.System.SHOW_TOUCHES, 0);
		isShowRenameDialog = getState("SHOW_RENAME_DIALOG");
		isGetDumpstate = getState("GET_DUMPSTATE");
		isGetApCpDump = getState("GET_DUMPSTATE_CP");

	}

	private void saveState(String key, boolean value) {
		SharedPreferences preferences = getApplicationContext()
				.getSharedPreferences("PROJECT_NAME",
						android.content.Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}

	private boolean getState(String key) {
		SharedPreferences preferences = getApplicationContext()
				.getSharedPreferences("PROJECT_NAME",
						android.content.Context.MODE_PRIVATE);
		return preferences.getBoolean(key, false);
	}

	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if ("com.sec.android.recordscreen.SRNotificationManagerService"
					.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				|| keyCode == KeyEvent.KEYCODE_HOME) {
			finish();
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Log.d(TAG, "onPause");

	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub'
		if (alertDialog != null) {
			alertDialog.dismiss();
			alertDialog = null;
		}
		super.onDestroy();
	}

}
