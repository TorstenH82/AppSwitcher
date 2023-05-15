package com.thf.AppSwitcher.utils;

import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.media.metrics.Event;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class UsageStatsUtil {
  private static final String TAG = "AppSwitcherService";
  private Context context;
  private SharedPreferencesHelper sharedPreferencesHelper;
  private UsageStatsCallbacks listener;
  private static String foregroundActivity;
  private Thread thread;

  List<AppDataIcon> selectedList = new ArrayList<>();

  public UsageStatsUtil(Context context, UsageStatsCallbacks listener) {
    this.context = context;
    this.sharedPreferencesHelper = new SharedPreferencesHelper(context);
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
        || (context.getPackageName().equals(packageName)
            && "com.thf.AppSwitcher.SwitchActivity".equals(activity))
        || "com.thf.AppSwitcherStarter".equals(packageName)
        || "com.google.android.permissioncontroller".equals(packageName)
        || "com.thf.FlowStarter".equals(packageName)
        || "android".equals(packageName)
        || ("com.ts.MainUI".equals(packageName)
            && "com.ts.main.navi.NaviMainActivity".equals(activity))) {
      return true;
    }
    return false;
  }

  private static class RecentUseComparator implements Comparator<UsageStats> {
    @Override
    public int compare(UsageStats lhs, UsageStats rhs) {
      return (lhs.getLastTimeUsed() > rhs.getLastTimeUsed())
          ? -1
          : (lhs.getLastTimeUsed() == rhs.getLastTimeUsed()) ? 0 : 1;
    }
  }

  public String getCurrentActivity() {
    return foregroundActivity != null ? foregroundActivity : "";
  }

  String lastCollectedKey = "";

  public void startProgress() {
    // do something long
    Runnable runnable =
        new Runnable() {
          private String fgActivity = null;

          @Override
          public void run() {

            selectedList = sharedPreferencesHelper.getSelected(false);

            while (true) {

              List<UsageEvents.Event> readUsageStatList = readUsageStats(context);
              Iterator<UsageEvents.Event> i = readUsageStatList.iterator();
              UsageEvents.Event s;
              fgActivity = null;

              while (i.hasNext()) {
                s = i.next(); // must be called before you can call i.remove()
                Boolean collected = false;

                String key = s.getPackageName() + "/" + s.getClassName();

                AppDataIcon app =
                    SharedPreferencesHelper.getAppDataFromListByKey(selectedList, key);

                if (app == null) {
                  key = s.getPackageName();
                  app = SharedPreferencesHelper.getAppDataFromListByKey(selectedList, key);
                }

                // collect recent apps
                if (app != null && !key.equals(lastCollectedKey)) {
                  sharedPreferencesHelper.putIntoRecentsList(new AppData(app));
                  Log.i(TAG, "Collected: " + key);
                  lastCollectedKey = key;
                }

                // current foreground app
                if (!ignoreActivity(s.getPackageName(), s.getClassName())) {
                  fgActivity = s.getPackageName() + "/" + s.getClassName();
                }
              } // end loop on usage data

              if (fgActivity != null && !fgActivity.equals(foregroundActivity)) {
                foregroundActivity = fgActivity;
                Log.d(TAG, "foreground app is " + foregroundActivity);
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

              /*
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
                  Thread.sleep(100);
                } catch (InterruptedException ex) {
                  return;
                }
              */
              if (Thread.interrupted()) {
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
  // private static long id = 0;
  private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  private ArrayList<UsageEvents.Event> readUsageStats(Context context) {

    ArrayList<UsageEvents.Event> mEventList = new ArrayList<>();
    UsageStatsManager mUsageStatsManager =
        (UsageStatsManager) context.getSystemService(Service.USAGE_STATS_SERVICE);

    long endTime = System.currentTimeMillis();
    // mEventList.clear();
    if (readFromTimestamp == 0) {
      Log.i(TAG, "read usage stat of last h");
      readFromTimestamp = endTime - 1000 * 3600;
    }

    // The inclusive beginning of the range of stats
    // The exclusive end of the range of stats

    UsageEvents usageEvents = mUsageStatsManager.queryEvents(readFromTimestamp, endTime);
    UsageEvents.Event event = new UsageEvents.Event();
    while (usageEvents.hasNextEvent()) {

      usageEvents.getNextEvent(event);

      if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
        mEventList.add(event);
        /*
        Log.d(
            TAG,
            id
                + " - resumed "
                + event.getPackageName()
                + "/"
                + event.getClassName()
                + " - "
                + formatter.format(new Date(event.getTimeStamp())));
        */
        readFromTimestamp = event.getTimeStamp() + 1;
      }
    }
    // readFromTimestamp = endTime + 1;
    // id++;
    return mEventList;
  }
}
