package com.sec.android.recordscreen;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.Toast;

public class DisplayToast extends Activity {
	Context mContext;
	private AlertDialog.Builder builder;
	String sysdump_time;
	String dialog_message;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		mContext = getApplicationContext();
		builder = new AlertDialog.Builder(this);
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			

			int min_time_to_pause = bundle.getInt("TOAST_TIME_TO_PAUSE");
			int min_time_to_stop = bundle.getInt("TOAST_TIME_TO_STOP");
			int start_get_dum = bundle.getInt("START_GET_DUMP");
			int start_copy_to_sdcard = bundle.getInt("START_COPY_TO_SDCARD");
			int get_dump_done = bundle.getInt("GET_DUMP_DONE");
			int save_video = bundle.getInt("TOAST_SAVE_VIDEO");
			int video_saved = bundle.getInt("TOAST_VIDEO_SAVED");
			int finish_rename = bundle.getInt("TOAST_ENTER_NEW_NAME");
			int cancel_record = bundle.getInt("TOAST_CANCEL_RECORD");
			sysdump_time = bundle.getString("SYSDUMP_NAME");

			if (sysdump_time != null) {
				Log.d("svr", "sysdump_time = " + sysdump_time);
				showDumpResult();
			}

			if (min_time_to_pause != 0)
				showToast(min_time_to_pause);
			else if (min_time_to_stop != 0)
				showToast(min_time_to_stop);
			else if (start_get_dum != 0)
				showToast(start_get_dum);
			else if (start_copy_to_sdcard != 0)//
				showToast(start_copy_to_sdcard);
			else if (get_dump_done != 0)
				showToast(get_dump_done);
			else if (save_video != 0)
				showToast(save_video);
			else if (video_saved != 0)
				showToast(video_saved);
			else if (finish_rename != 0)
				showToast(finish_rename);
			else if (cancel_record != 0)
				showToast(cancel_record);
		}

	}

	public void showDumpResult() {
		String inFile_dumpState = "/data/log/dumpState_" + sysdump_time
				+ ".log";
		File oFile_dumpState = new File(inFile_dumpState);

		if (oFile_dumpState.exists()) {
			Long dumpstate_file_len = oFile_dumpState.length();
			dialog_message = "Saved Location :\n"
					+ oFile_dumpState.getAbsoluteFile() + "\n("
					+ dumpstate_file_len / 1024 + "Kb)" + "\n";
			if (dumpstate_file_len < 1024) {
				dialog_message += "dumpstate is still running.\n";
				dialog_message += "Please retry to get dumpstate about 2 minutes later.\n";
			}
		} else
			dialog_message = "Get dumpstate fail";

		if (builder != null) {
			// builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setTitle("Dump Result");
			builder.setMessage(dialog_message);
			builder.setPositiveButton(R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {

							finish();
						}
					});
			builder.setOnKeyListener(new DialogInterface.OnKeyListener() {

				@Override
				public boolean onKey(DialogInterface dialog, int keyCode,
						KeyEvent event) {
					// TODO Auto-generated method stub
					if (keyCode == KeyEvent.KEYCODE_BACK)
						finish();
					return false;
				}
			});
			builder.setCancelable(true);
			// builder.show();
			AlertDialog dialog = builder.create();
			dialog.show();
			sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
		}
	}

	public void showToast(int string) {
		Toast.makeText(mContext, string, Toast.LENGTH_SHORT).cancel();
		Toast.makeText(mContext, string, Toast.LENGTH_SHORT).show();
		finish();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		finish();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		finish();
	}

}
