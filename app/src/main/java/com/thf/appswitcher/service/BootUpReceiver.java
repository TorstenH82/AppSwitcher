package com.thf.AppSwitcher.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import com.thf.AppSwitcher.StartServiceActivity;
import com.thf.AppSwitcher.service.AppSwitcherService;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;

public class BootUpReceiver extends BroadcastReceiver {
  private static final String TAG = "AppSwitcherService";

  @Override
  public void onReceive(Context context, Intent intent) {
    // Boolean run = false;
    String action = intent.getAction();
    if ("autochips.intent.action.QB_POWERON".equals(action)
        || "com.ts.main.uiaccon".equals(action)
        || "com.qf.action.ACC_ON".equals(action)) {
      Log.i(TAG, "received " + action);

      SharedPreferencesHelper sharedPreferencesHelper = new SharedPreferencesHelper(context);
      if (!sharedPreferencesHelper.getBoolean("accWake")) {
        Log.i(TAG, "ACC wake is not enabled");
        return;
      }
      Intent intentSrv = new Intent(context, AppSwitcherService.class);
      intentSrv.setAction(AppSwitcherService.ACTION_WAKE_UP);
      context.startForegroundService(intentSrv);
      return;
    }

    if ("com.ts.main.DEAL_KEY".equals(action)) {
      Log.i(TAG, "received " + action);
      Intent intentSrv = new Intent(context, AppSwitcherService.class);
      intentSrv.setAction(AppSwitcherService.ACTION_KEY);
      int key = intent.getExtras().getInt("key");
      intentSrv.putExtra("key", key);
      String topact = intent.getExtras().getString("topact");
      intentSrv.putExtra("topact", topact);
      context.startForegroundService(intentSrv);
      return;
    }

    if ("autochips.intent.action.QB_POWEROFF".equals(action)
        || "com.ts.main.uiaccoff".equals(action)
        || "com.qf.action.ACC_OFF".equals(action)) {
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
  }
}
