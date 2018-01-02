package com.sec.android.recordscreen;

import java.util.List;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

public class ScreenRecordCancelDialogActivity extends Activity {

	private SRNotificationManagerService mNotificationManager;
	private Notification mNotification;
	String TAG = "svrCANCELDIALG";
	TextView time;
	final static String SCREEN_RECORD_CANCEL = "com.sec.android.recordscreen.cancel";
	final static String STOP_SERVICE = "com.sec.android.recordscreen.stop_service";
	RemoteViews remoteViews;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = getIntent().getExtras();
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
		if (bundle != null) {
			boolean isCancelGetDump = bundle.getBoolean("cancel_get_dump");
			// Log.d("SVR", "isCancelGetDump=" + isCancelGetDump);
			if (isCancelGetDump)
				showCancelGetDumpsateDialog();
		} else
			showCancelRecordingDialog();

	}

	public void showCancelRecordingDialog() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(R.string.screen_record_notification_title)
				.setMessage(R.string.cancel_record_screen)
				.setOnKeyListener(new DialogInterface.OnKeyListener() {

					@Override
					public boolean onKey(DialogInterface dialog, int keyCode,
							KeyEvent event) {
						// TODO Auto-generated method stub
						if (keyCode == KeyEvent.KEYCODE_BACK)
							finish();
						return false;
					}
				});
		dialog.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						Intent filter = new Intent();
						filter.setAction(SCREEN_RECORD_CANCEL);
						sendBroadcast(filter);
						Log.d(TAG, "is RenameActivity running = "
								+ isRenameActivityRunning());
						if (isRenameActivityRunning())
							RenameDialog.renameDlg.finish();
						finish();
					}
				});
		dialog.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						finish();
					}
				});
		AlertDialog cancelDialog = dialog.create();
		cancelDialog.show();
	}

	public void showCancelGetDumpsateDialog() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(R.string.screen_record_notification_title)
				.setMessage(R.string.cancel_get_dump)
				.setOnKeyListener(new DialogInterface.OnKeyListener() {

					@Override
					public boolean onKey(DialogInterface dialog, int keyCode,
							KeyEvent event) {
						// TODO Auto-generated method stub
						if (keyCode == KeyEvent.KEYCODE_BACK)
							finish();
						return false;
					}
				});
		dialog.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						Intent filter = new Intent();
						filter.setAction(STOP_SERVICE);
						sendBroadcast(filter);
						if (isRenameActivityRunning())
							RenameDialog.renameDlg.finish();
						Toast.makeText(getApplicationContext(),
								"Your dumpstate has not been saved",
								Toast.LENGTH_SHORT).show();

						finish();
					}
				});
		dialog.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						finish();
					}
				});
		AlertDialog cancelDialog = dialog.create();
		cancelDialog.show();
	}

	protected Boolean isRenameActivityRunning() {
		ActivityManager activityManager = (ActivityManager) getBaseContext()
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningTaskInfo> tasks = activityManager
				.getRunningTasks(Integer.MAX_VALUE);

		for (ActivityManager.RunningTaskInfo task : tasks) {
			if ("com.sec.android.recordscreen.RenameDialog"
					.equalsIgnoreCase(task.baseActivity.getClassName()))
				return true;
		}

		return false;
	}
}