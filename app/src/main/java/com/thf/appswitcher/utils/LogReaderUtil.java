package com.thf.AppSwitcher.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LogReaderUtil {
	private static final String TAG = "AppSwitcherService";

	public static final int ACTION_ON_PRESS = 1;
	public static final int ACTION_SHORT_PRESS = 2;
	public static final int ACTION_LONG_PRESS = 3;

	private String logTag;
	private String logOnPress;
	private String logShortPress;
	private String logLongPress;
	private boolean stop = false;

	private Handler execHandler = new Handler(Looper.getMainLooper());

	public LogReaderUtil(Handler handler, String logTag, String logOnPress, String logShortPress, String logLongPress) {
		this.handler = handler;
		this.logTag = logTag;
		this.logOnPress = logOnPress;
		this.logShortPress = logShortPress;
		this.logLongPress = logLongPress;
	}

	private Thread thread;

	public void stopProgress() {
		this.stop = true;		 
		Log.i(TAG, "Stopped Log reader");
	}

	private Handler handler = new Handler(Looper.getMainLooper());

	public void startProgress() {
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				try {
					String[] command = new String[] { "logcat", "-v", "time", "-s", logTag };
					Process process = Runtime.getRuntime().exec("logcat -c");
					process = Runtime.getRuntime().exec(command);
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

					String line = "";

					while ((line = bufferedReader.readLine()) != null) {
						if (stop) {
							break;
						}
						
						if (line.contains(logTag)) {
							int action = 0;
							if (line.contains(logOnPress)) {
								action = ACTION_ON_PRESS;
							} else if (line.contains(logShortPress)) {
								action = ACTION_SHORT_PRESS;
							} else if (line.contains(logLongPress)) {
								action = ACTION_LONG_PRESS;
							}

							if (action != 0) {
								Message completeMessage = handler.obtainMessage(action);
								completeMessage.sendToTarget();
							}
						}
					}

				} catch (IOException e) {
					Log.w(TAG, e);
				}
			 
			}
		};

		Thread thread = new Thread(runnable); //, Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
	}

}