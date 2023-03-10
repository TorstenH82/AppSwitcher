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
        if (!Utils.isPackageInstalled(context, packageName)){
            Log.d(TAG, "Package " + packageName + " is not installed");
            return;
        }
        
        
        Log.d(TAG, "Run activity " + packageName + "/" + activity);
        ComponentName name = new ComponentName(packageName, activity);
        Intent intentStartMedia = new Intent(Intent.ACTION_MAIN);
        intentStartMedia.addCategory(Intent.CATEGORY_LAUNCHER);
        intentStartMedia.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intentStartMedia.setComponent(name);
        context.startActivity(intentStartMedia);
    }
}
