package com.thf.AppSwitcher.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.thf.AppSwitcher.utils.AppData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

public class ActivityUtil {
  private static final String TAG = "AppSwitcherActivityUtil";

  private Context context;
  private Handler handler;
  private String category;
  private Boolean stop = false;
  private volatile List<AppData> appDataList = new ArrayList<>();
  private Handler execHandler = new Handler(Looper.getMainLooper());

  public ActivityUtil(Context context, Handler handler, String category) {
    this.context = context;
    this.handler = handler;
    this.category = category;
  }

  public void stopProgress() {
    this.stop = true;
  }

  public void startProgress() {
    // do something long
    Runnable runnable =
        new Runnable() {
          @Override
          public void run() {

            Boolean run = true;
            String cat = "app";

            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent, 0);
            Collections.sort(
                resolveInfoList, new ResolveInfo.DisplayNameComparator(packageManager));

            for (ResolveInfo resolveInfo : resolveInfoList) {

              AppData app =
                  new AppData(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
              try {
                Resources res =
                    packageManager.getResourcesForApplication(
                        resolveInfo.activityInfo.applicationInfo);
                int labelRes = resolveInfo.labelRes;
                if (labelRes == 0) labelRes = resolveInfo.activityInfo.labelRes;
                app.setActivityDescription(res.getString(labelRes));
                // PackageManager.NameNotFoundException
              } catch (Exception ignore) {
              }
              app.setName(resolveInfo.loadLabel(packageManager).toString());
              app.setCategory(cat);
              appDataList.add(app);

              if ("activity".equals(category)) {

                try {
                  ActivityInfo[] activityInfos =
                      packageManager.getPackageInfo(
                              resolveInfo.activityInfo.packageName, PackageManager.GET_ACTIVITIES)
                          .activities;

                  for (ActivityInfo activityInfo : activityInfos) {
                    if (!activityInfo.exported) continue;
                    AppData act = new AppData(app);
                    try {
                      Resources res =
                          packageManager.getResourcesForApplication(activityInfo.applicationInfo);
                      if (activityInfo.labelRes != 0)
                        act.setActivityDescription(res.getString(activityInfo.labelRes));
                    } catch (Exception ignore) {
                    }
                    act.setActivityName(activityInfo.name);
                    act.setCategory("activity");

                    appDataList.add(act);
                  }

                } catch (Exception ignore) {
                }
              }
            }

            /*
                        Hashtable<String, String> htCollected = new Hashtable<String, String>();

                        while (!stop) {

                          // Intent intent = new Intent(Intent.ACTION_MAIN, null);
                          Intent intent = new Intent(Intent.ACTION_MAIN, null);
                          if (cat != "activity") {
                            intent.addCategory(Intent.CATEGORY_LAUNCHER);
                          }
                          PackageManager packageManager = context.getPackageManager();

                          List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent, 0);
                          Collections.sort(
                              resolveInfoList, new ResolveInfo.DisplayNameComparator(packageManager));

                          for (ResolveInfo resolveInfo : resolveInfoList) {

                            if (cat != "activity"
                                || htCollected.containsKey(resolveInfo.activityInfo.packageName)) {

                              if (!resolveInfo.activityInfo.exported) {
                                continue;
                              }

                              AppData app = new AppData();

                              app = new AppData();
                              try {
                                Resources res =
                                    packageManager.getResourcesForApplication(
                                        resolveInfo.activityInfo.applicationInfo);
                                int labelRes = resolveInfo.labelRes;
                                if (labelRes == 0) labelRes = resolveInfo.activityInfo.labelRes;
                                app.setActivityDescription(res.getString(labelRes));
                                // PackageManager.NameNotFoundException
                              } catch (Exception ignore) {
                              }
                              // app.setName(resolveInfo.loadLabel(packageManager).toString());
                              app.setPackageName(resolveInfo.activityInfo.packageName);
                              // app.setIcon(resolveInfo.activityInfo.loadIcon(packageManager));
                              app.setActivityName(resolveInfo.activityInfo.name);
                              app.setCategory(cat);
                              if (cat != "activity") {
                                app.setName(resolveInfo.loadLabel(packageManager).toString());
                                htCollected.put(
                                    resolveInfo.activityInfo.packageName,
                                    resolveInfo.loadLabel(packageManager).toString());
                              } else {
                                app.setName(htCollected.get(resolveInfo.activityInfo.packageName));
                              }
                              appDataList.add(app);
                            }
                          }

                          if (category == "activity" && cat != "activity") cat = "activity";
                          else {
                            stop = true;
                          }
                        }
            */
            Collections.sort(
                appDataList,
                new Comparator<AppData>() {
                  public int compare(AppData o1, AppData o2) {
                    // compare two instance of `Score` and return `int` as
                    // result.
                    int cmp = o1.getName().compareTo(o2.getName());
                    if (cmp == 0) {
                      cmp = o1.getPackageName().compareTo(o2.getPackageName());
                    }
                    if (cmp == 0) {
                      cmp = o2.getCategory().compareTo(o1.getCategory());
                    }
                    return cmp;
                  }
                });

            execHandler.post(
                new Runnable() {
                  @Override
                  public void run() {
                    Message completeMessage = handler.obtainMessage(555, "CONNECTED");
                    completeMessage.sendToTarget();
                  }
                });
          }
        };
    new Thread(runnable).start();
  }

  public List<AppData> getValue() {
    return appDataList;
  }

  public AppDataIcon getLauncher() {
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_HOME);
    PackageManager packageManager = context.getPackageManager();
    ResolveInfo resolveInfo = packageManager.resolveActivity(intent, 0);
    // return resolveInfo.activityInfo.packageName + "/" + resolveInfo.activityInfo.name;

    AppData app = new AppData();

    try {
      Resources res =
          packageManager.getResourcesForApplication(resolveInfo.activityInfo.applicationInfo);
      int labelRes = resolveInfo.labelRes;
      if (labelRes == 0) labelRes = resolveInfo.activityInfo.labelRes;
      app.setActivityDescription(res.getString(labelRes));
      // PackageManager.NameNotFoundException
    } catch (Exception ignore) {
    }

    app.setPackageName(resolveInfo.activityInfo.packageName);
    app.setActivityName(resolveInfo.activityInfo.name);
    app.setCategory("launcher");
    app.setName(resolveInfo.loadLabel(packageManager).toString());
        AppDataIcon appIcon = new AppDataIcon(app);
    Drawable icon = app.getIcon(context);
    if (icon != null) {
      icon.mutate();
      appIcon.setIcon(icon);
    }
    return appIcon;
  }
}
