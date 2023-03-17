package com.thf.AppSwitcher.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import com.thf.AppSwitcher.StartServiceActivity;
import com.thf.AppSwitcher.service.AppSwitcherService;

public class BootUpReceiver extends BroadcastReceiver {
  private static final String TAG = "AppSwitcherService";

  @Override
  public void onReceive(Context context, Intent intent) {
    // Boolean run = false;
    String action = intent.getAction();
    if ("autochips.intent.action.QB_POWERON".equals(action)
        || "com.ts.main.uiaccon".equals(action)) {
      Log.i(TAG, "received " + action);
      Intent intentSrv = new Intent(context, AppSwitcherService.class);
      intentSrv.setAction(AppSwitcherService.ACTION_WAKE_UP);
      context.startForegroundService(intentSrv);
      return;
    }
    if ("autochips.intent.action.QB_POWEROFF".equals(action)
        || "com.ts.main.uiaccoff".equals(action)) {
      Log.i(TAG, "received " + action);
      Intent intentSrv = new Intent(context, AppSwitcherService.class);
      intentSrv.setAction(AppSwitcherService.ACTION_SLEEP);
      context.startForegroundService(intentSrv);
      return;
    }
    if ("android.intent.action.BOOT_COMPLETEDx".equals(action)) {
      Log.i(TAG, "received " + action);
      Intent intentSrv = new Intent(context, StartServiceActivity.class);
           intentSrv.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intentSrv);
      return;
    }

    /*
    if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
        Log.d(TAG, "received boot completed");
        run = true;
    } else {
        Log.d(TAG, "received action: " + intent.getAction());
        Toast.makeText(
                        context,
                        "AppSwitcher received action: " + intent.getAction(),
                        Toast.LENGTH_LONG)
                .show();
        run = true;
    }

    if (run) {
        Intent i = new Intent(context, AppSwitcherService.class);
        // i.putExtra("times", 5);
        context.startForegroundService(i);
    }
    */
  }
}
