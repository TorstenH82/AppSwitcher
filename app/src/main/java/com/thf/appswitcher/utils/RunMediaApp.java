package com.thf.appswitcher.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.thf.AppSwitcher.utils.AppData;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;
import com.thf.AppSwitcher.utils.Utils;
import java.util.Iterator;
import java.util.List;

public class RunMediaApp implements Runnable {
    private static final String TAG = "AppSwitcherService";
    private String foregroundApp;
    private Context context;

    public RunMediaApp(Context context, String foregroundApp) {
        this.context = context;
        this.foregroundApp = foregroundApp;
    }

    @Override
    public void run() {
        int delay = SharedPreferencesHelper.getInteger(context, "runMediaAppDelay");
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            return;
        }

        // String foregroundApp = usageStatsUtil.getCurrentActivity();
        List<AppData> selectedList = SharedPreferencesHelper.loadList(context, "selected");

        if (Utils.listContainsKey(selectedList, foregroundApp, null)
                || Utils.listContainsKey(selectedList, foregroundApp.split("/")[0], null)) {
            Log.d(TAG, "navi or media app (" + foregroundApp + ") already in foreground");
            return;
        }

        List<AppData> recentsAppList = SharedPreferencesHelper.getRecentsList(context);

        if (!recentsAppList.isEmpty()) {
            Iterator<AppData> i = recentsAppList.iterator();
            while (i.hasNext()) {
                AppData s = i.next();
                if (Utils.listContainsKey(selectedList, s.getKey(), "media")) {
                    Log.d(TAG, "autostart of " + s.getName());
                    ComponentName name = new ComponentName(s.getPackageName(), s.getActivityName());
                    Intent intentStartMedia = new Intent(Intent.ACTION_MAIN);
                    intentStartMedia.addCategory(Intent.CATEGORY_LAUNCHER);
                    intentStartMedia.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    intentStartMedia.setComponent(name);
                    context.startActivity(intentStartMedia);

                    if (SharedPreferencesHelper.getBoolean(context, "runMediaAppTwice")) {
                        // go to home screen, wait a second and start media app again
                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                        startMain.addCategory(Intent.CATEGORY_HOME);
                        startMain.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        context.startActivity(startMain);
                        context.startActivity(intentStartMedia);
                    }
                    break;
                }
            }
        }
    }
}
