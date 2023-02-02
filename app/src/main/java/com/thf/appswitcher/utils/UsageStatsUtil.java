package com.thf.AppSwitcher.utils;

import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UsageStatsUtil {
	private static final String TAG = "AppSwitcherService";
	//private ReadLogs asActivity;
	//private Handler handler;
	private Boolean stop = false;
	private Context context;
	private UsageStatsCallbacks listener;

	//Hashtable<String, AppData> relevantNavis;
	List<AppData> selectedList = new ArrayList<>();
	//private Handler execHandler = new Handler(Looper.getMainLooper());

	public UsageStatsUtil(Context context, UsageStatsCallbacks listener) {
		this.context = context;
		this.listener = listener;
		//this.handler = handler;
	}

	public interface UsageStatsCallbacks {
		public void onForegroundApp(String foregroundPackage);
	}

	private Thread thread;

	public void stopProgress() {
		this.stop = true;
		Log.i(TAG, "Stopped UsageStats reader");
	}

	public String getCurrentActivity() {
		String foregroundActivity = "";
		UsageStatsManager mUsageStatsManager = (UsageStatsManager) context
				.getSystemService(Service.USAGE_STATS_SERVICE);
		long time = System.currentTimeMillis();

		UsageEvents usageEvents = mUsageStatsManager.queryEvents(time - 1000 * 3600, time + 1000);
		UsageEvents.Event event = new UsageEvents.Event();
		while (usageEvents.hasNextEvent()) {
			usageEvents.getNextEvent(event);
			if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED
					&& !"com.thf.AppSwitcher".equals(event.getPackageName())
					&& !"com.thf.AppSwitcherStarter".equals(event.getPackageName())
					&& !"com.thf.FlowStarter".equals(event.getPackageName())
					&& !("com.ts.MainUI".equals(event.getPackageName())
							&& "com.ts.main.navi.NaviMainActivity".equals(event.getClassName()))) {
				foregroundActivity = event.getPackageName() + "/" + event.getClassName();
				//event.getTimeStamp();
			}
		}
		return foregroundActivity;
	}

	public void startProgress() {
		// do something long
		Runnable runnable = new Runnable() {
			@Override
			public void run() {

				selectedList = SharedPreferencesHelper.loadList(context, "selected");

				while (!stop) {

					List<UsageEvents.Event> readUsageStatList = readUsageStats(context);

					//Log.i(TAG, Integer.toString(readUsageStatList.size()));

					Iterator<UsageEvents.Event> i = readUsageStatList.iterator();
					UsageEvents.Event s = null;
					while (i.hasNext()) {
						s = i.next(); // must be called before you can call i.remove()
						Boolean collected = false;

						String key = s.getPackageName() + "/" + s.getClassName();
						AppData app = Utils.getAppDataFromListByKey(selectedList, key);

						if (app != null) {
							SharedPreferencesHelper.putIntoRecentsList(context, app);
							collected = true;
						} else {
							key = s.getPackageName();
							app = Utils.getAppDataFromListByKey(selectedList, key);
							if (app != null) {
								SharedPreferencesHelper.putIntoRecentsList(context, app);
								collected = true;
							}
						}

						if (!collected) {
							//Log.d(TAG, "Not collected: " + key);
						} else {
							Log.i(TAG, "Collected: " + key);
						}
					}

					if (listener != null && s != null) {
						String packageName = s.getPackageName();
						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								listener.onForegroundApp(packageName);
							}
						});
					}
					 
					Utils.wait(500);
				}
			}
		};
		thread = new Thread(runnable); //, Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
	}

	private static long readFromTimestamp = 0;

	private ArrayList<UsageEvents.Event> readUsageStats(Context context) {

		ArrayList<UsageEvents.Event> mEventList = new ArrayList<>();
		UsageStatsManager mUsageStatsManager = (UsageStatsManager) context
				.getSystemService(Service.USAGE_STATS_SERVICE);

		long time = System.currentTimeMillis();
		//mEventList.clear();
		if (readFromTimestamp == 0) {
			Log.i(TAG, "read usage stat of last h");
			readFromTimestamp = time - 1000 * 3600;
		}

		UsageEvents usageEvents = mUsageStatsManager.queryEvents(readFromTimestamp, time);

		while (usageEvents.hasNextEvent()) {
			UsageEvents.Event event = new UsageEvents.Event();
			usageEvents.getNextEvent(event);
			if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
				mEventList.add(event);
				readFromTimestamp = event.getTimeStamp() + 1;
			}
		}
		return mEventList;
	}

}