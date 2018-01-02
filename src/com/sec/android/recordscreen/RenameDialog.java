package com.sec.android.recordscreen;

import java.io.File;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RenameDialog extends Activity {
	Context mContext;
	EditText rename_entry;
	AlertDialog dialog;
	String TAG = "SVRRenameDialog";
	String oldName;
	boolean isShowRenameDg;
	boolean isGetDumpstate;
	boolean isGetApCpDump;
	long start;
	final static String SCREEN_RECORD_SET_NEWNAME = "com.sec.android.recordscreen.set_newname";
	final static String SCREEN_RECORD_STOP = "com.sec.android.recordscreen.stop";
	private final int MAX_NAME_LENGTH = 50;

	private Toast mToast;
	public final static String INVALID_CHAR[] = { "\\", "/", ":", "*", "?",
			"\"", "<", ">", "|", "\n" };
	String newName = "";
	public static RenameDialog renameDlg;
	String workingPath = Environment.getExternalStorageDirectory() + "/";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub

		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		mContext = getApplicationContext();
		sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
		renameDlg = this;
		Log.d(TAG, "RenameDialog");
		// if(MainActivity.isShowRenameDialog)
		String mergeFileName = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(Calendar.getInstance().getTime());

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			oldName = extras.getString("video_name");
			isGetDumpstate = extras.getBoolean("GET_DUMPSTATE");
			isGetApCpDump = extras.getBoolean("GET_DUMPSTATE_CP");
		}

		if (isGetDumpstate || isGetApCpDump) {

			Toast.makeText(mContext, "Start getting dumpstate...",
					Toast.LENGTH_LONG).show();
			Handler myHandle = new Handler();
			myHandle.postDelayed(new Runnable() {

				@Override
				public void run() { // TODO Auto-generated method stub
					showRenameDialog();
				}
			}, 3000);

		} else

			showRenameDialog();

	}

	public void showRenameDialog() {
		Log.d(TAG, "showRenameDialg");

		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		View renameLayout = inflater.inflate(R.layout.alert_rename_dialog,
				(ViewGroup) findViewById(R.id.rename_dialog_layout));
		rename_entry = (EditText) renameLayout.findViewById(R.id.rename_entry);
		String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(Calendar.getInstance().getTime());
		rename_entry.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
				| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		rename_entry.setText(oldName);

		rename_entry.selectAll();
		openSip();
		rename_entry.requestFocus();
		rename_entry.isInTouchMode();

		InputFilter filter = new InputFilter() {
			@Override
			public CharSequence filter(CharSequence text, int start, int end,
					Spanned spanned, int dstStart, int dstEnd) {
				if (end - start > MAX_NAME_LENGTH * 2)
					end = start + MAX_NAME_LENGTH * 2;
				String origTxt = text.subSequence(start, end).toString();
				String validTxt = origTxt;
				boolean invalidFlag = false;

				for (int i = 0; i < RenameDialog.INVALID_CHAR.length; ++i) {
					int validTxtLength = validTxt.length();
					for (int j = 0; j < validTxtLength; ++j) {
						int index = validTxt
								.indexOf(RenameDialog.INVALID_CHAR[i]);
						if (index >= 0) {
							invalidFlag = true;
							if (index < validTxt.length()) {
								// exclude the invalid character from the input
								// text
								validTxt = validTxt.substring(0, index)
										+ validTxt.substring(index + 1);
							}
						}
						// when using DBC invalid character, it worked only in
						// eclipse
						// so check DBC code directly
						else {
							char c = RenameDialog.INVALID_CHAR[i].charAt(0);
							if (c >= 0x21 && c < 0x7e && c != 0x3f) {
								c += 0xfee0;
								int iDBC = validTxt.indexOf(c);
								if (iDBC >= 0) {
									invalidFlag = true;
									if (iDBC < validTxt.length()) {
										// exclude the invalid character from
										// the input text
										validTxt = validTxt.substring(0, iDBC)
												+ validTxt.substring(iDBC + 1);
									}
								}
							}
						}
					}
				}

				if (invalidFlag) { // has invalid characters
					showToast("Invalid character", Toast.LENGTH_SHORT);
					return validTxt;
				}
				return null; // pass!
			}
		};

		InputFilter.LengthFilter lengthFilter = new InputFilter.LengthFilter(
				MAX_NAME_LENGTH) {
			@Override
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				CharSequence rst = super.filter(source, start, end, dest,
						dstart, dend);
				if (rst != null) {
					showToast(
							mContext.getResources().getString(
									R.string.max_character, MAX_NAME_LENGTH),
							Toast.LENGTH_SHORT);
				}
				return rst;
			}
		};

		rename_entry.setPrivateImeOptions("inputType=filename");
		rename_entry.setFilters(new InputFilter[] { filter, lengthFilter });

		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Rename");
		builder.setView(renameLayout);
		builder.setOnKeyListener(new DialogInterface.OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				// TODO Auto-generated method stub
				if (keyCode == KeyEvent.KEYCODE_BACK
						|| keyCode == KeyEvent.KEYCODE_HOME) {
					sendRename(oldName);
					finish();
				}
				Toast.makeText(getApplicationContext(),
						"Your video has been saved", Toast.LENGTH_SHORT);
				return false;
			}
		});
		builder.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
					}
				});
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						sendRename(oldName);
						// hideSoftInput(rename_entry.getWindowToken());
						if (dialog != null)
							dialog.dismiss();
						finish();
					}
				});
		builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				hideSoftInput(rename_entry.getWindowToken());
				dialog.dismiss();
				// TODO Auto-generated method stub
				// builder = null;
			}
		});

		dialog = builder.create();
		dialog. getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); 
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				openSip();
			}
		});

		dialog.show();
	
		
		rename_entry.setFocusable(true);
		rename_entry.setFocusableInTouchMode(true);

		rename_entry.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					if (keyCode == KeyEvent.KEYCODE_ENTER
							|| keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
						if (oldName != null
								&& oldName.equals(rename_entry.getText()
										.toString())) {
							return true;
						}

						String filenameToRename = rename_entry.getText()
								.toString().trim();
						/*
						 * if (fileAlreadyExist(filenameToRename)) { return
						 * true; }
						 */

						String mString = rename_entry.getText().toString();
						if (mString.trim().isEmpty()) {
							return true;
						}

					}
				}
				return false;
			}
		});
		Button positiveButton = dialog
				.getButton(DialogInterface.BUTTON_POSITIVE);
	
		positiveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				String filenameToRename = rename_entry.getText().toString()
						.trim();
				String filePath = workingPath+filenameToRename+".mp4";
				File file = new File(filePath);
				if (file.exists()) {
					Log.d(TAG,"file exist");
					Toast.makeText(mContext,
							"This file name is already in used",
							Toast.LENGTH_SHORT).show();
					return;
				}

				String mString = rename_entry.getText().toString();
				if (mString.trim().isEmpty()) {
					Toast.makeText(mContext, "Name cannot be empty",
							Toast.LENGTH_SHORT).show();
					return;
				}

				newName = rename_entry.getText().toString();
				sendRename(newName);
				hideSoftInput(rename_entry.getWindowToken());
				dialog.dismiss();
				finish();
			}
		});

	}
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
			Log.d(TAG,"focus");
	        try
	        {
	           if(!hasFocus)
	           {
	                Object service  = getSystemService("statusbar");
	                Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
	                Method collapse = statusbarManager.getMethod("collapse");
	                collapse .setAccessible(true);
	                collapse .invoke(service);
	           }
	        }
	        catch(Exception ex)
	        {
	        }
	}
	

	private void openSip() {

		InputMethodManager imm = (InputMethodManager) mContext
				.getSystemService(Context.INPUT_METHOD_SERVICE);

		imm.showSoftInput(rename_entry, InputMethodManager.SHOW_IMPLICIT);

	}

	public void sendRename(String name) {
		Intent intent = new Intent();
		intent.putExtra("new_name", name);
		intent.setAction(SCREEN_RECORD_SET_NEWNAME);
		sendBroadcast(intent);
	}

	

	private void showToast(String string, int lenght) {
		if (mToast != null) {
			mToast.cancel();
			mToast = null;
		}
		mToast = Toast.makeText(mContext, string, lenght);
		if (mToast != null)
			mToast.show();
	}

	public boolean fileExistance(String fname) {

		String workingPath = Environment.getExternalStorageDirectory() + "/";
		String fileName = workingPath + rename_entry.getText().toString()
				+ ".mp4";
		File file = new File(fileName);
		return file.exists();
	}

	private void hideSoftInput(IBinder windowToken) {

		InputMethodManager imm = (InputMethodManager) mContext
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(windowToken, 0);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		//Log.d(TAG, "onPause");
	
		super.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
	//	Log.d(TAG, "onResume");
		openSip();
		super.onResume();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
	//	Log.d(TAG, "onStop");
		
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
	//	Log.d(TAG, "onDestroy");
		super.onDestroy();
	}


}
