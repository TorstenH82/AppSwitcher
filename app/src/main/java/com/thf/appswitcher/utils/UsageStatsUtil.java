package com.thf.AppSwitcher.utils;

import android.app.ActivityManager;
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
  private Context context;
  private UsageStatsCallbacks listener;
  private static String foregroundActivity;
  private Thread thread;

  List<AppData> selectedList = new ArrayList<>();

  public UsageStatsUtil(Context context, UsageStatsCallbacks listener) {
    this.context = context;
    this.listener = listener;
  }

  public interface UsageStatsCallbacks {
    public void onForegroundApp(String foregroundPackage);
  }

  public void stopProgress() {
    if (thread != null && thread.isAlive()) {
      thread.interrupt();
      Log.i(TAG, "Stopped UsageStats reader");
      thread = null;
      foregroundActivity = null;
    }
  }

  private boolean ignoreActivity(String packageName, String activity) {
    if (packageName == null
        || context.getPackageName().equals(packageName)
        || "com.thf.AppSwitcherStarter".equals(packageName)
        || "com.thf.FlowStarter".equals(packageName)
        || ("com.ts.MainUI".equals(packageName)
            && "com.ts.main.navi.NaviMainActivity".equals(activity))) {
      return true;
    }
    return false;
  }

  public String getCurrentActivity() {
    if (UsageStatsUtil.foregroundActivity != null) {
      return UsageStatsUtil.foregroundActivity;
    }

    String foregroundActivity = "";
    UsageStatsManager mUsageStatsManager =
        (UsageStatsManager) context.getSystemService(Service.USAGE_STATS_SERVICE);
    long time = System.currentTimeMillis();

    UsageEvents usageEvents = mUsageStatsManager.queryEvents(time - 1000 * 3600, time + 1000);
    UsageEvents.Event event = new UsageEvents.Event();
    while (usageEvents.hasNextEvent()) {
      usageEvents.getNextEvent(event);
      if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED
          && !ignoreActivity(event.getPackageName(), event.getClassName())) {
        foregroundActivity = event.getPackageName() + "/" + event.getClassName();
      }
    }
    return foregroundActivity;
  }

  String lastCollectedKey = "";

  public void startProgress() {
    // do something long
    Runnable runnable =
        new Runnable() {
          @Override
          public void run() {

            selectedList = SharedPreferencesHelper.loadList(context, "selected");

            while (true) {

              List<UsageEvents.Event> readUsageStatList = readUsageStats(context);

              Iterator<UsageEvents.Event> i = readUsageStatList.iterator();
              UsageEvents.Event s = null;

              while (i.hasNext()) {
                s = i.next(); // must be called before you can call i.remove()
                Boolean collected = false;

                String key = s.getPackageName() + "/" + s.getClassName();
                AppData app = Utils.getAppDataFromListByKey(selectedList, key);

                if (app == null) {
                  key = s.getPackageName();
                  app = Utils.getAppDataFromListByKey(selectedList, key);
                }

                if (app != null && !key.equals(lastCollectedKey)) {
                  /*
                  if ("app".equals(app.getCategory())) {
                    app.setActivityName(s.getClassName());
                    Log.i(TAG, "Collected with key: " + app.getKey());
                  }
                  */
                  SharedPreferencesHelper.putIntoRecentsList(context, app);
                  Log.i(TAG, "Collected: " + key);
                  lastCollectedKey = key;
                }
              }

              if (s != null) {
                String packageName = s.getPackageName();
                String activity = s.getClassName();

                if (!ignoreActivity(packageName, activity)) {
                  if (foregroundActivity == null
                      || !foregroundActivity.equals(packageName + "/" + activity)) {
                    foregroundActivity = packageName + "/" + activity;
                    if (listener != null) {
                      new Handler(Looper.getMainLooper())
                          .post(
                              new Runnable() {
                                @Override
                                public void run() {
                                  listener.onForegroundApp(foregroundActivity);
                                }
                              });
                    }
                  }
                }
              }
              try {
                Thread.sleep(500);
              } catch (InterruptedException ex) {
                return;
              }
            }
          }
        };
    if (thread == null || !thread.isAlive()) {
      thread = new Thread(runnable); // , Process.THREAD_PRIORITY_BACKGROUND);
      thread.start();
      Log.i(TAG, "Started UsageStats reader");
    }
  }

  private static long readFromTimestamp = 0;

  private ArrayList<UsageEvents.Event> readUsageStats(Context context) {

    ArrayList<UsageEvents.Event> mEventList = new ArrayList<>();
    UsageStatsManager mUsageStatsManager =
        (UsageStatsManager) context.getSystemService(Service.USAGE_STATS_SERVICE);

    long time = System.currentTimeMillis();
    // mEventList.clear();
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
