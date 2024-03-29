package com.thf.AppSwitcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.thf.AppSwitcher.utils.AppData;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;
import com.thf.AppSwitcher.utils.SwitchAppsAdapter;
import com.thf.AppSwitcher.utils.Utils;
import com.thf.AppSwitcher.utils.AppDataIcon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class SwitchActivity extends Activity {
  private static final String TAG = "AppSwitcherService";
  private AppSwitcherApp mApplication;

  private static SwitchAppsAdapter adapter;
  private static Context context;
  private SharedPreferencesHelper sharedPreferencesHelper;
  private static int dialogDelay;

  private static SwitchDialog switchDialog;

  public Handler handler = new Handler(Looper.getMainLooper());
  // private boolean disableNaviMainActivity = false;
  private boolean showClock = true;
  private boolean showEqualizer = false;
  private float brightness;
  private static boolean grayscale = true;
  private static String foregroundApp;

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mApplication = (AppSwitcherApp) getApplicationContext();
    mApplication.setSwitchActivityRunning(true);
    context = getApplicationContext();

    sharedPreferencesHelper = new SharedPreferencesHelper(context);

    Log.d(TAG, "create SwitchActivity");

    dialogDelay = sharedPreferencesHelper.getInteger("dialogDelay");
    Log.i(TAG, "Dialog delay: " + Integer.toString(dialogDelay));
    // disableNaviMainActivity = sharedPreferencesHelper.getBoolean("disableNaviStart");
    showClock = sharedPreferencesHelper.getBoolean("showClock");
    showEqualizer = sharedPreferencesHelper.getBoolean("showEqualizer");
    brightness = ((float) sharedPreferencesHelper.getInteger("itemsBrightness")) / 100;
    grayscale = sharedPreferencesHelper.getBoolean("grayscaleIcons");

    new Thread(runnablePrepareData).start();

    switchDialog =
        new SwitchDialog(
            SwitchActivity.this,
            switchDialogListener,
            dialogDelay,
            brightness,
            grayscale,
            showClock,
            showEqualizer);
  }

  private SwitchDialog.SwitchDialogCallbacks switchDialogListener =
      new SwitchDialog.SwitchDialogCallbacks() {
        @Override
        public void onResult(AppData app) {
          if (app != null) {
            Intent intent = new Intent();
            // intent = new Intent(Intent.ACTION_MAIN);
            if ("launcher".equals(app.getCategory())) {
              Log.d(TAG, "start home");
              intent = new Intent(Intent.ACTION_MAIN);
              intent.addCategory(Intent.CATEGORY_HOME);
              intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              startActivity(intent);
            } else if (!"cancel".equals(app.getCategory())) {
              ComponentName name = new ComponentName(app.getPackageName(), app.getActivityName());
              // intent.addCategory(Intent.CATEGORY_LAUNCHER);
              intent.setFlags(
                  Intent.FLAG_ACTIVITY_NEW_TASK
                      | Intent.FLAG_ACTIVITY_SINGLE_TOP
                      | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                      | Intent.FLAG_ACTIVITY_NO_ANIMATION);
              intent.setComponent(name);
              startActivity(intent);
            }
          }
          finish();
        }
      };

  @Override
  protected void onPause() {
    super.onPause();
    Log.d(TAG, "pause SwitchActivity");
    // do not unregister here because NaviStartActivity may become fg app
    // LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    // if (switchDialog != null) switchDialog.action(SwitchDialog.Action.CLOSE);
    // if (disableNaviMainActivity) Utils.enableDisableNaviMainActivity(context, false,
    // utilCallbacks);
  }

  private Utils.UtilCallbacks utilCallbacks =
      new Utils.UtilCallbacks() {
        @Override
        public void onException(Throwable e) {
          Toast.makeText(context, "error" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
      };

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "resume SwitchActivity");
    // unregister here because we cant do it in onPause
    // LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);

    //  LocalBroadcastManager.getInstance(this)
    //      .registerReceiver(messageReceiver, new IntentFilter("switch-message"));

    if (!switchDialog.isShowing()) {
      LocalBroadcastManager.getInstance(this)
          .registerReceiver(messageReceiver, new IntentFilter("switch-message"));
      switchDialog.show();
    }

    // if (disableNaviMainActivity) Utils.enableDisableNaviMainActivity(context, true,
    // utilCallbacks);
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();

    Log.d(TAG, "stop SwitchActivity");
    if (switchDialog != null) switchDialog.action(SwitchDialog.Action.CLOSE);
    LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);

    mApplication.setSwitchActivityRunning(false);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    Log.d(TAG, "destroy SwitchActivity");
    // LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    // if (switchDialog != null) switchDialog.action(SwitchDialog.Action.CLOSE);
    // if (disableNaviMainActivity) Utils.enableDisableNaviMainActivity(context, false,
    // utilCallbacks);
  }

  private BroadcastReceiver messageReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          // Extract data included in the Intent
          Log.i(TAG, "broadcast message received");
          // int yourInteger = intent.getIntExtra("my-integer", -1); // -1 is going to be
          // used as the default value
          boolean close = intent.getBooleanExtra("close", false);
          boolean onPress = intent.getBooleanExtra("onPress", false);

          if (close) {
            switchDialog.action(SwitchDialog.Action.CLOSE);
            finish();
            return;
          }

          if (onPress) {
            // nothing to do because bomb already disarmed
            // next bc will come after user releases swc button
            switchDialog.action(SwitchDialog.Action.NOTHING);
            return;
          }

          Log.d(TAG, "jump to next entry");
          switchDialog.action(SwitchDialog.Action.NEXT);
        }
      };

  private Runnable runnablePrepareData =
      new Runnable() {

        // private List<AppData> newAppList = new ArrayList<AppData>();
        private List<AppDataIcon> newAppList = new ArrayList<AppDataIcon>();

        @Override
        public void run() {

          List<AppDataIcon> selectedList = sharedPreferencesHelper.getSelected(false);

          boolean launcherIsInForeground = false;
          boolean addLauncher = sharedPreferencesHelper.getBoolean("addLauncher");

          if (sharedPreferencesHelper.getBoolean("smartList")) {
            String foregroundApp = "";
            try {
              Intent intent = getIntent();
              foregroundApp = intent.getStringExtra("foregroundApp");
              Log.i(TAG, "foreground app from intent: " + foregroundApp);

            } catch (Exception ex) {
              Log.e(TAG, ex.getMessage());
            }

            if (foregroundApp == null) {
              foregroundApp = "-/-";
            }

            List<AppData> recentsAppList = sharedPreferencesHelper.getRecentsList();

            boolean mediaAppInForeground = false;
            AppData naviInForeground = new AppData();
            boolean naviIsInForeground = false;

            if (SharedPreferencesHelper.appDataListIconContainsKey(
                selectedList, foregroundApp, "media")) {
              mediaAppInForeground = true;
            } else if (SharedPreferencesHelper.appDataListIconContainsKey(
                selectedList, foregroundApp.split("/")[0], "media")) {
              foregroundApp = foregroundApp.split("/")[0];
              mediaAppInForeground = true;
            } else if (SharedPreferencesHelper.appDataListIconContainsKey(
                selectedList, foregroundApp.split("/")[0], "navi")) {
              foregroundApp = foregroundApp.split("/")[0];
              naviIsInForeground = true;
            } else if (foregroundApp.split("/")[0].equals(
                mApplication.getLauncher().getPackageName())) {
              launcherIsInForeground = true;
            }

            Iterator<AppData> i = recentsAppList.iterator();
            int sort = -1;
            boolean prioPosSet = false;

            while (i.hasNext()) {
              AppData r = i.next(); // must be called before you can call i.remove()

              Log.d(TAG, "recent " + r.getKey());

              // skip current app and apps not selected by user anymore
              if (TextUtils.equals(r.getKey(), foregroundApp)
                  || !SharedPreferencesHelper.appDataListIconContainsKey(
                      selectedList, r.getKey(), null)) continue;

              AppDataIcon app = new AppDataIcon(r);
              int idx = selectedList.indexOf(app);
              if (idx == -1) continue;
              app = selectedList.get(idx);

              Boolean posSet = false;

              // special handling for recent navis
              if (SharedPreferencesHelper.appDataListIconContainsKey(
                  selectedList, r.getKey(), "navi")) {

                // navi is not in front, media app in front
                // -> offer recent navi on 1st pos
                if (!naviIsInForeground && mediaAppInForeground && !prioPosSet) {
                  app.setSort(-1);
                  prioPosSet = true;
                  posSet = true;

                  // navi is not in front, media app not in front
                  // --> offer recent navi on 2nd pos
                } else if (!naviIsInForeground && !mediaAppInForeground && !prioPosSet) {
                  app.setSort(1);
                  prioPosSet = true;
                  posSet = true;

                  // navi is in front and previous app is also a navi
                  // --> offer recent navi on 2nd pos
                } else if (sort == -1 && naviIsInForeground) {
                  app.setSort(1); // not on 1st position

                  posSet = true;
                }
              }

              if (!posSet) {
                sort++;
                // positions 1 is reserved
                if (sort == 1) sort = 2;

                app.setSort(sort);
              }

              newAppList.add(0, app);
              selectedList.remove(r);
            }

            // add apps not removed from selected list by looping on recent apps
            for (AppDataIcon app : selectedList) {
              if (!app.getKey().equals(foregroundApp)) {
                app.setSort(9999);
                newAppList.add(app);
              }
            }

          } else {
            newAppList = selectedList;
          }
          Collections.sort(
              newAppList,
              new Comparator<AppDataIcon>() {
                public int compare(AppDataIcon o1, AppDataIcon o2) {
                  // compare two instance of `AppData` and return `int` as
                  // result.
                  int cmp = Integer.compare(o1.getSort(), o2.getSort());
                  if (cmp == 0) {
                    cmp = o1.getDescription().compareTo(o2.getDescription());
                  }
                  return cmp;
                }
              });

          if (addLauncher) {
            AppDataIcon app = new AppDataIcon();
            if (sharedPreferencesHelper.getBoolean("genericHome")) {
              app.setIcon(getDrawable(R.drawable.home));
              app.setName("Home");
              app.setCategory("launcher");
            } else {
              app = mApplication.getLauncher();
            }

            // launcher always on 2nd position. Only on 1st if there is no other entry
            if (newAppList.size() > 0) {
              // newAppList.add(1, mApplication.getLauncher());
              newAppList.add(1, app);
            } else {
              // newAppList.add(mApplication.getLauncher());
              newAppList.add(app);
            }
          }

          if (sharedPreferencesHelper.getBoolean("addCancel")) {
            AppDataIcon app = new AppDataIcon();
            app.setIcon(getDrawable(R.drawable.cancel));
            app.setName("Back");
            app.setCategory("cancel");
            newAppList.add(0, app);
          }
          // add the icons here
          /*
          for (AppData app : newAppList) {
            Drawable icon = app.getIcon(context);
            if (icon == null) continue;
            icon.mutate();
            AppDataIcon appIcon = new AppDataIcon(app);
            appIcon.setIcon(icon);
            newAppListIcon.add(appIcon);
          }
          */

          handler.post(
              new Runnable() {
                @Override
                public void run() {
                  switchDialog.setItems(newAppList);
                }
              });
        }
      };
}
