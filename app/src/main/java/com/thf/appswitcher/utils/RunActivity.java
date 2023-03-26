package com.thf.AppSwitcher.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.thf.AppSwitcher.utils.AppData;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;
import com.thf.AppSwitcher.utils.Utils;
import java.util.Iterator;
import java.util.List;

public class RunActivity implements Runnable {
  private static final String TAG = "AppSwitcherService";

  private String packageName;
  private String activity;
  private Context context;

  public RunActivity(Context context, String packageName, String activity) {
    this.context = context;
    this.packageName = packageName;
    this.activity = activity;
  }

  @Override
  public void run() {
    if (!Utils.isPackageInstalled(context, packageName)) {
      Log.d(TAG, "package " + packageName + " is not installed");
      return;
    }

    Log.d(TAG, "run activity " + packageName + "/" + activity);
    ComponentName name = new ComponentName(packageName, activity);
    Intent intent = new Intent(Intent.ACTION_MAIN);
    // intent.addCategory(Intent.CATEGORY_LAUNCHER);
    intent.setFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            | Intent.FLAG_ACTIVITY_NO_ANIMATION);
    intent.setComponent(name);
    context.startActivity(intent);

    //switch to home and start activity again    
    Intent startMain = new Intent(Intent.ACTION_MAIN);
    startMain.addCategory(Intent.CATEGORY_HOME);
    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
    context.startActivity(startMain);
    context.startActivity(intent);
  }
}
