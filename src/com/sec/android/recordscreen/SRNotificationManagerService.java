 package com.sec.android.recordscreen;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.sec.android.app.SecProductFeature_COMMON;
import com.sec.android.app.SecProductFeature_RIL;

public class SRNotificationManagerService extends Service {
	private final int myNotificationId = 0;
	public static final int SIGNAL_KILL = 1;
	private NotificationManager mNotificationManager;
	private Notification mNotification;
	RemoteViews remoteViews;
	Context mContext;
	long delay = 0;
	final static String SCREEN_RECORD_CANCEL = "com.sec.android.recordscreen.cancel";
	final static String SCREEN_RECORD_STOP = "com.sec.android.recordscreen.stop";
	final static String SCREEN_RECORD_PAUSE = "com.sec.android.recordscreen.pause";
	final static String SCREEN_RECORD_START = "com.sec.android.recordscreen.start";
	final static String SCREEN_RECORD_SET_NEWNAME = "com.sec.android.recordscreen.set_newname";
	final static String STOP_SERVICE = "com.sec.android.recordscreen.stop_service";
	final static String SCREEN_RECORD_CANCEL_DUMP = "com.sec.android.recordscreen.cancel_dump";
	private static final boolean isMarvell = "mrvl"
			.equalsIgnoreCase(SystemProperties.get("ro.board.platform",
					"Unknown").trim());
	static final long[] ROTATE_0 = new long[] { 1, 0, 0, 1, 0, 0, 1, 0, 0 };
	static final long[] ROTATE_90 = new long[] { 0, 1, -1, 0, 0, 0, 1, 0, 0 };
	static final long[] ROTATE_180 = new long[] { -1, 0, 0, -1, 0, 0, 1, 0, 0 };
	static final long[] ROTATE_270 = new long[] { 0, -1, 1, 0, 0, 0, 1, 0, 0 };

	private long[] rotate0 = new long[] { 0x00010000, 0, 0, 0, 0x00010000, 0,
			0, 0, 0x40000000 };
	private long[] rotate90 = new long[] { 0, 0x00010000, 0, -0x00010000, 0, 0,
			0, 0, 0x40000000 };
	private long[] rotate180 = new long[] { 0x00010000, 0, 0, 0, 0x00010000, 0,
			0, 0, 0x40000000 };
	private long[] rotate270 = new long[] { -0x00010000, 0, 0, 0, -0x00010000,
			0, 0, 0, 0x40000000 };
	private static final String DEBUG_LEVEL_LOW_STRING = "0x4f4c";
	private static final String DEBUG_LEVEL_MID_STRING = "0x494d";
	private static final String DEBUG_LEVEL_HIGH_STRING = "0x4948";
	private static final int IPC_DUMP_DONE = 1012;
	private static final int QUERY_MODEMLOG_DONE = 1014;
	private String debugLevel = DEBUG_LEVEL_LOW_STRING;
	private Messenger mServiceMessenger = null;
	static int WIDTH,HEIGHT;

	private static int bindservice_flag = 0;
	static ArrayList<String> videosToMerge;
	private byte[] buf = new byte[1024];
	private static AsyncTask<String, Integer, String> mergeVideos;
	private static String workingPath;
	// private static boolean flag;
	long timeCounter = 0;
	final static String TAG = " SVRSRNotificationManagerService";
	private long startTime = 0L;
	long timeInMillies = 0L;
	long timeSwap = 0L;
	long finalTime = 0L;
	int flag = 0;
	String fileSum;
	long duration;
	private Timer timer;
	int bitRate;
	int maxTime;
	String fileName;
	String newName;
	String sysdump_time;
	boolean setTimeoutonStartCommand = true;
	boolean stopDueToTimeOut = false;
	boolean isShowRenameDialog = false;
	boolean isGetDumpstate = false;
	boolean isGetApCp = false;
	boolean isMerging = false;
	boolean isCancel = false;
	boolean isPaused = false;
	boolean isRenameCalled = false;
	boolean isRenameDialogShowing = false;
	private boolean iscpdumpdone = false;
	long start, end;
	int count = 0;
	boolean isRecorded = false;
	String videoMergedName;
	Intent stopRecord;
	boolean isStopClicked = false;
	public boolean isDumpstateRunning = false;
	File newFile;
	private OemCommands mOem = null;
	private static final String TCPDUMP_PROPERTY = "ril.tcpdumping";
	private static final boolean isEOS2 = SystemProperties
			.get("ro.board.platform", "Unknown").trim().startsWith("EOS2")
			|| SystemProperties.get("ro.board.platform", "Unknown").trim()
					.startsWith("u2");
	private String outFile;
	public Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case QUERY_MODEMLOG_DONE: {
				int error = msg.getData().getInt("error");
				iscpdumpdone = true;
				if (error == 0) {
					Log.i(TAG, "MODEMLOG_DONE Success");
				} else {
					Log.i(TAG, "MODEMLOG_DONE fail");
				}
				break;
			}
			default:
				break;
			}
		}
	};
	private Messenger mSvcModeMessenger = new Messenger(mHandler);

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		IntentFilter filter = new IntentFilter();
		filter.addAction(SCREEN_RECORD_CANCEL);
		filter.addAction(SCREEN_RECORD_STOP);
		filter.addAction(SCREEN_RECORD_PAUSE);
		filter.addAction(SCREEN_RECORD_START);
		filter.addAction(SCREEN_RECORD_SET_NEWNAME);
		filter.addAction(SCREEN_RECORD_CANCEL_DUMP);
		filter.addAction(STOP_SERVICE);
		mOem = new OemCommands();
		connectToRilService();
		registerReceiver(screenRecordReceiver, filter,
				"com.sec.android.screenrecord.permission.LAUNCH_RECORD_SCREEN",
				null);
		Log.d(TAG, "onCreate");
		mContext = getApplicationContext();
		videosToMerge = new ArrayList<String>();
		if (videosToMerge.size() > 0)
			videosToMerge.clear();
		this.workingPath = Environment.getExternalStorageDirectory() + "/";
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "service");
		Bundle b = intent.getExtras();
		if (b != null) {
			bitRate = b.getInt("Resolution");
			maxTime = b.getInt("Duration");
			isShowRenameDialog = b.getBoolean("SHOW_RENAME_DIALOG");
			isGetDumpstate = b.getBoolean("GET_DUMPSTATE");
			isGetApCp = b.getBoolean("GET_DUMPSTATE_CP");
			WIDTH = b.getInt("device_width");
			HEIGHT = b.getInt("device_height");
		}
		startRecordScreen(Integer.toString(bitRate));
		updateNotification();
		long startTime = System.currentTimeMillis();
		Handler myHandler = new Handler();
		myHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				sendBroadcast(new Intent(SCREEN_RECORD_STOP));
				stopDueToTimeOut = true;
			}
		}, maxTime * 1000);
		return super.onStartCommand(intent, flags, startId);
	}

	private ServiceConnection mSecPhoneServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// Log.d(TAG, "onServiceConnected()");
			mServiceMessenger = new Messenger(service);
			bindservice_flag = 1;
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.d(TAG, "onServiceDisconnected()");
			mServiceMessenger = null;
		}
	};

	private void connectToRilService() {
		Log.i(TAG, "connect To Ril service");
		Intent intent = new Intent();

		String strRilDaemon2Status = SystemProperties.get(
				"init.svc.ril-daemon2", "stopped").toLowerCase();
		if ("running".equals(strRilDaemon2Status)) {
			intent.setClassName("com.sec.phone",
					"com.sec.phone.SecPhoneService");
			Log.i(TAG, "com.sec.phone.SecPhoneService");

		} else {
			intent.setClassName("com.sec.phone",
					"com.sec.phone.SecPhoneService");
		}

		bindService(intent, mSecPhoneServiceConnection, BIND_AUTO_CREATE);
	}

	private BroadcastReceiver screenRecordReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (action.equals(SCREEN_RECORD_START)) {
				Log.d(TAG, "SCREEN_RECORD_START");
				updateNotification();
			}
			if (action.equals(SCREEN_RECORD_STOP)) {
				if (isStopClicked)
					return;
			

				if (isShowRenameDialog)
					isRenameDialogShowing = true;

				//Log.d(TAG, "stop receive");
				end = System.currentTimeMillis();
				long d = end - start;

				if (d < 3000) {
					Log.d(TAG, "d = " + d);
					startToastIntent("TOAST_TIME_TO_STOP",
							R.string.toast_time_to_stop);
					return;
				}

				isStopClicked = true;
				int size = videosToMerge.size();
			//	Log.d(TAG, "VIDEO SIZE=" + size);

				if (timer != null) {
					timer.cancel();
					updateSavingVideoNotification();
				}
				stopRecordScreen();
				if (!isShowRenameDialog && !isGetDumpstate && !isGetApCp)
					hideNotification();
				stopShowTouches();
				if (isGetDumpstate || isGetApCp)
					startGetDumpsys();
				if (isShowRenameDialog()) {
					showRenameDialog();
				}
				if (size >= 2) {
					isMerging = true;
					MergeVideo(workingPath, videosToMerge);

					if (isMerging && !isGetDumpstate && !isGetApCp
							&& isShowRenameDialog())
						updateSavingVideoNotification();

				} else {
					if (!isShowRenameDialog) {

						Thread renameThread = new Thread(new Runnable() {
							public void run() {
								String hidenName = workingPath
										+ videosToMerge.get(0).toString();
								String visibleName = workingPath
										+ videosToMerge.get(0).toString()
												.substring(1);
								File hidenFile = new File(hidenName);
								File visibleFile = new File(visibleName);
								if (hidenFile.exists()) {
									hidenFile.renameTo(visibleFile);
								}

								try {
									rescanSdcard();
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});

						renameThread.start();

					}
					if (!isShowRenameDialog) {
						startToastIntent("TOAST_VIDEO_SAVED",
								R.string.toast_video_saved);

						if (!isGetDumpstate && !isGetApCp) {
							stopSilentLog();
							hideNotification();
							stopService();
						}
					}

				}
			}
			if (action.equals(SCREEN_RECORD_SET_NEWNAME)) {
				Log.d(TAG, "SCREEN_RECORD_SET_NEWNAME");

				Bundle b = intent.getExtras();
				if (b != null)
					newName = b.getString("new_name");
				// Log.d(TAG, "newName is " + newName);
				isShowRenameDialog = false;
				isRenameCalled = true;
				String oldName;
				oldName = workingPath + fileName + ".mp4";
				if (count > 1)
					oldName = workingPath + ".VSR-" + fileName.substring(1)
							+ ".mp4";
				else
					oldName = workingPath + videosToMerge.get(0).toString();

				String renamedName = workingPath + newName + ".mp4";
				Log.d(TAG, "new name = " + renamedName);
				File oldFile = new File(oldName);
				newFile = new File(renamedName);
				if (oldFile.exists()) {
				//	Log.d(TAG, "oldname exist");
					oldFile.renameTo(newFile);

					try {
						rescanSdcard();

					} catch (Exception e) {

					}
					startToastIntent("TOAST_VIDEO_SAVED",
							R.string.toast_video_saved);
					if (!isRunningRumpstate() && isMerging == false
							&& !isGetLog()) {
						Log.d(TAG,"stop service due to NEW NAME saved");
						stopSilentLog();
						hideNotification();
						stopService();

					}

				}

			}
			if (action.equals(STOP_SERVICE)) {
				hideNotification();
				stopService();
			}
			if (action.equals(SCREEN_RECORD_CANCEL)) {
				Log.d(TAG, "cancel");
				isCancel = true;
				timer.cancel();
				hideNotification();

				stopRecordScreen();
				removeVideo();
				unregisterReceiver(screenRecordReceiver);
				stopService();
			}
			if (action.equals(SCREEN_RECORD_CANCEL_DUMP)) {
				if (isShowRenameDialog()) {
					startToastIntent("TOAST_ENTER_NEW_NAME",  R.string.toast_enter_new_name);
					return;
				}
				else
					showCancelDlg();

			}
			if (action.equals(SCREEN_RECORD_PAUSE)) {

				pauseRecord();
			}

		}
	};

	public void stopShowTouches() {
		Settings.System.putInt(this.getContentResolver(),
				Settings.System.SHOW_TOUCHES, 0);
	}

	public void startToastIntent(String key, int value) {
		Intent intent = new Intent(this, DisplayToast.class);
		Bundle bundle = new Bundle();
		bundle.putInt(key, value);

		intent.putExtras(bundle);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	public void showDumpResult() {
		Intent intent = new Intent(this, DisplayToast.class);
		Bundle bundle = new Bundle();
		bundle.putString("SYSDUMP_NAME", sysdump_time);
		intent.putExtras(bundle);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	boolean isMidOrHighDebugLevel() {
		String ramdumpstate = SystemProperties.get("ro.debug_level", "Unknown");
		debugLevel = ramdumpstate;
		if (DEBUG_LEVEL_HIGH_STRING.equals(ramdumpstate)
				|| DEBUG_LEVEL_MID_STRING.equals(ramdumpstate))
			return true;
		else
			return false;

	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		timer.cancel(); //
		// unregisterReceiver(screenRecordReceiver);
		// hideNotification(); // stopService();
	}

	private Notification makeScreenRecordNotification(String string) {
		String title = "Screen recorder";
		Notification.Builder builder = new Notification.Builder(this)
				.setSmallIcon(R.drawable.videocapture_icon).setTicker((string))
				.setAutoCancel(true).setOngoing(true);
		Notification recordNotice = builder.build();
		return recordNotice;
	}

	private RemoteViews createRecordRecordingRemoteViews() {
		final RemoteViews remoteViews = new RemoteViews(getPackageName(),
				R.layout.remoteview_record_recording);

		/*
		 * if (isShowRenameDialog) { stopRecord = new Intent(getApplication(),
		 * RenameDialog.class); stopRecord.putExtra("video_name",
		 * fileName.substring(1)); stopRecord.putExtra("start_time", start);
		 * stopRecord.putExtra("GET_DUMPSTATE", isGetDumpstate);
		 * stopRecord.putExtra("GET_DUMPSTATE_CP", isGetApCp); PendingIntent
		 * stopPendingRecord = PendingIntent.getActivity(this, 1, stopRecord,
		 * PendingIntent.FLAG_UPDATE_CURRENT);
		 * remoteViews.setOnClickPendingIntent(R.id.stop_icon,
		 * stopPendingRecord); } else {
		 */

		PendingIntent stopRecording = PendingIntent.getBroadcast(
				getApplicationContext(), 0, new Intent(SCREEN_RECORD_STOP), 0);
		remoteViews.setOnClickPendingIntent(R.id.stop_icon, stopRecording);
		// }
		// stopRecord.setAction(SCREEN_RECORD_STOP);
		Log.d(TAG, "fileName=" + fileName);

		PendingIntent pauseRecording = PendingIntent.getBroadcast(
				getApplicationContext(), 0, new Intent(SCREEN_RECORD_PAUSE), 0);
		remoteViews.setOnClickPendingIntent(R.id.pause_icon, pauseRecording);

		Intent cancelRecord = new Intent(mContext,
				ScreenRecordCancelDialogActivity.class);
		PendingIntent cancelPendingRecord = PendingIntent.getActivity(this, 0,
				cancelRecord, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.cancel_icon,
				cancelPendingRecord);

		return remoteViews;

	}

	public void updateNotification() {
		Log.d(TAG, "updateNotification");
		mNotification = makeScreenRecordNotification(getString(R.string.screen_record_notification_title));
		mNotification.flags = Notification.PRIORITY_MAX;

		remoteViews = createRecordRecordingRemoteViews();
		mNotification.contentView = remoteViews;
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationManager.notify(0, mNotification);
		startTime = SystemClock.uptimeMillis();
		setTimingNotification();
	}

	public void updatePausedNotification() {
		timeSwap += timeInMillies;
		remoteViews.setViewVisibility(R.id.record_image, View.INVISIBLE);
		remoteViews.setImageViewResource(R.id.pause_icon,
				R.drawable.quick_panel_icon_playrec_btn);
		mNotification.contentView = remoteViews;
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationManager.notify(0, mNotification);

	}

	public void UpdateResumeNotification() {
		remoteViews.setViewVisibility(R.id.record_image, View.VISIBLE);
		remoteViews.setImageViewResource(R.id.pause_icon,
				R.drawable.quick_panel_icon_pause_btn);
		mNotification.contentView = remoteViews;
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationManager.notify(0, mNotification);
		startTime = SystemClock.uptimeMillis();
		setTimingNotification();
	}

	public void updateGetDupstateNotification() {
		mNotification = makeScreenRecordNotification("Get dumpstate/logcat");
		mNotification.flags = Notification.PRIORITY_MAX;

		remoteViews = new RemoteViews(getPackageName(),
				R.layout.get_dumpstate_notification);
		/*
		 * Intent cancelGetDump = new Intent(mContext,
		 * ScreenRecordCancelDialogActivity.class);
		 * cancelGetDump.putExtra("cancel_get_dump", true);
		 */

		PendingIntent cancelGetDump = PendingIntent.getBroadcast(
				getApplicationContext(), 0, new Intent(
						SCREEN_RECORD_CANCEL_DUMP), 0);
		remoteViews.setOnClickPendingIntent(R.id.cancel_icon, cancelGetDump);

		/*
		 * PendingIntent cancelGetDumpPending = PendingIntent.getActivity(this,
		 * 0, cancelGetDump, PendingIntent.FLAG_UPDATE_CURRENT);
		 * remoteViews.setOnClickPendingIntent(R.id.cancel_icon,
		 * cancelGetDumpPending);
		 */
		mNotification.contentView = remoteViews;
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationManager.notify(0, mNotification);

	}

	public void updateSavingVideoNotification() {
		mNotification = makeScreenRecordNotification("Saving video");
		mNotification.flags = Notification.PRIORITY_MAX;

		remoteViews = new RemoteViews(getPackageName(),
				R.layout.saving_video_notification);
		Intent cancelGetDump = new Intent(mContext,
				ScreenRecordCancelDialogActivity.class);
		PendingIntent cancelGetDumpPending = PendingIntent.getActivity(this, 0,
				cancelGetDump, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.cancel_icon,
				cancelGetDumpPending);

		mNotification.contentView = remoteViews;
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationManager.notify(0, mNotification);

	}

	public boolean hideNotification() {
		Log.d(TAG, "hideNotification");
		if (mNotificationManager != null) {
			mNotificationManager.cancel(0);
			mNotificationManager = null;
			mNotification = null;
			this.stopSelf();
		}
		return true;
	}

	public void setTimingNotification() {
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				remoteViews.setTextViewText(R.id.text, formatTime(timeCounter));
				if (remoteViews != null) {
					mNotification.contentView = remoteViews;
				}
				timeCounter++;
				mNotificationManager.notify(0, mNotification);
			}
		}, 0, 1000);
	}

	private String formatTime(long time) {
		String res = "";
		res += time / 60 + ":";
		if (time % 60 < 10)
			res += "0";
		res += (time % 60);
		return res;
	}

	public void showRenameDialog() {
		Intent intent = new Intent(this, RenameDialog.class);
		intent.putExtra("video_name", fileName.substring(1));
		intent.putExtra("GET_DUMPSTATE", isGetDumpstate);
		intent.putExtra("GET_DUMPSTATE_CP", isGetApCp);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	public void showCancelDlg() {
		Intent cancelGetDump = new Intent(mContext,
				ScreenRecordCancelDialogActivity.class);
		cancelGetDump.putExtra("cancel_get_dump", true);
		cancelGetDump.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(cancelGetDump);
	}

	public void pauseRecord() {
		if (isRenameDialogShowing) {
			startToastIntent("TOAST_ENTER_NEW_NAME",
					R.string.toast_enter_new_name);
			return;
		}
		end = System.currentTimeMillis();
		long d = end - start;
		if (d < 3000) {
			startToastIntent("TOAST_TIME_TO_PAUSE",
					R.string.toast_time_to_pause);
			return;
		}
		isPaused = true;
		flag ^= 1;
		if (flag == 1) {
			// count++;
			if (timer != null)
				timer.cancel();
			Log.d(TAG, "PAUSE");

			updatePausedNotification();
			stopRecordScreen();

		} else {
			Log.d(TAG, "RESUME");
			UpdateResumeNotification();
			startRecordScreen(Integer.toString(bitRate));
		}
	}

	public void startGetDumpsys() {
		updateGetDupstateNotification();
		if (isGetApCp) {
			getDumpstateAll();
			return;
		}
		getDumpstate("");

	}

	public void getDumpstate(final String command) {
		Log.d(TAG, "Get Dumpstate");
		final StringBuilder sbConsole = new StringBuilder();
		final StringBuilder sbErrors = new StringBuilder();
		sysdump_time = new SimpleDateFormat("yyyyMMddHHmm").format(Calendar
				.getInstance().getTime());

		AsyncTask<Void, Integer, Void> getDumpTask = new AsyncTask<Void, Integer, Void>() {
			@Override
			protected void onPreExecute() {
			};

			@Override
			protected Void doInBackground(Void... objects) {
				Log.d(TAG, "<--START_Get dumpstate");
				SendData(mOem.OEM_IPC_DUMP_BIN);
				sysdump_time = new SimpleDateFormat("yyyyMMddHHmm")
						.format(Calendar.getInstance().getTime());
				stopSilentLog();
				File dataLogDirectory = new File("/data/log");
				File btsnoop_log = new File(
						"/mnt/sdcard/Android/data/btsnoop_hci.log");
				File btsnoop_log_old = new File(
						"/mnt/sdcard/Android/data/btsnoop_hci.log.old");
				File hsuart_log = new File(
						"/mnt/sdcard/Android/data/[BT]msm_serial_hs.log");
				File hsuart_log_old = new File(
						"/mnt/sdcard/Android/data/[BT]msm_serial_hs.log.old");

				if (!dataLogDirectory.exists()) {
					dataLogDirectory.mkdir();
				}
				if (!isShowRenameDialog)
					startToastIntent("START_GET_DUMP",
							R.string.toast_start_get_dump);
				isDumpstateRunning = true;
				if ("gardaltetmo".equals(SystemProperties.get(
						"ro.product.name", "UNKNOWN"))) {
					mOem.DoShellCmd("dumpstate -r > /data/log/dumpState_"
							+ sysdump_time + ".log");
				} else {
					mOem.DoShellCmd("bugreport > /data/log/dumpState_"
							+ sysdump_time + ".log");
				}

				if (SystemProperties.get("persist.security.mdm.SElogs", "1")
						.equals("1")) {
					getSEAndroidLogs(); // get SEAndroid logs
				}
				getTSPLogs(); // get TSP logs

				if (btsnoop_log.exists()) {
					Log.d(TAG, "btsnoop_hci.log exists!! ");
					outFile = "/data/log/btsnoop_hci_" + sysdump_time + ".log";
					WriteToSDcard("/mnt/sdcard/Android/data/btsnoop_hci.log",
							outFile, "btsnoop_hci.log");
				}

				if (btsnoop_log_old.exists()) {
					Log.d(TAG, "btsnoop_hci.log.old exists!! ");
					outFile = "/data/log/btsnoop_hci_" + sysdump_time
							+ ".log.old";
					WriteToSDcard(
							"/mnt/sdcard/Android/data/btsnoop_hci.log.old",
							outFile, "btsnoop_hci.log.old");
				}

				if (hsuart_log.exists()) {
					Log.d(TAG, "[BT]msm_serial_hs.log exists!! ");
					outFile = "/data/log/[BT]msm_serial_hs_" + sysdump_time
							+ ".log";
					WriteToSDcard(
							"/mnt/sdcard/Android/data/[BT]msm_serial_hs.log",
							outFile, "[BT]msm_serial_hs.log");
				}

				if (hsuart_log_old.exists()) {
					Log.d(TAG, "[BT]msm_serial_hs.log.old exists!! ");
					outFile = "/data/log/[BT]msm_serial_hs_" + sysdump_time
							+ ".log.old";
					WriteToSDcard(
							"/mnt/sdcard/Android/data/[BT]msm_serial_hs.log.old",
							outFile, "[BT]msm_serial_hs.log.old");
				}
				return null;
			}

			protected void onProgressUpdate(Integer... values) {
				super.onProgressUpdate(values);
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				// Log.d(TAG, "duration = " + duration);
				Log.d(TAG, "Get dumpsys DONE-->");

				// hideProgressDialog();
				checkCopyToSdcard();

			}
		};
		getDumpTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

	}

	void stopSilentLog() {
		String silentlogging = String.valueOf(SystemProperties
				.get("dev.silentlog.on"));
		if ("On".equalsIgnoreCase(silentlogging)) {
			Intent SvcIntent = new Intent();
			SvcIntent.setClassName("com.sec.modem.settings",
					"com.sec.modem.settings.cplogging.SilentLogService");
			SvcIntent.putExtra("action", 3);
			startService(SvcIntent);
		}
	}

	private void getDumpstateAll() {
		sysdump_time = new SimpleDateFormat("yyyyMMddHHmm").format(Calendar
				.getInstance().getTime());
		Log.d(TAG, "Get dumpstate all");

		AsyncTask<Void, Integer, Void> getDump = new AsyncTask<Void, Integer, Void>() {
			@Override
			protected void onPreExecute() {
			};

			@Override
			protected Void doInBackground(Void... objects) {
				Log.d(TAG, "<--START_Get dumpstate");
				stopSilentLog();
				SendData(mOem.OEM_IPC_DUMP_BIN);
				Log.i(TAG, "run dumpstate");
				File dataLogDirectory = new File("/data/log");
				File btsnoop_log = new File(
						"/mnt/sdcard/Android/data/btsnoop_hci.log");
				File btsnoop_log_old = new File(
						"/mnt/sdcard/Android/data/btsnoop_hci.log.old");
				File hsuart_log = new File(
						"/mnt/sdcard/Android/data/[BT]msm_serial_hs.log");
				File hsuart_log_old = new File(
						"/mnt/sdcard/Android/data/[BT]msm_serial_hs.log.old");

				if (!dataLogDirectory.exists()) {
					dataLogDirectory.mkdir();
				}
				if (!isShowRenameDialog)
					startToastIntent("START_GET_DUMP",
							R.string.toast_start_get_dump);
				isDumpstateRunning = true;
				if ("gardaltetmo".equals(SystemProperties.get(
						"ro.product.name", "UNKNOWN"))) {
					mOem.DoShellCmd("dumpstate -r > /data/log/dumpState_"
							+ sysdump_time + ".log");
				} else {
					mOem.DoShellCmd("bugreport > /data/log/dumpState_"
							+ sysdump_time + ".log");
				}

				if (SystemProperties.get("persist.security.mdm.SElogs", "1")
						.equals("1")) {
					getSEAndroidLogs(); // get SEAndroid logs
				}
				getTSPLogs(); // get TSP logs

				if (btsnoop_log.exists()) {
					Log.d(TAG, "btsnoop_hci.log exists!! ");
					outFile = "/data/log/btsnoop_hci_" + sysdump_time + ".log";
					WriteToSDcard("/mnt/sdcard/Android/data/btsnoop_hci.log",
							outFile, "btsnoop_hci.log");
				}

				if (btsnoop_log_old.exists()) {
					Log.d(TAG, "btsnoop_hci.log.old exists!! ");
					outFile = "/data/log/btsnoop_hci_" + sysdump_time
							+ ".log.old";
					WriteToSDcard(
							"/mnt/sdcard/Android/data/btsnoop_hci.log.old",
							outFile, "btsnoop_hci.log.old");
				}

				if (hsuart_log.exists()) {
					Log.d(TAG, "[BT]msm_serial_hs.log exists!! ");
					outFile = "/data/log/[BT]msm_serial_hs_" + sysdump_time
							+ ".log";
					WriteToSDcard(
							"/mnt/sdcard/Android/data/[BT]msm_serial_hs.log",
							outFile, "[BT]msm_serial_hs.log");
				}

				if (hsuart_log_old.exists()) {
					Log.d(TAG, "[BT]msm_serial_hs.log.old exists!! ");
					outFile = "/data/log/[BT]msm_serial_hs_" + sysdump_time
							+ ".log.old";
					WriteToSDcard(
							"/mnt/sdcard/Android/data/[BT]msm_serial_hs.log.old",
							outFile, "[BT]msm_serial_hs.log.old");
				}

				return null;
			}

			protected void onProgressUpdate(Integer... values) {
				super.onProgressUpdate(values);
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				Log.d(TAG, "Get dumpsys DONE-->");
			}
		};
		getDump.execute();
		AsyncTask<Void, Integer, Void> getCp = new AsyncTask<Void, Integer, Void>() {
			@Override
			protected void onPreExecute() {
			};

			@Override
			protected Void doInBackground(Void... objects) {
				Log.d(TAG, "<--START_Get CP");

				mOem.DoShellCmd("logcat -v threadtime -b radio -d -f /data/log/radio_"
						+ sysdump_time + ".log");

				Log.i(TAG, "SendData(mOem.OEM_MODEM_LOG)");
				if (isEOS2) {
					SystemProperties.set("sys.trace.control", "path=dump");
					// iscpdumpdone = true;
					Log.i(TAG, "MODEMLOG_DONE Success, isEOS2=true");
				} else if (SecProductFeature_RIL.SEC_PRODUCT_FEATURE_RIL_SILENTLOG_STE) {
					String silentlogging = String.valueOf(SystemProperties
							.get("dev.silentlog.on"));
					Log.i(TAG, "Silent Log : " + silentlogging);
					if ("Off".equalsIgnoreCase(silentlogging)) {
						if (mOem.DoShellCmd("debug_interface_proxy --command=\"trace --trigger=/data/log/err\"") == 1) {
							iscpdumpdone = true;
						}
						Log.d(TAG, "send request modem logging to STE modem");
					} else {
						iscpdumpdone = true;
					}
				} else {
					SendData(mOem.OEM_MODEM_LOG);
				}

				return null;
			}

			protected void onProgressUpdate(Integer... values) {
				super.onProgressUpdate(values);
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				// Log.d(TAG "duration = " + duration);
				Log.d(TAG, "Get CP DONE-->");
				// checkCopyToSdcard();
			}
		};
		getCp.execute();

		String tcpdumping = String.valueOf(SystemProperties
				.get(TCPDUMP_PROPERTY));
		AsyncTask<Void, Integer, Void> getTCP = new AsyncTask<Void, Integer, Void>() {
			@Override
			protected void onPreExecute() {
			};

			@Override
			protected Void doInBackground(Void... objects) {
				Log.d(TAG, "<--START_Get TCP");

				Intent tcpDumpSvcIntent = new Intent().setClassName(
						"com.sec.tcpdumpservice",
						"com.sec.tcpdumpservice.TcpDumpService");
				String productship = String.valueOf(SystemProperties
						.get("ro.product_ship"));

				if ("false".equals(productship) && isMidOrHighDebugLevel()) {
					try {
						Log.d(TAG, "falseequals(productship)");
						stopService(tcpDumpSvcIntent); // auto TcpDump service
						Thread.sleep(1000);
						startService(tcpDumpSvcIntent); // auto TcpDump service
					} catch (InterruptedException e) {
						Log.e(TAG, "tcpDumpThread - exception occurred");
					}
				}
				return null;
			}

			protected void onProgressUpdate(Integer... values) {
				super.onProgressUpdate(values);
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				// Log.d(TAG, "duration = " + duration);
				Log.d(TAG, "Get TCP DONE-->");
				checkCopyToSdcard();

			}
		};
		getTCP.execute();
	}

	private boolean getTSPLogs() {
		final String srcName = "/proc/tsp_msg";
		final String dstName = "/data/log/tsp_msg_" + sysdump_time + ".log";
		boolean result = false;
		File srcFile = new File(srcName);

		if (srcFile != null && srcFile.exists()) {
			Log.i(TAG, "getTSPLogs : get TSP logs to " + dstName);
			result = WriteToSDcard(srcName, dstName, "TSP");
		} else {
			Log.i(TAG, "getTSPLogs : kernel node is not exist");
		}
		return result;
	}

	private boolean getSEAndroidLogs() {
		final String srcName = "/proc/avc_msg";
		final String dstName = "/data/log/avc_msg_" + sysdump_time + ".log";
		boolean result = false;
		File srcFile = new File(srcName);

		if (srcFile != null && srcFile.exists()) {
			Log.i(TAG, "getSEAndroidLogs : get SEAndroid logs to " + dstName);
			result = WriteToSDcard(srcName, dstName, "SEAndroid");
		} else {
			Log.i(TAG, "getSEAndroidLogs : kernel node is not exist");
		}
		return result;
	}

	public boolean WriteToSDcard(String in, String out, String DumpType) {
		Log.i(TAG, "write to sdcard DumpType : " + DumpType);
		boolean result = true;
		int n;
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(in);
			fos = new FileOutputStream(out);

			while ((n = fis.read(buf)) > -1) {
				fos.write(buf, 0, n);
			}
			fos.flush();
		} catch (FileNotFoundException fnfe) {
			Log.e(TAG, "fnfe : " + fnfe.getMessage());
			result = false;
			System.err.println("// Exception from");
			Log.d(TAG,
					"FileNotFoundException : " + Log.getStackTraceString(fnfe));
		} catch (Exception e) {
			result = false;
			Log.d(TAG, "Exception : " + Log.getStackTraceString(e));
		} finally {
			if (fos != null) {
				try {
					fos.getFD().sync();
				} catch (Exception e) {
					Log.d(TAG, "Exception : " + Log.getStackTraceString(e));
				}
				try {
					fos.close();
				} catch (Exception e) {
					Log.d(TAG, "Exception : " + Log.getStackTraceString(e));
				}
			}

			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
					Log.d(TAG, "Exception : " + Log.getStackTraceString(e));
				}
			}
		} // end of finally

		return (result);
	}

	private void checkCopyToSdcard() {
		final StringBuilder sbConsole = new StringBuilder();
		final StringBuilder sbErrors = new StringBuilder();

		AsyncTask<Void, Integer, Void> recordTask = new AsyncTask<Void, Integer, Void>() {
			@Override
			protected void onPreExecute() {

			};

			@Override
			protected Void doInBackground(Void... objects) {
				startToastIntent("START_COPY_TO_SDCARD",
						R.string.toast_copy_to_sdcard);
				File dataLogDirectory = new File("/data/log");
				File silentLogDirectory = new File("/data/slog");
				File dataCPLogDirectory = new File("/data/log/err");
				File dataCPLogDirectoryEfs = new File("/efs/root/ERR");
				File dataCPCrashLogDirectory = new File("/tombstones/mdm");
				File dataCPLogNewDirectory = new File("/data/cp_log");
				File sdcardLogDirectory = new File("/mnt/sdcard/log");
				File sdcardCPCrashLogDirectory = new File("/mnt/sdcard/log/cp");
				File btlog = new File("/data/app/bt.log");

				File NVMDirectory = new File("/NVM");
				File sdcardNVMDirectory = new File("/mnt/sdcard/log/NVM");

				// if no exist sdcardLogDirectory, first make it.
				if (!sdcardLogDirectory.exists()) {
					sdcardLogDirectory.mkdir();
				}

				if (SecProductFeature_COMMON.SEC_PRODUCT_FEATURE_COMMON_ENABLE_EXT_DEBUG_CUSTOMER_SVC) {
					File dropboxLogDirectory = new File("/data/system/dropbox");
					File sdcardDropboxLogDirectory = new File(
							"/mnt/sdcard/log/dropbox");
					copyDirectory(dropboxLogDirectory,
							sdcardDropboxLogDirectory);

					File dataSRLogDirectory = new File("/data/radio");
					copyDirectory(dataSRLogDirectory, sdcardLogDirectory);
				}

				copyDirectory(dataLogDirectory, sdcardLogDirectory);
				/*
				 * copyDirectory(silentLogDirectory, sdcardLogDirectory);
				 * copyDirectory(dataCPCrashLogDirectory,
				 * sdcardCPCrashLogDirectory);
				 * copyDirectory(dataCPLogNewDirectory,
				 * sdcardCPCrashLogDirectory);
				 * 
				 * if (isMarvell) { File cpCrashDumpFile = new
				 * File("data/com_DDR_RW.bin"); if(cpCrashDumpFile.exists()){
				 * Log.d(TAG, "com_DDR_RW.bin file is exist");
				 * copyDirectory(cpCrashDumpFile,sdcardNVMDirectory); }else{
				 * Log.i(TAG, "com_DDR_RW.bin file is not exist"); }
				 * copyDirectory(NVMDirectory, sdcardNVMDirectory); }
				 */

				/*
				 * if
				 * (dataCPLogDirectory.getPath().contains(dataLogDirectory.getPath
				 * ()) == false) { copyDirectory(dataCPLogDirectory,
				 * sdcardLogDirectory); }
				 * 
				 * if
				 * (dataCPLogDirectoryEfs.getPath().contains(dataLogDirectory.
				 * getPath()) == false) { copyDirectory(dataCPLogDirectoryEfs,
				 * sdcardLogDirectory); }
				 * 
				 * if (btlog.exists()) { Log.i(TAG, "btlog.exists == true");
				 * WriteToSDcard("/data/app/bt.log", "/mnt/sdcard/log/bt.log",
				 * "bt.log"); }
				 * 
				 * // handler.sendEmptyMessage(0); Log.i(TAG,
				 * "broadcast media mounted = " +
				 * Environment.getExternalStorageDirectory()); Intent intent =
				 * new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" +
				 * Environment.getExternalStorageDirectory().getPath()));
				 * sendBroadcast(intent);
				 */
				return null;
			}

			protected void onProgressUpdate(Integer... values) {
				super.onProgressUpdate(values);
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				try {
					rescanSdcard();
				} catch (Exception e) {

				}
				showDumpResult();
				if (!isMerging) {
					hideNotification();
					stopService();
				} else
					updateGetDupstateNotification();
				isDumpstateRunning = false;

			}
		};
		recordTask.execute();

	}

	public void copyDirectory(File src, File dest) {
	//	Log.d(TAG, "copyDirectory : " + src + " / " + dest);

		if (src.isDirectory()) {
			if (!dest.exists()) {
				dest.mkdir();
			}

			String[] fileList = src.list();

			if (fileList == null || fileList.length <= 0) {
				return;
			}

			for (int i = 0; i < fileList.length; i++) {
				copyDirectory(new File(src, fileList[i]), new File(dest,
						fileList[i]));
			}
		} else {

			FileInputStream fin = null;
			FileOutputStream fout = null;

			try {
				fin = new FileInputStream(src);
				fout = new FileOutputStream(dest);
				// Copy the bits from instream to outstream
				byte[] buffer = new byte[1024];
				int len;

				while ((len = fin.read(buffer)) > 0) {
					fout.write(buffer, 0, len);
				}
				fout.flush();
			} catch (FileNotFoundException fnfe) {
				System.err.println("// Exception from");
				Log.d(TAG,
						"FileNotFoundException : "
								+ Log.getStackTraceString(fnfe));
			} catch (Exception e) {
				Log.d(TAG, "Exception : " + Log.getStackTraceString(e));
			} finally {
				if (fin != null) {
					try {
						fin.close();
					} catch (Exception e) {
						Log.d(TAG, "Exception : " + Log.getStackTraceString(e));
					}
				}

				if (fout != null) {
					try {
						fout.close();
					} catch (Exception e) {
						Log.d(TAG, "Exception : " + Log.getStackTraceString(e));
					}
				}
			} // end of finally
		}
	}

	private String getName() {
		String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(Calendar.getInstance().getTime()) + ".mp4";
		String name = Environment.getExternalStorageDirectory()
				.getAbsolutePath().toString()
				+ "/VSR-" + fileName;
		return name;
	}

	private void startRecordScreen(String bitRate) {

		Log.d(TAG, "startRecordScreen");
		isRecorded = false;
		start = System.currentTimeMillis();
		// Log.d(TAG, "--start = " + start);
		StringBuilder stringBuilder = new StringBuilder(
				"screenrecord --verbose ");
		fileName = new SimpleDateFormat(".yyyyMMdd_HHmmss").format(Calendar
				.getInstance().getTime());
		String name = Environment.getExternalStorageDirectory()
				.getAbsolutePath().toString()
				+ "/" + fileName + ".mp4";
		stringBuilder.append("  --bit-rate ").append(bitRate).append(" --size ").append(WIDTH).append("x").append(HEIGHT).append(" ").append(name);
		// stringBuilder.append("  --bit-rate 8000000").append(" ").append(name);
		workingPath = Environment.getExternalStorageDirectory() + "/";// Environment.getExternalStorageDirectory().getAbsolutePath().toString()+"/";
		videosToMerge.add(fileName + ".mp4");
		count++;
		record(stringBuilder.toString());

	}

	public void stopRecordScreen() {
		Log.d(TAG, "stopRecordScreen()");
		String pid = null;
		Process p;
		try {
			// get screenrecord id
			p = Runtime.getRuntime().exec("ps");
			p.waitFor();
			StringBuffer sb = new StringBuffer();
			InputStreamReader isr = new InputStreamReader(p.getInputStream());
			int ch;
			char[] buf = new char[1024];
			try {
				while ((ch = isr.read(buf)) != -1) {
					sb.append(buf, 0, ch);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			String[] processLinesAr = sb.toString().split("\n");
			for (String line : processLinesAr) {
				String[] comps = line.split("[\\s]+");
				if (comps.length == 9) {
					if (comps[8]
							.equalsIgnoreCase(getString(R.string.screenrecord))) {
						pid = comps[1];
						android.os.Process.sendSignal(Integer.parseInt(pid),
								SIGNAL_KILL);
					}
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void record(final String command) {
		final StringBuilder sbConsole = new StringBuilder();
		final StringBuilder sbErrors = new StringBuilder();

		AsyncTask<Void, Integer, Void> recordTask = new AsyncTask<Void, Integer, Void>() {
			@Override
			protected Void doInBackground(Void... objects) {
				Log.d(TAG, "<--START_RECORD.doInBackground()");
				DataOutputStream os = null;
				BufferedReader isReader = null;
				BufferedReader esReader = null;
				try {
					long start = System.currentTimeMillis();
					Process process = Runtime.getRuntime().exec(command);
					isReader = new BufferedReader(new InputStreamReader(
							process.getInputStream()));
					esReader = new BufferedReader(new InputStreamReader(
							process.getErrorStream()));
					os = new DataOutputStream(process.getOutputStream());
					os.writeBytes("exit\n");
					os.flush();
					String line;
					process.waitFor();
					long end = System.currentTimeMillis();
					duration = (end - start) / 1000;
					int d = (int) (end - start);
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "Exception: ", e);
				} catch (InterruptedException e) {
					Log.e(getClass().getSimpleName(), "Exception: ", e);
				}

				return null;
			}

			protected void onProgressUpdate(Integer... values) {
				super.onProgressUpdate(values);
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				// Log.d(TAG, "duration = " + duration);
				Log.d(TAG, "STOP_RECORD onPostExecute-->");
				isRecorded = true;

				if (duration >= 180 && timeCounter < maxTime) {
					stopRecordScreen();
					startRecordScreen(Integer.toString(bitRate));
				}

			}
		};
		recordTask.execute();

	}

	void removeVideo() {
		Log.d(TAG, "Remove video");
		for (int i = videosToMerge.size() - 1; i >= 0; i--) {
			String name = workingPath + videosToMerge.get(i).toString();
			File file = new File(name);
			boolean deleted = file.delete();

			/*Log.e(TAG, "deleted NAME =" + videosToMerge.get(i).toString()
					+ String.valueOf(deleted));*/

			videosToMerge.remove(i);
		}
		isMerging = false;
		String hidenName = workingPath + ".VSR-" + fileName.substring(1)
				+ ".mp4";
		File hidenFile = new File(hidenName);
		if (isCancel) {
			if (hidenFile.exists()) {
				hidenFile.delete();
				Log.d(TAG, "file need to removed exist");
			}

			startToastIntent("TOAST_CANCEL_RECORD",
					R.string.toast_cancel_record);
		}

		isMerging = false;
		if (!isCancel && (!isShowRenameDialog())) {
			// Log.d(TAG, "Hidden merge = " + hidenName);
			String visibleName = workingPath + fileName.substring(1) + ".mp4";
			File visibleFile = new File(visibleName);
			if (hidenFile.exists()) {
				if (isRenameCalled == false)
					hidenFile.renameTo(visibleFile);
				else
					hidenFile.renameTo(newFile);
				try {
					rescanSdcard();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				startToastIntent("TOAST_VIDEO_SAVED",
						R.string.toast_video_saved);
				Log.d(TAG, "MERGEVIDEOonPostExecute");
			}/*
			 * else Log.d(TAG, "merged hiden not exist");
			 */

		}
		// Log.d(TAG, "isDumpstateRunning = " + isDumpstateRunning);

		if ((!isShowRenameDialog()) && !isGetLog() && isMerging == false) {
			Log.d(TAG, "stop due to dk");
			stopSilentLog();
			hideNotification();
			stopService();
		}
	}

	boolean isShowRenameDialog() {
		Log.d(TAG, "isShowRenameDialog = " + isShowRenameDialog);
		return isShowRenameDialog;
	}

	public boolean isRunningRumpstate() {
		return isDumpstateRunning;
	}

	boolean isGetLog() {
		return isGetApCp || isGetDumpstate;
	}

	void removeSmallSize() {
		for (int i = videosToMerge.size() - 1; i >= 0; i--) {
			String name = workingPath + videosToMerge.get(i).toString();
			File file = new File(name);
			Long size = (long) file.length();
		//	Log.d(TAG, "i=" + i + " name = " + name + " size = " + size);
			if (size < 50) {
				boolean deleted = file.delete();
				videosToMerge.remove(i);
				/*Log.d(TAG, "delete small size " + name + " deleted = "
						+ deleted);*/
			}
		}

	}

	void stopService() {
		Log.d(TAG, "stopService");
		System.exit(0);
		stopSelf();
	}

	private void rescanSdcard() throws Exception {
		Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED,
				Uri.parse("file://"
						+ Environment.getExternalStorageDirectory().getPath()));
		sendBroadcast(intent);
	}

	public void MergeVideo(final String workingPath,
			ArrayList<String> videoToMerge) {
		this.workingPath = workingPath;
		this.videosToMerge = videosToMerge;

		AsyncTask<Void, Integer, Void> mergeVideoTask = new AsyncTask<Void, Integer, Void>() {

			@Override
			protected Void doInBackground(Void... arg0) {
				// TODO Auto-generated method stub
				Log.d(TAG, "MERGEVIDEO doInBackground");
				isMerging = true;
				removeSmallSize();
				int count = videosToMerge.size();
				Log.d(TAG, "count SIZE=" + count);
				try {
					Movie[] inMovies = new Movie[count];
					for (int i = 0; i < count; i++) {

						String file = workingPath
								+ videosToMerge.get(i).toString();
						inMovies[i] = MovieCreator.build(file);
					}
					List<Track> videoTracks = new LinkedList<Track>();
					List<Track> audioTracks = new LinkedList<Track>();

					for (Movie m : inMovies) {
						for (Track t : m.getTracks()) {
							if (t.getHandler().equals("soun")) {
								audioTracks.add(t);
							}
							if (t.getHandler().equals("vide")) {
								videoTracks.add(t);
							}
						}
					}

					Movie result = new Movie();

					if (audioTracks.size() > 0) {
						result.addTrack(new AppendTrack(audioTracks
								.toArray(new Track[audioTracks.size()])));
					}
					if (videoTracks.size() > 0) {
						result.addTrack(new AppendTrack(videoTracks
								.toArray(new Track[videoTracks.size()])));
					}

					Container out = new DefaultMp4Builder().build(result);

					String name = workingPath + ".VSR-" + fileName.substring(1)
							+ ".mp4";
					Log.d(TAG, "merge file = " + name);

					RandomAccessFile ram = new RandomAccessFile(name, "rw");
					FileChannel fc = ram.getChannel();
					out.writeContainer(fc);
					ram.close();
					fc.close();

				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				// TODO Auto-generated method stub
				super.onPostExecute(result);
				removeVideo();
			}

		};
		mergeVideoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

	}

	private void SendData(int cmd) {
		byte[] data = null;
		int tempCMD;

		if (cmd == mOem.OEM_OEM_DUMPSTATE_MODEM_LOG_AUTO_START) {
			tempCMD = mOem.OEM_MODEM_LOG;
		} else if (cmd == mOem.OEM_DUMPSTATE_ALL) {
			tempCMD = mOem.OEM_DUMPSTATE;
		} else {
			tempCMD = cmd;
		}

		data = mOem.StartSysDumpData(tempCMD);

		if (data == null) {
			Log.i(TAG, " err - data is NULL");
			return;
		}

		if (cmd == mOem.OEM_IPC_DUMP_BIN) {
			invokeOemRilRequestRaw(data, mHandler.obtainMessage(IPC_DUMP_DONE));

		} else if (cmd == mOem.OEM_MODEM_LOG) {
			invokeOemRilRequestRaw(data,
					mHandler.obtainMessage(QUERY_MODEMLOG_DONE));
		} else if (cmd == mOem.OEM_IPC_DUMP_BIN) {
			invokeOemRilRequestRaw(data, mHandler.obtainMessage(IPC_DUMP_DONE));
		}
	}

	private void invokeOemRilRequestRaw(byte[] data, Message response) {
		Bundle req = response.getData();
		req.putByteArray("request", data);
		response.setData(req);
		response.replyTo = mSvcModeMessenger;
		int cnt = 0;

		try {
			for (cnt = 0; cnt < 10; cnt++) {
				if (bindservice_flag == 1) {
					mServiceMessenger.send(response);
					break;
				} else {
					try {
						Log.i(TAG, "mServiceMessenger is NULL");
						Thread.sleep(200);
					} catch (InterruptedException ie) {
						;
						;
					} catch (NullPointerException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (RemoteException e) {
			;
			;
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
}
