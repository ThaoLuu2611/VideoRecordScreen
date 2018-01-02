package com.sec.android.recordscreen;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;

public class OemCommands {
	String TAG = "SVROemCommands";
    final int OEM_IPC_DUMP_BIN = 0x09;
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
	    private static final int CP_POPUP_UI_ENABLE = 0x01;
	    private static final int CP_POPUP_UI_DISABLE = 0x02;
	    private String TCPDUMP_INTERFACE = "any"; 
	    private static int iCPPopupUIState = CP_POPUP_UI_DISABLE;
		final int OEM_SYSDUMP_FUNCTAG = 7;


        final int OEM_FUNCTION_ID_MISC = 17;
        final int OEM_LOGCAT_MAIN = 0x01;
        final int OEM_DUMPSTATE = 0x03;
        final int OEM_START_RIL_LOG = 0x0C;
        final int OEM_DEL_RIL_LOG = 0x0D;
        final int OEM_DPRAM_DUMP = 0X0E;
        final int OEM_GCF_MODE_GET = 0x0F;
        final int OEM_GCF_MODE_SET = 0x10;
        final int OEM_NV_DATA_BACKUP = 0x11;
        final int OEM_MODEM_LOG = 0x12;
        final int OEM_OEM_DUMPSTATE_MODEM_LOG_AUTO_START = 0x13;
        final int OEM_DUMPSTATE_ALL = 0x14; // DUMP STATE/LOGCAT + CP DUMP
        final int OEM_TCPDUMP_START = 0x15; // Apply for tcpdump
        final int OEM_TCPDUMP_STOP = 0x16; // Apply for tcpdump
        final int OEM_GET_PHONE_DEBUG_MSG = 0x1A;//CP POPUP UI Control
        final int OEM_SET_PHONE_DEBUG_MSG = 0x1B;//CP POPUP UI Control
        final int OEM_MODEM_FORCE_CRASH_EXIT = 0x17;
        final int OEM_MISC_SILENT_LOGGING_CONTROL = 0x40;

        byte[] StartSysDumpData(int cmd) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);

            try {
                dos.writeByte(OEM_SYSDUMP_FUNCTAG);
                dos.writeByte(cmd);
                Log.d(TAG, "cmd : " + cmd);

                if (cmd == OEM_TCPDUMP_START) {
                    byte[] tcpdump_interface_byte = TCPDUMP_INTERFACE.getBytes();
                    Log.d(TAG, "dos.writeByte length: "
                          + Integer.toString(tcpdump_interface_byte.length));
                    dos.writeShort(tcpdump_interface_byte.length + 4);

                    for (int i = 0; i < tcpdump_interface_byte.length; i++) {
                        dos.writeByte(tcpdump_interface_byte[i]);
                    }
                } else if (cmd == OEM_MODEM_FORCE_CRASH_EXIT) {
                    Log.d(TAG, "OEM_MODEM_FORCE_CRASH_EXIT by user");
                    dos.writeShort(5);
                    dos.writeByte(0);
                } else if (cmd == OEM_SET_PHONE_DEBUG_MSG) {
                    dos.writeShort(5);
                    if (iCPPopupUIState == CP_POPUP_UI_DISABLE) {
                        dos.writeByte(CP_POPUP_UI_ENABLE);
                    } else {
                        dos.writeByte(CP_POPUP_UI_DISABLE);
                    }
                } else {
                    Log.d(TAG, "dos.writeByte(4)");
                    dos.writeShort(4);
                }
            } catch (IOException e) {
                Log.d(TAG, "IOException in getServMQueryData!!!");
                return null;
            }

            return bos.toByteArray();
        }
        byte[] StartSilentData(boolean mode) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);

            try {
                dos.writeByte(OEM_FUNCTION_ID_MISC);
                dos.writeByte(OEM_MISC_SILENT_LOGGING_CONTROL);
                Log.d(TAG, "cmd : " + OEM_MISC_SILENT_LOGGING_CONTROL);
                Log.d(TAG, "dos.writeByte(5)");
                dos.writeShort(5);

                if(mode == true) {
                    dos.writeByte(0x00);
                } else {
                    dos.writeByte(0x01);
                }
            } catch (IOException e) {
                Log.d(TAG, "IOException in getServMQueryData!!!");
                return null;
            }

            return bos.toByteArray();
        }

        byte[] StartSilentDataCP2(boolean mode) {

            char MODEM_GSM = 4;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            char fileSize = 7 + 1;
            try {
                dos.writeByte(OEM_LOGCAT_MAIN);
                dos.writeByte(0x01);
                dos.writeShort(fileSize);
                dos.writeByte(MODEM_GSM);
                dos.writeByte(0x1);
				if(mode == true){
				  dos.writeByte(0x06);
				} else {
				  dos.writeByte(0x14);
				}
                dos.writeByte(0x00);
            } catch (IOException e) {
                Log.d(TAG, "IOException in getServMQueryData!!!");
                return null;
            }
            return bos.toByteArray();
        }
    	int DoShellCmd(String cmd) {
    		Log.i(TAG, "DoShellCmd : " + cmd);
    		Process p = null;
    		String[] shell_command = { "/system/bin/sh", "-c", cmd };

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
    		Log.i(TAG, "DoShellCmd done: " + cmd);
    		return 1;
    	}

 
}
