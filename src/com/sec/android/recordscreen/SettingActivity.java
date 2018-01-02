package com.sec.android.recordscreen;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import LibOTPSecurity.OTPSecurity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.BadTokenException;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sec.android.app.SecProductFeature_COMMON;
import com.sec.android.app.SecProductFeature_RIL;

public class SettingActivity extends Activity {
	ListView listView;
	CheckBox selectAll;
	ArrayAdapter<String> adapter;
	String TAG = "svrSettingActivity";
	int count;
	private AlertDialog.Builder builder;
	Button okBtn;
	Button delDumpBtn;
	Button mSilentLog;
	Button mTcpDump;
	Button mApCpDump;
	Intent stateIntent;
	AlertDialog mDialog;
	private static Toast mToast;
	private SharedPreferences settings;
	private ProgressDialog progressDialog;
	 private InputMethodManager im;
	String debugLevel;
//	private static final int QUERY_TCP_DUMP_DONE = 1;
	private static final int QUERY_TCP_DUMP_START = 2;
	private static final int SILENT_LOG_START = 3;
//	private static final int QUERY_SILENTLOG_DONE = 4;
	private String TCPDUMP_INTERFACE = "any";
    private static final int CP_POPUP_UI_ENABLE = 0x01;
    
    
    private static final int CP_POPUP_UI_DISABLE = 0x02;
    private static int iCPPopupUIState = CP_POPUP_UI_DISABLE;

    private Messenger mServiceMessenger = null;
    private static int bindservice_flag = 0;
	String month, day, hour, min, sec;
	final int OEM_RAMDUMP_MODE = 0x0A;
	final int OEM_SYSDUMP_FUNCTAG = 7;
	private static final String DEBUG_LEVEL_LOW_STRING = "0x4f4c";
	private static final String DEBUG_LEVEL_MID_STRING = "0x494d";
	private static final String DEBUG_LEVEL_HIGH_STRING = "0x4948";
	private static final int RAMDUMP_MODE_LOW = 0x0;
	private static final int RAMDUMP_MODE_MID = 0x1;
	private static final int RAMDUMP_MODE_HIGH = 0x2;
	private static final int RAMDUMP_MODE_AUTO = 0x3;
	private static final boolean wifiOnly = false;
	private static final String DELETE_DUMP_DIR = "/data/log";
	private static final String workingPath = Environment
			.getExternalStorageDirectory() + "/log";
	  private static final int RAMDUMP_MODE_DONE = 1007;
	    private static final int ENABLE_DBG_DONE = 1008;
	    private static final int QUERY_DONE = 1009;
	    private static final int QUERY_DBG_STATE_DONE = 1010;
	    private static final int QUERY_RAMDUMP_STATE_DONE = 1011;
	    private static final int QUERY_SILENTLOG_DONE = 1004;
	    private static final int QUERY_DUMPSTATE_DONE = 1005;
	    private static final int QUERY_DUMPSTATE_ALL_DONE = 1016;
	    private static final int IPC_DUMP_DONE = 1012;
	    private static final int QUERY_FD_STATE_DONE = 1013;
	    private static final int QUERY_MODEMLOG_DONE = 1014;
	    private static final int QUERY_DUMPSTATE_DONE_AND_MODEM_DUMP_START = 1015;
	    private static final int QUERY_TCP_DUMP_DONE = 1017;
	    private static final int QUERY_COPY_CARD_DONE = 1018;
	    private static final int QUERY_CP_DUMP_START = 1019;
	    private static final int QUERY_CP_DUMP_TIMEOUT = 1020;
	    private static final int WIFI_TCPDUMP_DONE = 1029;
	    // activity request
	    private static final int DIAG_MDLOG_REQUEST = 10;
	    //CP POPUP UI Control
	    private static final int QUERY_CP_POPUP_UI_STATE_DONE = 1021;
	    private static final int TOGGLE_CP_POPUP_UI_STATE_DONE = 1022;
	private OemCommands mOem = null;
	Context mContext;
	private static final boolean isBroadcom = SecProductFeature_COMMON.SEC_PRODUCT_FEATURE_COMMON_HW_VENDOR_CONFIG
			.equals("Blueberry");
	private static final boolean isEOS2 = SystemProperties
			.get("ro.board.platform", "Unknown").trim().startsWith("EOS2")
			|| SystemProperties.get("ro.board.platform", "Unknown").trim()
					.startsWith("u2");
	  public Handler mHandler = new Handler() {
	        public void handleMessage(Message msg) {
	            switch (msg.what) {
	            case QUERY_DONE: {
	                hideProgressDialog();
	                Log.i(TAG, "Sys dump Success");
	                break;
	            }
	            case QUERY_TCP_DUMP_DONE: {
	                int error = msg.getData().getInt("error");

	                if (error == 0) {
	                    hideProgressDialog();
	                    Log.i(TAG, "QUERY_TCP_DUMP_DONE Success");
	                    mTcpDump.setText("TCP DUMP STOP");
	                    // SystemProperties.set(TCPDUMP_PROPERTY,"On" );
	                } else {
	                    hideProgressDialog();
	                    // set back text to "START"
	                    mTcpDump.setText("TCP DUMP START");
	                    ResultMessage("TCP DUMP error\n(bind: Network is down)");
	                    Log.i(TAG, "QUERY_TCP_DUMP_DONE fail");
	                }

	                break;
	            }
	     
	            case WIFI_TCPDUMP_DONE:
	                Integer value = (Integer)msg.obj;
	                if (value == 0) {                   // start ok
	                    mTcpDump.setText("TCP DUMP STOP");
	                } else if (value == 1) {            // end ok
	                    mTcpDump.setText("TCP DUMP START");
	                } else {                                    // start fail
	                    ResultMessage("TCP DUMP error\n(bind: Network is down)");
	                    Log.i(TAG, "WIFI_TCPDUMP_DONE fail");
	                }
	            break;
	            case QUERY_SILENTLOG_DONE: {
	                String silentlogging = String.valueOf(SystemProperties.get("dev.silentlog.on"));

	               
	                    if("On".equalsIgnoreCase(silentlogging)) {
	                        mSilentLog.setText("Silent Log : On");
	                    } else {
	                        mSilentLog.setText("Silent Log : Off");
	                    }
	                

	                hideProgressDialog();
	                break;
	            }
	            default:
	                break;
	            }
	        }
	    };
	    private Messenger mSvcModeMessenger = new Messenger(mHandler);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.more_options_setting);
		mContext = getApplicationContext();
		builder = new AlertDialog.Builder(this);
		mOem = new OemCommands();
		connectToRilService();
		listView = (ListView) findViewById(R.id.optionList);
		okBtn = (Button) findViewById(R.id.ok);
		delDumpBtn = (Button) findViewById(R.id.deleteDump);

		mSilentLog = (Button) findViewById(R.id.silentLog);
		String silentlogging = String.valueOf(SystemProperties
				.get("dev.silentlog.on"));
		if ("On".equalsIgnoreCase(silentlogging))
			mSilentLog.setText("Silent Log : On");
		else
			mSilentLog.setText("Silent Log : Off");
		mSilentLog.setOnClickListener(silentClicked);

		if (isBroadcom && !isEOS2)
			mSilentLog.setVisibility(View.GONE);

		mTcpDump = (Button) findViewById(R.id.tcpDump);
		if (wifiOnly) {
			String tcpdumping = String.valueOf(SystemProperties
					.get("net.tcpdumping"));
			if (tcpdumping.equals("On") == true) {
				mTcpDump.setText("TCP DUMP STOP");
			}
		} else {
			String tcpdumping = String.valueOf(SystemProperties
					.get("ril.tcpdumping"));
			if (tcpdumping.equals("On") == true) {
				mTcpDump.setText("TCP DUMP STOP");
			}
		}
		mTcpDump.setOnClickListener(tcpDumpClicked);

		selectAll = (CheckBox) findViewById(R.id.select_all_cb);
		String[] options = new String[] { "Show touches point",
				"Show rename dialog after stop recording",
				"Get dumpstate/logcat", "Get dumpstate/logcat/modem" };
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_multiple_choice, options);
		listView.setAdapter(adapter);
		count = listView.getCount();
		selectAll.setChecked(getState("select_all"));
		for (int i = 0; i < count; i++) {
			listView.setItemChecked(i, getState(Integer.toString(i)));
		}

		stateIntent = new Intent();
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int i, long arg3) {
				// TODO Auto-generated method stub

				if (listView.getCheckedItemCount() == count)
					selectAll.setChecked(true);
				else
					selectAll.setChecked(false);
				saveState(Integer.toString(i), listView.isItemChecked(i));
				saveState("select_all", selectAll.isChecked());
				stateIntent.putExtra(Integer.toString(i),
						listView.isItemChecked(i));
				Log.d(TAG, "i = " + listView.isItemChecked(i));
			}
		});

		selectAll.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				for (int i = 0; i < count; i++) {
					if (selectAll.isChecked())
						listView.setItemChecked(i, true);
					else
						listView.setItemChecked(i, false);
					saveState(Integer.toString(i), listView.isItemChecked(i));
					stateIntent.putExtra(Integer.toString(i),
							listView.isItemChecked(i));
					adapter.notifyDataSetChanged();
					Log.d(TAG, "i = " + listView.isItemChecked(i));
				}
				saveState("select_all", selectAll.isChecked());
			}
		});

		sendStateIntent();
		okBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				sendStateIntent();
				finish();
			}
		});
		delDumpBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				deleteDirectory(DELETE_DUMP_DIR);
				deleteDirectory(workingPath);
				Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri
						.parse("file://"
								+ Environment.getExternalStorageDirectory()
										.getPath()));
				sendBroadcast(intent);
				Toast.makeText(getApplication(), "Delete dump!",
						Toast.LENGTH_SHORT).show();
			}
		});
	}
    private ServiceConnection mSecPhoneServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "onServiceConnected()");
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

        String strRilDaemon2Status = SystemProperties.get("init.svc.ril-daemon2", "stopped")
             .toLowerCase();
        if("running".equals(strRilDaemon2Status)) {
       
                intent.setClassName("com.sec.phone", "com.sec.phone.SecPhoneService");
                Log.i(TAG, "com.sec.phone.SecPhoneService");
            
        } else {
            intent.setClassName("com.sec.phone", "com.sec.phone.SecPhoneService");
        }

        bindService(intent, mSecPhoneServiceConnection, BIND_AUTO_CREATE);
    }
	 private boolean checkForNoAuthorityAndNotEngBuild(){

	        settings = getSharedPreferences("SYSDUMPOTP", 0);
	        boolean auth = settings.getBoolean("ril.OTPAuth", false);
	        String buildtype = String.valueOf(SystemProperties.get("ro.build.type"));
	        if(auth == false && (buildtype.compareToIgnoreCase("eng") != 0)){
	            Log.e(TAG,"It's user binary");
	            return true;
	        } else{
	            Log.e(TAG,"It's eng binary");
	            return false;
	        }
	    }
/*	    private void showOTPAlertDialogForAuth(){

	        final AlertDialog.Builder alertOTP = new AlertDialog.Builder(SettingActivity.this);
	        final EditText inputOTP = new EditText(SettingActivity.this);
	        alertOTP.setTitle("OTP Authentication");
	        final String d = Long.toString(Double.doubleToLongBits(Math.random()), 36)
	                         .substring(0, 5);
	        alertOTP.setMessage("Key : " + d);
	        alertOTP.setView(inputOTP);
	        alertOTP.setPositiveButton("OK ", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {

	            String KeyStr = inputOTP.getText().toString().trim();

	            OTPSecurity Otp = new OTPSecurity();
	            boolean bOptd = Otp.CheckOTP(KeyStr, d);

	            if (bOptd) {
	                SharedPreferences.Editor setAuth = settings.edit();
	                setAuth.putBoolean("ril.OTPAuth", true);
	                setAuth.commit();
	                ResultMessage("OTP Authentication enabled!");
	                 return;
	            } else {
	                ResultMessage("OTP Authentication Failed!");
	                return;
	            }

	        }
	        });
	        alertOTP.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

	        public void onClick(DialogInterface dialog, int whichButton) {
	            return;
	        }
	        });
	        Log.e(TAG,"Showing alertDialogOTP");
	        alertOTP.show();
	    }*/
	View.OnClickListener tcpDumpClicked = new View.OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			 // OTP OK or ENG MODE

                 if (mTcpDump.getText().toString().endsWith("START")) {
                     final List<String> InterfaceNames = new ArrayList<String>();

                     try {
                         for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en!=null?en.hasMoreElements():false;) {
                             if(en != null) {
                                 NetworkInterface intf = en.nextElement();
                                 InterfaceNames.add(intf.getDisplayName());
                             }
                         }
                     } catch (Exception ex) {
                         Log.e(TAG, ex.toString());
                     }

                     final String[] StrInterfaceNames = new String[InterfaceNames.size() + 1];
                     StrInterfaceNames[0] = "any";

                     for (int i = 0; i < InterfaceNames.size(); i++) {
                         StrInterfaceNames[i + 1] = InterfaceNames.get(i);
                     }

                     final AlertDialog.Builder alert = new AlertDialog.Builder(SettingActivity.this);
                     final TextView input = new TextView(SettingActivity.this);
                     Resources resource;
                     input.setTextSize(getResources().getDimension(R.dimen.tcp_type_size));
                     alert.setView(input);
                     
        
  /*                   input.postDelayed(new Runnable() {
                         public void run() {
                             InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                             keyboard.hideSoftInputFromWindow(
                                     input.getWindowToken(), 0);
                         }
                     }, 200);*/
                     alert.setSingleChoiceItems(StrInterfaceNames, -1,
                     new DialogInterface.OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialog, int which) {
                             // TODO Auto-generated method stub
                             input.setText(StrInterfaceNames[which]);
                         }
                     });
                     alert.setPositiveButton("OK ", new DialogInterface.OnClickListener() {
                         public void onClick(DialogInterface dialog, int whichButton) {
                             TCPDUMP_INTERFACE = input.getText().toString().trim();

                             if (TCPDUMP_INTERFACE.isEmpty()) {
                                 ResultMessage("Please choose Interface");
                                 return;
                             }

                             if (wifiOnly ) {
                                 startStopWiFiTcpdump(true, TCPDUMP_INTERFACE);
                             } else {
                                 Thread tcpDumpThread = new Thread(new Runnable() {
                                     public void run() {
                                         try {
                                             Log.d(TAG, "Stop TcpDumpLoggingService");
                                             Intent tcpDumpSvcIntent = new Intent().setClassName("com.sec.tcpdumpservice", "com.sec.tcpdumpservice.TcpDumpService");
                                             stopService(tcpDumpSvcIntent);
                                             Thread.sleep(1000);
                                         } catch (InterruptedException e) {
                                            Log.e(TAG, "tcpDumpThread - exception occurred");
                                         }
                                         SendData(mOem.OEM_TCPDUMP_START);
                                     }
                                 });
                                 tcpDumpThread.start();
                             }
                         }
                     });
                     alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                         public void onClick(DialogInterface dialog, int whichButton) {
                             return;
                         }
                     });
                     alert.show();
                 } else if (mTcpDump.getText().toString().endsWith("STOP")) {
                     if (wifiOnly) {
                         startStopWiFiTcpdump(false, null);
                     } else {
                         SendData(mOem.OEM_TCPDUMP_STOP);
                         Thread tcpDumpThread = new Thread(new Runnable() {
                             public void run() {
                                 Log.d(TAG, "Start TcpDumpLoggingService");
                                 try {
                                     Thread.sleep(1000);
                                     Intent tcpDumpSvcIntent = new Intent().setClassName("com.sec.tcpdumpservice", "com.sec.tcpdumpservice.TcpDumpService");
                                     startService(tcpDumpSvcIntent);
                                 } catch (InterruptedException e) {
                                     Log.e(TAG, "tcpDumpThread - exception occurred");
                                 }
                             }
                         });
                         tcpDumpThread.start();
                     }
                     ResultMessage("TCP DUMP OK.");
                     mTcpDump.setText("TCP DUMP START");
                 }
             
		}
			
			
	};
	View.OnClickListener silentClicked = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub

			if (!isMidOrHighDebugLevel()) {
				Toast.makeText(
						mContext,
						"Current debug level is low \nStarting reboot to change debug level to MID",
						Toast.LENGTH_LONG).show();
				Handler myHandle = new Handler();
				myHandle.postDelayed(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						doRebootNSave(DEBUG_LEVEL_MID_STRING);
					}
				}, 1500);
				return;
			}

			String cpChip = String.valueOf(SystemProperties.get(
					"ril.modem.board", "Unknown"));
			Log.d(TAG, "cpChip = " + cpChip);
			String silentlogging = String.valueOf(SystemProperties
					.get("dev.silentlog.on"));
			Log.d(TAG, "silentLogging = " + silentlogging);
			String chip = SecProductFeature_COMMON.SEC_PRODUCT_FEATURE_COMMON_HW_VENDOR_CONFIG;
			Log.d(TAG, "CHIP = " + chip);
			// RNTFIX::for ML <
			if (SecProductFeature_RIL.SEC_PRODUCT_FEATURE_RIL_SILENTLOG_STE) {
				if (silentlogging.equalsIgnoreCase("On") != true) {
					long when = System.currentTimeMillis();
					mSilentLog.setText("Silent Log : On");
					chooseSilentLog();
					Log.i(TAG, "Send Silent LOG true.");
					SystemProperties.set("dev.silentlog.on", "On");

				} else {
					mSilentLog.setText("Silent Log : Off");
					SystemProperties.set("dev.silentlog.on", "Off");
					mOem.DoShellCmd("debug_interface_proxy --command=\"trace --stopall\"");
					Log.i(TAG, "Send Silent LOG false.");

					// RNTFIX::for ML >
				}
			} else if (cpChip.startsWith("XMM")) {
				Log.d(TAG, "Silence saving : start now! ");
				byte SILENTLOG_ON[] = { '1', '\0' };
				byte SILENTLOG_OFF[] = { '0', '\0' };
				String sysFs = "sys/devices/virtual/misc/umts_dm0/dm_state";
				silentlogging = String.valueOf(SystemProperties
						.get("dev.silentlog.on"));

				if (silentlogging.equalsIgnoreCase("On") != true) {
					mSilentLog.setText("Silent Log : On");
					mOem.DoShellCmd("rm /data/slog/*");
					 SendData_Silentlog(true);
					setSysfsFile(sysFs, SILENTLOG_ON);
					Log.i(TAG, "Send Silent LOG true.");
					SystemProperties.set("dev.silentlog.on", "On");
				} else {
					mSilentLog.setText("Silent Log : Off");
					 SendData_Silentlog(false);
					setSysfsFile(sysFs, SILENTLOG_OFF);
					Log.i(TAG, "Send Silent LOG false.");
					SystemProperties.set("dev.silentlog.on", "Off");
					mOem.DoShellCmd("chmod 664 /data/slog/*");
				}
			} else {
				// [ for MUM/SilentLog
				if (UserHandle.myUserId() != 0) {
					showToast(getApplicationContext(),
							"It is Sub user mode.\nPlease Turn on/off SilentLog in Owner mode");
					mToast.setText("It is Sub user mode.\nPlease Turn on/off SilentLog in Owner mode");

					return;
				}
				// ] for MUM/SilentLog

				silentlogging = String.valueOf(SystemProperties
						.get("dev.silentlog.on"));

				if ("On".equalsIgnoreCase(silentlogging)) {
					Log.d(TAG, "Silence saving : start now! ");
					Intent SvcIntent = new Intent();
					SvcIntent
							.setClassName("com.sec.modem.settings",
									"com.sec.modem.settings.cplogging.SilentLogService");
					SvcIntent
							.putExtra("action", 2/* =SilentLog.SILENT_LOG_START */);
					startService(SvcIntent);
					Log.i(TAG, "progress dialog show");
					showProgressDialog("Wait...");
					// Implement the thread, because of the ProgressDialog.
					Thread thread = new Thread(new Runnable() {
						public void run() {
							try {
								Thread.sleep(7000);
								/*
								 * showToast(getApplicationContext(),
								 * "silent log done");
								 */
								mHandler.sendEmptyMessage(QUERY_SILENTLOG_DONE);
							} catch (Exception e) {
								// mSilentLog.setText("changed.");
								Log.e(TAG, "exceptoin : " + e.toString());
							}
						}
					});
					thread.start();
				} else {
					chooseSilentLog();
					mHandler.sendEmptyMessage(QUERY_SILENTLOG_DONE);

				}
			}
		}//
	};
    private void SendData_Silentlog(boolean mode) {
        byte[] data = null;
          data = mOem.StartSilentData(mode);
        if (data == null) {
            Log.i(TAG, " err - data is NULL");
            return;
        }
        invokeOemRilRequestRaw(data, mHandler.obtainMessage(QUERY_DONE));
    }
   

	public void setSysfsFile(String sysFs, byte value[]) {
		FileOutputStream out = null;

		try {
			Log.i(TAG, "setSysfsFile() called!");

			out = new FileOutputStream(sysFs);
			Log.i(TAG, "FileOutputStream success!");
			out.write(value);
			Log.i(TAG, "write success!");
		} catch (IOException e) {
			e.printStackTrace();
			Log.i(TAG, "file writing error");
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
				Log.i(TAG, "close error");
			}

			Log.i(TAG, "close success!");
		}
	}



	private void chooseSilentLog() {
		Intent intent = new Intent(this, SilentLogOption.class);
		intent.addFlags(intent.FLAG_ACTIVITY_NO_USER_ACTION);
		// startActivity(intent);
		((Activity) this).startActivityForResult(intent, 0);
	}

	private void doRebootNSave(String state) {
		String androiddebug = "debug" + state;
		Log.i(TAG, "Set debug: " + androiddebug);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		if (DEBUG_LEVEL_LOW_STRING.equals(state)) {
			RamdumpMode(RAMDUMP_MODE_LOW);
		} else if (DEBUG_LEVEL_MID_STRING.equals(state)) {
			RamdumpMode(RAMDUMP_MODE_MID);
		} else if (DEBUG_LEVEL_HIGH_STRING.equals(state)) {
			RamdumpMode(RAMDUMP_MODE_HIGH);
		}
		pm.reboot(androiddebug);
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

	private void RamdumpMode(int mode) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);

		try {
			dos.writeByte(OEM_SYSDUMP_FUNCTAG);
			dos.writeByte(OEM_RAMDUMP_MODE);
			dos.writeShort(5);
			dos.writeByte(mode);
		} catch (IOException e) {
		} finally {
			try {
				dos.close();
			} catch (IOException e) {
				;
				;// This is debug code; exceptions aren't interesting.
			}
		}

	}

	public void sendStateIntent() {
		for (int i = 0; i < count; i++) {
			stateIntent
					.putExtra(Integer.toString(i), listView.isItemChecked(i));
			setResult(RESULT_OK, stateIntent);
		}
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

	public void showToast(Context context, final String toastText) {
		if (mToast == null) {
			Log.d(TAG, "showToast(), mMaxCharsInputToast is null");
		}

		if (mToast != null) {
			mToast.cancel();
		}
		mToast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT);
		mToast.show();
	}

	public void deleteDirectory(String path) {
		Log.d(TAG, "deleteDirectory : " + path);
		File file = null;
		String[] list = null;

		try {
			file = new File(path);
			list = file.list();

			if (list.length != 0) {
				for (int i = 0; i < list.length; i++) {
					File delFile = new File(path + File.separator + list[i]);

					if (delFile.isDirectory()) {
						if (!delFile.delete()) {
							deleteDirectory(path + File.separator + list[i]);
						}
					} else {
						delFile.delete();
					}
				}
			}

			if (!path.equals(DELETE_DUMP_DIR)) {
				file.delete();
			}
		} catch (NullPointerException e) {
			Log.d(TAG, "NullPointerException");
		} catch (Exception e) {
			Log.d(TAG, "Unexpected " + e);
		} finally {
			return;
		}
	}

	private boolean showProgressDialog(String msg) {
		Log.i(TAG, "showProgressDialog()");

		if (isFinishing()) {
			Log.i(TAG, "isFinishing()");
			return false;
		}

		if (progressDialog == null) {
			progressDialog = new ProgressDialog(SettingActivity.this);
		}

		try {
			progressDialog.setMessage(msg);
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(false);
			progressDialog.show();
		} catch (BadTokenException e) {
			Log.d(TAG, "BadTokenException");
		}

		return true;
	}

	private boolean hideProgressDialog() {
		Log.i(TAG, "hideProgressDialog()");

		if (progressDialog == null) {
			return false;
		}

		try {
			if (progressDialog.isShowing()
					&& (progressDialog.getWindow() != null)) {
				progressDialog.dismiss();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			progressDialog = null;
		}

		return true;
	}


    private void startStopWiFiTcpdump(final boolean start, final String infName) {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                LocalSocket socket = null;
                try {
                    int readSize = 0;
                    socket = new LocalSocket();
                    socket.connect(new LocalSocketAddress("/data/.tcpdump_socket", LocalSocketAddress.Namespace.FILESYSTEM));

                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                    if (start) {
                        dos.write(infName.getBytes());
                    } else {
                        dos.write("stop".getBytes());
                    }
                    dos.flush();

                    while (true) {
                        byte [] byteMsg = new byte[2];
                        readSize = dis.read(byteMsg);

                        if (readSize == 0) {
                            continue;
                        }
                        if (readSize == -1) {
                            Log.e(TAG, "din.read() error");
                            break;
                        }
                        String receiveMsg = new String(byteMsg);
                        Log.d(TAG, "Received Msg : " + receiveMsg);

                        Message msg = new Message();
                      //  msg.what = WIFI_TCPDUMP_DONE;
                        if ("OK".equals(receiveMsg)) {
                            if (start) {
                                msg.obj = 0;
                       //         mHandler.sendMessage(msg);
                            } else {
                                msg.obj = 1;
                          //      mHandler.sendMessage(msg);
                            }
                            break;
                        } else {
                            msg.obj = 2;
                          //  mHandler.sendMessage(msg);
                            break;
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Exception occurred: " + e.toString());
                } finally {
                    try {
                        if (socket != null) {
                            Log.e(TAG, "Socket Closed.");
                            socket.close();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception occurred: " + e.toString());
                    }
                }
            }
        });
        thread.start();
    }

    public void ResultMessage(String message) {
        if (builder != null) {
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setTitle("Dump Result");
            builder.setMessage(message);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setCancelable(true);
            builder.show();
        }
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

        if (cmd == mOem.OEM_TCPDUMP_STOP) {
            invokeOemRilRequestRaw(data, mHandler.obtainMessage(QUERY_DONE));
        } 
        else if (cmd == mOem.OEM_TCPDUMP_START) {
            invokeOemRilRequestRaw(data, mHandler.obtainMessage(QUERY_TCP_DUMP_DONE));
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
                        ;;
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (RemoteException e) {
            ;;
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		Log.d(TAG, "onPause");
		finish();
		super.onPause();
	}
    @Override
    protected void onDestroy() {
    	// TODO Auto-generated method stub
    	super.onDestroy();
        unbindService(mSecPhoneServiceConnection);
        mSecPhoneServiceConnection = null;
        bindservice_flag = 0;
        hideProgressDialog();
    }
 
}
