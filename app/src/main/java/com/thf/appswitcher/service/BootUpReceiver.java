package com.thf.AppSwitcher.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import com.thf.AppSwitcher.service.AppSwitcherService;

public class BootUpReceiver extends BroadcastReceiver {
	private static final String TAG = "AppSwitcherService";

	@Override
	public void onReceive(Context context, Intent intent) {
		Boolean run = false;
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Log.d(TAG, "received boot completed");
			run = true;
		} else {
			Log.d(TAG, "received action: " + intent.getAction());
			Toast.makeText(context,"AppSwitcher received action: " + intent.getAction(), Toast.LENGTH_LONG).show();
			run = true;
		}

		if (run) {
			Intent i = new Intent(context, AppSwitcherService.class);
			//i.putExtra("times", 5);
			context.startForegroundService(i);
		}

	}
}