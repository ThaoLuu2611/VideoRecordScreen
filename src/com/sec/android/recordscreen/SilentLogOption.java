
package com.sec.android.recordscreen;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.FileUtils;
import java.io.IOException;
import android.os.SystemProperties;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager.BadTokenException;

import android.util.Log;
import java.lang.Process;
import java.lang.Runtime;
import com.sec.android.app.SecProductFeature_RIL;

public class SilentLogOption extends Activity {
    private static final String TAG = "SilentLogOption";
    private static final int QUERY_SILENTLOG_DONE = 1004;
    private ProgressDialog progressDialog;
    private AlertDialog alert;

    @Override
    protected void onResume() {
        super.onResume();
        if (SecProductFeature_RIL.SEC_PRODUCT_FEATURE_RIL_SILENTLOG_STE)
            chooseSilentLogOptionSTE();
        else if (SecProductFeature_RIL.SEC_PRODUCT_FEATURE_RIL_MULTISIM_DUOS_TDSCDMA)
            chooseSilentLogOptionQSC();
        else
            chooseSilentLogOption();
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        hideProgressDialog();
        hideSilentLogOption();
        super.onDestroy();
    }

    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case QUERY_SILENTLOG_DONE: {
                hideProgressDialog();
                hideSilentLogOption();
                setResult(RESULT_OK);
                finish();
                break;
            }
            default:
                break;
            }
        }
    };

    private void chooseSilentLogOptionSTE() {
        if (alert != null) {
            Log.i(TAG, "chooseSilentLogOption() : Choose Dialog is already created");
            return;
        }
        final CharSequence[] items = {
                "USB", "SDCARD"
        };

        String silentlogging = null;
        silentlogging = String.valueOf(SystemProperties.get("dev.silentlog.on"));
        Log.i(TAG, "Silent Log : " + silentlogging);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Silent Log");
        int pos = -1;
        builder.setSingleChoiceItems(items, pos, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {      
                Log.i(TAG, "Silence saving : start now! ");
                switch (item) {
                    case 0:
                        DoShellCmd("debug_interface_proxy --command=\"trace --usb=start\"");
                        SystemProperties.get("dev.silentlog.on","On");
                        break;
                    case 1:
                        DoShellCmd("debug_interface_proxy --command=\"trace --usb=stop\"");
                        SystemProperties.get("dev.silentlog.on","On");
                        break;
                }
               onDestroy();
               finish();
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface arg0) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        alert = builder.create();
        alert.show();
    }

    private void chooseSilentLogOptionQSC() {
        if (alert != null) {
            Log.i(TAG, "chooseSilentLogOption() : Choose Dialog is already created");
            return;
        }

        // final int GPS = 2; // Unused
        // final int Audio = 1; // Unused
        // final int Default = 0; // Unused
        final CharSequence[] items = {
            "Default", "Audio", "GPS", "VoLTE", "QSC"
        };
        String silentlogging = null;
        silentlogging = String.valueOf(SystemProperties.get("dev.silentlog.on"));
        Log.i(TAG, "Silent Log : " + silentlogging);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Silent Log");
        int pos = -1;
        /*
         * if("On".equalsIgnoreCase(silentlogging)) { pos = 0; } else
         * if("Audio".equalsIgnoreCase(silentlogging)) { pos = 1; } else
         * if("GPS".equalsIgnoreCase(silentlogging)) { pos = 2; }
         */
        builder.setSingleChoiceItems(items, pos, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                Log.i(TAG, "Silence saving : start now! ");
                Intent SvcIntent = new Intent();
                SvcIntent.setClassName("com.sec.modem.settings",
                                       "com.sec.modem.settings.cplogging.SilentLogService");

                switch (item) {
                case 0:
                    SystemProperties.set("sys.silentlog.choose_cp", "MDM");
                    SvcIntent.putExtra("modem_profile", "default");
                    break;
                case 1:
                    SystemProperties.set("sys.silentlog.choose_cp", "MDM");
                    SvcIntent.putExtra("modem_profile", "audio");
                    break;
                case 2:
                    SystemProperties.set("sys.silentlog.choose_cp", "MDM");
                    SvcIntent.putExtra("modem_profile", "gps");
                    break;
                case 3:
                    SystemProperties.set("sys.silentlog.choose_cp", "MDM");
                    SvcIntent.putExtra("modem_profile", "volte");
                    break;
                case 4:
                    SystemProperties.set("sys.silentlog.choose_cp", "QSC");
                    SvcIntent.putExtra("modem_profile", "qsc");
                    break;
                default:
                    break;
                }

                SvcIntent.putExtra("action", 2/* =SilentLog.SILENT_LOG_START */);
                startService(SvcIntent);
                Log.i(TAG, "progress dialog show");
                showProgressDialog("Wait...");
                // Implement the thread, because of the ProgressDialog.
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(7000);
                            mHandler.sendEmptyMessage(QUERY_SILENTLOG_DONE);
                        } catch (Exception e) {
                            // mSilentLog.setText("changed.");
                            Log.e(TAG, "exceptoin : " + e.toString());
                        }
                    }
                });
                thread.start();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface arg0) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        alert = builder.create();
        alert.show();
    }

    private void chooseSilentLogOption() {
        if (alert != null) {
            Log.i(TAG, "chooseSilentLogOption() : Choose Dialog is already created");
            return;
        }

        // final int GPS = 2; // Unused
        // final int Audio = 1; // Unused
        // final int Default = 0; // Unused
        final CharSequence[] items = {
            "Default", "Audio", "GPS", "VoLTE"
        };
        String silentlogging = null;
        silentlogging = String.valueOf(SystemProperties.get("dev.silentlog.on"));
        Log.i(TAG, "Silent Log : " + silentlogging);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Silent Log");
        int pos = -1;
        /*
         * if("On".equalsIgnoreCase(silentlogging)) { pos = 0; } else
         * if("Audio".equalsIgnoreCase(silentlogging)) { pos = 1; } else
         * if("GPS".equalsIgnoreCase(silentlogging)) { pos = 2; }
         */
        builder.setSingleChoiceItems(items, pos, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                Log.i(TAG, "Silence saving : start now! ");
                Intent SvcIntent = new Intent();
                SvcIntent.setClassName("com.sec.modem.settings",
                                       "com.sec.modem.settings.cplogging.SilentLogService");

                switch (item) {
                case 0:
                    SvcIntent.putExtra("modem_profile", "default");
                    break;
                case 1:
                    SvcIntent.putExtra("modem_profile", "audio");
                    break;
                case 2:
                    SvcIntent.putExtra("modem_profile", "gps");
                    break;
                case 3:
                    SvcIntent.putExtra("modem_profile", "volte");
                    break;
                default:
                    break;
                }

                SvcIntent.putExtra("action", 2/* =SilentLog.SILENT_LOG_START */);
                startService(SvcIntent);
                Log.i(TAG, "progress dialog show");
                showProgressDialog("Wait...");
                // Implement the thread, because of the ProgressDialog.
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(7000);
                            mHandler.sendEmptyMessage(QUERY_SILENTLOG_DONE);
                        } catch (Exception e) {
                            // mSilentLog.setText("changed.");
                            Log.e(TAG, "exceptoin : " + e.toString());
                        }
                    }
                });
                thread.start();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface arg0) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        alert = builder.create();
        alert.show();
    }

    private boolean showProgressDialog(String msg) {
        Log.i(TAG, "showProgressDialog()");

        if (isFinishing()) {
            Log.i(TAG, "isFinishing()");
            return false;
        }

        if (progressDialog == null) {
            progressDialog = new ProgressDialog(SilentLogOption.this);
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
            if (progressDialog.isShowing() && (progressDialog.getWindow() != null)) {
                progressDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            progressDialog = null;
        }

        return true;
    }

    private boolean hideSilentLogOption() {
        Log.i(TAG, "hideSilentLogOption()");

        if (alert == null) {
            return false;
        }

        try {
            if (alert.isShowing() && (alert.getWindow() != null)) {
                alert.dismiss();
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            alert = null;
        }
        return true;
    }

    int DoShellCmd(String cmd) {
        Log.i(TAG, "DoShellCmd : " + cmd);
        Process p = null;
        String[] shell_command = {
            "/system/bin/sh", "-c", cmd
        };
        try {
            Log.i(TAG, "exec command");
            p = Runtime.getRuntime().exec(shell_command);
            p.waitFor();
            Log.i(TAG, "exec done");
        } catch (IOException exception) {
            Log.e(TAG, "DoShellCmd - IOException");
            return -1;
        } catch (SecurityException exception) {
            Log.e(TAG, "DoShellCmd - SecurityException");
            return -1;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        }

        Log.i(TAG, "DoShellCmd done");

        return 1;
    }
}
