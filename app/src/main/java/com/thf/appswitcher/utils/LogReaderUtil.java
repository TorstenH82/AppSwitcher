package com.thf.AppSwitcher.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Year;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;

public class LogReaderUtil {
  private static final String TAG = "AppSwitcherService";

  public static final int ACTION_ON_PRESS = 1;
  public static final int ACTION_SHORT_PRESS = 2;
  public static final int ACTION_LONG_PRESS = 3;

  private String logTag;
  private String logOnPress;
  private String logShortPress;
  private String logLongPress;

  // private Handler execHandler = new Handler(Looper.getMainLooper());

  private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  public LogReaderUtil(
      Handler handler,
      String logTag,
      String logOnPress,
      String logShortPress,
      String logLongPress) {
    this.handler = handler;
    this.logTag = logTag;
    if (!"".equals(logOnPress)) this.logOnPress = logOnPress;
    this.logShortPress = logShortPress;
    if (!"".equals(logLongPress)) this.logLongPress = logLongPress;
  }

  private static Thread thread;

  public void stopProgress() {

    if (thread != null && thread.isAlive()) {
      thread.interrupt();
      Log.i(TAG, "Stopped Log reader");
      thread = null;
    }
  }

  private Handler handler = new Handler(Looper.getMainLooper());

  public void startProgress() {
    Runnable runnable =
        new Runnable() {

          @Override
          public void run() {
            try {
              ProcessBuilder pb = new ProcessBuilder("logcat", "-v", "time", "-s", logTag);
              Process process = pb.start(); // start the process

              BufferedReader bufferedReader =
                  new BufferedReader(new InputStreamReader(process.getInputStream()));

              String line = "";
              boolean use = false;
              Date timestampStart = Calendar.getInstance().getTime();

              while ((line = bufferedReader.readLine()) != null) {
                if (Thread.interrupted()) {
                  process.destroyForcibly();
                  return;
                }

                if (!line.contains(logTag)) continue;

                if (!use) {
                  String sTimestamp =
                      Calendar.getInstance().get(Calendar.YEAR)
                          + "-"
                          + line.split(" ")[0]
                          + " "
                          + line.split(" ")[1];
                  try {
                    Date timestamp = simpleDateFormat.parse(sTimestamp);
                    if (timestamp.after(timestampStart)) {
                      use = true;
                    } else {
                      continue;
                    }
                  } catch (java.text.ParseException ex) {
                    Log.e(TAG, ex.getMessage());
                  }
                }

                int action = 0;
                if (logOnPress != null && line.contains(logOnPress)) {
                  action = ACTION_ON_PRESS;
                } else if (line.contains(logShortPress)) {
                  action = ACTION_SHORT_PRESS;
                } else if (logLongPress != null && line.contains(logLongPress)) {
                  action = ACTION_LONG_PRESS;
                }

                if (action != 0) {
                  Message completeMessage = handler.obtainMessage(action);
                  completeMessage.sendToTarget();
                }
              }

            } catch (IOException ex) {
              Log.e(TAG, ex.getMessage());
            }
          }
        };

    if (thread == null || !thread.isAlive()) {
      thread = new Thread(runnable); // , Process.THREAD_PRIORITY_BACKGROUND);
      thread.start();
      Log.i(TAG, "Started Log reader");
    }
  }
}
