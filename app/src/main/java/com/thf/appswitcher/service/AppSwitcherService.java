package com.thf.AppSwitcher.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.Message;
import android.text.TextUtils;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.thf.AppSwitcher.AppSwitcherApp;
import com.thf.AppSwitcher.GetPermissionsActivity;
import com.thf.AppSwitcher.OverlayWindow;
import com.thf.AppSwitcher.R;
import com.thf.AppSwitcher.SettingsActivity;
import com.thf.AppSwitcher.SwitchActivity;
import com.thf.AppSwitcher.utils.AppData;
import com.thf.AppSwitcher.utils.LogReaderUtil;
import com.thf.AppSwitcher.utils.SimpleDialog;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;
import com.thf.AppSwitcher.utils.StartServiceActivity;
import com.thf.AppSwitcher.utils.SunriseSunset;
import com.thf.AppSwitcher.utils.UsageStatsUtil;
import com.thf.AppSwitcher.utils.Utils;
//import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class AppSwitcherService extends Service {
	private static final String TAG = "AppSwitcherService";
	private AppSwitcherApp mApplication;
	private Looper mServiceLooper;
	private Context context;
	private LogReaderUtil logReaderUtil;
	private UsageStatsUtil usageStatsUtil;
	private SunriseSunset sunriseSunset;
	private String logTag;
	private String logOnPress;
	private String logShortPress;
	private String logLongPress;
	
	private static Boolean disableNaviMainActivity = false;
	private Thread threadMediaApp;
	private int runMediaAppDelay = 0;
	
	private OverlayWindow overlayWindow;
	private int dimMode;
	public static final int DIM_MODE_ON = 1;
	public static final int DIM_MODE_OFF = 0;
	public static final int DIM_MODE_AUTO = 2;
	/*
	public Runnable runnable = new Runnable() {
		@Override
		public void run() {
		}
	};
	*/
	public Handler logHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(android.os.Message msg) {
			int action = msg.what;
			Log.i(TAG, "Received LogReaderUtil action: " + action);
			
			if (threadMediaApp != null && threadMediaApp.isAlive())
			threadMediaApp.interrupt();
			
			switch (action) {
				case LogReaderUtil.ACTION_ON_PRESS:
				// got action on press - short or long press will follow
				if (mApplication.getSwitchActivityRunning()) {
					Log.i(TAG, "send onPress broadcast - short or long press will follow");
					Intent intent = new Intent("switch-message");
					intent.putExtra("onPress", true);
					LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
				}
				break;
				case LogReaderUtil.ACTION_LONG_PRESS:
				// send bc to close if activity is running
				if (mApplication.getSwitchActivityRunning()) {
					Log.i(TAG, "send broadcast to close to dialog");
					Intent intent = new Intent("switch-message");
					intent.putExtra("close", true);
					LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
					// switch to home on long press
					} else {
					Intent startMain = new Intent(Intent.ACTION_MAIN);
					startMain.addCategory(Intent.CATEGORY_HOME);
					startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(startMain);
				}
				break;
				case LogReaderUtil.ACTION_SHORT_PRESS:
				if (!mApplication.getSwitchActivityRunning()) {
					//get the current foreground app
					String foregroundApp = usageStatsUtil.getCurrentActivity();
					
					Log.i(TAG, "start dialog");
					Intent intent = new Intent(context, SwitchActivity.class);
					intent.putExtra("foregroundApp", foregroundApp);
					intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
					//intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
					try {
						startActivity(intent);
						} catch (Exception ex) {
						Log.e(TAG, ex.getMessage());
					}
					} else {
					Log.i(TAG, "send broadcast to dialog");
					Intent intent = new Intent("switch-message");
					// Adding some data
					//intent.putExtra("my-integer", 1);
					LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
				}
				break;
			}
			
		}
	};
	
	public AppSwitcherService() {
	}
	
	private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals("selected")) {
				usageStatsUtil.stopProgress();
				usageStatsUtil = new UsageStatsUtil(context, usageStatsCallbacks);
				usageStatsUtil.startProgress();
				} else if (key.equals("logTag")) {
				logTag = sharedPreferences.getString(key, getResources().getString(R.string.pref_logTag));
				} else if (key.equals("logOnPress")) {
				logOnPress = sharedPreferences.getString(key, getResources().getString(R.string.pref_logOnPress));
				} else if (key.equals("logShortPress")) {
				logShortPress = sharedPreferences.getString(key, getResources().getString(R.string.pref_logShortPress));
				} else if (key.equals("logLongPress")) {
				logLongPress = sharedPreferences.getString(key, getResources().getString(R.string.pref_logLongPress));
				} else if (key.equals("dimMode")) {
				dimMode = SharedPreferencesHelper.getInteger(context, "dimMode");
				updateNotification("running...");
				switch (dimMode) {
					case DIM_MODE_ON:
					sunriseSunset.disableAuto();
					overlayWindow.show();
					break;
					case DIM_MODE_OFF:
					sunriseSunset.disableAuto();
					overlayWindow.hide();
					break;
					case DIM_MODE_AUTO:
					if (sunriseSunset.getLocation() == null) {
						sunriseSunset = new SunriseSunset(context, sunriseSunsetCallbacks);
					}
					sunriseSunset.enableAuto();
					break;
				}
				
				} else if (key.equals("dimScreen")) {
				overlayWindow.setBrightness(SharedPreferencesHelper.getInteger(context, "dimScreen"));
			}
			if (key.equals("logTag") || key.equals("logOnPress") || key.equals("logShortPress")
			|| key.equals("logLongPress")) {
				logReaderUtil.stopProgress();
				logReaderUtil = new LogReaderUtil(logHandler, logTag, logOnPress, logShortPress, logLongPress);
				logReaderUtil.startProgress();
			}
			
		}
	};
	
	@Override
	public void onCreate() {
		isRunning = true;
		mApplication = (AppSwitcherApp) getApplicationContext();
		mApplication.registerPermissionGrantedCallback(new AppSwitcherApp.PermissionGrantedCallbacks() {
			@Override
			public void onGrant(String permission, boolean granted) {
				if (granted) {
					sunriseSunset = new SunriseSunset(context, sunriseSunsetCallbacks);
					switch (dimMode) {
						case DIM_MODE_ON:
						overlayWindow.show();
						break;
						case DIM_MODE_AUTO:
						sunriseSunset.enableAuto();
						break;
					}
					} else {
					if (dimMode == DIM_MODE_AUTO) {
						dimMode = DIM_MODE_OFF;
						SharedPreferencesHelper.setInteger(context, "dimMode", AppSwitcherService.DIM_MODE_OFF);
					}
				}
			}
		});
		
		context = this;
		Notification notification = getNotification("running...");
		createchannel();
		startForeground(NOTIFCATION_ID, notification);
		//createchannel();
		
		logTag = SharedPreferencesHelper.getString(context, "logTag");
		logOnPress = SharedPreferencesHelper.getString(context, "logOnPress");
		logShortPress = SharedPreferencesHelper.getString(context, "logShortPress");
		logLongPress = SharedPreferencesHelper.getString(context, "logLongPress");
		
		logReaderUtil = new LogReaderUtil(logHandler, logTag, logOnPress, logShortPress, logLongPress);
		logReaderUtil.startProgress();
		
		usageStatsUtil = new UsageStatsUtil(this, usageStatsCallbacks);
		usageStatsUtil.startProgress();
		
		SharedPreferences sharedPreferences = this.getSharedPreferences("USERDATA", MODE_PRIVATE);
		sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
		
		//Utils.enableDisableHideActivity(context, true, utilCallbacks);
		
		boolean runMediaApp = SharedPreferencesHelper.getBoolean(getApplicationContext(), "runMediaApp");
		if (runMediaApp) {
			runMediaAppDelay = SharedPreferencesHelper.getInteger(getApplicationContext(), "runMediaAppDelay");
			threadMediaApp = new Thread(runnableLastMediaApp);
			threadMediaApp.start();
		}
		
		boolean enableAutomate = SharedPreferencesHelper.getBoolean(getApplicationContext(), "enableAutomateSrv");
		if (enableAutomate) {
			Utils.enableService(context, context.getString(R.string.automatePackage),
			context.getString(R.string.automateService), utilCallbacks);
			
			String automateFlow = SharedPreferencesHelper.getString(context, "automateFlow");
			if (!"".equals(automateFlow) && !context.getString(R.string.pref_automateFlow).equals(automateFlow))
			Utils.startAutomateFlow(context, automateFlow, utilCallbacks);
		}
		
		overlayWindow = new OverlayWindow(getApplicationContext(),
		SharedPreferencesHelper.getInteger(context, "dimScreen"));
		dimMode = SharedPreferencesHelper.getInteger(context, "dimMode");
		sunriseSunset = new SunriseSunset(context, sunriseSunsetCallbacks);
		switch (dimMode) {
			case DIM_MODE_ON:
			overlayWindow.show();
			break;
			case DIM_MODE_AUTO:
			sunriseSunset.enableAuto();
			break;
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		if (ACTION_STOP_SERVICE.equals(intent.getAction())) {
			Log.d(TAG, "called to cancel service due to action " + intent.getAction());
			
			stopForeground(true);
			stopSelf();
			return START_NOT_STICKY;
			} else if (ACTION_OPEN_SETTINGS.equals(intent.getAction())) {
			Log.d(TAG, "called to open settings");
			Intent intentSettings = new Intent(context, SettingsActivity.class);
			intentSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			//intentSettings.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intentSettings);
			} else {
			Toast.makeText(this, "Starting AppSwitcher Service", Toast.LENGTH_SHORT).show();
			Log.i(TAG, "Starting AppSwitcher Service");
		}
		
		// If we get killed, after returning from here, restart
		return START_STICKY;
		
	}
	
	private String ACTION_STOP_SERVICE = "ACTION_STOP";
	private String ACTION_OPEN_SETTINGS = "ACTION_SETTINGS";
	private static Integer NOTIFCATION_ID = 1337;
	
	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}
	
	@Override
	public void onDestroy() {
		isRunning = false;
		
		if (logReaderUtil != null) {
			logReaderUtil.stopProgress();
		}
		
		if (usageStatsUtil != null) {
			usageStatsUtil.stopProgress();
		}
		
		if (sunriseSunset != null) {
			sunriseSunset.disableAuto();
		}
		
		if (overlayWindow != null)
		overlayWindow.hide();
		
		//Utils.enableDisableHideActivity(context, false, utilCallbacks);
		
		Toast.makeText(this, "AppSwitcher Service stopped", Toast.LENGTH_LONG).show();
	}
	
	// build a persistent notification and return it.
	public Notification getNotification(String message) {
		
		Intent stopSelf = new Intent(this, AppSwitcherService.class);
		stopSelf.setAction(this.ACTION_STOP_SERVICE);
		PendingIntent pStopSelf = PendingIntent.getService(this, 0, stopSelf, PendingIntent.FLAG_CANCEL_CURRENT);
		
		Intent openSettings = new Intent(this, AppSwitcherService.class);
		openSettings.setAction(this.ACTION_OPEN_SETTINGS);
		PendingIntent pOpenSettings = PendingIntent.getService(this, 0, openSettings,
		PendingIntent.FLAG_CANCEL_CURRENT);
		
		return new NotificationCompat.Builder(getApplicationContext(), id1).setSmallIcon(R.drawable.transparenticon)
		.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
		R.mipmap.ic_stat_ic_launcher_adaptive_fore))
		.setOngoing(true) //persistent notification!
		.setChannelId(id1).setContentTitle("AppSwitcher Service") //Title message top row.
		.setContentText(message) //message when looking at the notification, second row
		.addAction(R.mipmap.ic_stat_ic_launcher_adaptive_fore, "Stop", pStopSelf)
		.addAction(R.mipmap.ic_stat_ic_launcher_adaptive_fore, "Settings", pOpenSettings).build(); //finally build and return a Notification.
		
	}
	
	private void updateNotification(String message) {
		Notification notification = getNotification(message);
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFCATION_ID, notification);
	}
	
	private static boolean isRunning = false;
	
	public static boolean isRunning() {
		return isRunning;
	}
	
	public static String id1 = "test_channel_01";
	
	private void createchannel() {
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationChannel mChannel = new NotificationChannel(id1, getString(R.string.channel_name), //name of the channel
		NotificationManager.IMPORTANCE_LOW); //importance level
		//important level: default is is high on the phone.  high is urgent on the phone.  low is medium, so none is low?
		// Configure the notification channel.
		mChannel.setDescription(getString(R.string.channel_description));
		mChannel.enableLights(true);
		// Sets the notification light color for notifications posted to this channel, if the device supports this feature.
		mChannel.setShowBadge(true);
		nm.createNotificationChannel(mChannel);
	}
	
	private Runnable runnableLastMediaApp = new Runnable() {
		@Override
		public void run() {
			
			try {
				Thread.sleep(runMediaAppDelay);
				} catch (InterruptedException e) {
				return;
			}
			
			String foregroundApp = usageStatsUtil.getCurrentActivity();
			List<AppData> selectedList = SharedPreferencesHelper.loadList(context, "selected");
			
			if (Utils.listContainsKey(selectedList, foregroundApp, null)
			|| Utils.listContainsKey(selectedList, foregroundApp.split("/")[0], null)) {
				Log.d(TAG, "navi or media app (" + foregroundApp + ") already in foreground");
				return;
			}
			
			List<AppData> recentsAppList = SharedPreferencesHelper.getRecentsList(getApplicationContext());
			
			if (!recentsAppList.isEmpty()) {
				Iterator<AppData> i = recentsAppList.iterator();
				while (i.hasNext()) {
					AppData s = i.next();
					if (Utils.listContainsKey(selectedList, s.getKey(), "media")) {
						Log.d(TAG, "autostart of " + s.getName());
						ComponentName name = new ComponentName(s.getPackageName(), s.getActivityName());
						Intent intentStartMedia = new Intent(Intent.ACTION_MAIN);
						intentStartMedia.addCategory(Intent.CATEGORY_LAUNCHER);
						intentStartMedia
						.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
						intentStartMedia.setComponent(name);
						startActivity(intentStartMedia);
						
						if (SharedPreferencesHelper.getBoolean(getApplicationContext(), "runMediaAppTwice")) {
							//go to home screen, wait a second and start media app again
							Intent startMain = new Intent(Intent.ACTION_MAIN);
							startMain.addCategory(Intent.CATEGORY_HOME);
							startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
							startActivity(startMain);
							try {
								Thread.sleep(runMediaAppDelay);
								} catch (InterruptedException e) {
								return;
							}
							startActivity(intentStartMedia);
						}
						break;
					}
				}
			}
		}
	};
	
	private Utils.UtilCallbacks utilCallbacks = new Utils.UtilCallbacks() {
		@Override
		public void onException(Throwable e) {
			Toast.makeText(context, "Error occured: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
		
	};
	
	private SunriseSunset.SunriseSunsetCallbacks sunriseSunsetCallbacks = new SunriseSunset.SunriseSunsetCallbacks() {
		@Override
		public void onSunrise() {
			overlayWindow.hide();
			updateNotification("Auto dimming active (off)");
		}
		
		@Override
		public void onSunset() {
			overlayWindow.show();
			updateNotification("Auto dimming active (on)");
		}
		
		@Override
		public void onMissingPermission() {
			Intent intentGetPermission = new Intent(context, GetPermissionsActivity.class);
			intentGetPermission.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intentGetPermission.putExtra("permission", "LOCATION");
			startActivity(intentGetPermission);
		}
	};
	
	private boolean tempHidden = false;
	private UsageStatsUtil.UsageStatsCallbacks usageStatsCallbacks = new UsageStatsUtil.UsageStatsCallbacks() {
		@Override
		public void onForegroundApp(String foregroundPackage) {
			if ("com.android.vending".equals(foregroundPackage) && mApplication.getOverlayVisibility()) {
				overlayWindow.hide();
				tempHidden = true;
				} else if (tempHidden && !"com.android.vending".equals(foregroundPackage)) {
				overlayWindow.show();
				tempHidden = false;
			}
			
		}
	};
	
}