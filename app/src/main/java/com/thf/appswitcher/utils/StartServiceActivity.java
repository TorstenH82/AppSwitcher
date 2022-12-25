package com.thf.AppSwitcher.utils;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.thf.AppSwitcher.R;
import com.thf.AppSwitcher.service.AppSwitcherService;
import java.util.List;

public class StartServiceActivity extends AppCompatActivity {

	//public static String id1 = "test_channel_01";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//createchannel();
		if (!AppSwitcherService.isRunning()) {
			Intent intent = new Intent(getBaseContext(), AppSwitcherService.class);
			intent.putExtra("times", 5);
			startForegroundService(intent);
			
			
			/*
			Boolean runMediaApp = SharedPreferencesHelper.LoadBoolean(getApplicationContext(), "runMediaApp");
			if (runMediaApp) {
				List<AppData> recentsAppList = SharedPreferencesHelper.getRecentsList(getApplicationContext());
				if (!recentsAppList.isEmpty()) {
					AppData app = recentsAppList.get(0);
					ComponentName name = new ComponentName(app.getPackageName(), app.getActivityName());
					Intent i = new Intent(Intent.ACTION_MAIN);
					i.addCategory(Intent.CATEGORY_LAUNCHER);
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
					i.setComponent(name);
					startActivity(i);
				}
			}
			*/
		}

		//If the API is below 26, then you have to use this
		//startService(number5);
		finish();
	}

	/**
	* for API 26+ create notification channels
	*/
	/*
	private void createchannel() {
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationChannel mChannel = new NotificationChannel(id1, getString(R.string.channel_name),  //name of the channel
		NotificationManager.IMPORTANCE_LOW);   //importance level
		//important level: default is is high on the phone.  high is urgent on the phone.  low is medium, so none is low?
		// Configure the notification channel.
		mChannel.setDescription(getString(R.string.channel_description));
		mChannel.enableLights(true);
		// Sets the notification light color for notifications posted to this channel, if the device supports this feature.
		mChannel.setShowBadge(true);
		nm.createNotificationChannel(mChannel);
	}
	*/
}