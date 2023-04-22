package com.thf.AppSwitcher.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.thf.AppSwitcher.utils.AppData;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;
import com.thf.AppSwitcher.utils.UsageStatsUtil;
import com.thf.AppSwitcher.utils.Utils;
import java.util.Iterator;
import java.util.List;

public class RunMediaApp implements Runnable {
  private static final String TAG = "AppSwitcherService";
  private UsageStatsUtil usageStatsUtil;
  private Context context;
  private SharedPreferencesHelper sharedPreferencesHelper;

  public RunMediaApp(Context context, UsageStatsUtil usageStatsUtil) {
    this.context = context;
    this.sharedPreferencesHelper = new SharedPreferencesHelper(context);
    this.usageStatsUtil = usageStatsUtil;
  }

  @Override
  public void run() {
    int delay = sharedPreferencesHelper.getInteger("runMediaAppDelay");
    try {
      Thread.sleep(delay);
    } catch (InterruptedException e) {
      Log.i(TAG, "start of media app interrupted");
      return;
    }

    String foregroundApp = usageStatsUtil.getCurrentActivity();
    List<AppData> selectedList = sharedPreferencesHelper.loadList("selected");

    if (SharedPreferencesHelper.appDataListContainsKey(selectedList, foregroundApp, null)
        || SharedPreferencesHelper.appDataListContainsKey(
            selectedList, foregroundApp.split("/")[0], null)) {
      Log.d(TAG, "navi or media app (" + foregroundApp + ") already in foreground");
      return;
    }

    List<AppData> recentsAppList = sharedPreferencesHelper.getRecentsList();

    if (!recentsAppList.isEmpty()) {
      Iterator<AppData> i = recentsAppList.iterator();
      while (i.hasNext()) {
        AppData s = i.next();
        if (SharedPreferencesHelper.appDataListContainsKey(selectedList, s.getKey(), "media")) {
          Log.d(TAG, "autostart of " + s.getName());
          ComponentName name = new ComponentName(s.getPackageName(), s.getActivityName());
          Intent intentStartMedia = new Intent(Intent.ACTION_MAIN);
          //intentStartMedia.addCategory(Intent.CATEGORY_LAUNCHER);
          intentStartMedia.setFlags(
              Intent.FLAG_ACTIVITY_NEW_TASK
                  | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                  | Intent.FLAG_ACTIVITY_NO_ANIMATION);
          intentStartMedia.setComponent(name);
          context.startActivity(intentStartMedia);

          if (sharedPreferencesHelper.getBoolean("runMediaAppTwice")) {
            // go to home screen, wait a second and start media app again
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            context.startActivity(startMain);
            context.startActivity(intentStartMedia);
          }
          break;
        }
      }
    }
  }
}
