package com.thf.AppSwitcher.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.thf.AppSwitcher.AppSwitcherApp;
import com.thf.AppSwitcher.R;
import java.time.Year;

public class AutoLinkBroadcast implements Runnable {
  private static final String TAG = "AppSwitcherService";

  private static String packageName;
  private static String broadcastReceiver;
  private Context context;
  private static String command;
  private static Thread thread;
  private int loops = 1;

  public AutoLinkBroadcast(Context context) {
    this.context = context;
    AutoLinkBroadcast.packageName = context.getString(R.string.autoLinkPackage);
    AutoLinkBroadcast.broadcastReceiver = context.getString(R.string.autoLinkReceiver);
  }

  @Override
  public void run() {
    if (!Utils.isPackageInstalled(context, packageName)) {
      Log.d(TAG, "package " + packageName + " is not installed");
      return;
    }

    ComponentName componentName = new ComponentName(packageName, broadcastReceiver);
    Intent intent = new Intent();
    intent.setComponent(componentName);
    intent.setAction(packageName);
    intent.putExtra("command", command);

    int i = 0;
    int loops = this.loops;
    while (i < loops) {
      Log.d(TAG, "send command '" + command + "' to '" + packageName + "'");
      context.sendBroadcast(intent);
      i++;
      try {
        Thread.sleep(2 * 1000);
      } catch (InterruptedException ex) {
        Log.i(TAG, "interrupted AutoLinkBroadcast");
        return;
      }
    }
  }

  public void setNight(boolean multi) {
    runThread("REQ_NIGHT_MODE_CMD", multi);
  }

  public void setDay(boolean multi) {
    runThread("REQ_DAY_MODE_CMD", multi);
  }

  private void runThread(String command, boolean multi) {
    stop();
    AutoLinkBroadcast.command = command;
    this.loops = (multi ? 60 : 1);
    thread = new Thread(this);
    thread.start();
  }

  public void stop() {
    if (thread != null && thread.isAlive()) {
      thread.interrupt();
    }
  }
}
